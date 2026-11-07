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
    public static void chargedBeamUsesNormalizedAnimationAndCooldown(GameTestHelper helper)
    {
        var player = new net.minecraftforge.common.util.FakePlayer(helper.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "fisk-beam-test"));
        var hero = com.fiskmods.heroes.common.hero.Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:vision");
        var data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(player);
        if (hero == null || data == null) { helper.fail("Vision or player data is missing"); return; }
        var slots = new net.minecraft.world.entity.EquipmentSlot[] {net.minecraft.world.entity.EquipmentSlot.HEAD,
                net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.LEGS,
                net.minecraft.world.entity.EquipmentSlot.FEET};
        for (int i = 0; i < 4; ++i)
            if (hero.getDefaultIteration().hasPieceOfSet(i)) player.setItemSlot(slots[i],
                    com.fiskmods.heroes.common.hero.ItemHeroArmor.create(hero.getDefaultIteration(),
                            com.fiskmods.heroes.common.item.ModItems.heroArmor[i]));
        com.fiskmods.heroes.common.hero.HeroTracker.update(player);
        var entry = hero.getPowerContainer().getEntry(FiskHeroes.id("charged_beam"));
        if (entry == null) { helper.fail("Vision charged beam is missing"); return; }
        int chargeTime = Math.max(1, entry.getInt(player, com.fiskmods.heroes.common.hero.power.PowerProperty.CHARGE_TIME));
        int cooldownTime = Math.max(1, entry.getInt(player, com.fiskmods.heroes.common.hero.power.PowerProperty.COOLDOWN_TIME));
        var vars = data.getData();
        com.fiskmods.heroes.common.hero.ability.AbilityHandler.onAbilityKey(player, 1, true);
        for (int i = 0; i < chargeTime; ++i) entry.getModifier().tick(player, entry, data);
        if (Math.abs(vars.get(com.fiskmods.heroes.common.data.var.Vars.BEAM_CHARGE) - 1.0F) > 0.0001F)
        { helper.fail("Beam charge must reach one after chargeTime ticks"); return; }
        for (int i = 0; i < 4; ++i) entry.getModifier().tick(player, entry, data);
        if (vars.get(com.fiskmods.heroes.common.data.var.Vars.BEAM_SHOOTING) <= 0.0F
                || vars.get(com.fiskmods.heroes.common.data.var.Vars.BEAM_SHOOTING_TIMER) <= 0.0F)
        { helper.fail("A fully charged held beam must animate its firing phase"); return; }
        com.fiskmods.heroes.common.hero.ability.AbilityHandler.onAbilityKey(player, 1, false);
        entry.getModifier().tick(player, entry, data);
        float cooling = vars.get(com.fiskmods.heroes.common.data.var.Vars.BEAM_CHARGE);
        if (vars.get(com.fiskmods.heroes.common.data.var.Vars.BEAM_CHARGING)
                || Math.abs(cooling - (1.0F - 1.0F / cooldownTime)) > 0.0001F)
        { helper.fail("Releasing the beam must start normalized cooldown instead of erasing charge"); return; }
        for (int i = 0; i < cooldownTime + 2; ++i) entry.getModifier().tick(player, entry, data);
        if (vars.get(com.fiskmods.heroes.common.data.var.Vars.BEAM_CHARGE) != 0.0F
                || vars.get(com.fiskmods.heroes.common.data.var.Vars.BEAM_SHOOTING_TIMER) != 0.0F)
        { helper.fail("Beam animation and charge must finish cooling down"); return; }
        com.fiskmods.heroes.common.hero.ability.AbilityHandler.clear(player);
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void capeSegmentsAreContinuousAndHaveCorrectLength(GameTestHelper helper)
    {
        float length = 24.0F;
        int segments = 24;
        float step = length / 16.0F / segments;
        for (int i = 0; i < segments; ++i)
        {
            float top = com.fiskmods.heroes.common.util.CapeMeshMath.segmentY(i, length, segments);
            float next = com.fiskmods.heroes.common.util.CapeMeshMath.segmentY(i + 1, length, segments);
            if (Math.abs(top + step - next) > 0.00001F)
            { helper.fail("Cape segments must meet without gaps"); return; }
        }
        if (Math.abs(com.fiskmods.heroes.common.util.CapeMeshMath.segmentY(segments, length, segments) - 1.5F) > 0.00001F)
        { helper.fail("The cape must have its declared length, not double it"); return; }
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void deployedCapeUsesTwoSidedWingGeometry(GameTestHelper helper)
    {
        float folded = com.fiskmods.heroes.common.util.CapeMeshMath.wingFlare(0.5F, 0.0D, 0.0D);
        float deployed = com.fiskmods.heroes.common.util.CapeMeshMath.wingFlare(1.0F, 0.0D, 0.0D);
        float diving = com.fiskmods.heroes.common.util.CapeMeshMath.wingFlare(1.0F, -1.0D, 1.0D);
        float w = 14.0F / 16.0F;
        float l = 22.0F / 16.0F;
        if (folded != 0.0F || deployed != 1.0F || Math.abs(diving - 1.0F / 6.0F) > 0.0001F
                || Math.abs(com.fiskmods.heroes.common.util.CapeMeshMath.wingOuterX(0, w, folded) - w / 2) > 0.0001F
                || Math.abs(com.fiskmods.heroes.common.util.CapeMeshMath.wingOuterX(0, w, deployed) - w * 2.75F) > 0.0001F
                || Math.abs(com.fiskmods.heroes.common.util.CapeMeshMath.wingOuterY(4, l, deployed) - l * 1.4F) > 0.0001F)
        { helper.fail("Cape wings must unfold from the original UV and geometry proportions"); return; }
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void fallingJumpDeploysBatmanCapeAndLandingResets(GameTestHelper helper)
    {
        var player = new net.minecraftforge.common.util.FakePlayer(helper.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "fisk-glide-test"));
        var hero = com.fiskmods.heroes.common.hero.Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:batman_dceu");
        var data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(player);
        if (hero == null || data == null) { helper.fail("Batman or player data is missing"); return; }
        var slots = new net.minecraft.world.entity.EquipmentSlot[] {net.minecraft.world.entity.EquipmentSlot.HEAD,
                net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.LEGS,
                net.minecraft.world.entity.EquipmentSlot.FEET};
        for (int i = 0; i < 4; ++i)
            if (hero.getDefaultIteration().hasPieceOfSet(i)) player.setItemSlot(slots[i],
                    com.fiskmods.heroes.common.hero.ItemHeroArmor.create(hero.getDefaultIteration(),
                            com.fiskmods.heroes.common.item.ModItems.heroArmor[i]));
        com.fiskmods.heroes.common.hero.HeroTracker.update(player);
        var entry = hero.getPowerContainer().getEntry(FiskHeroes.id("gliding"));
        if (entry == null || !entry.getBoolean(player, com.fiskmods.heroes.common.hero.power.PowerProperty.CAN_JUMP_ACTIVATE))
        { helper.fail("Batman gliding power is missing its jump control"); return; }
        player.setOnGround(false);
        player.setDeltaMovement(0.0D, -0.4D, 0.0D);
        com.fiskmods.heroes.common.network.PacketInput.applyServerInput(player, true, false, 0, 0, false);
        if (!data.getData().get(com.fiskmods.heroes.common.data.var.Vars.GLIDING))
        { helper.fail("A falling jump must deploy the cape"); return; }
        for (int i = 0; i < 12; ++i) entry.getModifier().tick(player, entry, data);
        if (data.getData().get(com.fiskmods.heroes.common.data.var.Vars.WING_ANIMATION_TIMER) != 1.0F
                || data.getData().get(com.fiskmods.heroes.common.data.var.Vars.GLIDING_TIMER) != 1.0F
                || player.getDeltaMovement().y < -0.121D)
        { helper.fail("Gliding must unfold the cape, normalize the pose and slow the fall"); return; }
        player.setOnGround(true);
        for (int i = 0; i < 8; ++i) entry.getModifier().tick(player, entry, data);
        if (data.getData().get(com.fiskmods.heroes.common.data.var.Vars.GLIDING)
                || data.getData().get(com.fiskmods.heroes.common.data.var.Vars.WING_ANIMATION_TIMER) != 0.0F)
        { helper.fail("Landing must stop gliding and fold the cape"); return; }
        com.fiskmods.heroes.common.network.PacketInput.clearServerState(player);
        com.fiskmods.heroes.common.data.PlayerInputTracker.clear(player);
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void controlledGlidingDistinguishesHoverForwardAndJetpack(GameTestHelper helper)
    {
        var player = new net.minecraftforge.common.util.FakePlayer(helper.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "fisk-hover-test"));
        var data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(player);
        var hero = com.fiskmods.heroes.common.hero.Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:batman_dceu");
        var base = hero != null ? hero.getPowerContainer().getEntry(FiskHeroes.id("gliding")) : null;
        if (data == null || base == null) { helper.fail("Gliding data is missing"); return; }
        var entry = new com.fiskmods.heroes.common.hero.power.ModifierEntry(base.getModifier(), null);
        entry.setProperty(com.fiskmods.heroes.common.hero.power.PowerProperty.IS_CONTROLLED, true);
        var vars = data.getData();
        var v = com.fiskmods.heroes.common.data.var.Vars.GLIDING;
        player.setOnGround(false);
        vars.set(v, true);
        player.setDeltaMovement(0, -0.4, 0);
        com.fiskmods.heroes.common.data.PlayerInputTracker.set(player, false, false, 0, 0);
        entry.getModifier().tick(player, entry, data);
        if (Math.abs(player.getDeltaMovement().y + 0.29D) > 0.001D
                || vars.get(com.fiskmods.heroes.common.data.var.Vars.GLIDE_FLYING))
        { helper.fail("Controlled gliding without forward input must hover and brake its descent"); return; }
        com.fiskmods.heroes.common.data.PlayerInputTracker.set(player, true, false, 0, 0);
        entry.getModifier().tick(player, entry, data);
        if (!vars.get(com.fiskmods.heroes.common.data.var.Vars.JETPACKING)
                || Math.abs(player.getDeltaMovement().y + 0.13D) > 0.001D)
        { helper.fail("Jump while hovering must activate the jetpack boost"); return; }
        com.fiskmods.heroes.common.data.PlayerInputTracker.set(player, true, false, 1, 0);
        entry.getModifier().tick(player, entry, data);
        if (!vars.get(com.fiskmods.heroes.common.data.var.Vars.GLIDE_FLYING)
                || vars.get(com.fiskmods.heroes.common.data.var.Vars.JETPACKING))
        { helper.fail("Forward movement must select glide flight instead of jetpacking"); return; }
        com.fiskmods.heroes.common.data.PlayerInputTracker.clear(player);
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void armorTextureTreesRespectConditionsAndNull(GameTestHelper helper)
    {
        var tree = com.google.gson.JsonParser.parseString("""
                {"vars:SPEEDING":{"true":{"renderLayer":{"LEGGINGS":"layer2_on"},"default":"layer1_on"}},
                 "renderLayer":{"LEGGINGS":"layer2"},"default":"layer1"}
                """);
        java.util.Map<String, String> vars = new java.util.HashMap<>();
        vars.put("vars:SPEEDING", "false");
        vars.put("renderLayer", "LEGGINGS");
        if (!"layer2".equals(com.fiskmods.heroes.common.hero.render.SuitTextureTree.resolve(tree, vars::get)))
        { helper.fail("Inactive armor must select the normal leggings texture"); return; }
        vars.put("vars:SPEEDING", "true");
        if (!"layer2_on".equals(com.fiskmods.heroes.common.hero.render.SuitTextureTree.resolve(tree, vars::get)))
        { helper.fail("Active armor must select the conditional leggings texture"); return; }
        vars.put("renderLayer", "HELMET");
        if (!"layer1_on".equals(com.fiskmods.heroes.common.hero.render.SuitTextureTree.resolve(tree, vars::get)))
        { helper.fail("Active armor must use the conditional default for other slots"); return; }
        tree = com.google.gson.JsonParser.parseString("""
                {"vars:MASK_OPEN":{"true":{"renderLayer":{"HELMET":"null"}}},"default":"lights"}
                """);
        vars.put("vars:MASK_OPEN", "true");
        if (!"null".equals(com.fiskmods.heroes.common.hero.render.SuitTextureTree.resolve(tree, vars::get)))
        { helper.fail("A selected null texture must not fall back to glowing closed-mask eyes"); return; }
        vars.put("renderLayer", "CHESTPLATE");
        if (!"lights".equals(com.fiskmods.heroes.common.hero.render.SuitTextureTree.resolve(tree, vars::get)))
        { helper.fail("An unmatched nested branch must fall through to the default"); return; }
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void transformationTimersAndCooldownsAreNormalized(GameTestHelper helper)
    {
        var player = helper.makeMockPlayer();
        var data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(player);
        var hero = com.fiskmods.heroes.common.hero.Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:ant_man");
        if (data == null || hero == null) { helper.fail("Ant-Man data missing"); return; }
        data.setHero(hero.getDefaultIteration());
        var transform = hero.getPowerContainer().getEntry(FiskHeroes.id("transformation"));
        var cooldown = hero.getPowerContainer().getEntry(FiskHeroes.id("cooldown"));
        if (transform == null || cooldown == null) { helper.fail("Transformation or cooldown missing"); return; }
        var vars = data.getData();
        vars.set(com.fiskmods.heroes.common.data.var.Vars.GIANT_MODE, true);
        for (int i = 0; i < 5; ++i) transform.getModifier().tick(player, transform, data);
        if (Math.abs(vars.get(com.fiskmods.heroes.common.data.var.Vars.GIANT_MODE_TIMER) - 0.5F) > 0.001F)
        { helper.fail("Ten-tick transformation must reach half after five ticks"); return; }
        cooldown.getModifier().tick(player, cooldown, data);
        float elapsed = vars.get(com.fiskmods.heroes.common.data.var.Vars.GIANT_MODE_COOLDOWN);
        if (Math.abs(elapsed - 1.0F / 2400.0F) > 0.00001F)
        { helper.fail("Cooldown must be a normalized fraction of duration"); return; }
        vars.set(com.fiskmods.heroes.common.data.var.Vars.GIANT_MODE_COOLDOWN, 0.9999F);
        cooldown.getModifier().tick(player, cooldown, data);
        if (vars.get(com.fiskmods.heroes.common.data.var.Vars.GIANT_MODE)
                || vars.get(com.fiskmods.heroes.common.data.var.Vars.GIANT_MODE_COOLDOWN) != 1.0F)
        { helper.fail("A full cooldown must switch giant mode off at one"); return; }
        cooldown.getModifier().tick(player, cooldown, data);
        if (Math.abs(vars.get(com.fiskmods.heroes.common.data.var.Vars.GIANT_MODE_COOLDOWN) - (1.0F - 1.0F / 2400.0F)) > 0.00001F)
        { helper.fail("Cooldown recovery must use the normalized recovery rate"); return; }
        transform.getModifier().tick(player, transform, data);
        if (Math.abs(vars.get(com.fiskmods.heroes.common.data.var.Vars.GIANT_MODE_TIMER) - 0.4F) > 0.001F)
        { helper.fail("Transformation must reverse in normalized steps"); return; }
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void antManGiantModeGrowsAndReturns(GameTestHelper helper)
    {
        var player = helper.makeMockPlayer();
        var data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(player);
        var hero = com.fiskmods.heroes.common.hero.Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:ant_man");
        if (data == null || hero == null) { helper.fail("Ant-Man data missing"); return; }
        var pos = helper.absolutePos(new net.minecraft.core.BlockPos(2, 2, 2));
        player.setPos(pos.getX() + 0.5D, 120.0D, pos.getZ() + 0.5D);
        var slots = new net.minecraft.world.entity.EquipmentSlot[] {net.minecraft.world.entity.EquipmentSlot.HEAD,
                net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.LEGS,
                net.minecraft.world.entity.EquipmentSlot.FEET};
        for (int slot = 0; slot < slots.length; ++slot)
            player.setItemSlot(slots[slot], com.fiskmods.heroes.common.hero.ItemHeroArmor.create(
                    hero.getDefaultIteration(), com.fiskmods.heroes.common.item.ModItems.heroArmor[slot]));
        com.fiskmods.heroes.common.hero.HeroTracker.update(player);
        data.getData().set(com.fiskmods.heroes.common.data.var.Vars.SCALE, 1.0F);
        var action = hero.getKeyBindFuncs().get("func_GIANT_MODE");
        if (action == null) { helper.fail("Ant-Man's giant mode key is missing"); return; }
        action.call(new com.fiskmods.heroes.pack.js.JSEntity(player), new com.fiskmods.heroes.pack.js.JSManager());
        for (int i = 0; i < 12; ++i)
        {
            com.fiskmods.heroes.common.hero.ability.ModifierHandler.tick(player);
            com.fiskmods.heroes.common.hero.ability.PlayerSizeHandler.refreshIfChanged(player);
        }
        var vars = data.getData();
        if (Math.abs(vars.get(com.fiskmods.heroes.common.data.var.Vars.SCALE) - 5.0F) > 0.001F
                || vars.get(com.fiskmods.heroes.common.data.var.Vars.SIZE_STATE) != 0
                || Math.abs(player.getBbWidth() - 3.0F) > 0.001F
                || Math.abs(vars.get(com.fiskmods.heroes.common.data.var.Vars.GIANT_MODE_TIMER) - 1.0F) > 0.001F)
        { helper.fail("Giant-mode growth: scale=" + vars.get(com.fiskmods.heroes.common.data.var.Vars.SCALE)
                + ", state=" + vars.get(com.fiskmods.heroes.common.data.var.Vars.SIZE_STATE)
                + ", width=" + player.getBbWidth() + ", timer="
                + vars.get(com.fiskmods.heroes.common.data.var.Vars.GIANT_MODE_TIMER)); return; }
        action.call(new com.fiskmods.heroes.pack.js.JSEntity(player), new com.fiskmods.heroes.pack.js.JSManager());
        for (int i = 0; i < 12; ++i)
        {
            com.fiskmods.heroes.common.hero.ability.ModifierHandler.tick(player);
            com.fiskmods.heroes.common.hero.ability.PlayerSizeHandler.refreshIfChanged(player);
        }
        if (Math.abs(vars.get(com.fiskmods.heroes.common.data.var.Vars.SCALE) - 1.0F) > 0.001F
                || vars.get(com.fiskmods.heroes.common.data.var.Vars.SIZE_STATE) != 0
                || Math.abs(player.getBbWidth() - 0.6F) > 0.001F)
        { helper.fail("Disabling giant mode must restore the normal size and collision width"); return; }
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void sizeManipulationChangesPlayerDimensions(GameTestHelper helper)
    {
        var player = helper.makeMockPlayer();
        var data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(player);
        var hero = com.fiskmods.heroes.common.hero.Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:ant_man");
        if (data == null || hero == null) { helper.fail("Ant-Man data missing"); return; }
        data.setHero(hero.getDefaultIteration());
        var vars = data.getData();
        vars.set(com.fiskmods.heroes.common.data.var.Vars.SCALE, 1.0F);
        var entry = hero.getPowerContainer().getEntries().stream()
                .filter(e -> e.getModifier().getId().equals(FiskHeroes.id("size_manipulation")) && e.getCard() != null && e.getCard().endsWith("|small"))
                .findFirst().orElse(null);
        if (entry == null || !entry.isModifierEnabled(player, data)) { helper.fail("Small-size card missing"); return; }
        float initialWidth = player.getBbWidth();
        float initialEye = player.getEyeHeight();
        entry.getModifier().onActivate(player, entry, data);
        for (int i = 0; i < 8; ++i)
        {
            entry.getModifier().tick(player, entry, data);
            com.fiskmods.heroes.common.hero.ability.PlayerSizeHandler.refreshIfChanged(player);
        }
        if (Math.abs(vars.get(com.fiskmods.heroes.common.data.var.Vars.SCALE) - 0.0625F) > 0.001F
                || vars.get(com.fiskmods.heroes.common.data.var.Vars.SIZE_STATE) != 0
                || Math.abs(player.getBbWidth() - initialWidth * 0.0625F) > 0.001F
                || Math.abs(player.getEyeHeight() - initialEye * 0.0625F) > 0.001F)
        { helper.fail("Shrink must finish and update both collision width and eye height"); return; }
        // Removing the suit must undo the scale even though SCALE is a persisted data variable.
        com.fiskmods.heroes.common.hero.HeroTracker.update(player, false);
        com.fiskmods.heroes.common.hero.ability.PlayerSizeHandler.refreshIfChanged(player);
        if (vars.get(com.fiskmods.heroes.common.data.var.Vars.SCALE) != 1.0F
                || Math.abs(player.getBbWidth() - initialWidth) > 0.001F)
        { helper.fail("Removing the suit must restore scale and collision dimensions"); return; }
        helper.succeed();
    }

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
            new PacketInput(true, false, 100, -50, true).encode(encoded);
            new PacketInput(encoded).encode(reencoded);
            if (!reencoded.readBoolean() || reencoded.readBoolean()
                    || reencoded.readByte() != 1 || reencoded.readByte() != -1
                    || !reencoded.readBoolean() || reencoded.readableBytes() != 0)
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
    public static void fallResistanceUsesDistanceAndFlightRules(GameTestHelper helper)
    {
        var player = helper.makeMockPlayer();
        var data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(player);
        var flash = com.fiskmods.heroes.common.hero.Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:the_flash");
        var vision = com.fiskmods.heroes.common.hero.Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:vision");
        var obsidian = com.fiskmods.heroes.common.hero.Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:obsidian");
        if (data == null || flash == null || vision == null || obsidian == null)
        { helper.fail("Fall test heroes or player data missing"); return; }
        data.setHero(flash.getDefaultIteration());
        com.fiskmods.heroes.common.hero.attribute.SHAttributes.applyModifiers(player);
        var fall = new net.minecraftforge.event.entity.living.LivingFallEvent(player, 20.0F, 1.0F);
        com.fiskmods.heroes.common.event.CommonEventHandler.onLivingFall(fall);
        if (fall.isCanceled() || Math.abs(fall.getDistance() - 16.0F) > 0.001F || fall.getDamageMultiplier() != 1.0F)
        { helper.fail("The Flash's flat four-block resistance must subtract four from fall distance"); return; }
        data.setHero(vision.getDefaultIteration());
        com.fiskmods.heroes.common.hero.attribute.SHAttributes.applyModifiers(player);
        fall = new net.minecraftforge.event.entity.living.LivingFallEvent(player, 100.0F, 1.0F);
        com.fiskmods.heroes.common.event.CommonEventHandler.onLivingFall(fall);
        if (fall.isCanceled() || Math.abs(fall.getDistance()) > 0.001F)
        { helper.fail("Vision's multiplicative resistance must negate the fall distance"); return; }
        data.setHero(obsidian.getDefaultIteration());
        com.fiskmods.heroes.common.hero.attribute.SHAttributes.applyModifiers(player);
        data.getData().set(com.fiskmods.heroes.common.data.var.Vars.SHADOWFORM, false);
        fall = new net.minecraftforge.event.entity.living.LivingFallEvent(player, 20.0F, 1.0F);
        com.fiskmods.heroes.common.event.CommonEventHandler.onLivingFall(fall);
        if (fall.isCanceled() || Math.abs(fall.getDistance() - 16.0F) > 0.001F)
        { helper.fail("Disabled flight must not grant fall immunity"); return; }
        data.getData().set(com.fiskmods.heroes.common.data.var.Vars.SHADOWFORM, true);
        fall = new net.minecraftforge.event.entity.living.LivingFallEvent(player, 20.0F, 1.0F);
        com.fiskmods.heroes.common.event.CommonEventHandler.onLivingFall(fall);
        if (!fall.isCanceled()) { helper.fail("Enabled flight must cancel fall damage"); return; }
        data.setHero(flash.getDefaultIteration());
        com.fiskmods.heroes.common.hero.attribute.SHAttributes.applyModifiers(player);
        data.getData().set(com.fiskmods.heroes.common.data.var.Vars.SCALE, 2.0F);
        fall = new net.minecraftforge.event.entity.living.LivingFallEvent(player, 20.0F, 1.0F);
        com.fiskmods.heroes.common.event.CommonEventHandler.onLivingFall(fall);
        if (Math.abs(fall.getDistance() - 8.0F) > 0.001F)
            helper.fail("Giant scale must divide the distance remaining after resistance");
        else helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void jumpingUsesHeroHeightAndActiveLeapCard(GameTestHelper helper)
    {
        var player = helper.makeMockPlayer();
        var data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(player);
        var hero = com.fiskmods.heroes.common.hero.Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:spider_man");
        if (data == null || hero == null) { helper.fail("Spider-Man or player data missing"); return; }
        data.setHero(hero.getDefaultIteration());
        com.fiskmods.heroes.common.hero.attribute.SHAttributes.applyModifiers(player);
        data.getData().set(com.fiskmods.heroes.common.data.var.Vars.TICKS_SINCE_SWINGING, (short) 100);
        player.setYRot(0);
        player.setSprinting(false);
        player.setDeltaMovement(0, 0.42D, 0);
        com.fiskmods.heroes.common.hero.ability.ModifierHandler.onJump(player);
        if (Math.abs(player.getDeltaMovement().y - 0.795D) > 0.001D || player.getDeltaMovement().horizontalDistanceSqr() > 1e-8)
        { helper.fail("Jump height applies even without sprinting, but sprint leap must not"); return; }
        player.setSprinting(true);
        com.fiskmods.heroes.common.data.PlayerInputTracker.set(player, false, false, 1, 0);
        player.setDeltaMovement(0, 0.42D, 0);
        com.fiskmods.heroes.common.hero.ability.ModifierHandler.onJump(player);
        if (Math.abs(player.getDeltaMovement().y - 1.045D) > 0.001D
                || Math.abs(player.getDeltaMovement().z - 1.0D) > 0.001D)
        { helper.fail("A sprinting normal leap must use the configured forward/up pair"); return; }
        data.getData().set(com.fiskmods.heroes.common.data.var.Vars.TICKS_SINCE_SWINGING, (short) 0);
        player.setDeltaMovement(0, 0.42D, 0);
        com.fiskmods.heroes.common.hero.ability.ModifierHandler.onJump(player);
        if (Math.abs(player.getDeltaMovement().y - 1.545D) > 0.001D
                || Math.abs(player.getDeltaMovement().z - 2.0D) > 0.001D
                || data.getData().get(com.fiskmods.heroes.common.data.var.Vars.TICKS_SINCE_SWINGING) != 5)
        { helper.fail("Post-swing jump must select only the springboard card and consume its window"); return; }
        player.setDeltaMovement(0, 0.42D, 0);
        com.fiskmods.heroes.common.hero.ability.ModifierHandler.onJump(player);
        com.fiskmods.heroes.common.data.PlayerInputTracker.clear(player);
        if (Math.abs(player.getDeltaMovement().y - 1.045D) > 0.001D
                || Math.abs(player.getDeltaMovement().z - 1.0D) > 0.001D)
            helper.fail("The springboard must not repeat after its window was consumed");
        else helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void swingTimerTracksAttachmentAndLanding(GameTestHelper helper)
    {
        var player = helper.makeMockPlayer();
        var data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(player);
        var hero = com.fiskmods.heroes.common.hero.Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:spider_man");
        if (data == null || hero == null) { helper.fail("Spider-Man or player data missing"); return; }
        data.setHero(hero.getDefaultIteration());
        var timer = com.fiskmods.heroes.common.data.var.Vars.TICKS_SINCE_SWINGING;
        if (data.getData().get(timer) != Short.MAX_VALUE)
        { helper.fail("An unused web swing must not start in the springboard window"); return; }
        player.setOnGround(false);
        com.fiskmods.heroes.common.hero.ability.WebSwingHandler.updateSwingTimer(player, data, true);
        com.fiskmods.heroes.common.hero.ability.WebSwingHandler.updateSwingTimer(player, data, false);
        if (data.getData().get(timer) != 0)
        { helper.fail("A released swing remains recent until landing or jumping"); return; }
        player.setOnGround(true);
        for (int i = 0; i < 5; ++i)
            com.fiskmods.heroes.common.hero.ability.WebSwingHandler.updateSwingTimer(player, data, false);
        if (data.getData().get(timer) != 5) { helper.fail("Landing must advance the springboard window"); return; }
        data.getData().set(timer, (short) 0);
        // Integrated clients and servers share PlayerInputTracker's static map.
        com.fiskmods.heroes.common.data.PlayerInputTracker.set(player, true, false, 0, 0);
        PacketInput.applyServerInput(player, true, false, 0, 0, false);
        if (data.getData().get(timer) != 5)
        { helper.fail("A server-side jump press must consume the springboard window even in singleplayer"); return; }
        data.getData().set(timer, (short) 0);
        PacketInput.applyServerInput(player, true, false, 0, 0, false);
        if (data.getData().get(timer) != 0)
        { helper.fail("Holding jump must not repeatedly consume the springboard window"); return; }
        PacketInput.applyServerInput(player, false, false, 0, 0, false);
        PacketInput.applyServerInput(player, true, false, 0, 0, false);
        if (data.getData().get(timer) != 5)
        { helper.fail("A subsequent jump press must consume a new springboard window"); return; }
        PacketInput.clearServerState(player);
        com.fiskmods.heroes.common.data.PlayerInputTracker.clear(player);
        data.setHero(null);
        com.fiskmods.heroes.common.hero.ability.WebSwingHandler.updateSwingTimer(player, data, false);
        if (data.getData().get(timer) != Short.MAX_VALUE) helper.fail("Taking off the web-swing suit must clear its springboard state");
        else helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void waterBreathingReplenishesAirWithoutCreatingPotion(GameTestHelper helper)
    {
        var player = helper.makeMockPlayer();
        var data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(player);
        var modifier = com.fiskmods.heroes.common.hero.modifier.Modifiers.WATER_BREATHING;
        var entry = new com.fiskmods.heroes.common.hero.power.ModifierEntry(modifier, null);
        player.setAirSupply(7);
        modifier.tick(player, entry, data);
        if (player.getAirSupply() != player.getMaxAirSupply()
                || player.hasEffect(net.minecraft.world.effect.MobEffects.WATER_BREATHING))
        { helper.fail("The suit must restore air without manufacturing a potion effect"); return; }
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.WATER_BREATHING, 5, 1));
        player.setAirSupply(-5);
        modifier.tick(player, entry, data);
        var external = player.getEffect(net.minecraft.world.effect.MobEffects.WATER_BREATHING);
        if (player.getAirSupply() != player.getMaxAirSupply() || external == null
                || external.getDuration() != 5 || external.getAmplifier() != 1)
            helper.fail("The suit must preserve independent water-breathing potions without extending them");
        else helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void velocityNineDisintegratesTheUserNotTheTerrain(GameTestHelper helper)
    {
        var player = helper.makeMockPlayer();
        var data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(player);
        var hero = com.fiskmods.heroes.common.hero.Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:trajectory");
        if (data == null || hero == null) { helper.fail("Trajectory or player data missing"); return; }
        var entry = hero.getPowerContainer().getEntry(FiskHeroes.id("speed_disintegration"));
        if (entry == null || entry.getFloat(player, com.fiskmods.heroes.common.hero.power.PowerProperty.FACTOR) != 30.0F)
        { helper.fail("Velocity Nine must load its 30 metre disintegration factor"); return; }
        data.setHero(hero.getDefaultIteration());
        data.getData().set(com.fiskmods.heroes.common.data.var.Vars.SPEEDING, true);
        data.getData().set(com.fiskmods.heroes.common.data.var.Vars.SPEED, (byte) 5);
        if (!entry.isModifierEnabled(player, data))
        { helper.fail("Trajectory's disintegration must activate at high speed"); return; }
        player.getAbilities().invulnerable = false;
        player.setHealth(20.0F);
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST,
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_CHESTPLATE));
        var block = new net.minecraft.core.BlockPos(1, 1, 1);
        helper.setBlock(block, net.minecraft.world.level.block.Blocks.OAK_PLANKS);
        var pos = helper.absolutePos(block);
        player.setPos(pos.getX(), pos.getY(), pos.getZ());
        player.setDeltaMovement(1.0D, 0.25D, 0.5D);
        var motion = player.getDeltaMovement();
        var mod = entry.getModifier();
        mod.tick(player, entry, data);
        for (int i = 0; i < 3; ++i)
        {
            ++player.tickCount;
            player.setPos(player.getX() + 11.0D, player.getY(), player.getZ());
            mod.tick(player, entry, data);
        }
        if (Math.abs(player.getHealth() - 19.0F) > 0.001F || !player.getDeltaMovement().equals(motion))
        { helper.fail("Fast movement must inflict armor-bypassing self-damage without changing motion"); return; }
        if (!helper.getLevel().getBlockState(helper.absolutePos(block)).is(net.minecraft.world.level.block.Blocks.OAK_PLANKS))
        { helper.fail("Speed disintegration must not destroy nearby blocks"); return; }
        player.tickCount += 2;
        player.setPos(player.getX() + 100.0D, player.getY(), player.getZ());
        mod.tick(player, entry, data);
        if (Math.abs(player.getHealth() - 19.0F) > 0.001F)
        { helper.fail("Movement while the modifier was inactive must not be charged later"); return; }
        data.getData().set(com.fiskmods.heroes.common.data.var.Vars.SPEED, (byte) 4);
        if (entry.isModifierEnabled(player, data)) helper.fail("Trajectory's lower speeds must not disintegrate the user");
        else helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void projectileDurabilityOnlyBlocksBulletsAndArrows(GameTestHelper helper)
    {
        var player = helper.makeMockPlayer();
        var attacker = helper.makeMockPlayer();
        var modifier = com.fiskmods.heroes.common.hero.modifier.Modifiers.PROJECTILE_IMMUNITY;
        var entry = new com.fiskmods.heroes.common.hero.power.ModifierEntry(modifier, null);
        entry.setProperty(com.fiskmods.heroes.common.hero.power.PowerProperty.IS_ABSOLUTE, true);
        var arrow = new net.minecraft.world.entity.projectile.Arrow(helper.getLevel(), attacker);
        var snowball = new net.minecraft.world.entity.projectile.Snowball(helper.getLevel(), attacker);
        var piercing = new com.fiskmods.heroes.common.entity.arrow.TrickArrowEntity(
                com.fiskmods.heroes.common.entity.ModEntities.TRICK_ARROW.get(), helper.getLevel(), attacker, "vibranium");
        if (!modifier.isImmuneTo(player, entry, player.damageSources().arrow(arrow, attacker), 8)
                || modifier.isImmuneTo(player, entry, player.damageSources().arrow(piercing, attacker), 8)
                || modifier.isImmuneTo(player, entry, player.damageSources().thrown(snowball, attacker), 8)
                || modifier.isImmuneTo(player, entry, player.damageSources().lava(), 8)
                || modifier.isImmuneTo(player, entry, player.damageSources().explosion(attacker, attacker), 8))
        { helper.fail("Durability must deflect arrows but not piercing arrows, other projectiles or environmental damage"); return; }
        boolean[] blocked = { false };
        com.fiskmods.heroes.common.hero.modifier.DamageGroups.withDamageProfile(java.util.Map.of("BULLET", 1.0D),
                () -> blocked[0] = modifier.isImmuneTo(player, entry, player.damageSources().generic(), 8));
        if (!blocked[0]) helper.fail("Durability must deflect bullet-typed hits even without a vanilla projectile entity");
        else helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void arrowCatchingWaitsForAHit(GameTestHelper helper)
    {
        var player = helper.makeMockPlayer();
        var attacker = helper.makeMockPlayer();
        var modifier = com.fiskmods.heroes.common.hero.modifier.Modifiers.ARROW_CATCHING;
        var entry = new com.fiskmods.heroes.common.hero.power.ModifierEntry(modifier, null);
        var arrow = new net.minecraft.world.entity.projectile.Arrow(helper.getLevel(), attacker);
        var pos = player.getEyePosition().add(player.getLookAngle());
        arrow.setPos(pos.x, pos.y - arrow.getBbHeight() * 0.5D, pos.z);
        helper.getLevel().addFreshEntity(arrow);
        modifier.tick(player, entry, com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(player));
        if (arrow.isRemoved() || !player.getMainHandItem().isEmpty())
        { helper.fail("An arrow merely passing nearby must not be caught"); return; }
        if (!modifier.isImmuneTo(player, entry, player.damageSources().arrow(arrow, attacker), 8)
                || !arrow.isRemoved() || !player.getMainHandItem().is(net.minecraft.world.item.Items.ARROW))
            helper.fail("A looked-at arrow must be caught on impact when the main hand is empty");
        else helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void shieldControlDistinguishesToggleAndHold(GameTestHelper helper)
    {
        var player = helper.makeMockPlayer();
        var data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(player);
        if (data == null) { helper.fail("Player data missing"); return; }
        var modifier = com.fiskmods.heroes.common.hero.modifier.Modifiers.SHIELD;
        var entry = new com.fiskmods.heroes.common.hero.power.ModifierEntry(modifier, null);
        var shield = com.fiskmods.heroes.common.data.var.Vars.SHIELD;
        var blocking = com.fiskmods.heroes.common.data.var.Vars.SHIELD_BLOCKING;
        modifier.onActivate(player, entry, data);
        modifier.tick(player, entry, data);
        if (!data.getData().get(shield) || data.getData().get(blocking))
        { helper.fail("A deployed toggle shield must wait for the use key before blocking"); return; }
        com.fiskmods.heroes.common.data.PlayerInputTracker.set(player, false, false, 0, 0, true);
        modifier.tick(player, entry, data);
        if (!data.getData().get(blocking)) { helper.fail("Use key must raise the deployed shield"); return; }
        com.fiskmods.heroes.common.data.PlayerInputTracker.set(player, false, false, 0, 0, false);
        modifier.tick(player, entry, data);
        if (data.getData().get(blocking) || !data.getData().get(shield))
        { helper.fail("Releasing use must stop blocking without retracting a toggle shield"); return; }
        modifier.onActivate(player, entry, data);
        entry.setProperty(com.fiskmods.heroes.common.hero.power.PowerProperty.IS_TOGGLE, false);
        modifier.onActivate(player, entry, data);
        modifier.tick(player, entry, data);
        if (!data.getData().get(shield) || !data.getData().get(blocking))
        { helper.fail("A hold shield must block while its ability key is held"); return; }
        modifier.onToggle(player, entry, data);
        modifier.tick(player, entry, data);
        com.fiskmods.heroes.common.data.PlayerInputTracker.clear(player);
        if (data.getData().get(shield) || data.getData().get(blocking))
            helper.fail("Releasing a hold shield must retract it");
        else helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void suitShieldUsesCoverageHealthAndRecovery(GameTestHelper helper)
    {
        var player = helper.makeMockPlayer();
        var attacker = helper.makeMockPlayer();
        var data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(player);
        if (data == null) { helper.fail("Player data missing"); return; }
        var vars = data.getData();
        var modifier = com.fiskmods.heroes.common.hero.modifier.Modifiers.SHIELD;
        var entry = new com.fiskmods.heroes.common.hero.power.ModifierEntry(modifier, null);
        var shield = com.google.gson.JsonParser.parseString("""
                {"health":10,"regeneration":20,"recoveryDelay":2,"cooldown":3}
                """);
        entry.setProperty(com.fiskmods.heroes.common.hero.power.PowerProperty.SHIELD, shield);
        player.setYRot(0);
        player.setXRot(0);
        attacker.setPos(player.getX(), player.getEyeY() - attacker.getBbHeight() * 0.5D, player.getZ() + 2);
        var source = player.damageSources().playerAttack(attacker);
        vars.set(com.fiskmods.heroes.common.data.var.Vars.SHIELD, true);
        vars.set(com.fiskmods.heroes.common.data.var.Vars.SHIELD_BLOCKING, true);
        if (modifier.isImmuneTo(player, entry, player.damageSources().lava(), 4))
        { helper.fail("A shield must not block sourceless environmental damage"); return; }
        attacker.setPos(player.getX(), attacker.getY(), player.getZ() - 2);
        if (modifier.isImmuneTo(player, entry, source, 4))
        { helper.fail("A frontal shield must not block attacks from behind"); return; }
        attacker.setPos(player.getX(), attacker.getY(), player.getZ() + 2);
        if (!modifier.isImmuneTo(player, entry, source, 4)
                || vars.get(com.fiskmods.heroes.common.data.var.Vars.SHIELD_DAMAGE) != 4.0F)
        { helper.fail("A frontal hit must be absorbed and accumulate damage"); return; }
        if (modifier.isImmuneTo(player, entry, source, 6)
                || vars.get(com.fiskmods.heroes.common.data.var.Vars.SHIELD_COOLDOWN) != 3)
        { helper.fail("A hit that breaks the shield must pass through and start its cooldown"); return; }
        if (modifier.isImmuneTo(player, entry, source, 1))
        { helper.fail("A shield on cooldown must not block"); return; }
        for (int i = 0; i < 3; ++i)
            com.fiskmods.heroes.common.hero.modifier.ShieldRecovery.tickState(data, shield.getAsJsonObject());
        if (vars.get(com.fiskmods.heroes.common.data.var.Vars.SHIELD_COOLDOWN) != 0
                || vars.get(com.fiskmods.heroes.common.data.var.Vars.SHIELD_DAMAGE) != 10.0F)
        { helper.fail("Shield cooldown must decrement in ticks without regenerating during cooldown"); return; }
        com.fiskmods.heroes.common.hero.modifier.ShieldRecovery.tickState(data, shield.getAsJsonObject());
        if (Math.abs(vars.get(com.fiskmods.heroes.common.data.var.Vars.SHIELD_DAMAGE) - 9.0F) > 0.01F)
            helper.fail("Shield damage must recover at its configured per-second rate after the delay");
        else helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void shieldCooldownContinuesWithoutTheSuit(GameTestHelper helper)
    {
        var player = helper.makeMockPlayer();
        var data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(player);
        var hero = com.fiskmods.heroes.common.hero.Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:guardian");
        if (data == null || hero == null) { helper.fail("Guardian or player data missing"); return; }
        for (int slot = 0; slot < 4; ++slot)
        {
            var item = com.fiskmods.heroes.common.hero.ItemHeroArmor.create(hero.getDefaultIteration(),
                    com.fiskmods.heroes.common.item.ModItems.heroArmor[slot]);
            player.setItemSlot(new net.minecraft.world.entity.EquipmentSlot[] {
                    net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.entity.EquipmentSlot.CHEST,
                    net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET }[slot], item);
        }
        com.fiskmods.heroes.common.hero.HeroTracker.update(player);
        var vars = data.getData();
        vars.set(com.fiskmods.heroes.common.data.var.Vars.SHIELD_DAMAGE, 10.0F);
        vars.set(com.fiskmods.heroes.common.data.var.Vars.SHIELD_COOLDOWN, (short) 2);
        com.fiskmods.heroes.common.hero.ability.ModifierHandler.tick(player);
        if (vars.get(com.fiskmods.heroes.common.data.var.Vars.SHIELD_COOLDOWN) != 1)
        { helper.fail("A worn shield cooldown must tick exactly once"); return; }
        for (var slot : new net.minecraft.world.entity.EquipmentSlot[] {net.minecraft.world.entity.EquipmentSlot.HEAD,
                net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.LEGS,
                net.minecraft.world.entity.EquipmentSlot.FEET})
            player.setItemSlot(slot, net.minecraft.world.item.ItemStack.EMPTY);
        com.fiskmods.heroes.common.hero.ability.ModifierHandler.tick(player);
        if (data.getHero() != null || vars.get(com.fiskmods.heroes.common.data.var.Vars.SHIELD_COOLDOWN) != 0
                || vars.get(com.fiskmods.heroes.common.data.var.Vars.SHIELD_DAMAGE) != 10.0F)
        { helper.fail("Removing the suit must not freeze or skip the shield cooldown"); return; }
        com.fiskmods.heroes.common.hero.ability.ModifierHandler.tick(player);
        if (Math.abs(vars.get(com.fiskmods.heroes.common.data.var.Vars.SHIELD_DAMAGE) - 9.5F) > 0.001F)
            helper.fail("An inactive shield must recover with the default rate even without a suit");
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
    public static void suitInvisibilityPreservesPotionsAndEndsOnSuitChange(GameTestHelper helper)
    {
        var player = helper.makeMockPlayer();
        var data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(player);
        var martian = com.fiskmods.heroes.common.hero.Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:martian_manhunter");
        var flash = com.fiskmods.heroes.common.hero.Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:the_flash");
        if (data == null || martian == null || flash == null)
        { helper.fail("Invisibility test heroes or player capability missing"); return; }
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST,
                com.fiskmods.heroes.common.hero.ItemHeroArmor.create(martian.getDefaultIteration(),
                        com.fiskmods.heroes.common.item.ModItems.heroArmor[1]));
        com.fiskmods.heroes.common.hero.HeroTracker.update(player, false);
        var entry = martian.getPowerContainer().getEntry(FiskHeroes.id("invisibility"));
        if (entry == null) { helper.fail("Martian invisibility modifier missing"); return; }
        var modifier = entry.getModifier();
        var timer = com.fiskmods.heroes.common.data.var.Vars.INVISIBILITY_TIMER;
        modifier.onActivate(player, entry, data);
        for (int i = 0; i < 11; ++i) modifier.tick(player, entry, data);
        if (!player.isInvisible() || player.hasEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY)
                || data.getData().get(timer) < 0.99F)
        { helper.fail("Suit invisibility must not create a potion and must animate its fade"); return; }
        modifier.onActivate(player, entry, data);
        modifier.tick(player, entry, data);
        if (player.isInvisible() || data.getData().get(timer) >= 1.0F)
        { helper.fail("Deactivating the suit must restore visibility without a potion delay"); return; }
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.INVISIBILITY, 200));
        modifier.onActivate(player, entry, data);
        modifier.tick(player, entry, data);
        modifier.onActivate(player, entry, data);
        if (!player.isInvisible() || player.getEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY).getDuration() != 200)
        { helper.fail("Disabling suit invisibility must preserve an independent potion"); return; }
        player.removeEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY);
        player.setInvisible(false);
        modifier.onActivate(player, entry, data);
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST,
                com.fiskmods.heroes.common.hero.ItemHeroArmor.create(flash.getDefaultIteration(),
                        com.fiskmods.heroes.common.item.ModItems.heroArmor[1]));
        com.fiskmods.heroes.common.hero.HeroTracker.update(player, false);
        if (player.isInvisible() || data.getData().get(com.fiskmods.heroes.common.data.var.Vars.INVISIBLE)
                || data.getData().get(timer) != 0.0F || data.isToggleEnabled(FiskHeroes.id("invisibility")))
            helper.fail("Switching to a suit without invisibility must clean up its state");
        else helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void soundDefinitionsResolveToAudioEvents(GameTestHelper helper)
    {
        var oneShot = com.fiskmods.heroes.common.sound.SHSounds.get(FiskHeroes.id("vision_intang_on"));
        var loop = com.fiskmods.heroes.common.sound.SHSounds.get(FiskHeroes.id("flight_cape_loop"));
        var loopDef = com.fiskmods.heroes.common.sound.SHSounds.getDefinition(FiskHeroes.id("flight_cape_loop"));
        if (oneShot == null || !oneShot.getLocation().equals(FiskHeroes.id("suit.vision.intangibility.enable"))
                || loop == null || !loop.getLocation().equals(FiskHeroes.id("modifier.flight.cape.loop"))
                || loopDef == null || !loopDef.isLoop())
            helper.fail("Sound definitions must resolve to their audio event, preserving loop metadata");
        else helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void soundManifestRetainsAliasesAndVariants(GameTestHelper helper)
    {
        var original = com.google.gson.JsonParser.parseString("""
                {"ambient.flight":{"category":"ambient","sounds":["ambient/flight_loop","ambient/flight_loop2"]},
                 "nexus.ignite":{"sounds":[{"name":"minecraft:mob/ghast/fireball4","pitch":1.1}]}}
                """).getAsJsonObject();
        var converted = com.fiskmods.heroes.common.sound.SoundManifest.normalize(original);
        var flight = converted.getAsJsonObject("ambient.flight").getAsJsonArray("sounds");
        var vanilla = converted.getAsJsonObject("nexus.ignite").getAsJsonArray("sounds").get(0).getAsJsonObject();
        if (flight.size() != 2 || !flight.get(0).getAsJsonObject().get("name").getAsString().equals("fiskheroes:ambient/flight_loop")
                || !flight.get(1).getAsJsonObject().get("name").getAsString().equals("fiskheroes:ambient/flight_loop2")
                || !vanilla.get("name").getAsString().equals("minecraft:entity.ghast.shoot")
                || !vanilla.get("type").getAsString().equals("event")
                || Math.abs(vanilla.get("pitch").getAsFloat() - 1.1F) > 0.001F
                || !original.getAsJsonObject("ambient.flight").getAsJsonArray("sounds").get(0).isJsonPrimitive())
            helper.fail("Original sound aliases, variants, vanilla events and pitch must survive conversion");
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
    public static void mysterioAimUnlocksHeldBeamAndRespectsRelease(GameTestHelper helper)
    {
        var player = new net.minecraftforge.common.util.FakePlayer(helper.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "fisk-aim-test"));
        var hero = com.fiskmods.heroes.common.hero.Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:mysterio");
        var data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(player);
        if (hero == null || data == null) { helper.fail("Mysterio or player data is missing"); return; }
        var slots = new net.minecraft.world.entity.EquipmentSlot[] {net.minecraft.world.entity.EquipmentSlot.HEAD,
                net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.LEGS,
                net.minecraft.world.entity.EquipmentSlot.FEET};
        for (int i = 0; i < 4; ++i)
            if (hero.getDefaultIteration().hasPieceOfSet(i)) player.setItemSlot(slots[i],
                    com.fiskmods.heroes.common.hero.ItemHeroArmor.create(hero.getDefaultIteration(),
                            com.fiskmods.heroes.common.item.ModItems.heroArmor[i]));
        com.fiskmods.heroes.common.hero.HeroTracker.update(player);
        var vars = data.getData();
        vars.set(com.fiskmods.heroes.common.data.var.Vars.TIME_SINCE_DAMAGED, (short) 20);
        com.fiskmods.heroes.common.hero.ability.AbilityHandler.onAbilityKey(player, 2, true);
        com.fiskmods.heroes.common.hero.ability.AbilityHandler.tickAiming(player, data);
        com.fiskmods.heroes.common.hero.ability.AbilityHandler.tickHeldAbilities(player);
        com.fiskmods.heroes.common.hero.ability.AbilityHandler.onAbilityKey(player, 2, false);
        if (vars.get(com.fiskmods.heroes.common.data.var.Vars.ENERGY_PROJECTION))
        { helper.fail("Releasing before aim settles must not bypass the beam predicate"); return; }
        for (int i = 0; i < 20; ++i)
            com.fiskmods.heroes.common.hero.ability.AbilityHandler.tickAiming(player, data);
        com.fiskmods.heroes.common.hero.ability.AbilityHandler.onAbilityKey(player, 2, true);
        for (int i = 0; i < 20; ++i)
        {
            com.fiskmods.heroes.common.hero.ability.AbilityHandler.tickAiming(player, data);
            com.fiskmods.heroes.common.hero.ability.AbilityHandler.tickHeldAbilities(player);
        }
        if (vars.get(com.fiskmods.heroes.common.data.var.Vars.AIMED_TIMER) != 1.0F
                || !vars.get(com.fiskmods.heroes.common.data.var.Vars.ENERGY_PROJECTION))
        { helper.fail("Held aim must reach its second phase and activate Mysterio's beam"); return; }
        com.fiskmods.heroes.common.hero.ability.AbilityHandler.onAbilityKey(player, 2, false);
        for (int i = 0; i < 20; ++i)
            com.fiskmods.heroes.common.hero.ability.AbilityHandler.tickAiming(player, data);
        if (vars.get(com.fiskmods.heroes.common.data.var.Vars.ENERGY_PROJECTION)
                || vars.get(com.fiskmods.heroes.common.data.var.Vars.AIMED_TIMER) != 0.0F)
        { helper.fail("Release must stop the beam and reset the aimed timer"); return; }
        com.fiskmods.heroes.common.hero.ability.AbilityHandler.clear(player);
        helper.succeed();
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
