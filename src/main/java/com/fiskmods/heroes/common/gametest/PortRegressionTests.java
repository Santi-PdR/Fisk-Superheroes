package com.fiskmods.heroes.common.gametest;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.hero.modifier.ModifierControlledFlight;
import com.fiskmods.heroes.common.network.PacketInput;
import com.fiskmods.heroes.common.util.TextureColorMath;

import io.netty.buffer.Unpooled;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(FiskHeroes.MODID)
@PrefixGameTestTemplate(false)
public final class PortRegressionTests
{
    private PortRegressionTests() {}

    @GameTest(template = "test_empty")
    public static void textureMasksAndHue(GameTestHelper helper)
    {
        int red = 0xFF0000FF; // NativeImage stores ABGR, not ARGB.
        int translucentBlue = 0x4000FF00;
        if (TextureColorMath.xor(red, 0x00112233) != red
                || TextureColorMath.xor(red, translucentBlue) != 0
                || TextureColorMath.or(red, 0x00112233) != red
                || TextureColorMath.or(red, translucentBlue) != translucentBlue
                || TextureColorMath.hue(red, 120.0F) != 0xFF00FF00
                || TextureColorMath.hue(red, -120.0F) != 0xFFFF0000
                || (TextureColorMath.hue(0x800000FF, 60.0F) >>> 24) != 0x80)
        {
            helper.fail("Texture mask/overlay/hue pixel operations differ from the original");
        }
        else helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void flightIsSteerableAndBounded(GameTestHelper helper)
    {
        Vec3 look = new Vec3(0, 0, 1);
        Vec3 idle = ModifierControlledFlight.controlledMotion(Vec3.ZERO, look, 0, 0, 0, false, false, false, 0.1F, 0.2F);
        Vec3 forward = ModifierControlledFlight.controlledMotion(Vec3.ZERO, look, 0, 1, 0, false, false, false, 0.1F, 0.2F);
        Vec3 left = ModifierControlledFlight.controlledMotion(Vec3.ZERO, look, 0, 0, 1, false, false, false, 0.1F, 0.2F);
        if (idle.horizontalDistanceSqr() > 1.0E-8D || forward.z <= 0 || left.x <= 0)
        {
            helper.fail("Idle flight must not propel forward; movement keys must steer it");
            return;
        }
        Vec3 boost = Vec3.ZERO;
        for (int i = 0; i < 1000; ++i)
        {
            boost = ModifierControlledFlight.controlledMotion(boost, look, 0, 0, 0, false, false, true, 0.1F, 0.2F);
        }
        if (boost.length() > 5.1 || boost.length() < 4.9)
        {
            helper.fail("Boost acceleration must be bounded by drag");
        }
        else helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void dataVariablesRoundTripAndFirstSync(GameTestHelper helper)
    {
        var source = new com.fiskmods.heroes.common.data.var.DataContainer();
        var target = new com.fiskmods.heroes.common.data.var.DataContainer();
        var vars = com.fiskmods.heroes.common.data.var.Vars.TIME_SINCE_DAMAGED;
        var anchor = com.fiskmods.heroes.common.data.var.Vars.WEB_ANCHOR_X;
        var flying = com.fiskmods.heroes.common.data.var.Vars.FLYING;
        if (source.get(com.fiskmods.heroes.common.data.var.Vars.SPEED) != 4
                || source.get(com.fiskmods.heroes.common.data.var.Vars.LIGHTSOUT_ID) != -1
                || source.get(com.fiskmods.heroes.common.data.var.Vars.UTILITY_BELT_TYPE) != -1)
        {
            helper.fail("Built-in data variables must preserve their explicit defaults");
            return;
        }
        source.set(vars, (short) 1234);
        source.set(anchor, -134.75D);
        source.set(flying, true);
        net.minecraft.nbt.CompoundTag saved = new net.minecraft.nbt.CompoundTag();
        source.writeTo(saved);
        target.readUpdate(source.writeDirty());
        if (target.get(vars) != 1234 || target.get(anchor) != -134.75D || !target.get(flying))
        {
            helper.fail("First dirty update must populate boolean, short and double values");
            return;
        }
        var reloaded = new com.fiskmods.heroes.common.data.var.DataContainer();
        reloaded.readFrom(saved);
        if (reloaded.get(vars) != 1234 || reloaded.get(anchor) != -134.75D || !reloaded.get(flying))
        {
            helper.fail("Data variables must survive NBT serialization");
            return;
        }
        source.clearDirty();
        source.set(vars, (short) 0);
        source.set(anchor, 0.0D);
        source.set(flying, false);
        target.readUpdate(source.writeDirty());
        if (target.get(vars) != 0 || target.get(anchor) != 0.0D || target.get(flying))
        {
            helper.fail("Dirty updates must also transmit values reset to their defaults");
            return;
        }
        target.setFromString(vars, "42");
        target.setFromString(anchor, "12.5");
        if (target.get(vars) != 42 || target.get(anchor) != 12.5D)
            helper.fail("Short and double script values must parse correctly");
        else helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void healingFactorWaitsAfterDamage(GameTestHelper helper)
    {
        var player = helper.makeMockPlayer();
        player.setHealth(5.0F);
        var data = new com.fiskmods.heroes.common.data.SHPlayerData();
        var modifier = new com.fiskmods.heroes.common.hero.modifier.ModifierHealingFactor(FiskHeroes.id("test_healing"));
        var entry = new com.fiskmods.heroes.common.hero.power.ModifierEntry(modifier, null);
        entry.setProperty(com.fiskmods.heroes.common.hero.power.PowerProperty.DELAY, 80);
        data.getData().set(com.fiskmods.heroes.common.data.var.Vars.TIME_SINCE_DAMAGED, (short) 79);
        modifier.tick(player, entry, data);
        if (player.getHealth() != 5.0F)
        {
            helper.fail("Healing factor must wait for its delay");
            return;
        }
        data.getData().set(com.fiskmods.heroes.common.data.var.Vars.TIME_SINCE_DAMAGED, (short) 80);
        modifier.tick(player, entry, data);
        modifier.tick(player, entry, data);
        if (player.getHealth() != 7.0F) helper.fail("Healing factor must heal each tick after its delay");
        else helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void tutridiumGeneratesInStoneAndDeepslate(GameTestHelper helper)
    {
        var relative = new net.minecraft.core.BlockPos(1, 1, 1);
        var pos = helper.absolutePos(relative);
        var random = net.minecraft.util.RandomSource.create(1234L);
        for (var base : new net.minecraft.world.level.block.Block[] {
                net.minecraft.world.level.block.Blocks.STONE, net.minecraft.world.level.block.Blocks.DEEPSLATE })
        {
            helper.setBlock(relative, base);
            if (!com.fiskmods.heroes.common.world.OreGenerationBiomeModifier.generateTutridiumVein(helper.getLevel(), random, pos)
                    || !helper.getLevel().getBlockState(pos).is(com.fiskmods.heroes.common.block.ModBlocks.TUTRIDIUM_ORE.get()))
            {
                helper.fail("Tutridium vein must replace stone and deepslate");
                return;
            }
        }
        helper.setBlock(relative, net.minecraft.world.level.block.Blocks.BEDROCK);
        if (com.fiskmods.heroes.common.world.OreGenerationBiomeModifier.generateTutridiumVein(helper.getLevel(), random, pos)
                || !helper.getLevel().getBlockState(pos).is(net.minecraft.world.level.block.Blocks.BEDROCK))
            helper.fail("Tutridium vein must not replace bedrock");
        else helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void movementPacketRoundTrip(GameTestHelper helper)
    {
        FriendlyByteBuf encoded = new FriendlyByteBuf(Unpooled.buffer());
        FriendlyByteBuf reencoded = new FriendlyByteBuf(Unpooled.buffer());
        try
        {
            new PacketInput(true, false, 100, -50).encode(encoded);
            new PacketInput(encoded).encode(reencoded);
            if (!reencoded.readBoolean() || reencoded.readBoolean()
                    || reencoded.readByte() != 1 || reencoded.readByte() != -1
                    || reencoded.readableBytes() != 0)
            {
                helper.fail("Directional input packet must round-trip with bounded axes");
            }
            else
            {
                encoded.clear();
                reencoded.clear();
                new com.fiskmods.heroes.common.network.PacketChangeSpeed(-30).encode(encoded);
                new com.fiskmods.heroes.common.network.PacketChangeSpeed(encoded).encode(reencoded);
                if (reencoded.readByte() != -1 || reencoded.readableBytes() != 0)
                    helper.fail("Speed scroll packet must encode only a bounded direction");
                else helper.succeed();
            }
        }
        finally
        {
            encoded.release();
            reencoded.release();
        }
    }
    @GameTest(template = "test_empty")
    public static void changingSuitsDoesNotGenerateEquipment(GameTestHelper helper)
    {
        var player = helper.makeMockPlayer();
        var data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(player);
        var hero = com.fiskmods.heroes.common.hero.Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:green_arrow");
        if (data == null || hero == null || hero.getEquipment().isEmpty())
        {
            helper.fail("Test requires player capability and an archer with declared equipment");
            return;
        }
        var chest = com.fiskmods.heroes.common.hero.ItemHeroArmor.create(hero.getDefaultIteration(),
                com.fiskmods.heroes.common.item.ModItems.heroArmor[1]);
        for (int i = 0; i < 3; ++i)
        {
            player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, chest.copy());
            com.fiskmods.heroes.common.hero.HeroTracker.update(player);
            if (data.getHero() == null)
            {
                helper.fail("Archer suit was not activated");
                return;
            }
            player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.item.ItemStack.EMPTY);
            com.fiskmods.heroes.common.hero.HeroTracker.update(player);
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); ++i)
        {
            if (!player.getInventory().getItem(i).isEmpty())
            {
                helper.fail("Changing suits must not generate free equipment");
                return;
            }
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void naturalRegenerationUsesVanillaFoodRules(GameTestHelper helper)
    {
        var player = helper.makeMockPlayer();
        var data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(player);
        var spider = com.fiskmods.heroes.common.hero.Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:spider_man");
        var trajectory = com.fiskmods.heroes.common.hero.Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:trajectory");
        if (data == null || spider == null || trajectory == null)
        {
            helper.fail("Player capability and regeneration heroes must be loaded");
            return;
        }
        var food = player.getFoodData();
        var natural = helper.getLevel().getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_NATURAL_REGENERATION);
        boolean oldNatural = natural.get();
        try
        {
            natural.set(true, helper.getLevel().getServer());
            data.setHero(spider.getDefaultIteration());
            player.setHealth(5.0F);
            food.setFoodLevel(18);
            food.setSaturation(0.0F);
            for (int i = 0; i < 19; ++i) food.tick(player);
            if (player.getHealth() != 5.0F) { helper.fail("Factor 4 regenerated before 20 ticks"); return; }
            food.tick(player);
            if (player.getHealth() != 6.0F || food.getExhaustionLevel() != 6.0F)
            { helper.fail("Factor 4 at 20 ticks: health=" + player.getHealth() + ", exhaustion=" + food.getExhaustionLevel() + ", interval=" + com.fiskmods.heroes.common.hero.modifier.ModifierRegeneration.healInterval(player, 80)); return; }

            player.setHealth(5.0F);
            food.setFoodLevel(20);
            food.setSaturation(6.0F);
            food.setExhaustion(0.0F);
            food.tick(player); food.tick(player);
            if (player.getHealth() != 5.0F) { helper.fail("Saturated regeneration occurred too early"); return; }
            food.tick(player);
            if (player.getHealth() != 6.0F || food.getExhaustionLevel() != 6.0F)
            { helper.fail("Saturated regeneration must retain vanilla healing and exhaustion"); return; }

            player.setHealth(5.0F);
            food.setFoodLevel(18);
            food.setSaturation(0.0F);
            food.setExhaustion(0.0F);
            data.setHero(trajectory.getDefaultIteration());
            for (int i = 0; i < 106; ++i) food.tick(player);
            if (player.getHealth() != 5.0F) { helper.fail("Factor 0.75 must slow regeneration"); return; }
            food.tick(player);
            if (player.getHealth() != 6.0F) { helper.fail("Factor 0.75 must heal at 107 ticks"); return; }

            player.setHealth(5.0F);
            natural.set(false, helper.getLevel().getServer());
            for (int i = 0; i < 120; ++i) food.tick(player);
            if (player.getHealth() != 5.0F) { helper.fail("Natural regeneration gamerule must be respected"); return; }
            natural.set(true, helper.getLevel().getServer());
            food.setFoodLevel(0);
            for (int i = 0; i < 79; ++i) food.tick(player);
            var tag = new net.minecraft.nbt.CompoundTag();
            food.addAdditionalSaveData(tag);
            if (tag.getInt("foodTickTimer") != 79) { helper.fail("Starvation timer was modified"); return; }
            food.tick(player);
            food.addAdditionalSaveData(tag);
            if (tag.getInt("foodTickTimer") != 0) { helper.fail("Starvation must tick at the vanilla interval"); return; }
            helper.succeed();
        }
        finally
        {
            natural.set(oldNatural, helper.getLevel().getServer());
        }
    }

    @GameTest(template = "test_empty")
    public static void heroPowersAreBakedAndTick(GameTestHelper helper)
    {
        int checked = 0;
        for (var hero : com.fiskmods.heroes.common.hero.Hero.REGISTRY.getHeroes())
        {
            var expected = new java.util.LinkedHashSet<String>();
            for (var id : hero.getPowers())
            {
                var power = com.fiskmods.heroes.common.hero.power.Power.REGISTRY.get(id);
                if (power == null) { helper.fail("Missing power " + id + " for " + hero.getName()); return; }
                expected.addAll(power.createEntries().keySet());
            }
            var actual = new java.util.LinkedHashSet<String>();
            for (var entry : hero.getPowerContainer().getEntries()) actual.add(entry.getCard());
            if (!expected.equals(actual))
            {
                helper.fail("Incorrect baked modifier cards for " + hero.getName() + ": expected " + expected + ", got " + actual);
                return;
            }
            if (!expected.isEmpty()) ++checked;
        }
        if (checked < 50) { helper.fail("Expected to verify powers for at least 50 built-in heroes"); return; }

        var player = helper.makeMockPlayer();
        var data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(player);
        var hero = com.fiskmods.heroes.common.hero.Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:deadpool_xmen");
        if (data == null || hero == null) { helper.fail("Deadpool or player data is missing"); return; }
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST,
                com.fiskmods.heroes.common.hero.ItemHeroArmor.create(hero.getDefaultIteration(),
                        com.fiskmods.heroes.common.item.ModItems.heroArmor[1]));
        player.setHealth(5.0F);
        data.getData().set(com.fiskmods.heroes.common.data.var.Vars.TIME_SINCE_DAMAGED, (short) 81);
        com.fiskmods.heroes.common.hero.ability.ModifierHandler.tick(player);
        if (player.getHealth() != 6.0F)
            helper.fail("A worn Deadpool suit must actually tick its healing factor");
        else helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void potionImmunityOnlyRemovesConfiguredEffects(GameTestHelper helper)
    {
        var player = helper.makeMockPlayer();
        var data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(player);
        var hero = com.fiskmods.heroes.common.hero.Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:deadpool_xmen");
        if (data == null || hero == null) { helper.fail("Deadpool or player data is missing"); return; }
        data.setHero(hero.getDefaultIteration());
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON, 200));
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WITHER, 200));
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WEAKNESS, 200));
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.REGENERATION, 200));
        com.fiskmods.heroes.common.hero.ability.ModifierHandler.tickPotionEffects(player);
        if (player.hasEffect(net.minecraft.world.effect.MobEffects.POISON)
                || player.hasEffect(net.minecraft.world.effect.MobEffects.WITHER)
                || !player.hasEffect(net.minecraft.world.effect.MobEffects.WEAKNESS)
                || !player.hasEffect(net.minecraft.world.effect.MobEffects.REGENERATION))
            helper.fail("Potion immunity must remove only Deadpool's listed poison and wither effects");
        else helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void playerDataSurvivesInvalidatedClone(GameTestHelper helper)
    {
        var original = helper.makeMockPlayer();
        var replacement = helper.makeMockPlayer();
        var oldData = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(original);
        var newData = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(replacement);
        if (oldData == null || newData == null) { helper.fail("Player data capabilities were not attached"); return; }
        var vars = com.fiskmods.heroes.common.data.var.Vars.TIME_SINCE_DAMAGED;
        oldData.getData().set(vars, (short) 123);
        oldData.setSpellCooldownUntil(FiskHeroes.id("test_cooldown"), 250L);
        original.invalidateCaps();
        if (com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(original) != null)
        { helper.fail("Old capability was not invalidated before cloning"); return; }
        com.fiskmods.heroes.common.data.SHDataCapabilities.onPlayerClone(
                new net.minecraftforge.event.entity.player.PlayerEvent.Clone(replacement, original, true));
        if (newData.getData().get(vars) != 123 || newData.getSpellCooldownUntil(FiskHeroes.id("test_cooldown")) != 250L
                || com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(original) != null)
            helper.fail("Clone must copy invalidated data and leave old capabilities invalidated");
        else helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void damageResetsHealingDelay(GameTestHelper helper)
    {
        var player = helper.makeMockPlayer();
        var data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(player);
        if (data == null) { helper.fail("Player data capability was not attached"); return; }
        var timer = com.fiskmods.heroes.common.data.var.Vars.TIME_SINCE_DAMAGED;
        data.getData().set(timer, (short) 120);
        player.setHealth(20.0F);
        player.getAbilities().invulnerable = false;
        if (!player.hurt(player.damageSources().generic(), 2.0F) || player.getHealth() >= 20.0F
                || data.getData().get(timer) != 0)
        { helper.fail("Real damage must reset the healing delay"); return; }
        data.getData().set(timer, (short) 120);
        var zeroDamage = new net.minecraftforge.event.entity.living.LivingDamageEvent(player, player.damageSources().generic(), 0.0F);
        com.fiskmods.heroes.common.event.CommonEventHandler.onLivingDamage(zeroDamage);
        if (data.getData().get(timer) != 120) helper.fail("Zero damage must not reset the healing delay");
        else helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void typedProjectileDamageSurvivesTheHurtEvent(GameTestHelper helper)
    {
        var target = helper.makeMockPlayer();
        var attacker = helper.makeMockPlayer();
        var data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(target);
        var hero = com.fiskmods.heroes.common.hero.Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:senor_cactus");
        if (data == null || hero == null) { helper.fail("Cactus hero or player data missing"); return; }
        data.setHero(hero.getDefaultIteration());
        target.getAbilities().invulnerable = false;
        attacker.getAbilities().invulnerable = false;
        target.setHealth(20.0F);
        attacker.setHealth(20.0F);
        var projectile = new net.minecraft.world.entity.projectile.Arrow(helper.getLevel(), attacker);
        var source = target.damageSources().arrow(projectile, attacker);
        var types = new com.google.gson.JsonObject();
        types.addProperty("CACTUS", 1.0F);
        var profile = new com.google.gson.JsonObject();
        profile.add("types", types);
        com.fiskmods.heroes.common.hero.modifier.DamageGroups.applyProfileDamage(target, attacker, source, 8.0F, profile);
        // LivingEntity.hurt can return true even if LivingHurtEvent was canceled.
        if (target.getHealth() != 20.0F || attacker.getHealth() != 20.0F)
        { helper.fail("A cactus-typed projectile must be immune and must not trigger thorns"); return; }
        target.invulnerableTime = 0;
        types.addProperty("CACTUS", 0.5F);
        if (!com.fiskmods.heroes.common.hero.modifier.DamageGroups.applyProfileDamage(target, attacker, source, 8.0F, profile)
                || Math.abs(target.getHealth() - 16.0F) > 0.01F || attacker.getHealth() != 20.0F)
        { helper.fail("Mixed projectile damage must retain its partial cactus immunity without retaliation"); return; }
        var batman = com.fiskmods.heroes.common.hero.Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:batman_dceu");
        if (batman == null) { helper.fail("Batman is missing"); return; }
        data.setHero(batman.getDefaultIteration());
        target.invulnerableTime = 0;
        target.setHealth(20.0F);
        types.remove("CACTUS");
        types.addProperty("BULLET", 1.0F);
        com.fiskmods.heroes.common.hero.modifier.DamageGroups.applyProfileDamage(target, attacker, source, 8.0F, profile);
        if (target.getHealth() != 20.0F)
        { helper.fail("Batman's bullet immunity must block a bullet-typed projectile"); return; }
        target.invulnerableTime = 0;
        if (!target.hurt(source, 8.0F) || Math.abs(target.getHealth() - 12.0F) > 0.01F)
        { helper.fail("Bullet immunity must not block an ordinary arrow"); return; }
        if (com.fiskmods.heroes.common.hero.modifier.DamageGroups.currentDamageProfile() != null)
            helper.fail("Typed damage profile leaked beyond its scope");
        else helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void thornsReflectOnlyMeleeAndPreserveNestedProfiles(GameTestHelper helper)
    {
        var target = helper.makeMockPlayer();
        var attacker = helper.makeMockPlayer();
        var targetData = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(target);
        var attackerData = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(attacker);
        var hero = com.fiskmods.heroes.common.hero.Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:senor_cactus");
        if (targetData == null || attackerData == null || hero == null)
        { helper.fail("Cactus hero or player data missing"); return; }
        targetData.setHero(hero.getDefaultIteration());
        target.getAbilities().invulnerable = false;
        attacker.getAbilities().invulnerable = false;
        target.setHealth(20.0F);
        attacker.setHealth(20.0F);
        if (!target.hurt(target.damageSources().playerAttack(attacker), 8.0F)
                || Math.abs(attacker.getHealth() - 18.0F) > 0.01F)
        { helper.fail("Thorns must return 25 percent of a direct melee hit"); return; }
        target.invulnerableTime = 0;
        attacker.invulnerableTime = 0;
        target.setHealth(20.0F);
        attacker.setHealth(20.0F);
        var arrow = new net.minecraft.world.entity.projectile.Arrow(helper.getLevel(), attacker);
        if (!target.hurt(target.damageSources().arrow(arrow, attacker), 8.0F) || attacker.getHealth() != 20.0F)
        { helper.fail("Thorns must not retaliate against a ranged attacker"); return; }
        target.invulnerableTime = 0;
        attacker.invulnerableTime = 0;
        target.setHealth(20.0F);
        attacker.setHealth(20.0F);
        attackerData.setHero(hero.getDefaultIteration());
        if (!target.hurt(target.damageSources().playerAttack(attacker), 8.0F)
                || Math.abs(target.getHealth() - 13.6F) > 0.01F || attacker.getHealth() != 20.0F)
        { helper.fail("Nested thorns must not recurse or erase the original 20 percent cactus attack profile"); return; }
        if (com.fiskmods.heroes.common.hero.modifier.DamageGroups.currentDamageProfile() != null)
            helper.fail("Nested melee profile leaked beyond the hit");
        else helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void resistanceUsesNativeTagsAndMixedFractions(GameTestHelper helper)
    {
        var player = helper.makeMockPlayer();
        var data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(player);
        var hero = com.fiskmods.heroes.common.hero.Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:senor_cactus");
        if (data == null || hero == null) { helper.fail("Cactus hero or player data missing"); return; }
        data.setHero(hero.getDefaultIteration());
        float lava = com.fiskmods.heroes.common.hero.ability.ModifierHandler.modifyDamage(player, player.damageSources().lava(), 8.0F);
        float freeze = com.fiskmods.heroes.common.hero.ability.ModifierHandler.modifyDamage(player, player.damageSources().freeze(), 8.0F);
        if (Math.abs(lava - 4.0F) > 0.01F || Math.abs(freeze - 8.0F) > 0.01F)
        { helper.fail("Fire resistance must apply to lava, not freezing"); return; }
        var modifier = com.fiskmods.heroes.common.hero.modifier.Modifiers.FIRE_RESISTANCE;
        var entry = new com.fiskmods.heroes.common.hero.power.ModifierEntry(modifier, null);
        entry.setProperty(com.fiskmods.heroes.common.hero.power.PowerProperty.FACTOR, 0.8F);
        float[] mixed = { 0.0F };
        com.fiskmods.heroes.common.hero.modifier.DamageGroups.withDamageProfile(java.util.Map.of("FIRE", 0.25D, "BLUNT", 0.75D),
                () -> mixed[0] = modifier.modifyDamage(player, entry, player.damageSources().generic(), 10.0F));
        if (Math.abs(mixed[0] - 9.5F) > 0.01F)
            helper.fail("Resistance must affect only the matching share of a mixed attack");
        else helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void intangibilityRespectsItsDamageModes(GameTestHelper helper)
    {
        var player = helper.makeMockPlayer();
        var attacker = helper.makeMockPlayer();
        var data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(player);
        var vision = com.fiskmods.heroes.common.hero.Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:vision");
        var martian = com.fiskmods.heroes.common.hero.Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:martian_manhunter");
        if (data == null || vision == null || martian == null)
        { helper.fail("Intangibility test heroes or player capability missing"); return; }
        var absolute = vision.getPowerContainer().getEntry(FiskHeroes.id("intangibility"));
        var normal = martian.getPowerContainer().getEntry(FiskHeroes.id("intangibility"));
        if (absolute == null || normal == null) { helper.fail("Intangibility modifier missing"); return; }
        data.setHero(vision.getDefaultIteration());
        data.getData().set(com.fiskmods.heroes.common.data.var.Vars.INTANGIBLE, true);
        var damage = player.damageSources();
        var melee = damage.playerAttack(attacker);
        var arrow = damage.arrow(new net.minecraft.world.entity.projectile.Arrow(helper.getLevel(), attacker), attacker);
        var mod = absolute.getModifier();
        if (!mod.isImmuneTo(player, absolute, damage.inWall(), 8)
                || !mod.isImmuneTo(player, absolute, melee, 8)
                || !mod.isImmuneTo(player, absolute, arrow, 8)
                || !mod.isImmuneTo(player, absolute, damage.fellOutOfWorld(), 8)
                || mod.isImmuneTo(player, absolute, damage.lava(), 8))
        { helper.fail("Absolute phase must block melee, arrows, void and suffocation, but not lava"); return; }
        if (mod.modifyOutgoingDamage(player, absolute, attacker, damage.playerAttack(player), 8) != 0)
        { helper.fail("Absolute phase must also prevent the wearer from dealing melee damage"); return; }
        data.setHero(martian.getDefaultIteration());
        if (!normal.getModifier().isImmuneTo(player, normal, damage.inWall(), 8)
                || normal.getModifier().isImmuneTo(player, normal, melee, 8)
                || normal.getModifier().isImmuneTo(player, normal, arrow, 8)
                || normal.getModifier().isImmuneTo(player, normal, damage.fellOutOfWorld(), 8))
        { helper.fail("Non-absolute phase must protect against suffocation only"); return; }
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                com.fiskmods.heroes.common.hero.modifier.ModEffects.PHASE_SUPPRESSANT.get(), 100));
        if (normal.getModifier().isImmuneTo(player, normal, damage.inWall(), 8))
            helper.fail("Phase suppressant must disable protection");
        else helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void phaseCollisionSurvivesPlayerTickAndCleansUp(GameTestHelper helper)
    {
        var player = helper.makeMockPlayer();
        var data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(player);
        var hero = com.fiskmods.heroes.common.hero.Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:vision");
        if (data == null || hero == null) { helper.fail("Vision or player data is missing"); return; }
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST,
                com.fiskmods.heroes.common.hero.ItemHeroArmor.create(hero.getDefaultIteration(),
                        com.fiskmods.heroes.common.item.ModItems.heroArmor[1]));
        com.fiskmods.heroes.common.hero.HeroTracker.update(player, false);
        var pos = helper.absolutePos(new net.minecraft.core.BlockPos(1, 1, 1));
        helper.setBlock(new net.minecraft.core.BlockPos(2, 1, 1), net.minecraft.world.level.block.Blocks.STONE);
        player.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        data.getData().set(com.fiskmods.heroes.common.data.var.Vars.INTANGIBLE, true);
        player.tick();
        if (!player.noPhysics) { helper.fail("Player tick must not cancel active phasing"); return; }
        double start = player.getX();
        player.move(net.minecraft.world.entity.MoverType.SELF, new Vec3(1.0D, 0.0D, 0.0D));
        if (player.getX() < start + 0.99D) { helper.fail("Phased player must move through a solid block"); return; }
        data.getData().set(com.fiskmods.heroes.common.data.var.Vars.INTANGIBLE, false);
        com.fiskmods.heroes.common.hero.modifier.IntangibilityPhysics.update(player, data);
        player.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        start = player.getX();
        player.move(net.minecraft.world.entity.MoverType.SELF, new Vec3(1.0D, 0.0D, 0.0D));
        if (player.noPhysics || player.getX() >= start + 0.99D)
        { helper.fail("Collision must return when phasing is switched off"); return; }
        data.getData().set(com.fiskmods.heroes.common.data.var.Vars.INTANGIBLE, true);
        com.fiskmods.heroes.common.hero.modifier.IntangibilityPhysics.update(player, data);
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.item.ItemStack.EMPTY);
        com.fiskmods.heroes.common.hero.HeroTracker.update(player, false);
        if (player.noPhysics || data.getData().get(com.fiskmods.heroes.common.data.var.Vars.INTANGIBLE))
            helper.fail("Taking off the suit must clear phasing and restore collision");
        else helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void defaultHeroModifiersTickWithoutExceptions(GameTestHelper helper)
    {
        int checked = 0;
        for (var hero : com.fiskmods.heroes.common.hero.Hero.REGISTRY.getHeroes())
        {
            var player = helper.makeMockPlayer();
            player.setPos(net.minecraft.world.phys.Vec3.atCenterOf(helper.absolutePos(new net.minecraft.core.BlockPos(1, 1, 1))));
            player.tickCount = 20;
            var data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(player);
            if (data == null) { helper.fail("Player data capability missing"); return; }
            data.setHero(hero.getDefaultIteration());
            for (var entry : hero.getPowerContainer().getEntries())
            {
                try
                {
                    if (entry.isEnabled() && entry.isModifierEnabled(player, data))
                    {
                        entry.getModifier().tick(player, entry, data);
                        ++checked;
                    }
                }
                catch (Exception e)
                {
                    helper.fail("Default tick failed for " + hero.getName() + " / " + entry.getCard() + ": " + e);
                    return;
                }
            }
        }
        if (checked < 100) helper.fail("Expected at least 100 active built-in modifier ticks, saw " + checked);
        else helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void speedsterMovementUsesSelectedLevel(GameTestHelper helper)
    {
        var player = helper.makeMockPlayer();
        var data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(player);
        var hero = com.fiskmods.heroes.common.hero.Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:the_flash");
        if (data == null || hero == null) { helper.fail("Flash or player data is missing"); return; }
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST,
                com.fiskmods.heroes.common.hero.ItemHeroArmor.create(hero.getDefaultIteration(),
                        com.fiskmods.heroes.common.item.ModItems.heroArmor[1]));
        com.fiskmods.heroes.common.hero.HeroTracker.update(player);
        var entry = hero.getPowerContainer().getEntry(FiskHeroes.id("super_speed"));
        if (entry == null) { helper.fail("Flash is missing super speed"); return; }
        com.fiskmods.heroes.common.hero.attribute.SHAttributes.applyModifiers(player);
        double normal = player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        float normalStep = player.getStepHeight();
        entry.getModifier().onActivate(player, entry, data);
        com.fiskmods.heroes.common.hero.attribute.SHAttributes.applyModifiers(player);
        double fast = player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        double expected = com.fiskmods.heroes.common.hero.attribute.SHAttributes.speedsterBonus(4);
        if (Math.abs((fast - normal) - expected) > 0.0001D)
        { helper.fail("Speed level four must apply its original movement attribute bonus"); return; }
        if (player.getStepHeight() < 0.999F)
        { helper.fail("Active super speed must allow one-block steps"); return; }
        com.fiskmods.heroes.common.hero.attribute.SHAttributes.applyModifiers(player);
        if (player.getStepHeight() > 1.001F)
        { helper.fail("Repeated speed updates must not stack the step bonus"); return; }

        var step = player.getAttribute(net.minecraftforge.common.ForgeMod.STEP_HEIGHT_ADDITION.get());
        var external = new net.minecraft.world.entity.ai.attributes.AttributeModifier(java.util.UUID.randomUUID(),
                "test-external-step", 0.3D, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION);
        step.addTransientModifier(external);
        com.fiskmods.heroes.common.hero.attribute.SHAttributes.applyModifiers(player);
        if (Math.abs(player.getStepHeight() - 1.0F) > 0.001F)
        { helper.fail("Speed step must adapt to other step modifiers"); return; }
        step.removeModifier(external);
        com.fiskmods.heroes.common.hero.attribute.SHAttributes.applyModifiers(player);
        if (Math.abs(player.getStepHeight() - 1.0F) > 0.001F)
        { helper.fail("Speed step must recover when another modifier is removed"); return; }
        var multiplier = new net.minecraft.world.entity.ai.attributes.AttributeModifier(java.util.UUID.randomUUID(),
                "test-external-multiplier", 1.0D, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.MULTIPLY_TOTAL);
        step.addTransientModifier(multiplier);
        com.fiskmods.heroes.common.hero.attribute.SHAttributes.applyModifiers(player);
        if (Math.abs(player.getStepHeight() - 1.0F) > 0.001F)
        { helper.fail("Speed step must account for other mods' attribute multipliers"); return; }
        step.removeModifier(multiplier);
        com.fiskmods.heroes.common.hero.attribute.SHAttributes.applyModifiers(player);

        var floor = new net.minecraft.core.BlockPos(1, 0, 1);
        var obstacle = new net.minecraft.core.BlockPos(2, 1, 1);
        helper.setBlock(floor, net.minecraft.world.level.block.Blocks.STONE);
        helper.setBlock(obstacle, net.minecraft.world.level.block.Blocks.STONE);
        var start = helper.absolutePos(floor);
        player.setPos(start.getX() + 0.5D, start.getY() + 1.0D, start.getZ() + 0.5D);
        player.setOnGround(true);
        player.move(net.minecraft.world.entity.MoverType.SELF, new net.minecraft.world.phys.Vec3(1.1D, -0.05D, 0.0D));
        if (player.getY() < start.getY() + 1.99D)
        { helper.fail("Active speedster must physically step onto a full block: y=" + player.getY()); return; }

        player.setSprinting(true);
        com.fiskmods.heroes.common.data.PlayerInputTracker.set(player, false, false, 0, 0);
        player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        entry.getModifier().tick(player, entry, data);
        if (data.getData().get(com.fiskmods.heroes.common.data.var.Vars.SPEED_SPRINTING)
                || player.getDeltaMovement().lengthSqr() != 0.0D)
        { helper.fail("A stationary speedster must not be propelled forward"); return; }
        com.fiskmods.heroes.common.data.PlayerInputTracker.set(player, false, false, 1, 0);
        entry.getModifier().tick(player, entry, data);
        if (!data.getData().get(com.fiskmods.heroes.common.data.var.Vars.SPEED_SPRINTING))
        { helper.fail("Moving sprint state must be synchronized to hero scripts"); return; }
        com.fiskmods.heroes.common.hero.modifier.ModifierSuperSpeed.changeSpeed(player, entry, data, -500);
        if (data.getData().get(com.fiskmods.heroes.common.data.var.Vars.SPEED) != 3)
        { helper.fail("Speed adjustment must advance one level at a time"); return; }
        com.fiskmods.heroes.common.hero.modifier.ModifierSuperSpeed.changeSpeed(player, entry, data, 1);
        com.fiskmods.heroes.common.hero.modifier.ModifierSuperSpeed.changeSpeed(player, entry, data, 1);
        if (data.getData().get(com.fiskmods.heroes.common.data.var.Vars.SPEED) != 4)
        { helper.fail("Speed adjustment must respect the suit's maximum level"); return; }
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.item.ItemStack.EMPTY);
        com.fiskmods.heroes.common.hero.HeroTracker.update(player);
        if (data.getData().get(com.fiskmods.heroes.common.data.var.Vars.SPEEDING)
                || data.isToggleEnabled(FiskHeroes.id("super_speed"))
                || player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)
                    .getModifier(java.util.UUID.fromString("388b19ef-9685-4881-be63-04bc862dca2d")) != null)
        { helper.fail("Removing the suit must clear super speed and its attribute"); return; }
        if (Math.abs(player.getStepHeight() - normalStep) > 0.001F)
        { helper.fail("Removing the suit must restore step height"); return; }
        com.fiskmods.heroes.common.data.PlayerInputTracker.clear(player);
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void slowMotionSupportsToggleAndHold(GameTestHelper helper)
    {
        var player = helper.makeMockPlayer();
        var data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(player);
        var flash = com.fiskmods.heroes.common.hero.Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:the_flash");
        var spider = com.fiskmods.heroes.common.hero.Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:spider_man");
        if (data == null || flash == null || spider == null) { helper.fail("Missing test heroes"); return; }
        var toggle = flash.getPowerContainer().getEntry(FiskHeroes.id("slow_motion"));
        var hold = spider.getPowerContainer().getEntry(FiskHeroes.id("slow_motion"));
        var property = com.fiskmods.heroes.common.hero.power.PowerProperty.IS_TOGGLE;
        var state = com.fiskmods.heroes.common.data.var.Vars.SLOW_MOTION;
        if (toggle == null || hold == null || !toggle.getBoolean(player, property) || hold.getBoolean(player, property))
        { helper.fail("Flash must toggle slow motion; Spider-Man must hold it"); return; }
        toggle.getModifier().onActivate(player, toggle, data);
        toggle.getModifier().onToggle(player, toggle, data);
        if (!data.getData().get(state)) { helper.fail("Toggle mode must stay on after release"); return; }
        toggle.getModifier().onActivate(player, toggle, data);
        if (data.getData().get(state)) { helper.fail("Second toggle must switch slow motion off"); return; }
        hold.getModifier().onActivate(player, hold, data);
        if (!data.getData().get(state)) { helper.fail("Hold mode must start on press"); return; }
        hold.getModifier().onToggle(player, hold, data);
        if (data.getData().get(state) || data.isToggleEnabled(FiskHeroes.id("slow_motion")))
            helper.fail("Hold mode must stop on release");
        else helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void suitSprintBonusOnlyAppliesWhileSprinting(GameTestHelper helper)
    {
        var player = helper.makeMockPlayer();
        var hero = com.fiskmods.heroes.common.hero.Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:green_arrow");
        if (hero == null) { helper.fail("Green Arrow is missing"); return; }
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST,
                com.fiskmods.heroes.common.hero.ItemHeroArmor.create(hero.getDefaultIteration(),
                        com.fiskmods.heroes.common.item.ModItems.heroArmor[1]));
        com.fiskmods.heroes.common.hero.HeroTracker.update(player);
        var attribute = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        player.setSprinting(false);
        com.fiskmods.heroes.common.hero.attribute.SHAttributes.applyModifiers(player);
        if (attribute.getModifiers().stream().anyMatch(mod -> mod.getName().equals("fiskheroes:sprint_speed")))
        { helper.fail("Sprint-only suit bonus applied while walking"); return; }
        player.setSprinting(true);
        com.fiskmods.heroes.common.hero.attribute.SHAttributes.applyModifiers(player);
        if (attribute.getModifiers().stream().noneMatch(mod -> mod.getName().equals("fiskheroes:sprint_speed")))
            helper.fail("Suit sprint bonus missing while sprinting");
        else helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void speedKeyAndScrollAreValidatedOnServer(GameTestHelper helper)
    {
        var player = new net.minecraftforge.common.util.FakePlayer(helper.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "fisk-speed-test"));
        var hero = com.fiskmods.heroes.common.hero.Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:the_flash");
        var data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(player);
        if (hero == null || data == null) { helper.fail("Flash or player data is missing"); return; }
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST,
                com.fiskmods.heroes.common.hero.ItemHeroArmor.create(hero.getDefaultIteration(),
                        com.fiskmods.heroes.common.item.ModItems.heroArmor[1]));
        com.fiskmods.heroes.common.hero.HeroTracker.update(player);
        com.fiskmods.heroes.common.hero.attribute.SHAttributes.applyModifiers(player);
        var speed = com.fiskmods.heroes.common.data.var.Vars.SPEED;
        var active = com.fiskmods.heroes.common.data.var.Vars.SPEEDING;
        if (com.fiskmods.heroes.common.network.PacketChangeSpeed.requestChange(player, -1))
        { helper.fail("Speed scroll without the held key must be rejected"); return; }
        com.fiskmods.heroes.common.hero.ability.AbilityHandler.onAbilityKey(player, 1, true);
        if (data.getData().get(active) || com.fiskmods.heroes.common.network.PacketChangeSpeed.requestChange(player, -1))
        { helper.fail("Speed must not toggle on key-down, or allow scrolling while disabled"); return; }
        com.fiskmods.heroes.common.hero.ability.AbilityHandler.onAbilityKey(player, 1, false);
        if (!data.getData().get(active)) { helper.fail("Speed must toggle on key release"); return; }
        com.fiskmods.heroes.common.hero.ability.AbilityHandler.onAbilityKey(player, 1, true);
        if (!com.fiskmods.heroes.common.network.PacketChangeSpeed.requestChange(player, -1)
                || data.getData().get(speed) != 3)
        { helper.fail("Held speed key must permit a one-level scroll"); return; }
        com.fiskmods.heroes.common.hero.ability.AbilityHandler.onAbilityKey(player, 2, false);
        com.fiskmods.heroes.common.hero.ability.AbilityHandler.onAbilityKey(player, 1, false);
        if (!data.getData().get(active)) { helper.fail("Scrolling must prevent the release toggle"); return; }
        com.fiskmods.heroes.common.hero.ability.AbilityHandler.onAbilityKey(player, 1, true);
        com.fiskmods.heroes.common.hero.ability.AbilityHandler.onAbilityKey(player, 1, false);
        if (data.getData().get(active)) { helper.fail("A subsequent tap must switch speed off"); return; }
        com.fiskmods.heroes.common.hero.ability.AbilityHandler.clear(player);
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void requiredSuitChecksCorrectPieces(GameTestHelper helper)
    {
        var player = helper.makeMockPlayer();
        var data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(player);
        var flash = com.fiskmods.heroes.common.hero.Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:the_flash");
        var spider = com.fiskmods.heroes.common.hero.Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:spider_man");
        var ironman = com.fiskmods.heroes.common.hero.Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:iron_man_mk50");
        if (data == null || flash == null || spider == null || ironman == null)
        { helper.fail("Suit test heroes or capability missing"); return; }
        {
            var flashIter = flash.getDefaultIteration();
            player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST,
                    com.fiskmods.heroes.common.hero.ItemHeroArmor.create(flashIter, com.fiskmods.heroes.common.item.ModItems.heroArmor[1]));
            com.fiskmods.heroes.common.hero.HeroTracker.update(player, false);
            if (data.getHero() != flashIter) { helper.fail("Partial suits must work when full suit is optional"); return; }
            com.fiskmods.heroes.common.hero.HeroTracker.update(player, true);
            if (data.getHero() != null || com.fiskmods.heroes.common.hero.HeroTracker.getWornSuit(player) != flashIter)
            { helper.fail("Partial suit must stay visible but not grant powers"); return; }
            for (int slot = 0; slot < 4; ++slot)
                player.setItemSlot(com.fiskmods.heroes.common.command.CommandSuit.slotFor(slot),
                        com.fiskmods.heroes.common.hero.ItemHeroArmor.create(flashIter, com.fiskmods.heroes.common.item.ModItems.heroArmor[slot]));
            com.fiskmods.heroes.common.hero.HeroTracker.update(player, true);
            if (data.getHero() != flashIter || !new com.fiskmods.heroes.pack.js.JSEntity(player).isWearingFullSuit())
            { helper.fail("A complete matching suit must activate"); return; }
            player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,
                    com.fiskmods.heroes.common.hero.ItemHeroArmor.create(spider.getDefaultIteration(), com.fiskmods.heroes.common.item.ModItems.heroArmor[0]));
            com.fiskmods.heroes.common.hero.HeroTracker.update(player, true);
            if (data.getHero() != null || new com.fiskmods.heroes.pack.js.JSEntity(player).isWearingFullSuit())
            { helper.fail("A mismatched hero piece must not count as a full suit"); return; }
            var spoof = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_HELMET);
            com.fiskmods.heroes.common.hero.ItemHeroArmor.setHero(spoof, flashIter);
            player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, spoof);
            com.fiskmods.heroes.common.hero.HeroTracker.update(player, true);
            if (data.getHero() != null || new com.fiskmods.heroes.pack.js.JSEntity(player).isWearingFullSuit())
            { helper.fail("Unrelated armor with hero NBT must not count as a suit piece"); return; }
            for (var slot : new net.minecraft.world.entity.EquipmentSlot[] { net.minecraft.world.entity.EquipmentSlot.HEAD,
                    net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.LEGS,
                    net.minecraft.world.entity.EquipmentSlot.FEET }) player.setItemSlot(slot, net.minecraft.world.item.ItemStack.EMPTY);
            var ironIter = ironman.getDefaultIteration();
            for (int slot = 0; slot < 4; ++slot)
                if (ironIter.hasPieceOfSet(slot))
                    player.setItemSlot(com.fiskmods.heroes.common.command.CommandSuit.slotFor(slot),
                            com.fiskmods.heroes.common.hero.ItemHeroArmor.create(ironIter, com.fiskmods.heroes.common.item.ModItems.heroArmor[slot]));
            com.fiskmods.heroes.common.hero.HeroTracker.update(player, true);
            if (data.getHero() != ironIter) { helper.fail("A complete one-piece suit must activate"); return; }
            player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.item.ItemStack.EMPTY);
            var fakeChest = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_CHESTPLATE);
            com.fiskmods.heroes.common.hero.ItemHeroArmor.setHero(fakeChest, flashIter);
            player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, fakeChest);
            if (com.fiskmods.heroes.common.hero.HeroTracker.getWornSuit(player) != null)
            { helper.fail("Tagged non-hero armor must not resolve as an active suit"); return; }
            helper.succeed();
        }
    }

    @GameTest(template = "test_empty")
    public static void suitCommandAcceptsNamespacedAndShortIds(GameTestHelper helper)
    {
        var player = new net.minecraftforge.common.util.FakePlayer(helper.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "suit-command-test"));
        var dispatcher = new com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack>();
        com.fiskmods.heroes.common.command.CommandSuit.register(dispatcher);
        var source = player.createCommandSourceStack().withPermission(2);
        for (String command : new String[] { "suit fiskheroes:the_flash", "suit the_flash", "suit fiskheroes:the_flash test" })
        {
            var parsed = dispatcher.parse(command, source);
            if (parsed.getReader().canRead() || parsed.getContext().getCommand() == null)
            { helper.fail("Suit command did not parse: " + command); return; }
        }
        helper.succeed();
    }

}
