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
    public static void tonfaSwingUsesIndependentPhysicalHandBits(GameTestHelper helper)
    {
        var player = new net.minecraftforge.common.util.FakePlayer(helper.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "tonfa-test"));
        var data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(player);
        if (data == null) { helper.fail("Player data missing"); return; }
        var item = com.fiskmods.heroes.common.item.ModItems.TACTICAL_TONFA.get();
        var main = new net.minecraft.world.item.ItemStack(item);
        var off = new net.minecraft.world.item.ItemStack(item);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, main);
        player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, off);
        item.onEntitySwing(main, player);
        byte right = data.getData().get(com.fiskmods.heroes.common.data.var.Vars.TONFA_STATE);
        item.onEntitySwing(off, player);
        byte both = data.getData().get(com.fiskmods.heroes.common.data.var.Vars.TONFA_STATE);
        if (right != 2 || both != 3)
        { helper.fail("Tonfa swings must flip the right and left bits independently"); return; }
        var receiver = new com.fiskmods.heroes.common.data.var.DataContainer();
        receiver.readUpdate(data.getData().writeDirty());
        if (receiver.get(com.fiskmods.heroes.common.data.var.Vars.TONFA_STATE) != 3)
        { helper.fail("Tonfa state must be included in dirty synchronization"); return; }
        player.setMainArm(net.minecraft.world.entity.HumanoidArm.LEFT);
        data.getData().set(com.fiskmods.heroes.common.data.var.Vars.TONFA_STATE, (byte) 0);
        item.onEntitySwing(main, player);
        if (data.getData().get(com.fiskmods.heroes.common.data.var.Vars.TONFA_STATE) != 1)
        { helper.fail("A left-handed main-hand swing must flip the left tonfa"); return; }
        item.onEntitySwing(off, player);
        if (data.getData().get(com.fiskmods.heroes.common.data.var.Vars.TONFA_STATE) != 3)
        { helper.fail("A left-handed offhand swing must flip the right tonfa"); return; }
        float rightRest = com.fiskmods.heroes.common.util.TonfaPoseMath.rotation((byte) 0, false, 0, false);
        float rightEnd = com.fiskmods.heroes.common.util.TonfaPoseMath.rotation((byte) 2, false, 1, true);
        float leftRest = com.fiskmods.heroes.common.util.TonfaPoseMath.rotation((byte) 2, true, 0, false);
        if (Math.abs(rightRest + 181) > 0.001F || Math.abs(rightEnd + 1) > 0.001F || Math.abs(leftRest - 181) > 0.001F)
        { helper.fail("Tonfa flip curve must follow the correct independent hand bit"); return; }
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void staffSwingFollowsBoundedAttackCurve(GameTestHelper helper)
    {
        if (Math.abs(com.fiskmods.heroes.common.util.StaffSwingMath.amount(0.0F)) > 0.0001F
                || Math.abs(com.fiskmods.heroes.common.util.StaffSwingMath.amount(0.5F) - 1.0F) > 0.0001F
                || Math.abs(com.fiskmods.heroes.common.util.StaffSwingMath.amount(1.0F)) > 0.0001F
                || Math.abs(com.fiskmods.heroes.common.util.StaffSwingMath.amount(-2.0F)) > 0.0001F
                || Math.abs(com.fiskmods.heroes.common.util.StaffSwingMath.amount(2.0F)) > 0.0001F
                || com.fiskmods.heroes.common.util.StaffSwingMath.amount(Float.NaN) != 0.0F)
        { helper.fail("Staff swing must peak at mid-attack and settle at both ends"); return; }
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void flightKickGuardOnlyExemptsBoundedPostFlightDescent(GameTestHelper helper)
    {
        if (!com.fiskmods.heroes.common.hero.ability.FlightKickGuard.isRapidDescent(200, 198, false)
                || com.fiskmods.heroes.common.hero.ability.FlightKickGuard.isRapidDescent(200, 199.9, false)
                || com.fiskmods.heroes.common.hero.ability.FlightKickGuard.isRapidDescent(200, 200, false)
                || com.fiskmods.heroes.common.hero.ability.FlightKickGuard.isRapidDescent(200, 202, false)
                || com.fiskmods.heroes.common.hero.ability.FlightKickGuard.isRapidDescent(200, 198, true)
                || com.fiskmods.heroes.common.hero.ability.FlightKickGuard.isRapidDescent(Double.NaN, 198, false))
        { helper.fail("Only rapid observed freefall may bypass the floating counter without suit flight"); return; }
        var state = new com.fiskmods.heroes.common.hero.ability.FlightKickGuard.Descent(200.0D);
        if (!state.update(true, 200.0D, false) || state.update(false, 200.0D, false)
                || state.update(false, 201.0D, false) || !state.update(false, 200.5D, false))
        { helper.fail("Only active flight or real downward motion may reset the floating counter"); return; }
        double y = 200.5D;
        for (int i = 0; i < 80; ++i) state.update(false, y -= 0.5D, false);
        if (!state.isFinished() || state.update(false, y - 1.0D, false))
        { helper.fail("Post-flight descent exemption must expire"); return; }
        var landed = new com.fiskmods.heroes.common.hero.ability.FlightKickGuard.Descent(100.0D);
        landed.update(true, 100.0D, false);
        if (landed.update(false, 99.0D, true) || !landed.isFinished())
        { helper.fail("Landing or riding must end the descent exemption"); return; }
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void flightAndShadowStatesFollowSuitAndTimeout(GameTestHelper helper)
    {
        var player = new net.minecraftforge.common.util.FakePlayer(helper.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "fisk-flight-state-test"));
        var data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(player);
        var vision = com.fiskmods.heroes.common.hero.Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:vision");
        var obsidian = com.fiskmods.heroes.common.hero.Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:obsidian");
        var flash = com.fiskmods.heroes.common.hero.Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:the_flash");
        if (data == null || vision == null || obsidian == null || flash == null)
        { helper.fail("Expected test suits are missing"); return; }
        var slots = new net.minecraft.world.entity.EquipmentSlot[] {net.minecraft.world.entity.EquipmentSlot.HEAD,
                net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.LEGS,
                net.minecraft.world.entity.EquipmentSlot.FEET};
        java.util.function.Consumer<com.fiskmods.heroes.common.hero.Hero> equip = hero -> {
            for (int i = 0; i < 4; ++i)
                player.setItemSlot(slots[i], com.fiskmods.heroes.common.hero.ItemHeroArmor.create(
                        hero.getDefaultIteration(), com.fiskmods.heroes.common.item.ModItems.heroArmor[i]));
            com.fiskmods.heroes.common.hero.HeroTracker.update(player);
        };
        var vars = data.getData();
        equip.accept(vision);
        var intangibility = vision.getPowerContainer().getEntry(FiskHeroes.id("intangibility"));
        var flight = vision.getPowerContainer().getEntry(FiskHeroes.id("controlled_flight"));
        if (intangibility == null || flight == null) { helper.fail("Vision powers missing"); return; }
        intangibility.getModifier().onActivate(player, intangibility, data);
        vars.set(com.fiskmods.heroes.common.data.var.Vars.INTANGIBLE, false); // timed out
        intangibility.getModifier().onActivate(player, intangibility, data);
        if (!vars.get(com.fiskmods.heroes.common.data.var.Vars.INTANGIBLE))
        { helper.fail("A timed-out intangibility must reactivate on the next press"); return; }
        vars.set(com.fiskmods.heroes.common.data.var.Vars.FLYING, true);
        flight.setToggled(player, true);
        equip.accept(flash);
        if (vars.get(com.fiskmods.heroes.common.data.var.Vars.FLYING) || flight.isToggled(player))
        { helper.fail("Switching to a suit without controlled flight must clear flight state"); return; }
        equip.accept(obsidian);
        var shadow = obsidian.getPowerContainer().getEntry(FiskHeroes.id("shadowform"));
        if (shadow == null) { helper.fail("Shadowform power missing"); return; }
        shadow.getModifier().onActivate(player, shadow, data);
        vars.set(com.fiskmods.heroes.common.data.var.Vars.SHADOWFORM, false); // timed out
        shadow.getModifier().onActivate(player, shadow, data);
        if (!vars.get(com.fiskmods.heroes.common.data.var.Vars.SHADOWFORM))
        { helper.fail("A timed-out shadowform must reactivate on the next press"); return; }
        equip.accept(flash);
        if (vars.get(com.fiskmods.heroes.common.data.var.Vars.SHADOWFORM) || shadow.isToggled(player))
        { helper.fail("Switching to a suit without shadowform must clear its state"); return; }
        com.fiskmods.heroes.common.hero.ability.AbilityHandler.clear(player);
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void webSwingTargetsLivingEntitiesAndRespectsBlocks(GameTestHelper helper)
    {
        var level = helper.getLevel();
        var player = new net.minecraftforge.common.util.FakePlayer(level,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "fisk-web-target-test"));
        var data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(player);
        var spider = com.fiskmods.heroes.common.hero.Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:spider_man");
        if (data == null || spider == null) { helper.fail("Web swing test setup missing"); return; }
        data.setHero(spider.getDefaultIteration());
        var vars = data.getData();
        vars.set(com.fiskmods.heroes.common.data.var.Vars.WEB_SWINGING, true);
        vars.set(com.fiskmods.heroes.common.data.var.Vars.UTILITY_BELT_TYPE, (byte) -1);
        var base = helper.absolutePos(new net.minecraft.core.BlockPos(1, 80, 1));
        player.setPos(base.getX() + 0.5, base.getY(), base.getZ() + 0.5);
        player.setYRot(0);
        player.setXRot(0);
        var target = net.minecraft.world.entity.EntityType.IRON_GOLEM.create(level);
        if (target == null) { helper.fail("Unable to create web target"); return; }
        target.setNoAi(true);
        target.setPos(player.getX(), player.getY(), player.getZ() + 5);
        level.addFreshEntity(target);
        var wall = base.offset(0, 1, 8);
        level.setBlockAndUpdate(wall, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        player.hurtMarked = false;
        if (!com.fiskmods.heroes.common.hero.ability.WebSwingHandler.interact(player)
                || !"entity".equals(player.getPersistentData().getCompound("FiskHeroesWebSwingAnchor").getString("Type"))
                || !vars.get(com.fiskmods.heroes.common.data.var.Vars.WEB_RAPPEL) || !player.hurtMarked)
        { helper.fail("Web must target a living entity before a farther block and send its launch impulse"); return; }
        double initialX = vars.get(com.fiskmods.heroes.common.data.var.Vars.WEB_ANCHOR_X);
        target.setPos(target.getX() + 1.0, target.getY(), target.getZ());
        player.setPos(player.getX(), player.getY(), player.getZ() - 12);
        player.setDeltaMovement(0, 0, -1);
        player.hurtMarked = false;
        player.fallDistance = 12;
        com.fiskmods.heroes.common.hero.ability.WebSwingHandler.tick(player);
        if (Math.abs(vars.get(com.fiskmods.heroes.common.data.var.Vars.WEB_ANCHOR_X) - initialX - 1.0) > 0.001
                || !player.hurtMarked || player.fallDistance != 0)
        { helper.fail("Moving entity anchors and rope correction must sync to the client"); return; }
        com.fiskmods.heroes.common.hero.ability.WebSwingHandler.release(player);
        if (vars.get(com.fiskmods.heroes.common.data.var.Vars.WEB_RAPPEL))
        { helper.fail("Release must clear the tether"); return; }
        player.setPos(base.getX() + 0.5, base.getY(), base.getZ() + 0.5);
        target.setPos(player.getX(), player.getY(), player.getZ() + 5);
        var blocker = base.offset(0, 1, 3);
        level.setBlockAndUpdate(blocker, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        if (!com.fiskmods.heroes.common.hero.ability.WebSwingHandler.interact(player)
                || !"block".equals(player.getPersistentData().getCompound("FiskHeroesWebSwingAnchor").getString("Type"))
                || player.getPersistentData().getCompound("FiskHeroesWebSwingAnchor").getLong("Block") != blocker.asLong())
        { helper.fail("Web must hit the blocking wall before the entity"); return; }
        com.fiskmods.heroes.common.hero.ability.WebSwingHandler.release(player);
        level.removeBlock(blocker, false);
        level.removeBlock(wall, false);
        target.discard();
        if (com.fiskmods.heroes.common.hero.ability.WebSwingHandler.interact(player))
        { helper.fail("Web must not attach when there is no target in range"); return; }
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void wallCrawlingUsesSyncedInputAndReleasesContact(GameTestHelper helper)
    {
        var player = helper.makeMockPlayer();
        var data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(player);
        var spider = com.fiskmods.heroes.common.hero.Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:spider_man");
        var flash = com.fiskmods.heroes.common.hero.Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:the_flash");
        if (data == null || spider == null || flash == null)
        { helper.fail("Wall crawl setup missing"); return; }
        var slots = new net.minecraft.world.entity.EquipmentSlot[] {net.minecraft.world.entity.EquipmentSlot.HEAD,
                net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.LEGS,
                net.minecraft.world.entity.EquipmentSlot.FEET};
        for (int i = 0; i < 4; ++i)
            player.setItemSlot(slots[i], com.fiskmods.heroes.common.hero.ItemHeroArmor.create(
                    spider.getDefaultIteration(), com.fiskmods.heroes.common.item.ModItems.heroArmor[i]));
        com.fiskmods.heroes.common.hero.HeroTracker.update(player);
        var entry = spider.getPowerContainer().getEntry(FiskHeroes.id("wall_crawling"));
        if (entry == null) { helper.fail("Spider-Man wall crawl missing"); return; }
        helper.setBlock(new net.minecraft.core.BlockPos(2, 1, 1), net.minecraft.world.level.block.Blocks.STONE);
        helper.setBlock(new net.minecraft.core.BlockPos(2, 2, 1), net.minecraft.world.level.block.Blocks.STONE);
        var wall = helper.absolutePos(new net.minecraft.core.BlockPos(2, 1, 1));
        player.setPos(wall.getX() - 0.3D, wall.getY(), wall.getZ() + 0.5D);
        player.setOnGround(false);
        player.horizontalCollision = true;
        player.zza = 0;
        player.fallDistance = 9;
        com.fiskmods.heroes.common.data.PlayerInputTracker.set(player, false, false, 1, 0);
        entry.getModifier().tick(player, entry, data);
        if (Math.abs(player.getDeltaMovement().y - 0.2D) > 0.0001D || player.fallDistance != 0
                || data.getData().get(com.fiskmods.heroes.common.data.var.Vars.WALL_CRAWLING) != 1)
        { helper.fail("Server climb must use synchronized forward input on wall contact"); return; }
        if (!com.fiskmods.heroes.common.hero.ability.WallCrawlContact.hasActiveContact(player))
        { helper.fail("Actual crawl-capable wall contact should qualify for the floating guard"); return; }
        player.horizontalCollision = false;
        com.fiskmods.heroes.common.data.PlayerInputTracker.set(player, false, false, 0, 0);
        entry.getModifier().tick(player, entry, data);
        if (player.getDeltaMovement().y != 0.0D
                || data.getData().get(com.fiskmods.heroes.common.data.var.Vars.WALL_CRAWLING) != 1)
        { helper.fail("Standing still against the wall must retain contact"); return; }
        com.fiskmods.heroes.common.data.PlayerInputTracker.set(player, false, true, 0, 0);
        entry.getModifier().tick(player, entry, data);
        if (Math.abs(player.getDeltaMovement().y + 0.2D) > 0.0001D)
        { helper.fail("Synchronized sneak must climb down"); return; }
        player.setPos(wall.getX() - 1.0D, wall.getY(), wall.getZ() + 0.5D);
        entry.getModifier().tick(player, entry, data);
        if (data.getData().get(com.fiskmods.heroes.common.data.var.Vars.WALL_CRAWLING) != 0
                || com.fiskmods.heroes.common.hero.ability.WallCrawlContact.hasActiveContact(player))
        { helper.fail("Wall crawl and floating exemption must release away from collision geometry"); return; }
        player.setPos(wall.getX() - 0.3D, wall.getY(), wall.getZ() + 0.5D);
        data.getData().set(com.fiskmods.heroes.common.data.var.Vars.WALL_CRAWLING, (byte) 1);
        for (int i = 0; i < 4; ++i)
            player.setItemSlot(slots[i], com.fiskmods.heroes.common.hero.ItemHeroArmor.create(
                    flash.getDefaultIteration(), com.fiskmods.heroes.common.item.ModItems.heroArmor[i]));
        com.fiskmods.heroes.common.hero.HeroTracker.update(player);
        if (data.getData().get(com.fiskmods.heroes.common.data.var.Vars.WALL_CRAWLING) != 0
                || com.fiskmods.heroes.common.hero.ability.WallCrawlContact.hasActiveContact(player))
        { helper.fail("Changing suits must clear wall crawl and its floating exemption"); return; }
        com.fiskmods.heroes.common.data.PlayerInputTracker.clear(player);
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void shadowformInvisibilityPreservesPotionsAndEndsOnSuitChange(GameTestHelper helper)
    {
        var player = helper.makeMockPlayer();
        var data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(player);
        var obsidian = com.fiskmods.heroes.common.hero.Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:obsidian");
        var flash = com.fiskmods.heroes.common.hero.Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:the_flash");
        if (data == null || obsidian == null || flash == null)
        { helper.fail("Shadowform test setup missing"); return; }
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST,
                com.fiskmods.heroes.common.hero.ItemHeroArmor.create(obsidian.getDefaultIteration(),
                        com.fiskmods.heroes.common.item.ModItems.heroArmor[1]));
        com.fiskmods.heroes.common.hero.HeroTracker.update(player, false);
        var entry = obsidian.getPowerContainer().getEntry(FiskHeroes.id("shadowform"));
        if (entry == null) { helper.fail("Shadowform missing"); return; }
        var modifier = entry.getModifier();
        modifier.onActivate(player, entry, data);
        modifier.tick(player, entry, data);
        if (!player.isInvisible() || player.hasEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY))
        { helper.fail("Shadowform should set invisibility without creating a potion"); return; }
        modifier.onActivate(player, entry, data);
        if (player.isInvisible())
        { helper.fail("Leaving shadowform should restore visibility immediately"); return; }
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.INVISIBILITY, 200));
        modifier.onActivate(player, entry, data);
        modifier.tick(player, entry, data);
        modifier.onActivate(player, entry, data);
        var potion = player.getEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY);
        if (!player.isInvisible() || potion == null || potion.getDuration() != 200)
        { helper.fail("Shadowform must not overwrite or remove an independent invisibility potion"); return; }
        player.removeEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY);
        player.setInvisible(false);
        modifier.onActivate(player, entry, data);
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST,
                com.fiskmods.heroes.common.hero.ItemHeroArmor.create(flash.getDefaultIteration(),
                        com.fiskmods.heroes.common.item.ModItems.heroArmor[1]));
        com.fiskmods.heroes.common.hero.HeroTracker.update(player, false);
        if (player.isInvisible() || data.getData().get(com.fiskmods.heroes.common.data.var.Vars.SHADOWFORM))
        { helper.fail("Changing to a suit without shadowform must restore visibility"); return; }
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void shadowformTakeoffOnlyOccursWhenEnabledOnGround(GameTestHelper helper)
    {
        var player = new net.minecraftforge.common.util.FakePlayer(helper.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "fisk-shadow-takeoff"));
        var hero = com.fiskmods.heroes.common.hero.Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:obsidian");
        var data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(player);
        var entry = hero != null ? hero.getPowerContainer().getEntry(FiskHeroes.id("shadowform")) : null;
        if (data == null || entry == null) { helper.fail("Obsidian shadowform missing"); return; }
        player.setOnGround(true);
        player.setDeltaMovement(0.1D, -0.08D, 0.2D);
        player.hurtMarked = false;
        entry.getModifier().onActivate(player, entry, data);
        var motion = player.getDeltaMovement();
        if (Math.abs(motion.y - 0.47D) > 0.00001D || motion.x != 0.1D || motion.z != 0.2D || !player.hurtMarked)
        { helper.fail("Enabling grounded shadowform must add and synchronize one takeoff impulse"); return; }
        player.hurtMarked = false;
        entry.getModifier().onActivate(player, entry, data);
        if (Math.abs(player.getDeltaMovement().y - 0.47D) > 0.00001D || player.hurtMarked)
        { helper.fail("Disabling shadowform must not launch the player"); return; }
        player.setOnGround(false);
        player.setDeltaMovement(0.0D, -0.2D, 0.0D);
        entry.getModifier().onActivate(player, entry, data);
        if (Math.abs(player.getDeltaMovement().y + 0.2D) > 0.00001D || player.hurtMarked)
        { helper.fail("Enabling shadowform midair must not add a takeoff impulse"); return; }
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void passiveFlightUsesExtraGravityAndRespectsShadowform(GameTestHelper helper)
    {
        var player = new net.minecraftforge.common.util.FakePlayer(helper.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "fisk-passive-flight-test"));
        var hero = com.fiskmods.heroes.common.hero.Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:obsidian");
        var data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(player);
        var entry = hero != null ? hero.getPowerContainer().getEntry(FiskHeroes.id("flight")) : null;
        if (data == null || entry == null) { helper.fail("Obsidian passive flight missing"); return; }
        var slots = new net.minecraft.world.entity.EquipmentSlot[] {net.minecraft.world.entity.EquipmentSlot.HEAD,
                net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.LEGS,
                net.minecraft.world.entity.EquipmentSlot.FEET};
        for (int i = 0; i < 4; ++i)
            player.setItemSlot(slots[i], com.fiskmods.heroes.common.hero.ItemHeroArmor.create(
                    hero.getDefaultIteration(), com.fiskmods.heroes.common.item.ModItems.heroArmor[i]));
        com.fiskmods.heroes.common.hero.HeroTracker.update(player);
        var flightPos = helper.absolutePos(new net.minecraft.core.BlockPos(1, 80, 1));
        player.setPos(flightPos.getX() + 0.5D, flightPos.getY(), flightPos.getZ() + 0.5D);
        player.setOnGround(false);
        if (com.fiskmods.heroes.common.hero.modifier.ModifierFlight.hasActiveFlight(player))
        { helper.fail("Passive flight must not bypass the shadowform predicate"); return; }
        data.getData().set(com.fiskmods.heroes.common.data.var.Vars.SHADOWFORM, true);
        if (!com.fiskmods.heroes.common.hero.modifier.ModifierFlight.hasActiveFlight(player))
        { helper.fail("Active shadowform must enable passive flight"); return; }
        com.fiskmods.heroes.common.data.PlayerInputTracker.set(player, false, false, 0, 0);
        player.setDeltaMovement(0.0D, -0.3D, 0.0D);
        player.hurtMarked = false;
        entry.getModifier().tick(player, entry, data);
        if (Math.abs(player.getDeltaMovement().y - (-0.23D)) > 0.0001D || player.hurtMarked)
        { helper.fail("Passive flight must apply lift without reapplying vanilla gravity or velocity packets"); return; }
        com.fiskmods.heroes.common.data.PlayerInputTracker.set(player, true, false, 0, 0);
        player.setDeltaMovement(0.0D, -0.3D, 0.0D);
        entry.getModifier().tick(player, entry, data);
        if (Math.abs(player.getDeltaMovement().y - (-0.105D)) > 0.0001D)
        { helper.fail("Jump input must add the original passive-flight thrust"); return; }
        player.getAbilities().flying = true;
        if (com.fiskmods.heroes.common.hero.modifier.ModifierFlight.hasActiveFlight(player))
        { helper.fail("Creative flight must not use the suit-flight exemption"); return; }
        player.getAbilities().flying = false;
        data.getData().set(com.fiskmods.heroes.common.data.var.Vars.SHADOWFORM, false);
        if (com.fiskmods.heroes.common.hero.modifier.ModifierFlight.hasActiveFlight(player))
        { helper.fail("Ending shadowform must restore the normal floating check"); return; }
        com.fiskmods.heroes.common.data.PlayerInputTracker.clear(player);
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void tentacleAttacksRespectWallsAndCancelledCharge(GameTestHelper helper)
    {
        var level = helper.getLevel();
        var player = new net.minecraftforge.common.util.FakePlayer(level,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "fisk-tentacle-test"));
        var hero = com.fiskmods.heroes.common.hero.Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:doctor_octopus");
        var data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(player);
        var entry = hero != null ? hero.getPowerContainer().getEntry(FiskHeroes.id("tentacles")) : null;
        if (data == null || entry == null || !(entry.getModifier() instanceof
                com.fiskmods.heroes.common.hero.modifier.ModifierTentacles tentacles))
        { helper.fail("Doctor Octopus or tentacles missing"); return; }
        var slots = new net.minecraft.world.entity.EquipmentSlot[] {net.minecraft.world.entity.EquipmentSlot.HEAD,
                net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.LEGS,
                net.minecraft.world.entity.EquipmentSlot.FEET};
        for (int i = 0; i < 4; ++i)
            player.setItemSlot(slots[i], com.fiskmods.heroes.common.hero.ItemHeroArmor.create(
                    hero.getDefaultIteration(), com.fiskmods.heroes.common.item.ModItems.heroArmor[i]));
        com.fiskmods.heroes.common.hero.HeroTracker.update(player);
        var base = helper.absolutePos(new net.minecraft.core.BlockPos(1, 80, 1));
        player.setPos(base.getX() + 0.5D, base.getY(), base.getZ() + 0.5D);
        player.setYRot(0.0F);
        player.setXRot(0.0F);
        var target = net.minecraft.world.entity.EntityType.IRON_GOLEM.create(level);
        if (target == null) { helper.fail("Could not create tentacle target"); return; }
        target.setNoAi(true);
        target.setPos(player.getX(), player.getY(), player.getZ() + 5.0D);
        level.addFreshEntity(target);
        var wall = base.offset(0, 1, 2);
        level.setBlockAndUpdate(wall, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        tentacles.activateKey(player, entry, data, "TENTACLES");
        float health = target.getHealth();
        target.setDeltaMovement(Vec3.ZERO);
        tentacles.activateKey(player, entry, data, "TENTACLE_JAB");
        tentacles.activateKey(player, entry, data, "TENTACLE_GRAB");
        if (target.getHealth() != health || target.getDeltaMovement().lengthSqr() != 0.0D)
        { helper.fail("A wall must block tentacle jab and grab"); return; }
        tentacles.activateKey(player, entry, data, "TENTACLE_STRIKE");
        player.getPersistentData().putLong("FiskHeroesTentacleStrikeStart", level.getGameTime() - 40);
        tentacles.onToggle(player, entry, data);
        if (target.getHealth() != health)
        { helper.fail("A wall must block a charged tentacle strike"); return; }
        level.setBlockAndUpdate(wall, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        tentacles.activateKey(player, entry, data, "TENTACLE_STRIKE");
        player.getPersistentData().putLong("FiskHeroesTentacleStrikeStart", level.getGameTime() - 40);
        tentacles.activateKey(player, entry, data, "TENTACLES");
        tentacles.onToggle(player, entry, data);
        if (target.getHealth() != health)
        { helper.fail("Retracting tentacles must cancel a held strike"); return; }
        tentacles.activateKey(player, entry, data, "TENTACLES");
        tentacles.activateKey(player, entry, data, "TENTACLE_JAB");
        if (target.getHealth() >= health)
        { helper.fail("Jab must hit an unobstructed target in range"); return; }
        float afterJab = target.getHealth();
        target.invulnerableTime = 0;
        tentacles.activateKey(player, entry, data, "TENTACLE_STRIKE");
        player.getPersistentData().putLong("FiskHeroesTentacleStrikeStart", level.getGameTime() - 40);
        tentacles.onToggle(player, entry, data);
        if (target.getHealth() >= afterJab)
        { helper.fail("An active charged strike must hit an unobstructed target"); return; }
        target.invulnerableTime = 0;
        com.fiskmods.heroes.common.hero.ability.AbilityHandler.onAbilityKey(player, 3, true);
        if (!player.getPersistentData().contains("FiskHeroesTentacleStrikeStart"))
        { helper.fail("Strike key must begin charging"); return; }
        player.getPersistentData().putLong("FiskHeroesTentacleStrikeStart", level.getGameTime() - 40);
        float beforeClear = target.getHealth();
        com.fiskmods.heroes.common.hero.ability.AbilityHandler.clear(player);
        tentacles.onToggle(player, entry, data);
        if (target.getHealth() != beforeClear || player.getPersistentData().contains("FiskHeroesTentacleStrikeStart"))
        { helper.fail("Cancelling held abilities must not release a strike"); return; }
        for (var slot : slots) player.setItemSlot(slot, net.minecraft.world.item.ItemStack.EMPTY);
        com.fiskmods.heroes.common.hero.HeroTracker.update(player);
        if (data.getData().get(com.fiskmods.heroes.common.data.var.Vars.TENTACLES_ACTIVE))
        { helper.fail("Removing the suit must reset tentacle state"); return; }
        target.discard();
        helper.succeed();
    }

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
    public static void repulsorShotRespectsRangeEvenWithinSingleTick(GameTestHelper helper)
    {
        var level = helper.getLevel();
        var player = net.minecraft.world.entity.EntityType.ZOMBIE.create(level);
        if (player == null) { helper.fail("Could not create repulsor shooter"); return; }
        player.setNoAi(true);
        var pos = helper.absolutePos(new net.minecraft.core.BlockPos(1, 80, 1));
        player.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        player.setYRot(0.0F);
        player.setXRot(0.0F);
        level.addFreshEntity(player);
        var target = net.minecraft.world.entity.EntityType.IRON_GOLEM.create(level);
        if (target == null) { helper.fail("Could not create repulsor target"); return; }
        target.setNoAi(true);
        target.setPos(player.getX(), player.getY(), player.getZ() + 6.0D);
        level.addFreshEntity(target);
        var shot = new com.fiskmods.heroes.common.entity.projectile.EnergyBoltEntity(
                player, null, 12.0F, false, 20.0F, 0.0F, 4.0D);
        level.addFreshEntity(shot);
        shot.tick();
        if (!shot.isRemoved() || target.getHealth() != 100.0F)
        { helper.fail("Fast repulsor traveled or damaged a target beyond its range"); return; }
        target.setPos(player.getX(), player.getY(), player.getZ() + 2.0D);
        var inRange = new com.fiskmods.heroes.common.entity.projectile.EnergyBoltEntity(
                player, null, 12.0F, false, 20.0F, 0.0F, 4.0D);
        level.addFreshEntity(inRange);
        inRange.tick();
        if (!inRange.isRemoved() || target.getHealth() >= 100.0F)
        { helper.fail("Repulsor did not hit a target within its range"); return; }
        target.discard();
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void repulsorRangePersistsAcrossSave(GameTestHelper helper)
    {
        var level = helper.getLevel();
        var player = net.minecraft.world.entity.EntityType.ZOMBIE.create(level);
        if (player == null) { helper.fail("Could not create repulsor shooter"); return; }
        player.setNoAi(true);
        var pos = helper.absolutePos(new net.minecraft.core.BlockPos(1, 80, 1));
        player.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        player.setYRot(0.0F);
        player.setXRot(0.0F);
        level.addFreshEntity(player);
        var shot = new com.fiskmods.heroes.common.entity.projectile.EnergyBoltEntity(
                player, null, 12.0F, false, 2.0F, 0.0F, 3.0D);
        level.addFreshEntity(shot);
        shot.tick();
        if (shot.isRemoved()) { helper.fail("Repulsor expired before its range"); return; }
        var tag = new net.minecraft.nbt.CompoundTag();
        shot.saveWithoutId(tag);
        shot.discard();
        var loaded = new com.fiskmods.heroes.common.entity.projectile.EnergyBoltEntity(
                com.fiskmods.heroes.common.entity.ModEntities.ENERGY_BOLT.get(), level);
        loaded.load(tag);
        loaded.tick();
        if (!loaded.isRemoved() || loaded.position().distanceTo(player.getEyePosition()) > 3.1D)
        { helper.fail("Saved repulsor forgot its remaining travel distance"); return; }
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void chargedBeamTracesMultipleTargetsAndStopsAtWalls(GameTestHelper helper)
    {
        var level = helper.getLevel();
        var player = new net.minecraftforge.common.util.FakePlayer(level,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "fisk-beam-target-test"));
        var hero = com.fiskmods.heroes.common.hero.Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:vision");
        var data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(player);
        var entry = hero != null ? hero.getPowerContainer().getEntry(FiskHeroes.id("charged_beam")) : null;
        if (data == null || entry == null) { helper.fail("Vision beam or data missing"); return; }
        var base = helper.absolutePos(new net.minecraft.core.BlockPos(1, 80, 1));
        player.setPos(base.getX() + 0.5D, base.getY(), base.getZ() + 0.5D);
        player.setYRot(0.0F);
        player.setXRot(0.0F);
        var a = net.minecraft.world.entity.EntityType.IRON_GOLEM.create(level);
        var b = net.minecraft.world.entity.EntityType.IRON_GOLEM.create(level);
        var blocked = net.minecraft.world.entity.EntityType.IRON_GOLEM.create(level);
        var offAxis = net.minecraft.world.entity.EntityType.IRON_GOLEM.create(level);
        if (a == null || b == null || blocked == null || offAxis == null)
        { helper.fail("Could not create beam targets"); return; }
        var targets = new net.minecraft.world.entity.animal.IronGolem[] {a, b, blocked, offAxis};
        int[] xs = {0, 0, 0, 3};
        int[] zs = {2, 4, 8, 3};
        for (int i = 0; i < targets.length; ++i)
        {
            targets[i].setNoAi(true);
            targets[i].setPos(player.getX() + xs[i], player.getY(), player.getZ() + zs[i]);
            level.addFreshEntity(targets[i]);
        }
        var wall = base.offset(0, 1, 6);
        level.setBlockAndUpdate(wall, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        var vars = data.getData();
        vars.set(com.fiskmods.heroes.common.data.var.Vars.SCALE, 1.0F);
        vars.set(com.fiskmods.heroes.common.data.var.Vars.BEAM_CHARGING, true);
        vars.set(com.fiskmods.heroes.common.data.var.Vars.BEAM_CHARGE, 1.0F);
        player.tickCount = 1;
        entry.getModifier().tick(player, entry, data);
        if (a.getHealth() != 100.0F || vars.get(com.fiskmods.heroes.common.data.var.Vars.HEAT_VISION_LENGTH) != 0.0D)
        { helper.fail("Beam must start at zero length before extending"); return; }
        entry.getModifier().tick(player, entry, data);
        double growingLength = vars.get(com.fiskmods.heroes.common.data.var.Vars.HEAT_VISION_LENGTH);
        if (Math.abs(growingLength - 32.0D / 7.0D) > 0.01D
                || Math.abs(a.getHealth() - 93.0F) > 0.01F || Math.abs(b.getHealth() - 93.0F) > 0.01F)
        { helper.fail("Extending beam must apply the configured 7 damage to both targets"); return; }
        vars.set(com.fiskmods.heroes.common.data.var.Vars.BEAM_SHOOTING, 0.5F);
        player.tickCount = 2;
        entry.getModifier().tick(player, entry, data);
        double stoppedLength = vars.get(com.fiskmods.heroes.common.data.var.Vars.HEAT_VISION_LENGTH);
        if (stoppedLength < 5.0D || stoppedLength > 6.0D || a.getHealth() != 93.0F || b.getHealth() != 93.0F
                || blocked.getHealth() != 100.0F || offAxis.getHealth() != 100.0F)
        { helper.fail("Wall, radius or vanilla hurt resistance was ignored"); return; }
        player.tickCount = 6;
        entry.getModifier().tick(player, entry, data);
        if (Math.abs(a.getHealth() - 86.0F) > 0.01F || Math.abs(b.getHealth() - 86.0F) > 0.01F
                || blocked.getHealth() != 100.0F || a.getDeltaMovement().z <= 0.0D)
        { helper.fail("Beam must refresh damage every six ticks and push targets forward"); return; }
        for (var target : targets) target.discard();
        level.removeBlock(wall, false);
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void tentacleVisualStatesSyncAndClear(GameTestHelper helper)
    {
        var level = helper.getLevel();
        var player = new net.minecraftforge.common.util.FakePlayer(level,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "fisk-tentacle-visual"));
        var hero = com.fiskmods.heroes.common.hero.Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:doctor_octopus");
        var data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(player);
        var entry = hero != null ? hero.getPowerContainer().getEntry(FiskHeroes.id("tentacles")) : null;
        if (data == null || entry == null || !(entry.getModifier() instanceof
                com.fiskmods.heroes.common.hero.modifier.ModifierTentacles tentacles))
        { helper.fail("Doctor Octopus tentacle power is missing"); return; }
        var slots = new net.minecraft.world.entity.EquipmentSlot[] {net.minecraft.world.entity.EquipmentSlot.HEAD,
                net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.LEGS,
                net.minecraft.world.entity.EquipmentSlot.FEET};
        for (int i = 0; i < 4; ++i)
            if (hero.getDefaultIteration().hasPieceOfSet(i)) player.setItemSlot(slots[i],
                    com.fiskmods.heroes.common.hero.ItemHeroArmor.create(hero.getDefaultIteration(),
                            com.fiskmods.heroes.common.item.ModItems.heroArmor[i]));
        com.fiskmods.heroes.common.hero.HeroTracker.update(player);
        tentacles.activateKey(player, entry, data, "TENTACLES");
        tentacles.activateKey(player, entry, data, "TENTACLE_JAB");
        if (data.getData().get(com.fiskmods.heroes.common.data.var.Vars.TENTACLE_VISUAL_ACTION)
                != com.fiskmods.heroes.common.util.TentacleVisualMath.JAB)
        { helper.fail("A jab, including a miss, must start its visual action"); return; }
        for (int i = 0; i < 8; ++i) tentacles.tick(player, entry, data);
        float progress = data.getData().get(com.fiskmods.heroes.common.data.var.Vars.TENTACLE_VISUAL_TIMER);
        var received = new com.fiskmods.heroes.common.data.var.DataContainer();
        received.readUpdate(data.getData().writeDirty());
        if (Math.abs(progress - 0.5F) > 0.0001F
                || received.get(com.fiskmods.heroes.common.data.var.Vars.TENTACLE_VISUAL_ACTION)
                    != com.fiskmods.heroes.common.util.TentacleVisualMath.JAB
                || Math.abs(received.get(com.fiskmods.heroes.common.data.var.Vars.TENTACLE_VISUAL_TIMER) - progress) > 0.0001F
                || com.fiskmods.heroes.common.util.TentacleVisualMath.reach(
                    com.fiskmods.heroes.common.util.TentacleVisualMath.JAB, progress) < 0.99F)
        { helper.fail("Tentacle action and progress must serialize and produce a lunge"); return; }
        for (int i = 0; i < 8; ++i) tentacles.tick(player, entry, data);
        if (data.getData().get(com.fiskmods.heroes.common.data.var.Vars.TENTACLE_VISUAL_ACTION) != 0)
        { helper.fail("Finished tentacle actions must clear"); return; }
        tentacles.activateKey(player, entry, data, "TENTACLE_STRIKE");
        player.getPersistentData().putLong("FiskHeroesTentacleStrikeStart", level.getGameTime() - 20);
        tentacles.tick(player, entry, data);
        if (data.getData().get(com.fiskmods.heroes.common.data.var.Vars.TENTACLE_VISUAL_TIMER) < 0.99F)
        { helper.fail("Charging must open the strike blades"); return; }
        tentacles.onToggle(player, entry, data);
        if (data.getData().get(com.fiskmods.heroes.common.data.var.Vars.TENTACLE_VISUAL_ACTION)
                != com.fiskmods.heroes.common.util.TentacleVisualMath.STRIKE)
        { helper.fail("A fully charged release must start the strike pose"); return; }
        tentacles.activateKey(player, entry, data, "TENTACLE_STRIKE");
        com.fiskmods.heroes.common.hero.modifier.ModifierTentacles.cancelStrike(player);
        if (data.getData().get(com.fiskmods.heroes.common.data.var.Vars.TENTACLE_VISUAL_ACTION) != 0)
        { helper.fail("Cancelling a charge must clear its pose"); return; }
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
    public static void wingsuitTrianglesFillTwoQuads(GameTestHelper helper)
    {
        int[] expected = {0, 1, 2, 2, 2, 1, 0, 0};
        if (com.fiskmods.heroes.common.util.TriangleQuad.vertexCount() != expected.length)
        { helper.fail("Each two-sided triangle must fill exactly two quads"); return; }
        for (int i = 0; i < expected.length; ++i)
        {
            if (com.fiskmods.heroes.common.util.TriangleQuad.corner(i) != expected[i]
                    || com.fiskmods.heroes.common.util.TriangleQuad.backFace(i) != (i >= 4))
            { helper.fail("Quad corners or face winding are incorrect"); return; }
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void cloudHexColorsPreserveMonitorBlue(GameTestHelper helper)
    {
        float[] color = com.fiskmods.heroes.common.util.CloudColorMath.hexGradient("0x00B8FF", "0x004BB2");
        if (color == null || color[0] != 0.0F || Math.abs(color[1] - 259.0F / 510.0F) > 0.0001F
                || Math.abs(color[2] - 433.0F / 510.0F) > 0.0001F
                || com.fiskmods.heroes.common.util.CloudColorMath.hexGradient("invalid", "0x004BB2") != null)
        { helper.fail("Hex cloud gradients must retain the configured RGB values"); return; }
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void glidingPoseOpensAndFoldsOnDive(GameTestHelper helper)
    {
        float folded = com.fiskmods.heroes.common.util.GlidingPoseMath.spread(0.0F, 0.0D, 0.0D);
        float level = com.fiskmods.heroes.common.util.GlidingPoseMath.spread(1.0F, 0.0D, 1.0D);
        float diving = com.fiskmods.heroes.common.util.GlidingPoseMath.spread(1.0F, -1.0D, 1.0D);
        if (Math.abs(folded) > 0.0001F || Math.abs(level - Math.PI / 2) > 0.0001F
                || Math.abs(diving - Math.PI / 12) > 0.0001F
                || Math.abs(com.fiskmods.heroes.common.util.GlidingPoseMath.bodyPitch(0.0F, 0.0F)) > 0.0001F
                || Math.abs(com.fiskmods.heroes.common.util.GlidingPoseMath.bodyPitch(1.0F, 0.0F) + 90.0F) > 0.0001F
                || Math.abs(com.fiskmods.heroes.common.util.GlidingPoseMath.bodyPitch(1.0F, 45.0F) + 135.0F) > 0.0001F)
        { helper.fail("Gliding limbs and body must deploy, and the limbs narrow during a steep dive"); return; }
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
                || data.getData().get(com.fiskmods.heroes.common.data.var.Vars.GLIDING_TIMER) != 1.0F)
        { helper.fail("Gliding must unfold the cape and normalize the pose"); return; }
        var pos = helper.absolutePos(new net.minecraft.core.BlockPos(1, 8, 1));
        player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        player.setYRot(0);
        player.setXRot(0);
        player.setDeltaMovement(0, -0.4, 0);
        double beforeZ = player.getZ();
        player.travel(Vec3.ZERO);
        if (player.getZ() <= beforeZ + 0.01 || player.getDeltaMovement().z <= 0.01
                || player.getDeltaMovement().y <= -0.4)
        { helper.fail("Glide travel hook must convert descent into forward movement"); return; }
        data.getData().set(com.fiskmods.heroes.common.data.var.Vars.GLIDING, false);
        Vec3 unchanged = player.getDeltaMovement();
        if (com.fiskmods.heroes.common.hero.ability.GlidingTravel.travel(player)
                || !unchanged.equals(player.getDeltaMovement()))
        { helper.fail("Inactive gliding must leave vanilla travel in control"); return; }
        data.getData().set(com.fiskmods.heroes.common.data.var.Vars.GLIDING, true);
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
    public static void glidingWallCollisionStopsAndHurtsOnlyWhenConfigured(GameTestHelper helper)
    {
        var player = helper.makeMockPlayer();
        var data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(player);
        var hero = com.fiskmods.heroes.common.hero.Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:batman_dceu");
        var base = hero != null ? hero.getPowerContainer().getEntry(FiskHeroes.id("gliding")) : null;
        if (data == null || base == null) { helper.fail("Gliding collision data is missing"); return; }
        var entry = new com.fiskmods.heroes.common.hero.power.ModifierEntry(base.getModifier(), null);
        var property = com.fiskmods.heroes.common.hero.power.PowerProperty.COLLISION;
        var active = com.fiskmods.heroes.common.data.var.Vars.GLIDING;
        var config = com.google.gson.JsonParser.parseString("""
                {"blocks":{"stopFlying":true,"takeDamage":false},"entities":{"dealDamage":false}}
                """);
        entry.setProperty(property, config);
        if (entry.get(property) == null || !entry.get(property).isJsonObject()
                || !com.fiskmods.heroes.common.hero.ability.GlidingCollision.flag(null, "blocks", "stopFlying"))
        { helper.fail("Collision objects and their default flags must be retained"); return; }
        player.getAbilities().invulnerable = false;
        player.setHealth(20.0F);
        player.horizontalCollision = true;
        player.setDeltaMovement(0, 0, 0);
        data.getData().set(active, true);
        entry.setToggled(player, true);
        com.fiskmods.heroes.common.hero.ability.GlidingCollision.apply(player, data, entry, 0.2D);
        if (!data.getData().get(active) || player.getHealth() != 20.0F)
        { helper.fail("A glancing collision below the speed threshold must not stop or hurt"); return; }
        com.fiskmods.heroes.common.hero.ability.GlidingCollision.apply(player, data, entry, 1.0D);
        if (data.getData().get(active) || entry.isToggled(player) || player.getHealth() != 20.0F)
        { helper.fail("A hard impact must stop flight without damage when the pack disables it"); return; }
        entry.setProperty(property, com.google.gson.JsonParser.parseString("""
                {"blocks":{"stopFlying":false,"takeDamage":true},"entities":{"dealDamage":false}}
                """));
        data.getData().set(active, true);
        com.fiskmods.heroes.common.hero.ability.GlidingCollision.apply(player, data, entry, 1.0D);
        if (!data.getData().get(active) || player.getHealth() >= 20.0F)
        { helper.fail("The damage and stop options must operate independently: gliding=" + data.getData().get(active) + ", health=" + player.getHealth()); return; }
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void glidingTravelDetectsActualWallImpact(GameTestHelper helper)
    {
        var player = helper.makeMockPlayer();
        var hero = com.fiskmods.heroes.common.hero.Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:batman_dceu");
        var data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(player);
        if (hero == null || data == null) { helper.fail("Batman is missing"); return; }
        var slots = new net.minecraft.world.entity.EquipmentSlot[] {net.minecraft.world.entity.EquipmentSlot.HEAD,
                net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.LEGS,
                net.minecraft.world.entity.EquipmentSlot.FEET};
        for (int i = 0; i < 4; ++i)
            if (hero.getDefaultIteration().hasPieceOfSet(i)) player.setItemSlot(slots[i],
                    com.fiskmods.heroes.common.hero.ItemHeroArmor.create(hero.getDefaultIteration(),
                            com.fiskmods.heroes.common.item.ModItems.heroArmor[i]));
        com.fiskmods.heroes.common.hero.HeroTracker.update(player);
        var entry = hero.getPowerContainer().getEntry(FiskHeroes.id("gliding"));
        if (entry == null || entry.get(com.fiskmods.heroes.common.hero.power.PowerProperty.COLLISION) == null)
        { helper.fail("Batman collision configuration was not loaded from the pack"); return; }
        helper.setBlock(new net.minecraft.core.BlockPos(1, 8, 2), net.minecraft.world.level.block.Blocks.STONE);
        helper.setBlock(new net.minecraft.core.BlockPos(1, 9, 2), net.minecraft.world.level.block.Blocks.STONE);
        var pos = helper.absolutePos(new net.minecraft.core.BlockPos(1, 8, 1));
        player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        player.setOnGround(false);
        player.getAbilities().invulnerable = false;
        player.setHealth(20.0F);
        player.setYRot(0); player.setXRot(0);
        player.setDeltaMovement(0, 0, 1.2D);
        data.getData().set(com.fiskmods.heroes.common.data.var.Vars.GLIDING, true);
        entry.setToggled(player, true);
        player.travel(Vec3.ZERO);
        if (!player.horizontalCollision || data.getData().get(com.fiskmods.heroes.common.data.var.Vars.GLIDING)
                || player.getHealth() >= 20.0F)
        { helper.fail("Batman wall: collision=" + player.horizontalCollision + ", gliding=" + data.getData().get(com.fiskmods.heroes.common.data.var.Vars.GLIDING) + ", health=" + player.getHealth() + ", motion=" + player.getDeltaMovement()); return; }
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void glidingEntityImpactDamagesKnocksBackAndStops(GameTestHelper helper)
    {
        var player = new net.minecraftforge.common.util.FakePlayer(helper.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "fisk-entity-impact"));
        var data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(player);
        var hero = com.fiskmods.heroes.common.hero.Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:batman_dceu");
        var base = hero != null ? hero.getPowerContainer().getEntry(FiskHeroes.id("gliding")) : null;
        if (data == null || base == null) { helper.fail("Gliding collision data is missing"); return; }
        var entry = new com.fiskmods.heroes.common.hero.power.ModifierEntry(base.getModifier(), null);
        var pos = helper.absolutePos(new net.minecraft.core.BlockPos(1, 8, 1));
        player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        var target = net.minecraft.world.entity.EntityType.COW.create(helper.getLevel());
        if (target == null) { helper.fail("Could not create target"); return; }
        target.setPos(player.getX(), player.getY(), player.getZ() + 0.8D);
        helper.getLevel().addFreshEntity(target);
        try
        {
            data.getData().set(com.fiskmods.heroes.common.data.var.Vars.GLIDING, true);
            player.setDeltaMovement(0, 0, 0.2D);
            com.fiskmods.heroes.common.hero.ability.GlidingCollision.apply(player, data, entry, 0.2D);
            if (target.getHealth() != target.getMaxHealth() || !data.getData().get(com.fiskmods.heroes.common.data.var.Vars.GLIDING))
            { helper.fail("A slow pass must not damage or stop flight"); return; }
            player.setDeltaMovement(0, 0, 1.2D);
            player.noPhysics = true;
            com.fiskmods.heroes.common.hero.ability.GlidingCollision.apply(player, data, entry, 1.2D);
            player.noPhysics = false;
            if (target.getHealth() != target.getMaxHealth() || !data.getData().get(com.fiskmods.heroes.common.data.var.Vars.GLIDING))
            { helper.fail("No-physics flight must not cause collision damage"); return; }
            var collision = com.fiskmods.heroes.common.hero.power.PowerProperty.COLLISION;
            entry.setProperty(collision, com.google.gson.JsonParser.parseString("""
                    {"entities":{"stopFlying":true,"dealDamage":false}}
                    """));
            com.fiskmods.heroes.common.hero.ability.GlidingCollision.apply(player, data, entry, 1.2D);
            if (target.getHealth() != target.getMaxHealth() || !data.getData().get(com.fiskmods.heroes.common.data.var.Vars.GLIDING))
            { helper.fail("A pack may disable entity impacts"); return; }
            entry.setProperty(collision, com.google.gson.JsonParser.parseString("""
                    {"entities":{"stopFlying":false,"dealDamage":true}}
                    """));
            com.fiskmods.heroes.common.hero.ability.GlidingCollision.apply(player, data, entry, 1.2D);
            if (target.getHealth() >= target.getMaxHealth() || target.getDeltaMovement().z <= 0.0D
                    || !data.getData().get(com.fiskmods.heroes.common.data.var.Vars.GLIDING))
            { helper.fail("Impact damage and knockback must not stop flight when the pack disables stopping"); return; }
            target.setHealth(target.getMaxHealth());
            target.invulnerableTime = 0;
            entry.setProperty(collision, com.google.gson.JsonParser.parseString("""
                    {"entities":{"stopFlying":true,"dealDamage":true}}
                    """));
            com.fiskmods.heroes.common.hero.ability.GlidingCollision.apply(player, data, entry, 1.2D);
            if (target.getHealth() >= target.getMaxHealth()
                    || data.getData().get(com.fiskmods.heroes.common.data.var.Vars.GLIDING))
            { helper.fail("A configured entity impact must damage the target and stop the glide"); return; }
        }
        finally { target.discard(); }
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void glidingTravelUsesPitchStallAndPoweredBoost(GameTestHelper helper)
    {
        var horizontalLook = new Vec3(0, 0, 1);
        var falling = new Vec3(0, -0.4, 0);
        var glide = com.fiskmods.heroes.common.hero.ability.GlidingTravel.step(
                falling, horizontalLook, 0, 1, 1, 1, 0, false, 0);
        var boosted = com.fiskmods.heroes.common.hero.ability.GlidingTravel.step(
                falling, horizontalLook, 0, 1, 1, 1, 0, true, 0.1);
        var stalled = com.fiskmods.heroes.common.hero.ability.GlidingTravel.step(
                falling, horizontalLook, 0, 1, 1, 1, 1, false, 0);
        var climbingLook = new Vec3(0, 0.5, Math.sqrt(0.75));
        var forward = new Vec3(0, -0.1, 1);
        var level = com.fiskmods.heroes.common.hero.ability.GlidingTravel.step(
                forward, horizontalLook, 0, 1, 1, 1, 0, false, 0);
        var climb = com.fiskmods.heroes.common.hero.ability.GlidingTravel.step(
                forward, climbingLook, -30, 1, 1, 1, 0, false, 0);
        if (glide.z <= 0 || glide.y <= falling.y - 0.08 || boosted.z <= glide.z
                || stalled.y >= glide.y || climb.y <= level.y || climb.z >= level.z)
        { helper.fail("Gliding must convert falling to forward speed, account for stall, boost and pitch"); return; }
        var vertical = com.fiskmods.heroes.common.hero.ability.GlidingTravel.step(
                forward, new Vec3(0, 1, 0), -90, 1, 1, 1, 0, false, 0);
        if (!Double.isFinite(vertical.x) || !Double.isFinite(vertical.y) || !Double.isFinite(vertical.z))
        { helper.fail("Looking straight up must not divide by zero"); return; }
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
        // A stale or externally re-enabled toggle must not remain permanently active at 100%.
        vars.set(com.fiskmods.heroes.common.data.var.Vars.GIANT_MODE, true);
        cooldown.getModifier().tick(player, cooldown, data);
        if (vars.get(com.fiskmods.heroes.common.data.var.Vars.GIANT_MODE))
        { helper.fail("A toggle at a full cooldown must immediately time out"); return; }
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
    public static void scaledPlayersUseScaledPoseCollisionBoxes(GameTestHelper helper)
    {
        class PosePlayer extends net.minecraftforge.common.util.FakePlayer
        {
            PosePlayer(net.minecraft.server.level.ServerLevel level)
            { super(level, new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "scaled-pose-test")); }
            void updatePose() { updatePlayerPose(); }
            net.minecraft.world.phys.AABB boxFor(net.minecraft.world.entity.Pose pose) { return getBoundingBoxForPose(pose); }
        }
        var level = helper.getLevel();
        var pos = helper.absolutePos(new net.minecraft.core.BlockPos(1, 180, 1));
        level.setBlockAndUpdate(pos.below(), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(pos.above(), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        var player = new PosePlayer(level);
        player.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        var data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(player);
        if (data == null) { helper.fail("Player size capability missing"); return; }
        var scale = com.fiskmods.heroes.common.data.var.Vars.SCALE;
        for (float factor : new float[] {0.0625F, 0.5F})
        {
            data.getData().set(scale, factor);
            com.fiskmods.heroes.common.hero.ability.PlayerSizeHandler.refreshIfChanged(player);
            player.updatePose();
            if (player.getPose() != net.minecraft.world.entity.Pose.STANDING
                    || Math.abs(player.getBbHeight() - 1.8F * factor) > 0.001F
                    || Math.abs(player.getEyeHeight() - 1.62F * factor) > 0.001F)
            { helper.fail("Shrunken player should stand at scaled eye height under a one-block ceiling"); return; }
        }
        player.setShiftKeyDown(true);
        player.updatePose();
        if (player.getPose() != net.minecraft.world.entity.Pose.CROUCHING)
        { helper.fail("Shrunken crouch must remain available"); return; }
        player.setShiftKeyDown(false);
        player.setSwimming(true);
        player.updatePose();
        if (player.getPose() != net.minecraft.world.entity.Pose.SWIMMING)
        { helper.fail("Actual swimming must still use the swimming pose"); return; }
        player.setSwimming(false);
        data.getData().set(scale, 1.0F);
        com.fiskmods.heroes.common.hero.ability.PlayerSizeHandler.refreshIfChanged(player);
        player.updatePose();
        if (player.getPose() != net.minecraft.world.entity.Pose.SWIMMING)
        { helper.fail("Normal-sized player must still crawl under a one-block ceiling"); return; }
        level.removeBlock(pos.above(), false);
        player.updatePose();
        if (player.getPose() != net.minecraft.world.entity.Pose.STANDING)
        { helper.fail("Normal player must stand again after leaving the tunnel"); return; }
        data.getData().set(scale, 5.0F);
        com.fiskmods.heroes.common.hero.ability.PlayerSizeHandler.refreshIfChanged(player);
        var giantBox = player.boxFor(net.minecraft.world.entity.Pose.STANDING);
        if (Math.abs(giantBox.getYsize() - 9.0D) > 0.001D || Math.abs(giantBox.getXsize() - 3.0D) > 0.001D)
        { helper.fail("Giant prospective pose must use giant dimensions"); return; }
        level.removeBlock(pos.below(), false);
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void grapplingGunReelsTargetsAndFiresSecondAnchor(GameTestHelper helper)
    {
        var level = helper.getLevel();
        var base = helper.absolutePos(new net.minecraft.core.BlockPos(1, 180, 1));
        var player = new net.minecraftforge.common.util.FakePlayer(level,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "grapple-test"));
        player.setPos(base.getX() + .5, base.getY(), base.getZ() + .5);
        var hero = com.fiskmods.heroes.common.hero.Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:batman_dceu");
        if (hero == null) { helper.fail("Batman missing"); return; }
        var slots = new net.minecraft.world.entity.EquipmentSlot[] {net.minecraft.world.entity.EquipmentSlot.HEAD,
                net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.LEGS,
                net.minecraft.world.entity.EquipmentSlot.FEET};
        for (int i = 0; i < 4; ++i)
            if (hero.getDefaultIteration().hasPieceOfSet(i)) player.setItemSlot(slots[i],
                    com.fiskmods.heroes.common.hero.ItemHeroArmor.create(hero.getDefaultIteration(),
                            com.fiskmods.heroes.common.item.ModItems.heroArmor[i]));
        com.fiskmods.heroes.common.hero.HeroTracker.update(player);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                new net.minecraft.world.item.ItemStack(com.fiskmods.heroes.common.item.ModItems.GRAPPLING_GUN.get()));
        var target = net.minecraft.world.entity.EntityType.IRON_GOLEM.create(level);
        if (target == null) { helper.fail("Grappling target missing"); return; }
        target.setNoAi(true); target.setNoGravity(true);
        target.setPos(player.getX(), player.getY(), player.getZ() + 3.0); level.addFreshEntity(target);
        var hook = new com.fiskmods.heroes.common.entity.projectile.GrapplingHookEntity(player);
        hook.setPos(player.getX(), player.getY() + 1.0, player.getZ());
        hook.setDeltaMovement(0, 0, 4); level.addFreshEntity(hook);
        player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        hook.tick();
        if (!hook.isAttached() || hook.getAttachedEntity() != target)
        { helper.fail("First hook must attach to the hit entity"); return; }
        if (target.getDeltaMovement().z >= 0 || player.getDeltaMovement().lengthSqr() > 0.0001)
        { helper.fail("Hooking an entity must reel the target in without pulling the shooter"); return; }
        target.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        var melee = new net.minecraftforge.event.entity.living.LivingHurtEvent(target,
                player.damageSources().playerAttack(player), 4.0F);
        com.fiskmods.heroes.common.event.CommonEventHandler.onLivingHurt(melee);
        if (Math.abs(melee.getAmount() - 5.0F) > .001F || target.getDeltaMovement().horizontalDistanceSqr() < 1.0)
        { helper.fail("Melee against hooked targets must gain 25% damage and four knockback levels"); return; }
        var result = player.getMainHandItem().use(level, player, net.minecraft.world.InteractionHand.MAIN_HAND);
        var pairs = level.getEntitiesOfClass(com.fiskmods.heroes.common.entity.projectile.GrapplingHookEntity.class,
                player.getBoundingBox().inflate(16), candidate -> candidate.isPair() && candidate.getOwner() == player);
        if (!result.getResult().consumesAction() || !hook.isRemoved() || pairs.size() != 1)
        { helper.fail("Second shot must replace the first hook with one paired projectile"); return; }
        var pair = pairs.get(0);
        if (pair.isAttached() || pair.getAttachedEntity() != target || pair.getDeltaMovement().lengthSqr() < 1)
        { helper.fail("Second hook must fly from the shooter with the target attached to its cable"); return; }
        var wall = base.offset(4, 0, 0);
        level.setBlockAndUpdate(wall, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        target.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        pair.setPos(player.getX(), player.getY() + .5, player.getZ());
        pair.setDeltaMovement(5, 0, 0);
        pair.tick();
        if (!pair.isAttached() || pair.getAttachedEntity() != target || target.getDeltaMovement().x <= 0)
        { helper.fail("Second hook must stick to the wall and pull the target toward it"); return; }
        var saved = new net.minecraft.nbt.CompoundTag(); pair.addAdditionalSaveData(saved);
        if (!saved.hasUUID("GrappleTarget") || !saved.getUUID("GrappleTarget").equals(target.getUUID()))
        { helper.fail("Grapple target must persist by UUID, not transient entity id"); return; }
        pair.discard();
        var restored = new com.fiskmods.heroes.common.entity.projectile.GrapplingHookEntity(
                com.fiskmods.heroes.common.entity.ModEntities.GRAPPLING_HOOK.get(), level);
        restored.readAdditionalSaveData(saved); restored.setOwner(player); level.addFreshEntity(restored);
        target.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        restored.tick();
        if (restored.getAttachedEntity() != target || target.getDeltaMovement().x <= 0)
        { helper.fail("Reloaded tether must resolve and pull the original target"); return; }
        restored.discard(); target.discard(); level.removeBlock(wall, false);
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void grapplingHookUsesGravityAndNormalCollision(GameTestHelper helper)
    {
        var player = helper.makeMockPlayer();
        var hook = new com.fiskmods.heroes.common.entity.projectile.GrapplingHookEntity(player);
        if (hook.isNoGravity()) { helper.fail("Flying grapple must be affected by gravity"); return; }
        var tag = new net.minecraft.nbt.CompoundTag();
        hook.addAdditionalSaveData(tag);
        if (tag.hasUUID("GrappleTarget")) { helper.fail("Unattached hook must not have a target"); return; }
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void trickArrowTypeDamageIsAppliedToActualImpacts(GameTestHelper helper)
    {
        var level = helper.getLevel();
        var base = helper.absolutePos(new net.minecraft.core.BlockPos(1, 180, 1));
        var player = helper.makeMockPlayer();
        player.setPos(base.getX() + .5, base.getY(), base.getZ() + .5);
        var target = net.minecraft.world.entity.EntityType.IRON_GOLEM.create(level);
        if (target == null) { helper.fail("Arrow target missing"); return; }
        target.setNoAi(true); target.setNoGravity(true);
        target.setPos(player.getX(), player.getY(), player.getZ() + 2); level.addFreshEntity(target);
        var normal = new com.fiskmods.heroes.common.entity.arrow.TrickArrowEntity(
                com.fiskmods.heroes.common.entity.ModEntities.TRICK_ARROW.get(), level, player, "normal");
        normal.setPos(player.getX(), player.getY() + 1, player.getZ()); normal.setDeltaMovement(0, 0, 2);
        level.addFreshEntity(normal); normal.tick();
        float ordinaryDamage = target.getMaxHealth() - target.getHealth();
        target.setHealth(target.getMaxHealth()); target.invulnerableTime = 0;
        var carrot = new com.fiskmods.heroes.common.entity.arrow.TrickArrowEntity(
                com.fiskmods.heroes.common.entity.ModEntities.TRICK_ARROW.get(), level, player, "carrot");
        carrot.setPos(player.getX(), player.getY() + 1, player.getZ()); carrot.setDeltaMovement(0, 0, 2);
        level.addFreshEntity(carrot); carrot.tick();
        float carrotDamage = target.getMaxHealth() - target.getHealth();
        if (Math.abs(ordinaryDamage - 4) > .01 || Math.abs(carrotDamage - 1) > .01)
        { helper.fail("Arrow type multipliers must affect actual damage: normal=" + ordinaryDamage + ", carrot=" + carrotDamage); return; }
        target.discard(); normal.discard(); carrot.discard(); helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void quiverBowCreativeAndTripleAmmoDoNotDuplicate(GameTestHelper helper)
    {
        var level = helper.getLevel();
        var base = helper.absolutePos(new net.minecraft.core.BlockPos(1, 180, 1));
        var player = new net.minecraftforge.common.util.FakePlayer(level,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "quiver-ammo-test"));
        player.setPos(base.getX() + .5, base.getY(), base.getZ() + .5);
        var bow = new net.minecraft.world.item.ItemStack(com.fiskmods.heroes.common.item.ModItems.COMPOUND_BOW.get());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, bow);
        var quiver = new net.minecraft.world.item.ItemStack(com.fiskmods.heroes.common.item.ModItems.QUIVER.get());
        var ammo = com.fiskmods.heroes.common.item.ItemTrickArrow.createStack("triple"); ammo.setCount(3);
        com.fiskmods.heroes.common.item.ItemQuiver.createInventory(quiver).setStackInSlot(0, ammo);
        player.getInventory().setItem(1, quiver);
        var item = (com.fiskmods.heroes.common.item.ItemCompoundBow) bow.getItem();
        player.getAbilities().instabuild = true;
        item.releaseUsing(bow, level, player, item.getUseDuration(bow) - 30);
        var bounds = player.getBoundingBox().inflate(4);
        var arrows = level.getEntitiesOfClass(com.fiskmods.heroes.common.entity.arrow.TrickArrowEntity.class,
                bounds, arrow -> arrow.getOwner() == player);
        if (arrows.size() != 3 || com.fiskmods.heroes.common.item.ItemQuiver.createInventory(quiver).getStackInSlot(0).getCount() != 3
                || arrows.stream().anyMatch(a -> a.pickup == net.minecraft.world.entity.projectile.AbstractArrow.Pickup.ALLOWED))
        { helper.fail("Creative triple shot must preserve quiver ammo and forbid survival pickup"); return; }
        arrows.forEach(net.minecraft.world.entity.Entity::discard);
        player.getAbilities().instabuild = false;
        item.releaseUsing(bow, level, player, item.getUseDuration(bow) - 30);
        arrows = level.getEntitiesOfClass(com.fiskmods.heroes.common.entity.arrow.TrickArrowEntity.class,
                bounds, arrow -> arrow.getOwner() == player && arrow.isAlive());
        if (arrows.size() != 3 || com.fiskmods.heroes.common.item.ItemQuiver.createInventory(quiver).getStackInSlot(0).getCount() != 2
                || arrows.stream().anyMatch(a -> a.pickup != net.minecraft.world.entity.projectile.AbstractArrow.Pickup.ALLOWED
                        || !"normal".equals(a.getArrowType())))
        { helper.fail("Survival triple shot must consume one bundle and fire three recoverable normal arrows"); return; }
        arrows.forEach(net.minecraft.world.entity.Entity::discard); helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void openQuiverCannotBeMovedOrReplaced(GameTestHelper helper)
    {
        var player = helper.makeMockPlayer();
        if (!com.fiskmods.heroes.common.item.ItemQuiver.isArrow(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SPECTRAL_ARROW))
                || com.fiskmods.heroes.common.item.ItemQuiver.isArrow(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STONE)))
        { helper.fail("Quiver must accept arrow items but reject ordinary items"); return; }
        var quiver = new net.minecraft.world.item.ItemStack(com.fiskmods.heroes.common.item.ModItems.QUIVER.get());
        com.fiskmods.heroes.common.item.ItemQuiver.createInventory(quiver).setStackInSlot(0,
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ARROW, 4));
        player.getInventory().selected = 0;
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, quiver);
        var menu = new com.fiskmods.heroes.common.item.QuiverMenu(1, player.getInventory(), net.minecraft.world.InteractionHand.MAIN_HAND);
        if (!menu.stillValid(player) || !menu.quickMoveStack(player, 32).isEmpty())
        { helper.fail("Open quiver must not be shift-moved"); return; }
        menu.clicked(0, 0, net.minecraft.world.inventory.ClickType.SWAP, player);
        if (player.getMainHandItem() != quiver || menu.getSlot(0).getItem().getCount() != 4)
        { helper.fail("Number key must not swap open quiver out of its hand"); return; }
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                new net.minecraft.world.item.ItemStack(com.fiskmods.heroes.common.item.ModItems.QUIVER.get()));
        if (menu.stillValid(player)) { helper.fail("Replacing the quiver must invalidate its old inventory"); return; }
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void cactusSpikeRingAndAimedBurstUseProjectiles(GameTestHelper helper)
    {
        var level = helper.getLevel(); var origin = helper.absolutePos(new net.minecraft.core.BlockPos(1, 180, 1));
        var player = helper.makeMockPlayer(); player.setPos(origin.getX() + .5, origin.getY(), origin.getZ() + .5);
        player.getAbilities().instabuild = false; player.getFoodData().setFoodLevel(20);
        var data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(player);
        var entry = new com.fiskmods.heroes.common.hero.power.ModifierEntry(com.fiskmods.heroes.common.hero.modifier.Modifiers.SPIKES, null);
        entry.setProperty(com.fiskmods.heroes.common.hero.power.PowerProperty.DAMAGE_PROFILE,
                com.google.gson.JsonParser.parseString("{\"damage\":4.5,\"types\":{\"CACTUS\":1.0,\"SHARP\":1.0}}"));
        entry.getModifier().onActivate(player, entry, data);
        var area = player.getBoundingBox().inflate(12);
        var spikes = level.getEntitiesOfClass(com.fiskmods.heroes.common.entity.projectile.CactusSpikeEntity.class, area,
                spike -> spike.getOwner() == player);
        if (spikes.size() != 72 || player.getFoodData().getExhaustionLevel() != 6.0F
                || spikes.stream().noneMatch(spike -> spike.getDeltaMovement().x > 4.4)
                || spikes.stream().noneMatch(spike -> spike.getDeltaMovement().x < -4.4))
        { helper.fail("Spike ring must fire 72 radial projectiles and cost six exhaustion"); return; }
        spikes.forEach(net.minecraft.world.entity.Entity::discard);
        float before = player.getFoodData().getExhaustionLevel();
        if (!com.fiskmods.heroes.common.hero.modifier.ModifierSpikeBurst.fireAimed(player, entry))
        { helper.fail("Aimed cactus burst should fire with adequate food"); return; }
        spikes = level.getEntitiesOfClass(com.fiskmods.heroes.common.entity.projectile.CactusSpikeEntity.class, area,
                spike -> spike.getOwner() == player && !spike.isRemoved());
        if (spikes.size() != 11 || Math.abs(player.getFoodData().getExhaustionLevel() - before - 1) > .001
                || spikes.stream().noneMatch(spike -> spike.getDeltaMovement().z > 4.4))
        { helper.fail("Aimed burst must fire eleven spikes forward and cost one exhaustion"); return; }
        var saved = new net.minecraft.nbt.CompoundTag(); spikes.get(0).addAdditionalSaveData(saved);
        var copy = new com.fiskmods.heroes.common.entity.projectile.CactusSpikeEntity(
                com.fiskmods.heroes.common.entity.ModEntities.CACTUS_SPIKE.get(), level);
        copy.readAdditionalSaveData(saved); var restored = new net.minecraft.nbt.CompoundTag(); copy.addAdditionalSaveData(restored);
        if (!restored.getString("DamageProfile").contains("4.5") || !restored.getString("DamageProfile").contains("CACTUS"))
        { helper.fail("Configured spike damage must survive projectile save and reload"); return; }
        spikes.forEach(net.minecraft.world.entity.Entity::discard);
        player.getFoodData().setFoodLevel(11);
        entry.getModifier().onActivate(player, entry, data);
        if (com.fiskmods.heroes.common.hero.modifier.ModifierSpikeBurst.fireAimed(player, entry)
                || !level.getEntitiesOfClass(com.fiskmods.heroes.common.entity.projectile.CactusSpikeEntity.class, area,
                    spike -> spike.getOwner() == player && !spike.isRemoved()).isEmpty())
        { helper.fail("Spike attacks must not fire below twelve hunger"); return; }
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void cactusSpikeAppliesConfiguredDamageAndCactusType(GameTestHelper helper)
    {
        var level = helper.getLevel(); var base = helper.absolutePos(new net.minecraft.core.BlockPos(1, 180, 1));
        var player = helper.makeMockPlayer(); player.setPos(base.getX(), base.getY(), base.getZ() + .5);
        var target = net.minecraft.world.entity.EntityType.IRON_GOLEM.create(level);
        if (target == null) { helper.fail("Could not create spike target"); return; }
        target.setNoAi(true); target.setNoGravity(true); target.setPos(base.getX() + 3, base.getY(), base.getZ() + .5); level.addFreshEntity(target);
        var spike = new com.fiskmods.heroes.common.entity.projectile.CactusSpikeEntity(player);
        spike.setDamageProfile(com.google.gson.JsonParser.parseString("{\"damage\":4.5,\"types\":{\"CACTUS\":1.0,\"SHARP\":1.0}}"));
        spike.setPos(base.getX() + 1, base.getY() + 1, base.getZ() + .5); spike.setDeltaMovement(4, 0, 0);
        level.addFreshEntity(spike); float health = target.getHealth(); spike.tick();
        if (!spike.isRemoved() || Math.abs(target.getHealth() - (health - 4.5F)) > .01)
        { helper.fail("Actual spike impact must apply its configured damage"); return; }
        var cactus = com.fiskmods.heroes.common.hero.Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:senor_cactus");
        var victim = helper.makeMockPlayer(); var data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(victim);
        if (cactus == null || data == null) { helper.fail("Cactus hero data missing"); return; }
        data.setHero(cactus.getDefaultIteration()); victim.getAbilities().invulnerable = false; victim.setHealth(20);
        var originalSpike = new com.fiskmods.heroes.common.entity.projectile.CactusSpikeEntity(player);
        var tag = new net.minecraft.nbt.CompoundTag(); originalSpike.addAdditionalSaveData(tag);
        var profile = com.google.gson.JsonParser.parseString(tag.getString("DamageProfile"));
        com.fiskmods.heroes.common.hero.modifier.DamageGroups.applyProfileDamage(victim, player,
                level.damageSources().thrown(originalSpike, player), 1.5F, profile);
        if (victim.getHealth() != 20.0F) { helper.fail("Default cactus spike must respect cactus immunity"); return; }
        target.discard(); helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void trickArrowEffectsRequireSuccessfulDamage(GameTestHelper helper)
    {
        var level = helper.getLevel(); var base = helper.absolutePos(new net.minecraft.core.BlockPos(1, 180, 1));
        var player = helper.makeMockPlayer(); player.setPos(base.getX(), base.getY(), base.getZ() + .5);
        var target = net.minecraft.world.entity.EntityType.IRON_GOLEM.create(level);
        if (target == null) { helper.fail("Could not create arrow target"); return; }
        target.setNoAi(true); target.setNoGravity(true); target.setInvulnerable(true);
        target.setPos(base.getX() + 3, base.getY(), base.getZ() + .5); level.addFreshEntity(target);
        for (String type : new String[] {"carrot", "gross", "phantom", "tutridium", "blaze", "fire_charge"})
        {
            var arrow = new com.fiskmods.heroes.common.entity.arrow.TrickArrowEntity(
                    com.fiskmods.heroes.common.entity.ModEntities.TRICK_ARROW.get(), level, player, type);
            arrow.setPos(base.getX() + 1, base.getY() + 1, base.getZ() + .5); arrow.setDeltaMovement(4, 0, 0);
            level.addFreshEntity(arrow); arrow.tick(); arrow.discard();
            if (!target.getActiveEffects().isEmpty() || target.isOnFire())
            { helper.fail(type + " arrow must not apply post-damage effects to an invulnerable target"); return; }
        }
        target.setInvulnerable(false);
        var carrot = new com.fiskmods.heroes.common.entity.arrow.TrickArrowEntity(
                com.fiskmods.heroes.common.entity.ModEntities.TRICK_ARROW.get(), level, player, "carrot");
        carrot.setPos(base.getX() + 1, base.getY() + 1, base.getZ() + .5); carrot.setDeltaMovement(4, 0, 0);
        level.addFreshEntity(carrot); carrot.tick();
        if (!target.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION))
        { helper.fail("A successful carrot arrow hit must still apply night vision"); return; }
        carrot.discard(); target.discard(); helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void enderArrowUsesImpactAndAvoidsObstructedDestination(GameTestHelper helper)
    {
        var level = helper.getLevel(); var base = helper.absolutePos(new net.minecraft.core.BlockPos(1, 180, 1));
        var wall = base.offset(4, 0, 0); level.setBlockAndUpdate(wall, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        var player = helper.makeMockPlayer(); player.setPos(base.getX(), base.getY(), base.getZ() + .5); player.fallDistance = 10;
        var arrow = new com.fiskmods.heroes.common.entity.arrow.TrickArrowEntity(
                com.fiskmods.heroes.common.entity.ModEntities.TRICK_ARROW.get(), level, player, "ender_pearl");
        arrow.setPos(base.getX() + 1, base.getY() + .5, base.getZ() + .5); arrow.setDeltaMovement(4, 0, 0);
        level.addFreshEntity(arrow); arrow.tick();
        if (Math.abs(player.getX() - (wall.getX() - .5)) > .01 || Math.abs(player.getY() - (wall.getY() + .5)) > .1
                || player.fallDistance != 0 || !"normal".equals(arrow.getArrowType()))
        { helper.fail("Ender arrow must teleport to the impact face, not its previous position"); return; }
        arrow.discard();
        player.setPos(base.getX(), base.getY(), base.getZ() + .5); player.fallDistance = 10;
        var obstruction = wall.west().above(); level.setBlockAndUpdate(obstruction, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        var blocked = new com.fiskmods.heroes.common.entity.arrow.TrickArrowEntity(
                com.fiskmods.heroes.common.entity.ModEntities.TRICK_ARROW.get(), level, player, "ender_pearl");
        blocked.setPos(base.getX() + 1, base.getY() + .5, base.getZ() + .5); blocked.setDeltaMovement(4, 0, 0);
        level.addFreshEntity(blocked); blocked.tick();
        if (Math.abs(player.getX() - base.getX()) > .01 || player.fallDistance != 10)
        { helper.fail("Ender arrow must not teleport a player into a solid ceiling"); return; }
        blocked.discard(); level.removeBlock(obstruction, false); level.removeBlock(wall, false); helper.succeed();
    }

    public static final class EnderArrowTeleportGuard
    {
        private final net.minecraft.world.entity.player.Player player;
        private boolean called;
        EnderArrowTeleportGuard(net.minecraft.world.entity.player.Player player) { this.player = player; }

        @net.minecraftforge.eventbus.api.SubscribeEvent
        public void onTeleport(net.minecraftforge.event.entity.EntityTeleportEvent event)
        {
            if (event.getEntity() == player)
            {
                called = true;
                event.setCanceled(true);
            }
        }
    }

    @GameTest(template = "test_empty")
    public static void enderArrowRespectsTeleportCancellation(GameTestHelper helper)
    {
        var level = helper.getLevel(); var base = helper.absolutePos(new net.minecraft.core.BlockPos(1, 180, 1));
        var wall = base.offset(4, 0, 0); level.setBlockAndUpdate(wall, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        var player = helper.makeMockPlayer(); player.setPos(base.getX(), base.getY(), base.getZ() + .5); player.fallDistance = 10;
        var guard = new EnderArrowTeleportGuard(player);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(guard);
        try
        {
            var arrow = new com.fiskmods.heroes.common.entity.arrow.TrickArrowEntity(
                    com.fiskmods.heroes.common.entity.ModEntities.TRICK_ARROW.get(), level, player, "ender_pearl");
            arrow.setPos(base.getX() + 1, base.getY() + .5, base.getZ() + .5); arrow.setDeltaMovement(4, 0, 0);
            level.addFreshEntity(arrow); arrow.tick(); arrow.discard();
            if (!guard.called || Math.abs(player.getX() - base.getX()) > .01 || player.fallDistance != 10)
            { helper.fail("A canceled Forge teleport event must prevent the ender arrow from moving or damaging the player"); return; }
        }
        finally
        {
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(guard);
            level.removeBlock(wall, false);
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void flashRingPartialSuitPreservesOtherArmor(GameTestHelper helper)
    {
        var level = helper.getLevel();
        var player = new net.minecraftforge.common.util.FakePlayer(level,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "flash-ring-test"));
        var oldHelmet = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_HELMET);
        oldHelmet.getOrCreateTag().putString("marker", "original helmet");
        var chest = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_CHESTPLATE);
        chest.getOrCreateTag().putString("marker", "leave equipped");
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, oldHelmet);
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, chest);
        var ring = new net.minecraft.world.item.ItemStack(com.fiskmods.heroes.common.item.ModItems.FLASH_RING.get());
        com.fiskmods.heroes.common.item.ItemFlashRing.setContainedArmor(ring,
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_HELMET));
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ring);
        ring.use(level, player, net.minecraft.world.InteractionHand.MAIN_HAND);
        var equipped = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);
        var keptChest = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
        boolean returned = player.getInventory().items.stream().anyMatch(stack ->
                stack.is(net.minecraft.world.item.Items.IRON_HELMET) && stack.hasTag()
                && "original helmet".equals(stack.getTag().getString("marker")));
        if (!equipped.is(net.minecraft.world.item.Items.DIAMOND_HELMET)
                || keptChest != chest || !returned
                || com.fiskmods.heroes.common.item.ItemFlashRing.getContainedArmor(ring) != null)
        { helper.fail("Deploying a partial ring must preserve unaffected armor and return displaced pieces"); return; }
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void flashRingReplacementClearsStaleHeroIdentity(GameTestHelper helper)
    {
        var ring = new net.minecraft.world.item.ItemStack(com.fiskmods.heroes.common.item.ModItems.FLASH_RING.get());
        ring.getOrCreateTag().putString(com.fiskmods.heroes.common.hero.ItemHeroArmor.TAG_HERO, "fiskheroes:the_flash");
        ring.getOrCreateTag().putString(com.fiskmods.heroes.common.hero.ItemHeroArmor.TAG_ITERATION, "default");
        com.fiskmods.heroes.common.item.ItemFlashRing.setContainedArmor(ring,
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_HELMET));
        if (ring.getTag().contains(com.fiskmods.heroes.common.hero.ItemHeroArmor.TAG_HERO)
                || ring.getTag().contains(com.fiskmods.heroes.common.hero.ItemHeroArmor.TAG_ITERATION)
                || com.fiskmods.heroes.common.item.ItemFlashRing.getContainedHero(ring) != null)
        { helper.fail("Replacing ring contents must remove any previous hero identity"); return; }
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void automaticQuiverCraftingAndRestock(GameTestHelper helper)
    {
        var level = helper.getLevel();
        var player = new net.minecraftforge.common.util.FakePlayer(level,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "auto-quiver-test"));
        var normal = new net.minecraft.world.item.ItemStack(com.fiskmods.heroes.common.item.ModItems.QUIVER.get());
        var stored = com.fiskmods.heroes.common.item.ItemTrickArrow.createStack("explosive"); stored.setCount(2);
        com.fiskmods.heroes.common.item.ItemQuiver.createInventory(normal).setStackInSlot(0, stored);
        normal.getOrCreateTag().putString("CustomTest", "preserved");
        var grid = new net.minecraft.world.inventory.TransientCraftingContainer(player.inventoryMenu, 2, 2);
        grid.setItem(0, normal); grid.setItem(1, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.HOPPER));
        var recipe = new com.fiskmods.heroes.common.recipe.AutomaticQuiverRecipe(
                new net.minecraft.resources.ResourceLocation("fiskheroes", "automatic_quiver"),
                net.minecraft.world.item.crafting.CraftingBookCategory.EQUIPMENT);
        if (!recipe.matches(grid, level)) { helper.fail("Quiver and hopper must make the automatic quiver"); return; }
        var auto = recipe.assemble(grid, level.registryAccess());
        if (!com.fiskmods.heroes.common.item.ItemQuiver.isAutomatic(auto)
                || !"preserved".equals(auto.getTag().getString("CustomTest"))
                || com.fiskmods.heroes.common.item.ItemQuiver.createInventory(auto).getStackInSlot(0).getCount() != 2)
        { helper.fail("Quiver conversion must preserve stored arrows and item data"); return; }
        grid.setItem(0, auto);
        if (recipe.matches(grid, level)) { helper.fail("Automatic quiver must not be converted a second time"); return; }
        player.getInventory().setItem(1, auto);
        var refill = com.fiskmods.heroes.common.item.ItemTrickArrow.createStack("normal"); refill.setCount(3);
        player.getInventory().setItem(2, refill);
        player.tickCount = 8;
        auto.getItem().inventoryTick(auto, level, player, 1, false);
        var loaded = com.fiskmods.heroes.common.item.ItemQuiver.createInventory(auto);
        if (refill.getCount() != 2 || loaded.getStackInSlot(1).getCount() != 1
                || !"normal".equals(com.fiskmods.heroes.common.item.ItemTrickArrow.getType(loaded.getStackInSlot(1))))
        { helper.fail("Automatic quiver must restock one trick arrow every eight ticks"); return; }
        player.getInventory().selected = 1;
        player.containerMenu = new com.fiskmods.heroes.common.item.QuiverMenu(1, player.getInventory(), net.minecraft.world.InteractionHand.MAIN_HAND);
        auto.getItem().inventoryTick(auto, level, player, 1, false);
        if (refill.getCount() != 2) { helper.fail("Auto-restock must pause while its inventory is open"); return; }
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void payloadArrowRecipesPreserveIngredients(GameTestHelper helper)
    {
        var level = helper.getLevel();
        var player = new net.minecraftforge.common.util.FakePlayer(level,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "payload-recipe-test"));
        var grid = new net.minecraft.world.inventory.TransientCraftingContainer(player.inventoryMenu, 2, 2);
        var normal = com.fiskmods.heroes.common.item.ItemTrickArrow.createStack("normal");
        grid.setItem(0, normal);
        var potion = net.minecraft.world.item.alchemy.PotionUtils.setPotion(
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.POTION),
                net.minecraft.world.item.alchemy.Potions.STRONG_HEALING);
        potion.getOrCreateTag().putInt("CustomPotionColor", 123456);
        grid.setItem(1, potion);
        var vial = level.getRecipeManager().byKey(new net.minecraft.resources.ResourceLocation("fiskheroes", "arrow_vial")).orElse(null);
        if (!(vial instanceof com.fiskmods.heroes.common.recipe.PayloadArrowRecipe vialRecipe) || !vialRecipe.matches(grid, level))
        { helper.fail("Loaded vial recipe must accept a normal arrow and drinkable potion"); return; }
        var vialArrow = vialRecipe.assemble(grid, level.registryAccess());
        var attached = com.fiskmods.heroes.common.item.ItemTrickArrow.getAttachedPotion(vialArrow);
        if (!attached.is(net.minecraft.world.item.Items.SPLASH_POTION)
                || net.minecraft.world.item.alchemy.PotionUtils.getPotion(attached) != net.minecraft.world.item.alchemy.Potions.STRONG_HEALING
                || attached.getTag().getInt("CustomPotionColor") != 123456)
        { helper.fail("Vial recipe must copy potion type and custom data into a splash payload"); return; }
        var projectile = (com.fiskmods.heroes.common.entity.arrow.TrickArrowEntity)
                ((com.fiskmods.heroes.common.item.ItemTrickArrow)vialArrow.getItem()).createArrow(level, vialArrow, player);
        if (net.minecraft.world.item.alchemy.PotionUtils.getPotion(projectile.getVialPotion()) != net.minecraft.world.item.alchemy.Potions.STRONG_HEALING)
        { helper.fail("Fired vial must use the crafted potion, not default poison"); return; }
        grid.setItem(1, net.minecraft.world.item.alchemy.PotionUtils.setPotion(
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SPLASH_POTION),
                net.minecraft.world.item.alchemy.Potions.SWIFTNESS));
        if (!vialRecipe.matches(grid, level) || net.minecraft.world.item.alchemy.PotionUtils.getPotion(
                com.fiskmods.heroes.common.item.ItemTrickArrow.getAttachedPotion(vialRecipe.assemble(grid, level.registryAccess())))
                != net.minecraft.world.item.alchemy.Potions.SWIFTNESS)
        { helper.fail("Splash-potion recipes must also retain their effect"); return; }
        var rocket = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.FIREWORK_ROCKET, 3);
        var fireworks = new net.minecraft.nbt.CompoundTag(); fireworks.putByte("Flight", (byte) 2);
        var explosions = new net.minecraft.nbt.ListTag(); var explosion = new net.minecraft.nbt.CompoundTag();
        explosion.putByte("Type", (byte) 1); explosion.putIntArray("Colors", new int[] { 0x123456 });
        explosions.add(explosion); fireworks.put("Explosions", explosions); rocket.getOrCreateTag().put("Fireworks", fireworks);
        grid.setItem(1, rocket);
        var firework = level.getRecipeManager().byKey(new net.minecraft.resources.ResourceLocation("fiskheroes", "arrow_firework")).orElse(null);
        if (!(firework instanceof com.fiskmods.heroes.common.recipe.PayloadArrowRecipe fireworkRecipe) || !fireworkRecipe.matches(grid, level))
        { helper.fail("Loaded firework recipe must accept a normal arrow and rocket"); return; }
        var fireworkArrow = fireworkRecipe.assemble(grid, level.registryAccess());
        var attachedRocket = com.fiskmods.heroes.common.item.ItemTrickArrow.getAttachedItem(fireworkArrow);
        var fired = (com.fiskmods.heroes.common.entity.arrow.TrickArrowEntity)
                ((com.fiskmods.heroes.common.item.ItemTrickArrow)fireworkArrow.getItem()).createArrow(level, fireworkArrow, player);
        if (attachedRocket.getCount() != 1 || !net.minecraft.world.item.ItemStack.isSameItemSameTags(rocket, attachedRocket)
                || !net.minecraft.world.item.ItemStack.isSameItemSameTags(attachedRocket, fired.getFireworkStack()))
        { helper.fail("Crafted and fired firework arrow must preserve the rocket's explosion and flight data"); return; }
        grid.setItem(2, normal.copy());
        if (fireworkRecipe.matches(grid, level)) { helper.fail("Recipe must reject extra ingredients"); return; }
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void vialArrowBurstsAndBecomesNormalOnImpact(GameTestHelper helper)
    {
        var level = helper.getLevel(); var base = helper.absolutePos(new net.minecraft.core.BlockPos(1, 180, 1));
        var wall = base.offset(4, 0, 0); level.setBlockAndUpdate(wall, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        var player = helper.makeMockPlayer(); player.setPos(base.getX(), base.getY(), base.getZ());
        var target = net.minecraft.world.entity.EntityType.IRON_GOLEM.create(level);
        if (target == null) { helper.fail("Could not create potion target"); return; }
        target.setNoAi(true); target.setNoGravity(true); target.setPos(wall.getX() + 1.5, wall.getY(), wall.getZ() + .5); level.addFreshEntity(target);
        var arrow = new com.fiskmods.heroes.common.entity.arrow.TrickArrowEntity(
                com.fiskmods.heroes.common.entity.ModEntities.TRICK_ARROW.get(), level, player, "vial");
        arrow.setVialPotion(net.minecraft.world.item.alchemy.PotionUtils.setPotion(
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SPLASH_POTION),
                net.minecraft.world.item.alchemy.Potions.SWIFTNESS));
        arrow.setPos(wall.getX() - .1, wall.getY() + .5, wall.getZ() + .5); arrow.setDeltaMovement(2, 0, 0);
        level.addFreshEntity(arrow); arrow.tick();
        if (!target.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED)
                || !arrow.isAnchored() || !"normal".equals(arrow.getArrowType()) || !arrow.getVialPotion().isEmpty())
        { helper.fail("Vial must splash its potion on impact and leave an ordinary arrow"); return; }
        arrow.discard(); target.discard(); level.removeBlock(wall, false); helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void fireworkArrowFlashesWithoutArrowDamage(GameTestHelper helper)
    {
        var level = helper.getLevel(); var base = helper.absolutePos(new net.minecraft.core.BlockPos(1, 180, 1));
        var player = helper.makeMockPlayer(); player.setPos(base.getX(), base.getY(), base.getZ() + .5);
        var target = net.minecraft.world.entity.EntityType.IRON_GOLEM.create(level);
        var creeper = net.minecraft.world.entity.EntityType.CREEPER.create(level);
        if (target == null || creeper == null) { helper.fail("Could not create firework targets"); return; }
        target.setNoAi(true); target.setNoGravity(true); target.setPos(base.getX() + 3, base.getY(), base.getZ() + .5); level.addFreshEntity(target);
        creeper.setNoAi(true); creeper.setNoGravity(true); creeper.setPos(base.getX() + 3, base.getY(), base.getZ() + 2.5); level.addFreshEntity(creeper);
        float health = target.getHealth();
        var item = com.fiskmods.heroes.common.item.ItemTrickArrow.createStack("firework");
        var arrow = (com.fiskmods.heroes.common.entity.arrow.TrickArrowEntity)
                ((com.fiskmods.heroes.common.item.ItemTrickArrow)item.getItem()).createArrow(level, item, player);
        arrow.setPos(base.getX() + 1, base.getY() + 1, base.getZ() + .5); arrow.setDeltaMovement(4, 0, 0); level.addFreshEntity(arrow); arrow.tick();
        var blind = target.getEffect(net.minecraft.world.effect.MobEffects.BLINDNESS);
        if (!arrow.isRemoved() || target.getHealth() != health || blind == null || blind.getDuration() != 120 || !creeper.isIgnited())
        { helper.fail("Firework impact must flash the direct target without arrow damage and ignite nearby creepers"); return; }
        target.discard(); creeper.discard(); helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void fireworkArrowDoesNotStackRepeatedFlickerAndTrail(GameTestHelper helper)
    {
        var level = helper.getLevel(); var base = helper.absolutePos(new net.minecraft.core.BlockPos(1, 180, 1));
        var player = helper.makeMockPlayer(); player.setPos(base.getX(), base.getY(), base.getZ() + .5);
        var target = net.minecraft.world.entity.EntityType.IRON_GOLEM.create(level);
        if (target == null) { helper.fail("Could not create firework target"); return; }
        target.setNoAi(true); target.setNoGravity(true); target.setPos(base.getX() + 3, base.getY(), base.getZ() + .5); level.addFreshEntity(target);
        var item = com.fiskmods.heroes.common.item.ItemTrickArrow.createStack("firework");
        var rocket = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.FIREWORK_ROCKET);
        var explosions = new net.minecraft.nbt.ListTag();
        for (int i = 0; i < 2; ++i)
        {
            var burst = new net.minecraft.nbt.CompoundTag(); burst.putByte("Type", (byte) 0);
            burst.putBoolean("Flicker", true); burst.putBoolean("Trail", true); explosions.add(burst);
        }
        var fireworks = new net.minecraft.nbt.CompoundTag(); fireworks.put("Explosions", explosions);
        rocket.getOrCreateTag().put("Fireworks", fireworks);
        com.fiskmods.heroes.common.item.ItemTrickArrow.setAttachedItem(item, rocket);
        var arrow = (com.fiskmods.heroes.common.entity.arrow.TrickArrowEntity)
                ((com.fiskmods.heroes.common.item.ItemTrickArrow)item.getItem()).createArrow(level, item, player);
        arrow.setPos(base.getX() + 1, base.getY() + 1, base.getZ() + .5); arrow.setDeltaMovement(4, 0, 0); level.addFreshEntity(arrow); arrow.tick();
        var effect = target.getEffect(net.minecraft.world.effect.MobEffects.BLINDNESS);
        if (effect == null || effect.getDuration() != 170)
        { helper.fail("Repeated flicker and trail must each increase flash duration only once"); return; }
        target.discard(); helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void fireballArrowIgnitesSplashTargetsAndLeavesArrow(GameTestHelper helper)
    {
        var level = helper.getLevel(); var base = helper.absolutePos(new net.minecraft.core.BlockPos(1, 180, 1));
        var wall = base.offset(4, 0, 0); level.setBlockAndUpdate(wall, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        var player = helper.makeMockPlayer(); player.setPos(base.getX(), base.getY(), base.getZ());
        var target = net.minecraft.world.entity.EntityType.IRON_GOLEM.create(level);
        if (target == null) { helper.fail("Could not create fireball target"); return; }
        target.setNoAi(true); target.setNoGravity(true); target.setPos(wall.getX() + 1.5, wall.getY(), wall.getZ() + .5); level.addFreshEntity(target);
        float health = target.getHealth();
        var arrow = new com.fiskmods.heroes.common.entity.arrow.TrickArrowEntity(
                com.fiskmods.heroes.common.entity.ModEntities.TRICK_ARROW.get(), level, player, "fireball");
        arrow.setPos(wall.getX() - .1, wall.getY() + .5, wall.getZ() + .5); arrow.setDeltaMovement(2, 0, 0);
        level.addFreshEntity(arrow); arrow.tick();
        if (target.getHealth() >= health || !target.isOnFire() || arrow.isRemoved()
                || !arrow.isAnchored() || !"normal".equals(arrow.getArrowType()))
        { helper.fail("Fireball burst must damage and ignite nearby targets and leave a normal arrow"); return; }
        arrow.discard(); target.discard(); level.removeBlock(wall, false); helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void torchArrowPlacesWallTorchAndRetainsUnplaceablePayload(GameTestHelper helper)
    {
        var level = helper.getLevel(); var base = helper.absolutePos(new net.minecraft.core.BlockPos(1, 180, 1));
        var wall = base.offset(4, 0, 0); level.setBlockAndUpdate(wall, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        var player = helper.makeMockPlayer(); player.setPos(base.getX(), base.getY(), base.getZ());
        var arrow = new com.fiskmods.heroes.common.entity.arrow.TrickArrowEntity(
                com.fiskmods.heroes.common.entity.ModEntities.TRICK_ARROW.get(), level, player, "torch");
        arrow.setPos(wall.getX() - .1, wall.getY() + .5, wall.getZ() + .5); arrow.setDeltaMovement(2, 0, 0);
        level.addFreshEntity(arrow); arrow.tick();
        var torch = level.getBlockState(wall.west());
        if (!torch.is(net.minecraft.world.level.block.Blocks.WALL_TORCH)
                || torch.getValue(net.minecraft.world.level.block.WallTorchBlock.FACING) != net.minecraft.core.Direction.WEST
                || !"normal".equals(arrow.getArrowType()))
        { helper.fail("Torch arrow must place a correctly oriented wall torch and consume its tip"); return; }
        arrow.discard(); level.removeBlock(wall.west(), false);
        var underside = new com.fiskmods.heroes.common.entity.arrow.TrickArrowEntity(
                com.fiskmods.heroes.common.entity.ModEntities.TRICK_ARROW.get(), level, player, "torch");
        underside.pickup = net.minecraft.world.entity.projectile.AbstractArrow.Pickup.ALLOWED;
        underside.setPos(wall.getX() + .5, wall.getY() - .1, wall.getZ() + .5); underside.setDeltaMovement(0, 2, 0);
        level.addFreshEntity(underside); underside.tick();
        if (!"torch".equals(underside.getArrowType()) || !level.isEmptyBlock(wall.below()))
        { helper.fail("A torch that cannot be placed on a ceiling must retain its payload"); return; }
        for (int i = 0; i < 8; ++i) underside.tick();
        underside.playerTouch(player);
        boolean returned = player.getInventory().items.stream().anyMatch(stack -> stack.is(com.fiskmods.heroes.common.item.ModItems.TRICK_ARROW.get())
                && "torch".equals(com.fiskmods.heroes.common.item.ItemTrickArrow.getType(stack)));
        if (!returned) { helper.fail("An unspent torch arrow must be retrievable with its tip"); return; }
        underside.discard(); level.removeBlock(wall, false); helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void carrotArrowCanBeEatenAndReturnsNormalArrow(GameTestHelper helper)
    {
        var player = helper.makeMockPlayer();
        player.getAbilities().instabuild = false;
        player.getFoodData().setFoodLevel(10);
        var carrot = com.fiskmods.heroes.common.item.ItemTrickArrow.createStack("carrot"); carrot.setCount(2);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, carrot);
        var item = carrot.getItem();
        if (!item.use(helper.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND).getResult().consumesAction()
                || item.getUseAnimation(carrot) != net.minecraft.world.item.UseAnim.EAT)
        { helper.fail("Carrot arrow must start eating when hungry"); return; }
        var result = item.finishUsingItem(carrot, helper.getLevel(), player);
        boolean returned = player.getInventory().items.stream().anyMatch(stack -> stack.getItem() instanceof com.fiskmods.heroes.common.item.ItemTrickArrow
                && "normal".equals(com.fiskmods.heroes.common.item.ItemTrickArrow.getType(stack)));
        if (player.getFoodData().getFoodLevel() != 14 || result.getCount() != 1 || !returned)
        { helper.fail("Eating carrot arrow must restore hunger and return one normal trick arrow"); return; }
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void teleportAvoidsHeadCollisionAndClearsFallDistance(GameTestHelper helper)
    {
        var level = helper.getLevel();
        var base = helper.absolutePos(new net.minecraft.core.BlockPos(1, 180, 1));
        var player = helper.makeMockPlayer();
        player.setPos(base.getX() + .5, base.getY(), base.getZ() + .5);
        player.setYRot(0); player.setXRot(0); player.fallDistance = 20;
        var data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(player);
        if (data == null) { helper.fail("Teleport data missing"); return; }
        var entry = new com.fiskmods.heroes.common.hero.power.ModifierEntry(
                com.fiskmods.heroes.common.hero.modifier.Modifiers.TELEPORTATION, null);
        entry.setProperty(com.fiskmods.heroes.common.hero.power.PowerProperty.RANGE, 8.0F);
        var wall = base.offset(0, 1, 4);
        level.setBlockAndUpdate(wall, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        entry.getModifier().onActivate(player, entry, data);
        if (player.getZ() >= wall.getZ() - .25 || player.getZ() <= base.getZ() + 1
                || !level.noCollision(player, player.getBoundingBox()) || player.fallDistance != 0)
        { helper.fail("Teleport must stop before head-level obstruction and clear fall distance"); return; }
        level.removeBlock(wall, false);
        player.setPos(base.getX() + .5, base.getY(), base.getZ() + .5);
        entry.getModifier().onActivate(player, entry, data);
        if (player.getZ() < base.getZ() + 8)
        { helper.fail("Teleport must still travel its full range in open space"); return; }
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void telekinesisReleaseAndStaleOwnershipAreRecoverable(GameTestHelper helper)
    {
        var level = helper.getLevel();
        var player = new net.minecraftforge.common.util.FakePlayer(level,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "telekinesis-cleanup-test"));
        var data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(player);
        var target = net.minecraft.world.entity.EntityType.COW.create(level);
        if (target == null || data == null) { helper.fail("Telekinesis test setup missing"); return; }
        target.setPos(helper.absolutePos(new net.minecraft.core.BlockPos(1, 180, 1)).getCenter());
        level.addFreshEntity(target);
        var tag = com.fiskmods.heroes.common.hero.modifier.TelekinesisState.GRABBED_BY_TAG;
        target.getPersistentData().putInt(tag, player.getId());
        data.getData().set(com.fiskmods.heroes.common.data.var.Vars.TELEKINESIS, true);
        data.getData().set(com.fiskmods.heroes.common.data.var.Vars.GRAB_ID, target.getId());
        com.fiskmods.heroes.common.hero.ability.AbilityHandler.clear(player);
        if (target.getPersistentData().contains(tag) || data.getData().get(com.fiskmods.heroes.common.data.var.Vars.GRAB_ID) != -1)
        { helper.fail("Suit change or disconnect must release the original grabbed entity"); return; }
        target.getPersistentData().putInt(tag, 987654321);
        if (!com.fiskmods.heroes.common.hero.modifier.TelekinesisState.canClaim(player, target)
                || target.getPersistentData().contains(tag))
        { helper.fail("A stale saved grab owner must not permanently lock the entity"); return; }
        target.discard(); helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void bouncingArrowsUseIncomingSpeedEvenCloseToWall(GameTestHelper helper)
    {
        var level = helper.getLevel();
        var base = helper.absolutePos(new net.minecraft.core.BlockPos(1, 180, 1));
        var wall = base.offset(4, 0, 0);
        level.setBlockAndUpdate(wall, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        var player = helper.makeMockPlayer();
        player.setPos(base.getX() + .5, base.getY(), base.getZ() + .5);
        for (String type : new String[] {"slime", "pufferfish"})
        {
            var arrow = new com.fiskmods.heroes.common.entity.arrow.TrickArrowEntity(
                    com.fiskmods.heroes.common.entity.ModEntities.TRICK_ARROW.get(), level, player, type);
            arrow.setPos(wall.getX() - .1, wall.getY() + .5, wall.getZ() + .5);
            arrow.setDeltaMovement(2, 0, 0); level.addFreshEntity(arrow); arrow.tick();
            double expected = "slime".equals(type) ? 1.2 : 1.0;
            if (arrow.isAnchored() || arrow.isRemoved() || Math.abs(arrow.getDeltaMovement().x + expected * .99) > .05)
            { helper.fail(type + " must bounce using approach velocity, not remaining distance to the wall"); return; }
            arrow.discard();
        }
        level.removeBlock(wall, false); helper.succeed();
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
    public static void controlledFlightTravelHoversAndRespondsToInput(GameTestHelper helper)
    {
        var player = new net.minecraftforge.common.util.FakePlayer(helper.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "fisk-flight-travel-test"));
        var data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(player);
        var hero = com.fiskmods.heroes.common.hero.Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:iron_man");
        if (data == null || hero == null) { helper.fail("Flight data or hero missing"); return; }
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST,
                com.fiskmods.heroes.common.hero.ItemHeroArmor.create(hero.getDefaultIteration(),
                        com.fiskmods.heroes.common.item.ModItems.heroArmor[1]));
        com.fiskmods.heroes.common.hero.HeroTracker.update(player);
        var pos = helper.absolutePos(new net.minecraft.core.BlockPos(1, 80, 1));
        player.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        player.setYRot(0.0F);
        player.setXRot(0.0F);
        player.setDeltaMovement(Vec3.ZERO);
        data.getData().set(com.fiskmods.heroes.common.data.var.Vars.FLYING, true);
        if (com.fiskmods.heroes.common.hero.ability.SuitTravel.controlledFlightEntry(player) == null)
        { helper.fail("Active controlled flight must qualify for the floating exemption"); return; }
        double y = player.getY();
        for (int i = 0; i < 100; ++i)
            if (!com.fiskmods.heroes.common.hero.ability.SuitTravel.controlledFlight(player))
            { helper.fail("Enabled controlled flight did not override travel"); return; }
        if (Math.abs(player.getY() - y) > 1.0E-7D)
        { helper.fail("Idle flight must not drift vertically"); return; }
        com.fiskmods.heroes.common.data.PlayerInputTracker.set(player, true, false, 0, 0);
        for (int i = 0; i < 20; ++i)
            com.fiskmods.heroes.common.hero.ability.SuitTravel.controlledFlight(player);
        if (player.getY() <= y + 0.5D)
        { helper.fail("Jump input must raise the flying player"); return; }
        double z = player.getZ();
        com.fiskmods.heroes.common.data.PlayerInputTracker.set(player, false, false, 1, 0);
        for (int i = 0; i < 20; ++i)
            com.fiskmods.heroes.common.hero.ability.SuitTravel.controlledFlight(player);
        if (player.getZ() <= z + 0.5D)
        { helper.fail("Forward input must steer the flying player"); return; }
        player.getAbilities().flying = true;
        if (com.fiskmods.heroes.common.hero.ability.SuitTravel.controlledFlight(player))
        { helper.fail("Creative flight must retain vanilla travel"); return; }
        player.getAbilities().flying = false;
        data.getData().set(com.fiskmods.heroes.common.data.var.Vars.FLYING, false);
        if (com.fiskmods.heroes.common.hero.ability.SuitTravel.controlledFlightEntry(player) != null)
        { helper.fail("Inactive suit flight must not qualify for the floating exemption"); return; }
        if (com.fiskmods.heroes.common.hero.ability.SuitTravel.controlledFlight(player))
        { helper.fail("Disabled suit flight must retain vanilla travel"); return; }
        com.fiskmods.heroes.common.data.PlayerInputTracker.clear(player);
        helper.succeed();
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

    @GameTest(template = "test_empty", timeoutTicks = 120)
    public static void ironManAimedShotsRequireAimAndCooldown(GameTestHelper helper)
    {
        var player = new net.minecraftforge.common.util.FakePlayer(helper.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "fisk-repulsor-test"));
        var hero = com.fiskmods.heroes.common.hero.Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:iron_man");
        if (hero == null) { helper.fail("Iron Man is missing"); return; }
        var slots = new net.minecraft.world.entity.EquipmentSlot[] {net.minecraft.world.entity.EquipmentSlot.HEAD,
                net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.LEGS,
                net.minecraft.world.entity.EquipmentSlot.FEET};
        for (int i = 0; i < 4; ++i)
            if (hero.getDefaultIteration().hasPieceOfSet(i)) player.setItemSlot(slots[i],
                    com.fiskmods.heroes.common.hero.ItemHeroArmor.create(hero.getDefaultIteration(),
                            com.fiskmods.heroes.common.item.ModItems.heroArmor[i]));
        if (com.fiskmods.heroes.common.hero.ability.AbilityHandler.fireAimedSuitShot(player))
        { helper.fail("Shot fired without aim"); return; }
        com.fiskmods.heroes.common.hero.ability.AbilityHandler.onAbilityKey(player, 1, true);
        if (!com.fiskmods.heroes.common.hero.ability.AbilityHandler.fireAimedSuitShot(player)
                || com.fiskmods.heroes.common.hero.ability.AbilityHandler.fireAimedSuitShot(player))
        { helper.fail("Shot must fire once then respect configured cooldown"); return; }
        com.fiskmods.heroes.common.hero.ability.AbilityHandler.onAbilityKey(player, 1, false);
        helper.runAfterDelay(31, () -> {
            if (com.fiskmods.heroes.common.hero.ability.AbilityHandler.fireAimedSuitShot(player))
            { helper.fail("Shot fired after aim was released"); return; }
            com.fiskmods.heroes.common.hero.ability.AbilityHandler.onAbilityKey(player, 1, true);
            if (!com.fiskmods.heroes.common.hero.ability.AbilityHandler.fireAimedSuitShot(player))
            { helper.fail("Shot did not recover after cooldown"); return; }
            com.fiskmods.heroes.common.hero.ability.AbilityHandler.clear(player);
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty")
    public static void mark50CannonHonorsNanitesAndAimPhase(GameTestHelper helper)
    {
        var player = new net.minecraftforge.common.util.FakePlayer(helper.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "fisk-cannon-test"));
        var hero = com.fiskmods.heroes.common.hero.Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:iron_man_mk50");
        var data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(player);
        if (hero == null || data == null) { helper.fail("Mark 50 or player data is missing"); return; }
        var slots = new net.minecraft.world.entity.EquipmentSlot[] {net.minecraft.world.entity.EquipmentSlot.HEAD,
                net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.LEGS,
                net.minecraft.world.entity.EquipmentSlot.FEET};
        for (int i = 0; i < 4; ++i)
            if (hero.getDefaultIteration().hasPieceOfSet(i)) player.setItemSlot(slots[i],
                    com.fiskmods.heroes.common.hero.ItemHeroArmor.create(hero.getDefaultIteration(),
                            com.fiskmods.heroes.common.item.ModItems.heroArmor[i]));
        com.fiskmods.heroes.common.hero.HeroTracker.update(player);
        com.fiskmods.heroes.common.hero.ability.AbilityHandler.onAbilityKey(player, 1, true);
        if (com.fiskmods.heroes.common.hero.ability.AbilityHandler.fireAimedSuitShot(player))
        { helper.fail("Cannon fired with nanites inactive"); return; }
        new com.fiskmods.heroes.pack.js.JSEntity(player).setData("fiskheroes:dyn/nanites", true);
        com.fiskmods.heroes.common.hero.ability.AbilityHandler.onAbilityKey(player, 1, false);
        com.fiskmods.heroes.common.hero.ability.AbilityHandler.onAbilityKey(player, 1, true);
        if (com.fiskmods.heroes.common.hero.ability.AbilityHandler.fireAimedSuitShot(player))
        { helper.fail("Cannon fired before aim settled"); return; }
        for (int i = 0; i < 20; ++i)
            com.fiskmods.heroes.common.hero.ability.AbilityHandler.tickAiming(player, data);
        if (!com.fiskmods.heroes.common.hero.ability.AbilityHandler.fireAimedSuitShot(player)
                || com.fiskmods.heroes.common.hero.ability.AbilityHandler.fireAimedSuitShot(player))
        { helper.fail("Cannon failed to fire after aiming or ignored cooldown"); return; }
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

    @GameTest(template = "test_empty")
    public static void crossbonesPunchChargesAndConsumesOnMelee(GameTestHelper helper)
    {
        var player = helper.makeMockPlayer();
        var target = helper.makeMockPlayer();
        var data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(player);
        var hero = com.fiskmods.heroes.common.hero.Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:crossbones");
        if (data == null || hero == null) { helper.fail("Crossbones or player data missing"); return; }
        data.setHero(hero.getDefaultIteration());
        var entry = hero.getPowerContainer().getEntries().stream()
                .filter(e -> e.getModifier().getId().getPath().equals("charged_punch")).findFirst().orElse(null);
        if (entry == null) { helper.fail("Charged punch missing"); return; }
        var mod = entry.getModifier();
        var vars = data.getData();
        mod.onActivate(player, entry, data);
        for (int i = 0; i < 5; i++) mod.tick(player, entry, data);
        if (!vars.get(com.fiskmods.heroes.common.data.var.Vars.PUNCHMODE)
                || vars.get(com.fiskmods.heroes.common.data.var.Vars.PUNCHMODE_TIMER) < 0.99F
                || vars.get(com.fiskmods.heroes.common.data.var.Vars.PUNCH_CHARGE) != 0.0F)
        { helper.fail("Mode should animate without charging while standing"); return; }
        player.setShiftKeyDown(true);
        for (int i = 0; i < 3; i++) mod.tick(player, entry, data);
        if (!vars.get(com.fiskmods.heroes.common.data.var.Vars.PUNCH_CHARGED)
                || vars.get(com.fiskmods.heroes.common.data.var.Vars.PUNCH_CHARGE) < 0.99F)
        { helper.fail("Crouching should charge the punch in three ticks"); return; }
        var profile = com.fiskmods.heroes.common.hero.attribute.SHAttributes.collect(player, hero);
        if (profile.get(com.fiskmods.heroes.common.hero.HeroAttribute.PUNCH_DAMAGE).amount() != 14.5D)
        { helper.fail("Charged mode should select the PUNCH attribute profile"); return; }
        player.setShiftKeyDown(false);
        mod.tick(player, entry, data);
        if (!vars.get(com.fiskmods.heroes.common.data.var.Vars.PUNCH_CHARGED))
        { helper.fail("Charge should persist after releasing crouch"); return; }
        mod.modifyOutgoingDamage(player, entry, target, player.damageSources().playerAttack(player), 3.0F);
        if (vars.get(com.fiskmods.heroes.common.data.var.Vars.PUNCH_CHARGED))
        { helper.fail("Melee hit should consume the charge"); return; }
        mod.onActivate(player, entry, data);
        for (int i = 0; i < 5; i++) mod.tick(player, entry, data);
        if (vars.get(com.fiskmods.heroes.common.data.var.Vars.PUNCHMODE_TIMER) > 0.001F
                || vars.get(com.fiskmods.heroes.common.data.var.Vars.PUNCH_CHARGE) > 0.001F)
        { helper.fail("Disabled punch mode should retract and clear its charge"); return; }
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STICK));
        mod.onActivate(player, entry, data);
        if (vars.get(com.fiskmods.heroes.common.data.var.Vars.PUNCHMODE))
            helper.fail("Punch mode must require an empty hand");
        else helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void playerAttackUsesCrossbonesPunchProfile(GameTestHelper helper)
    {
        var attacker = helper.makeMockPlayer();
        var target = helper.makeMockPlayer();
        var data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(attacker);
        var hero = com.fiskmods.heroes.common.hero.Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:crossbones");
        if (data == null || hero == null) { helper.fail("Crossbones or player capability missing"); return; }
        data.setHero(hero.getDefaultIteration());
        target.getAbilities().invulnerable = false;
        attacker.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED).setBaseValue(1000.0D);
        target.setHealth(20.0F);
        attacker.setPos(0, 0, 0);
        target.setPos(0, 0, 2);
        attacker.attack(target);
        if (Math.abs(target.getHealth() - 13.0F) > 0.02F)
        { helper.fail("A full-strength normal Crossbones punch should deal 7 damage, health=" + target.getHealth()); return; }
        var vars = data.getData();
        vars.set(com.fiskmods.heroes.common.data.var.Vars.PUNCHMODE, true);
        vars.set(com.fiskmods.heroes.common.data.var.Vars.PUNCH_CHARGED, true);
        target.invulnerableTime = 0;
        target.setHealth(20.0F);
        target.setDeltaMovement(0, 0, 0);
        attacker.setYRot(0);
        attacker.attack(target);
        if (Math.abs(target.getHealth() - 4.5F) > 0.02F)
        { helper.fail("Charged Crossbones punch should deal 15.5 damage, health=" + target.getHealth()); return; }
        if (vars.get(com.fiskmods.heroes.common.data.var.Vars.PUNCH_CHARGED))
        { helper.fail("Charged melee should consume its charge"); return; }
        // LivingEntity.hurt subsequently halves existing horizontal velocity and adds its
        // own 0.4 knockback. The profile must still leave more than vanilla's 0.4 impulse.
        if (target.getDeltaMovement().z < 0.9D)
        { helper.fail("Charged melee should add profile knockback, velocity=" + target.getDeltaMovement()); return; }
        target.invulnerableTime = 0;
        target.setHealth(20.0F);
        attacker.attack(target);
        if (Math.abs(target.getHealth() - 7.5F) > 0.02F)
        { helper.fail("Following hit should use BLUNT rather than the consumed charge, health=" + target.getHealth()); return; }
        if (Math.abs(attacker.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) - 1.0D) > 0.01D)
        { helper.fail("Temporary attack attribute must not leak after the hit"); return; }
        data.setHero(null);
        com.fiskmods.heroes.common.hero.attribute.SHAttributes.clearModifiers(attacker);
        target.invulnerableTime = 0;
        target.setHealth(20.0F);
        attacker.attack(target);
        if (Math.abs(target.getHealth() - 19.0F) > 0.02F)
            helper.fail("Removing the suit must restore vanilla attack damage, health=" + target.getHealth());
        else helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void weaponBonusesRespectEquipmentAndAttackCooldown(GameTestHelper helper)
    {
        var attacker = helper.makeMockPlayer();
        var target = helper.makeMockPlayer();
        var data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(attacker);
        var hero = com.fiskmods.heroes.common.hero.Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:deathstroke_dceu");
        if (data == null || hero == null) { helper.fail("Deathstroke or player capability missing"); return; }
        data.setHero(hero.getDefaultIteration());
        var attack = attacker.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        var speed = attacker.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED);
        target.getAbilities().invulnerable = false;
        speed.setBaseValue(1000.0D);
        var katana = new net.minecraft.world.item.ItemStack(com.fiskmods.heroes.common.item.ModItems.KATANA.get());
        attacker.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, katana);
        attacker.getAttributes().addTransientAttributeModifiers(katana.getAttributeModifiers(net.minecraft.world.entity.EquipmentSlot.MAINHAND));
        double base = attack.getValue();
        com.fiskmods.heroes.common.hero.ability.MeleeCombat.prepareAttack(attacker);
        if (Math.abs(attack.getValue() - base - 4.5D) > 0.01D)
        { helper.fail("Hero's katana should receive full weapon bonus"); return; }
        com.fiskmods.heroes.common.hero.ability.MeleeCombat.clearAttackBonus(attacker);
        attacker.getAttributes().removeAttributeModifiers(katana.getAttributeModifiers(net.minecraft.world.entity.EquipmentSlot.MAINHAND));
        var sword = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_SWORD);
        attacker.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, sword);
        attacker.getAttributes().addTransientAttributeModifiers(sword.getAttributeModifiers(net.minecraft.world.entity.EquipmentSlot.MAINHAND));
        base = attack.getValue();
        com.fiskmods.heroes.common.hero.ability.MeleeCombat.prepareAttack(attacker);
        if (Math.abs(attack.getValue() - base - 2.25D) > 0.01D)
        { helper.fail("Unrelated weapon should receive half the positive bonus"); return; }
        com.fiskmods.heroes.common.hero.ability.MeleeCombat.clearAttackBonus(attacker);
        target.setHealth(20.0F);
        attacker.attack(target);
        float full = 20.0F - target.getHealth();
        if (Math.abs(full - (float) (base + 2.25D)) > 0.05F)
        { helper.fail("Real weapon attack did not include its suit bonus: " + full); return; }
        // Immediately after an attack, a long cooldown should scale the suit bonus along with
        // vanilla damage, rather than adding its full value after the hit has been calculated.
        speed.setBaseValue(1.0D);
        target.invulnerableTime = 0;
        target.setHealth(20.0F);
        float strength = attacker.getAttackStrengthScale(0.5F);
        attacker.attack(target);
        float expected = full * (0.2F + strength * strength * 0.8F);
        if (Math.abs((20.0F - target.getHealth()) - expected) > 0.05F)
            helper.fail("Weapon bonus must follow vanilla cooldown, expected=" + expected + " dealt=" + (20.0F - target.getHealth()));
        else helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void abilityHudLayoutFitsAndHitboxesDoNotOverlap(GameTestHelper helper)
    {
        for (int width : new int[] { 160, 320, 640 })
        {
            for (int height : new int[] { 80, 120, 240, 480 })
            {
                for (int count : new int[] { 0, 1, 5, 10 })
                {
                    var widths = new java.util.ArrayList<Integer>();
                    for (int i = 0; i < count; ++i) widths.add(i % 2 == 0 ? 90 : 180);
                    var layout = com.fiskmods.heroes.common.util.SuitHudLayout.calculate(width, height, widths);
                    if (layout.rows().size() != count || layout.titleY() < 0)
                    { helper.fail("HUD omitted rows or placed its title offscreen"); return; }
                    for (int i = 0; i < count; ++i)
                    {
                        var a = layout.rows().get(i);
                        if (a.x() < 0 || a.y() < 0 || a.x() + a.width() > width || a.y() + a.height() > height
                                || a.y() < layout.titleY() + 10 || !a.contains(a.x() + 0.5D, a.y() + 0.5D))
                        { helper.fail("HUD row out of bounds at " + width + "x" + height + ": " + a); return; }
                        for (int j = i + 1; j < count; ++j)
                        {
                            var b = layout.rows().get(j);
                            if (a.x() < b.x() + b.width() && b.x() < a.x() + a.width()
                                    && a.y() < b.y() + b.height() && b.y() < a.y() + a.height())
                            { helper.fail("HUD rows overlap at " + width + "x" + height); return; }
                        }
                    }
                    if (height >= 120)
                        for (var row : layout.rows())
                            if (row.y() + row.height() > height - 60)
                            { helper.fail("HUD overlaps the reserved vanilla bottom area"); return; }
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void gunVisualHandAndLifetimeAreBounded(GameTestHelper helper)
    {
        var main = com.fiskmods.heroes.common.util.GunVisualMath.useOffhand(true, true, 16, 16);
        var off = com.fiskmods.heroes.common.util.GunVisualMath.useOffhand(true, true, 16, 15);
        if (main || !off || com.fiskmods.heroes.common.util.GunVisualMath.useOffhand(true, false, 16, 15)
                || com.fiskmods.heroes.common.util.GunVisualMath.useOffhand(false, true, 8, 7)
                || com.fiskmods.heroes.common.util.GunVisualMath.useOffhand(true, true, 16, 0))
        { helper.fail("Visual shot hand alternation must respect dual mode, occupied offhand and empty magazines"); return; }
        if (com.fiskmods.heroes.common.util.GunVisualMath.envelope(0, 5) != 1.0F
                || com.fiskmods.heroes.common.util.GunVisualMath.envelope(5, 5) != 0.0F
                || com.fiskmods.heroes.common.util.GunVisualMath.envelope(-1, 5) != 0.0F
                || com.fiskmods.heroes.common.util.GunVisualMath.envelope(Float.NaN, 5) != 0.0F)
        { helper.fail("Gun visual envelopes must expire and reject invalid ages"); return; }
        if (com.fiskmods.heroes.common.util.GunVisualMath.reloadPose(0.0F) != 0.0F
                || com.fiskmods.heroes.common.util.GunVisualMath.reloadPose(1.0F) != 0.0F
                || com.fiskmods.heroes.common.util.GunVisualMath.reloadPose(Float.NaN) != 0.0F
                || Math.abs(com.fiskmods.heroes.common.util.GunVisualMath.reloadPose(0.5F) - 1.0F) > 0.0001F
                || Math.abs(com.fiskmods.heroes.common.util.GunVisualMath.reloadPose(0.25F)
                        - com.fiskmods.heroes.common.util.GunVisualMath.reloadPose(0.75F)) > 0.0001F)
        { helper.fail("Gun reload visual must be bounded and return to rest"); return; }
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void desertEagleScopeRequiresSinglePistolAimAndCrouch(GameTestHelper helper)
    {
        var player = new net.minecraftforge.common.util.FakePlayer(helper.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "fisk-scope-test"));
        var hero = com.fiskmods.heroes.common.hero.Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:deadpool_xmen");
        var data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(player);
        if (hero == null || data == null) { helper.fail("Deadpool or player data missing"); return; }
        var slots = new net.minecraft.world.entity.EquipmentSlot[] {net.minecraft.world.entity.EquipmentSlot.HEAD,
                net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.LEGS,
                net.minecraft.world.entity.EquipmentSlot.FEET};
        for (int i = 0; i < 4; ++i)
            if (hero.getDefaultIteration().hasPieceOfSet(i)) player.setItemSlot(slots[i],
                    com.fiskmods.heroes.common.hero.ItemHeroArmor.create(hero.getDefaultIteration(),
                            com.fiskmods.heroes.common.item.ModItems.heroArmor[i]));
        var gun = new net.minecraft.world.item.ItemStack(com.fiskmods.heroes.common.item.ModItems.DESERT_EAGLE.get());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, gun);
        player.setShiftKeyDown(true);
        com.fiskmods.heroes.common.hero.HeroTracker.update(player);
        var vars = data.getData();
        com.fiskmods.heroes.common.hero.ability.AbilityHandler.tickAiming(player, data);
        if (vars.get(com.fiskmods.heroes.common.data.var.Vars.SCOPING))
        { helper.fail("Crouching without aim must not scope"); return; }
        com.fiskmods.heroes.common.hero.ability.AbilityHandler.onAbilityKey(player, -1, true);
        for (int i = 0; i < 12; ++i) com.fiskmods.heroes.common.hero.ability.AbilityHandler.tickAiming(player, data);
        if (!vars.get(com.fiskmods.heroes.common.data.var.Vars.SCOPING)
                || vars.get(com.fiskmods.heroes.common.data.var.Vars.SCOPE_TIMER) < 0.99F)
        { helper.fail("Single pistol should reach scoped state while crouched and aiming"); return; }
        gun.getOrCreateTag().putBoolean("Dual", true);
        for (int i = 0; i < 8; ++i) com.fiskmods.heroes.common.hero.ability.AbilityHandler.tickAiming(player, data);
        if (vars.get(com.fiskmods.heroes.common.data.var.Vars.SCOPING)
                || vars.get(com.fiskmods.heroes.common.data.var.Vars.SCOPE_TIMER) > 0.01F)
        { helper.fail("Dual pistols cannot scope and must leave the scope pose"); return; }
        gun.getOrCreateTag().putBoolean("Dual", false);
        for (int i = 0; i < 8; ++i) com.fiskmods.heroes.common.hero.ability.AbilityHandler.tickAiming(player, data);
        player.setShiftKeyDown(false);
        for (int i = 0; i < 8; ++i) com.fiskmods.heroes.common.hero.ability.AbilityHandler.tickAiming(player, data);
        if (vars.get(com.fiskmods.heroes.common.data.var.Vars.SCOPING)
                || vars.get(com.fiskmods.heroes.common.data.var.Vars.SCOPE_TIMER) > 0.01F)
        { helper.fail("Standing must release the scoped state"); return; }
        com.fiskmods.heroes.common.hero.ability.AbilityHandler.onAbilityKey(player, -1, false);
        com.fiskmods.heroes.common.hero.ability.AbilityHandler.clear(player);
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void energyWeaponsRequireAimAndScopeCorrectly(GameTestHelper helper)
    {
        var level = helper.getLevel();
        var player = new net.minecraftforge.common.util.FakePlayer(level,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "fisk-energy-gun-test"));
        var pos = helper.absolutePos(new net.minecraft.core.BlockPos(1, 80, 1));
        player.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        var data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(player);
        if (data == null) { helper.fail("Player data missing"); return; }
        var slots = new net.minecraft.world.entity.EquipmentSlot[] {net.minecraft.world.entity.EquipmentSlot.HEAD,
                net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.LEGS,
                net.minecraft.world.entity.EquipmentSlot.FEET};
        var guns = new com.fiskmods.heroes.common.item.ItemGun[] {
                (com.fiskmods.heroes.common.item.ItemGun) com.fiskmods.heroes.common.item.ModItems.CHRONOS_RIFLE.get(),
                (com.fiskmods.heroes.common.item.ItemGun) com.fiskmods.heroes.common.item.ModItems.RIP_HUNTERS_GUN.get()};
        String[] heroes = {"fiskheroes:chronos", "fiskheroes:rip_hunter"};
        for (int n = 0; n < guns.length; ++n)
        {
            var hero = com.fiskmods.heroes.common.hero.Hero.REGISTRY.getHeroIgnoreCase(heroes[n]);
            if (hero == null) { helper.fail("Energy gun hero missing"); return; }
            for (int i = 0; i < 4; ++i)
                player.setItemSlot(slots[i], hero.getDefaultIteration().hasPieceOfSet(i)
                        ? com.fiskmods.heroes.common.hero.ItemHeroArmor.create(hero.getDefaultIteration(),
                            com.fiskmods.heroes.common.item.ModItems.heroArmor[i]) : net.minecraft.world.item.ItemStack.EMPTY);
            var stack = new net.minecraft.world.item.ItemStack(guns[n]);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);
            com.fiskmods.heroes.common.hero.HeroTracker.update(player);
            var vars = data.getData();
            player.setShiftKeyDown(false);
            com.fiskmods.heroes.common.hero.ability.AbilityHandler.onAbilityKey(player, -1, false);
            for (int i = 0; i < 12; ++i) com.fiskmods.heroes.common.hero.ability.AbilityHandler.tickAiming(player, data);
            if (guns[n].fireFromAttack(player, stack) || stack.getOrCreateTag().contains("NextShotTick"))
            { helper.fail("Energy gun must not fire or consume cooldown without aim"); return; }
            com.fiskmods.heroes.common.hero.ability.AbilityHandler.onAbilityKey(player, -1, true);
            if (!guns[n].fireFromAttack(player, stack))
            { helper.fail("Aiming must allow the energy gun to fire"); return; }
            var bolts = level.getEntitiesOfClass(com.fiskmods.heroes.common.entity.projectile.EnergyBoltEntity.class,
                    player.getBoundingBox().inflate(2), bolt -> bolt.getOwner() == player);
            if (bolts.size() != 1) { helper.fail("Expected one unscoped energy bolt"); return; }
            var unscoped = new net.minecraft.nbt.CompoundTag();
            bolts.get(0).saveWithoutId(unscoped);
            bolts.get(0).discard();
            float baseDamage = n == 0 ? 9.0F : 6.0F;
            if (Math.abs(unscoped.getFloat("Damage") - baseDamage) > 0.001F
                    || unscoped.getBoolean("Explosive") != (n == 0)
                    || !unscoped.getString("DamageProfile").contains("ENERGY"))
            { helper.fail("Unscoped energy projectile damage, type or explosion is incorrect"); return; }
            player.setShiftKeyDown(true);
            for (int i = 0; i < 12; ++i) com.fiskmods.heroes.common.hero.ability.AbilityHandler.tickAiming(player, data);
            if (!vars.get(com.fiskmods.heroes.common.data.var.Vars.SCOPING)
                    || vars.get(com.fiskmods.heroes.common.data.var.Vars.SCOPE_TIMER) < 0.99F)
            { helper.fail("Crouch and fully raised aim must scope the energy gun"); return; }
            stack.getOrCreateTag().remove("NextShotTick");
            if (!guns[n].fireFromAttack(player, stack)) { helper.fail("Scoped shot failed"); return; }
            bolts = level.getEntitiesOfClass(com.fiskmods.heroes.common.entity.projectile.EnergyBoltEntity.class,
                    player.getBoundingBox().inflate(2), bolt -> bolt.getOwner() == player && bolt.isAlive());
            if (bolts.size() != 1) { helper.fail("Expected one scoped energy bolt"); return; }
            var scoped = new net.minecraft.nbt.CompoundTag();
            bolts.get(0).saveWithoutId(scoped);
            bolts.get(0).discard();
            if (Math.abs(scoped.getFloat("Damage") - baseDamage * 1.6F) > 0.001F)
            { helper.fail("Only scoped energy bolts should receive the 1.6x damage bonus"); return; }
            player.setShiftKeyDown(false);
            for (int i = 0; i < 8; ++i) com.fiskmods.heroes.common.hero.ability.AbilityHandler.tickAiming(player, data);
            if (vars.get(com.fiskmods.heroes.common.data.var.Vars.SCOPING)
                    || vars.get(com.fiskmods.heroes.common.data.var.Vars.SCOPE_TIMER) > 0.01F)
            { helper.fail("Standing must release the scope"); return; }
            com.fiskmods.heroes.common.hero.ability.AbilityHandler.onAbilityKey(player, -1, false);
            com.fiskmods.heroes.common.hero.ability.AbilityHandler.clear(player);
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void energyBoltsResetTargetHitImmunity(GameTestHelper helper)
    {
        var level = helper.getLevel();
        var shooter = net.minecraft.world.entity.EntityType.ZOMBIE.create(level);
        var target = net.minecraft.world.entity.EntityType.IRON_GOLEM.create(level);
        if (shooter == null || target == null) { helper.fail("Could not create energy bolt actors"); return; }
        shooter.setNoAi(true);
        target.setNoAi(true);
        var pos = helper.absolutePos(new net.minecraft.core.BlockPos(1, 80, 1));
        shooter.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        target.setPos(shooter.getX(), shooter.getY(), shooter.getZ() + 2.0D);
        shooter.setYRot(0.0F);
        shooter.setXRot(0.0F);
        level.addFreshEntity(shooter);
        level.addFreshEntity(target);
        for (int i = 0; i < 2; ++i)
        {
            var bolt = new com.fiskmods.heroes.common.entity.projectile.EnergyBoltEntity(
                    shooter, null, 6.0F, false, 4.0F, 0.0F);
            level.addFreshEntity(bolt);
            bolt.tick();
        }
        if (target.getHealth() > 88.01F)
        { helper.fail("Consecutive energy bolts must each damage the target despite vanilla hurt cooldown"); return; }
        target.discard();
        shooter.discard();
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void berettaPendingBurstStopsDuringReload(GameTestHelper helper)
    {
        var player = new net.minecraftforge.common.util.FakePlayer(helper.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "beretta-reload-test"));
        var hero = com.fiskmods.heroes.common.hero.Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:wild_dog");
        var data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(player);
        if (hero == null || data == null) { helper.fail("Wild Dog or player data missing"); return; }
        var slots = new net.minecraft.world.entity.EquipmentSlot[] {net.minecraft.world.entity.EquipmentSlot.HEAD,
                net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.LEGS,
                net.minecraft.world.entity.EquipmentSlot.FEET};
        for (int i = 0; i < 4; ++i)
            if (hero.getDefaultIteration().hasPieceOfSet(i)) player.setItemSlot(slots[i],
                    com.fiskmods.heroes.common.hero.ItemHeroArmor.create(hero.getDefaultIteration(),
                            com.fiskmods.heroes.common.item.ModItems.heroArmor[i]));
        var gun = (com.fiskmods.heroes.common.item.ItemBeretta93R) com.fiskmods.heroes.common.item.ModItems.BERETTA_93R.get();
        var stack = new net.minecraft.world.item.ItemStack(gun);
        stack.getOrCreateTag().putInt("Ammo", 10);
        stack.getOrCreateTag().putInt("BurstRemaining", 1);
        stack.getOrCreateTag().putLong("BurstNextTick", 0);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);
        com.fiskmods.heroes.common.hero.HeroTracker.update(player);
        data.getData().set(com.fiskmods.heroes.common.data.var.Vars.RELOAD_TIMER, 1.0F);
        gun.inventoryTick(stack, helper.getLevel(), player, 0, true);
        if (gun.getAmmo(stack) != 10 || stack.getTag().getInt("BurstRemaining") != 0)
        { helper.fail("A queued burst must be canceled without firing while reloading"); return; }
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void coldGunHitsFluidsAndStopsAtEntity(GameTestHelper helper)
    {
        var level = helper.getLevel();
        var player = new net.minecraftforge.common.util.FakePlayer(level,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "cold-gun-ray-test"));
        var hero = com.fiskmods.heroes.common.hero.Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:captain_cold");
        if (hero == null) { helper.fail("Captain Cold is missing"); return; }
        var slots = new net.minecraft.world.entity.EquipmentSlot[] {net.minecraft.world.entity.EquipmentSlot.HEAD,
                net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.LEGS,
                net.minecraft.world.entity.EquipmentSlot.FEET};
        for (int i = 0; i < 4; ++i)
            if (hero.getDefaultIteration().hasPieceOfSet(i)) player.setItemSlot(slots[i],
                    com.fiskmods.heroes.common.hero.ItemHeroArmor.create(hero.getDefaultIteration(),
                            com.fiskmods.heroes.common.item.ModItems.heroArmor[i]));
        var base = helper.absolutePos(new net.minecraft.core.BlockPos(1, 80, 1));
        player.setPos(base.getX() + 0.5, base.getY(), base.getZ() + 0.5);
        player.setYRot(0); player.setXRot(0);
        var gun = (com.fiskmods.heroes.common.item.ItemColdGun) com.fiskmods.heroes.common.item.ModItems.COLD_GUN.get();
        var pos = base.offset(0, 1, 4);
        var water = net.minecraft.world.level.block.Blocks.WATER;
        var lava = net.minecraft.world.level.block.Blocks.LAVA;
        java.util.function.BooleanSupplier shoot = () -> {
            var stack = new net.minecraft.world.item.ItemStack(gun);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);
            return gun.fireFromAttack(player, stack);
        };
        level.setBlockAndUpdate(pos, water.defaultBlockState());
        if (!shoot.getAsBoolean() || !level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.ICE))
        { helper.fail("Cold gun must hit and freeze water"); return; }
        level.setBlockAndUpdate(pos, lava.defaultBlockState());
        if (!shoot.getAsBoolean() || !level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.OBSIDIAN))
        { helper.fail("Cold gun must hit lava and create obsidian"); return; }
        level.setBlockAndUpdate(pos, water.defaultBlockState());
        var target = net.minecraft.world.entity.EntityType.IRON_GOLEM.create(level);
        if (target == null) { helper.fail("Cold gun target missing"); return; }
        target.setNoAi(true);
        target.setPos(player.getX(), player.getY(), player.getZ() + 2);
        level.addFreshEntity(target);
        if (!shoot.getAsBoolean() || target.getTicksFrozen() <= 0 || !level.getBlockState(pos).is(water))
        { helper.fail("A target before water must be frozen without affecting the block behind it"); return; }
        target.discard();
        level.removeBlock(pos, false);
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void gunInputDoesNotAlsoMineOrMelee(GameTestHelper helper)
    {
        var level = helper.getLevel();
        var player = new net.minecraftforge.common.util.FakePlayer(level,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "gun-input-test"));
        var target = net.minecraft.world.entity.EntityType.COW.create(level);
        if (target == null) { helper.fail("Gun input target missing"); return; }
        var position = helper.absolutePos(new net.minecraft.core.BlockPos(1, 80, 1));
        player.setPos(position.getX() + 0.5, position.getY(), position.getZ() + 0.5);
        target.setPos(player.getX(), player.getY(), player.getZ() + 2);
        level.addFreshEntity(target);
        for (var item : new net.minecraft.world.item.Item[] {com.fiskmods.heroes.common.item.ModItems.COLD_GUN.get(),
                com.fiskmods.heroes.common.item.ModItems.DESERT_EAGLE.get(), com.fiskmods.heroes.common.item.ModItems.BERETTA_93R.get()})
        {
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(item));
            var attack = new net.minecraftforge.event.entity.player.AttackEntityEvent(player, target);
            com.fiskmods.heroes.common.event.CommonEventHandler.onAttackEntity(attack);
            if (!attack.isCanceled()) { helper.fail("Gun melee must be canceled"); return; }
            var mine = new net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock(player, position,
                    net.minecraft.core.Direction.UP, net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock.Action.START);
            com.fiskmods.heroes.common.event.CommonEventHandler.onGunMine(mine);
            if (!mine.isCanceled()) { helper.fail("Gun mining must be canceled"); return; }
            var abort = new net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock(player, position,
                    net.minecraft.core.Direction.UP, net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock.Action.ABORT);
            com.fiskmods.heroes.common.event.CommonEventHandler.onGunMine(abort);
            if (abort.isCanceled()) { helper.fail("Mining abort must remain available after switching items"); return; }
        }
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
        var normalAttack = new net.minecraftforge.event.entity.player.AttackEntityEvent(player, target);
        com.fiskmods.heroes.common.event.CommonEventHandler.onAttackEntity(normalAttack);
        var normalMine = new net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock(player, position,
                net.minecraft.core.Direction.UP, net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock.Action.START);
        com.fiskmods.heroes.common.event.CommonEventHandler.onGunMine(normalMine);
        if (normalAttack.isCanceled() || normalMine.isCanceled())
        { helper.fail("Empty hands must retain vanilla attacks and mining"); return; }
        target.discard();
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void heatGunMeltsIceAndStopsAtEntity(GameTestHelper helper)
    {
        var level = helper.getLevel();
        var player = new net.minecraftforge.common.util.FakePlayer(level,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "heat-gun-ray-test"));
        var hero = com.fiskmods.heroes.common.hero.Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:heatwave");
        if (hero == null) { helper.fail("Heatwave is missing"); return; }
        var slots = new net.minecraft.world.entity.EquipmentSlot[] {net.minecraft.world.entity.EquipmentSlot.HEAD,
                net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.LEGS,
                net.minecraft.world.entity.EquipmentSlot.FEET};
        for (int i = 0; i < 4; ++i)
            if (hero.getDefaultIteration().hasPieceOfSet(i)) player.setItemSlot(slots[i],
                    com.fiskmods.heroes.common.hero.ItemHeroArmor.create(hero.getDefaultIteration(),
                            com.fiskmods.heroes.common.item.ModItems.heroArmor[i]));
        var base = helper.absolutePos(new net.minecraft.core.BlockPos(1, 80, 1));
        player.setPos(base.getX() + 0.5, base.getY(), base.getZ() + 0.5);
        player.setYRot(0); player.setXRot(0);
        var gun = (com.fiskmods.heroes.common.item.ItemHeatGun) com.fiskmods.heroes.common.item.ModItems.HEAT_GUN.get();
        var pos = base.offset(0, 1, 4);
        var ice = net.minecraft.world.level.block.Blocks.ICE;
        java.util.function.BooleanSupplier shoot = () -> {
            var stack = new net.minecraft.world.item.ItemStack(gun);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);
            return gun.fireFromAttack(player, stack);
        };
        level.setBlockAndUpdate(pos, ice.defaultBlockState());
        if (!shoot.getAsBoolean() || !level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.WATER))
        { helper.fail("Heat gun must melt ice into water in the overworld"); return; }
        level.setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.SNOW_BLOCK.defaultBlockState());
        if (!shoot.getAsBoolean() || !level.getBlockState(pos).isAir())
        { helper.fail("Heat gun must melt snow blocks"); return; }
        var fireSupport = pos.north().below();
        level.setBlockAndUpdate(fireSupport, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        if (!shoot.getAsBoolean() || !level.getBlockState(pos.north()).isAir())
        { helper.fail("Heat gun must not ignite nonflammable stone"); return; }
        level.setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.OAK_PLANKS.defaultBlockState());
        if (!shoot.getAsBoolean() || !level.getBlockState(pos.north()).is(net.minecraft.world.level.block.Blocks.FIRE))
        { helper.fail("Heat gun must ignite the exposed side of flammable blocks"); return; }
        level.removeBlock(pos.north(), false);
        level.removeBlock(fireSupport, false);
        level.setBlockAndUpdate(pos, ice.defaultBlockState());
        var target = net.minecraft.world.entity.EntityType.COW.create(level);
        if (target == null) { helper.fail("Heat gun target missing"); return; }
        target.setNoAi(true);
        target.setPos(player.getX(), player.getY(), player.getZ() + 2);
        level.addFreshEntity(target);
        if (!shoot.getAsBoolean() || !target.isOnFire() || !level.getBlockState(pos).is(ice))
        { helper.fail("A target before ice must catch fire without melting the ice behind it"); return; }
        target.discard();
        level.removeBlock(pos, false);
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void gunMetadataDoesNotTriggerReequip(GameTestHelper helper)
    {
        var gun = (com.fiskmods.heroes.common.item.ItemGun) com.fiskmods.heroes.common.item.ModItems.HEAT_GUN.get();
        var before = new net.minecraft.world.item.ItemStack(gun);
        var after = before.copy();
        after.getOrCreateTag().putLong("NextShotTick", 42);
        after.getOrCreateTag().putInt("Ammo", 3);
        if (gun.shouldCauseReequipAnimation(before, after, false)
                || !gun.shouldCauseReequipAnimation(before, after, true)
                || !gun.shouldCauseReequipAnimation(before, net.minecraft.world.item.ItemStack.EMPTY, false)
                || !gun.shouldCauseReequipAnimation(before, new net.minecraft.world.item.ItemStack(
                        com.fiskmods.heroes.common.item.ModItems.COLD_GUN.get()), false))
        { helper.fail("Gun NBT changes must not re-equip; slot, item and empty-hand changes still must"); return; }
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void heatFlamePacketPreservesClippedSegment(GameTestHelper helper)
    {
        Vec3 start = new Vec3(-12.25D, 83.125D, 5.5D);
        Vec3 end = new Vec3(-12.0D, 82.5D, 17.25D);
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        new com.fiskmods.heroes.common.network.PacketHeatGunFlame(37, start, end).encode(buffer);
        var decoded = new com.fiskmods.heroes.common.network.PacketHeatGunFlame(buffer);
        if (decoded.shooterId() != 37 || !start.equals(decoded.start()) || !end.equals(decoded.end())
                || buffer.readableBytes() != 0)
        { helper.fail("Heat flame visual packet must preserve the server-clipped segment"); return; }
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void coldBallImpactsTargetImmediately(GameTestHelper helper)
    {
        var level = helper.getLevel();
        var player = new net.minecraftforge.common.util.FakePlayer(level,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "cold-ball-test"));
        var base = helper.absolutePos(new net.minecraft.core.BlockPos(1, 80, 1));
        player.setPos(base.getX() + 0.5D, base.getY(), base.getZ() + 0.5D);
        player.setYRot(0.0F); player.setXRot(10.0F);
        var target = net.minecraft.world.entity.EntityType.COW.create(level);
        if (target == null) { helper.fail("Cold ball target missing"); return; }
        target.setNoAi(true); target.setPos(player.getX(), player.getY(), player.getZ() + 3.0D);
        level.addFreshEntity(target);
        var ball = new com.fiskmods.heroes.common.entity.projectile.ColdBallEntity(player);
        level.addFreshEntity(ball);
        helper.runAfterDelay(8, () -> {
            if (!ball.isRemoved()) { helper.fail("Cold ball must detonate on impact, without a grenade fuse"); return; }
            if (!target.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN)
                    || target.getTicksFrozen() <= 0 || target.getHealth() >= target.getMaxHealth())
            { helper.fail("Cold ball impact must damage and freeze the nearby target"); return; }
            if (player.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN))
            { helper.fail("Cold ball must not freeze its owner"); return; }
            target.discard();
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void coldBallFreezesWaterWithoutPassingThroughWall(GameTestHelper helper)
    {
        var level = helper.getLevel();
        var player = new net.minecraftforge.common.util.FakePlayer(level,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "cold-ball-wall-test"));
        var base = helper.absolutePos(new net.minecraft.core.BlockPos(1, 80, 1));
        player.setPos(base.getX() + 0.5D, base.getY(), base.getZ() + 0.5D);
        player.setYRot(0.0F); player.setXRot(0.0F);
        var wall = base.offset(0, 1, 2);
        level.setBlockAndUpdate(wall, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(wall.below(), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        var water = base.offset(0, 1, 1);
        level.setBlockAndUpdate(water, net.minecraft.world.level.block.Blocks.WATER.defaultBlockState());
        var target = net.minecraft.world.entity.EntityType.COW.create(level);
        if (target == null) { helper.fail("Cold ball wall target missing"); return; }
        target.setNoAi(true); target.setNoGravity(true); target.setPos(player.getX(), player.getY(), player.getZ() + 3.1D);
        level.addFreshEntity(target);
        var ball = new com.fiskmods.heroes.common.entity.projectile.ColdBallEntity(player);
        level.addFreshEntity(ball);
        helper.runAfterDelay(8, () -> {
            if (!ball.isRemoved() || !level.getBlockState(water).is(net.minecraft.world.level.block.Blocks.ICE))
            { helper.fail("Cold ball must freeze nearby water when it hits a wall"); return; }
            if (target.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN)
                    || target.getHealth() < target.getMaxHealth())
            { helper.fail("A solid wall must shield target: health=" + target.getHealth()
                    + ", frozen=" + target.getTicksFrozen() + ", slow="
                    + target.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN)
                    + ", pos=" + target.position() + ", wall=" + wall); return; }
            target.discard();
            level.removeBlock(wall, false); level.removeBlock(wall.below(), false); level.removeBlock(water, false);
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void batmanFreezeGrenadeUsesBeltAndDetonatesOnImpact(GameTestHelper helper)
    {
        var level = helper.getLevel();
        var player = new net.minecraftforge.common.util.FakePlayer(level,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "batman-freeze-test"));
        var hero = com.fiskmods.heroes.common.hero.Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:batman_dceu");
        if (hero == null) { helper.fail("Batman is missing"); return; }
        var slots = new net.minecraft.world.entity.EquipmentSlot[] {net.minecraft.world.entity.EquipmentSlot.HEAD,
                net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.LEGS,
                net.minecraft.world.entity.EquipmentSlot.FEET};
        for (int i = 0; i < 4; ++i)
            if (hero.getDefaultIteration().hasPieceOfSet(i)) player.setItemSlot(slots[i],
                    com.fiskmods.heroes.common.hero.ItemHeroArmor.create(hero.getDefaultIteration(),
                            com.fiskmods.heroes.common.item.ModItems.heroArmor[i]));
        com.fiskmods.heroes.common.hero.HeroTracker.update(player);
        var data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(player);
        if (data == null || data.getHero() == null) { helper.fail("Batman suit did not activate"); return; }
        var belt = com.fiskmods.heroes.common.hero.equipment.EquipmentHelper.getUtilityBelt(hero, player, data);
        if (belt == null) { helper.fail("Batman utility belt missing"); return; }
        var names = new java.util.ArrayList<>(com.fiskmods.heroes.common.hero.equipment.EquipmentHelper.equipmentOptions(belt).keySet());
        int selected = names.indexOf("fiskheroes:freeze_grenade");
        if (selected < 0) { helper.fail("Batman freeze grenade missing from belt"); return; }
        data.getData().set(com.fiskmods.heroes.common.data.var.Vars.UTILITY_BELT_TYPE, (byte) selected);
        var base = helper.absolutePos(new net.minecraft.core.BlockPos(1, 140, 1));
        player.setPos(base.getX() + 0.5D, base.getY(), base.getZ() + 0.5D);
        player.setYRot(0.0F); player.setXRot(0.0F);
        var target = net.minecraft.world.entity.EntityType.IRON_GOLEM.create(level);
        if (target == null) { helper.fail("Freeze grenade target missing"); return; }
        target.setNoAi(true); target.setNoGravity(true);
        target.setPos(player.getX(), player.getY(), player.getZ() + 3.0D);
        level.addFreshEntity(target);
        if (!com.fiskmods.heroes.common.hero.equipment.EquipmentHelper.useUtilityBelt(player))
        { helper.fail("Utility belt failed to launch freeze grenade"); return; }
        if (com.fiskmods.heroes.common.hero.equipment.EquipmentHelper.useUtilityBelt(player))
        { helper.fail("Freeze grenade must consume its one charge until cooldown"); return; }
        var projectiles = level.getEntitiesOfClass(com.fiskmods.heroes.common.entity.projectile.EquipmentProjectileEntity.class,
                player.getBoundingBox().inflate(5), projectile -> projectile.getOwner() == player);
        if (projectiles.size() != 1) { helper.fail("Expected exactly one belt projectile"); return; }
        var grenade = projectiles.get(0);
        var tag = new net.minecraft.nbt.CompoundTag(); grenade.addAdditionalSaveData(tag);
        if (tag.getInt("FuseTicks") != 0) { helper.fail("Freeze grenade must not have a timed fuse"); return; }
        helper.runAfterDelay(8, () -> {
            if (!grenade.isRemoved()) { helper.fail("Freeze grenade must detonate immediately on impact"); return; }
            if (!target.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN)
                    || target.getTicksFrozen() <= 0 || target.getHealth() >= target.getMaxHealth())
            { helper.fail("Freeze grenade must damage and freeze a nearby target"); return; }
            if (player.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN))
            { helper.fail("Freeze grenade must not freeze its thrower by default"); return; }
            target.discard(); helper.succeed();
        });
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void freezeGrenadeFreezesWaterAndWallShieldsTarget(GameTestHelper helper)
    {
        var level = helper.getLevel();
        var player = new net.minecraftforge.common.util.FakePlayer(level,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "freeze-wall-test"));
        var base = helper.absolutePos(new net.minecraft.core.BlockPos(1, 140, 1));
        player.setPos(base.getX() + 0.5D, base.getY(), base.getZ() + 0.5D);
        player.setYRot(0.0F); player.setXRot(0.0F);
        var wall = base.offset(0, 1, 3);
        level.setBlockAndUpdate(wall, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(wall.below(), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        var water = base.offset(0, 1, 2);
        level.setBlockAndUpdate(water, net.minecraft.world.level.block.Blocks.WATER.defaultBlockState());
        var target = net.minecraft.world.entity.EntityType.COW.create(level);
        if (target == null) { helper.fail("Freeze grenade wall target missing"); return; }
        target.setNoAi(true); target.setNoGravity(true);
        target.setPos(player.getX(), player.getY(), player.getZ() + 4.1D);
        level.addFreshEntity(target);
        var grenade = new com.fiskmods.heroes.common.entity.projectile.EquipmentProjectileEntity(player,
                "fiskheroes:freeze_grenade", new com.google.gson.JsonObject(),
                new net.minecraft.world.item.ItemStack(com.fiskmods.heroes.common.item.ModItems.FREEZE_GRENADE.get()), 0.0F);
        grenade.setDeltaMovement(0.0D, 0.0D, 1.25D);
        level.addFreshEntity(grenade);
        helper.runAfterDelay(8, () -> {
            if (!grenade.isRemoved() || !level.getBlockState(water).is(net.minecraft.world.level.block.Blocks.ICE))
            { helper.fail("Freeze grenade must freeze nearby water immediately on impact"); return; }
            if (target.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN)
                    || target.getHealth() < target.getMaxHealth())
            { helper.fail("Solid wall must shield targets from freeze grenade"); return; }
            target.discard(); level.removeBlock(wall, false); level.removeBlock(wall.below(), false);
            level.removeBlock(water, false); helper.succeed();
        });
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void normalGrenadeKeepsItsTimedFuse(GameTestHelper helper)
    {
        var level = helper.getLevel();
        var player = new net.minecraftforge.common.util.FakePlayer(level,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "timed-grenade-test"));
        var base = helper.absolutePos(new net.minecraft.core.BlockPos(1, 140, 1));
        player.setPos(base.getX() + 0.5D, base.getY(), base.getZ() + 0.5D);
        player.setYRot(0.0F); player.setXRot(0.0F);
        var wall = base.offset(0, 1, 2);
        level.setBlockAndUpdate(wall, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        var config = new com.google.gson.JsonObject(); config.addProperty("fuseTime", 20); config.addProperty("radius", 1);
        var grenade = new com.fiskmods.heroes.common.entity.projectile.EquipmentProjectileEntity(player,
                "fiskheroes:grenade", config,
                new net.minecraft.world.item.ItemStack(com.fiskmods.heroes.common.item.ModItems.GRENADE.get()), 0.0F);
        grenade.setDeltaMovement(0.0D, 0.0D, 1.75D);
        level.addFreshEntity(grenade);
        helper.runAfterDelay(2, () -> {
            if (grenade.isRemoved() || grenade.isNoGravity() || grenade.getDeltaMovement().z >= 0)
            { helper.fail("Normal grenade must bounce on impact without detonating: velocity=" + grenade.getDeltaMovement()); return; }
            helper.runAfterDelay(24, () -> {
                if (!grenade.isRemoved()) { helper.fail("Normal grenade fuse did not expire"); return; }
                level.removeBlock(wall, false); helper.succeed();
            });
        });
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void timedGrenadeBlastRespectsCoverAndFractionalRadius(GameTestHelper helper)
    {
        var level = helper.getLevel();
        var base = helper.absolutePos(new net.minecraft.core.BlockPos(1, 220, 1));
        var player = new net.minecraftforge.common.util.FakePlayer(level,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "grenade-cover-test"));
        player.setPos(base.getX() + 0.5, base.getY(), base.getZ() - 20.0);
        var config = new com.google.gson.JsonObject();
        config.addProperty("fuseTime", 5); config.addProperty("radius", 4.5F);
        var grenade = new com.fiskmods.heroes.common.entity.projectile.EquipmentProjectileEntity(player,
                "fiskheroes:grenade", config,
                new net.minecraft.world.item.ItemStack(com.fiskmods.heroes.common.item.ModItems.GRENADE.get()), 0.0F);
        grenade.setPos(base.getX() + 0.5, base.getY() + 1.0, base.getZ() + 0.5);
        grenade.setDeltaMovement(0, 0, 0); grenade.setNoGravity(true);
        for (int x = -2; x <= 2; ++x)
            for (int y = -1; y <= 3; ++y)
                level.setBlockAndUpdate(base.offset(x, y, 2), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        var exposed = net.minecraft.world.entity.EntityType.IRON_GOLEM.create(level);
        var shielded = net.minecraft.world.entity.EntityType.IRON_GOLEM.create(level);
        var edge = net.minecraft.world.entity.EntityType.IRON_GOLEM.create(level);
        if (exposed == null || shielded == null || edge == null) { helper.fail("Grenade targets missing"); return; }
        for (var target : java.util.List.of(exposed, shielded, edge)) { target.setNoAi(true); target.setNoGravity(true); }
        exposed.setPos(base.getX() + 0.5, base.getY() + 1.0, base.getZ() - 2.5);
        shielded.setPos(base.getX() + 0.5, base.getY() + 1.0, base.getZ() + 4.5);
        edge.setPos(base.getX() + 4.8, base.getY() + 1.0, base.getZ() + 0.5);
        level.addFreshEntity(exposed); level.addFreshEntity(shielded); level.addFreshEntity(edge);
        level.addFreshEntity(grenade);
        helper.runAfterDelay(6, () -> {
            if (!grenade.isRemoved()) { helper.fail("Timed grenade did not detonate"); return; }
            if (exposed.getHealth() >= exposed.getMaxHealth() || exposed.getDeltaMovement().lengthSqr() < 0.001D)
            { helper.fail("Exposed target must take damage and blast knockback"); return; }
            if (shielded.getHealth() < shielded.getMaxHealth() || shielded.getDeltaMovement().lengthSqr() > 0.001D)
            { helper.fail("Solid cover must block grenade damage and knockback: health=" + shielded.getHealth() + ", motion=" + shielded.getDeltaMovement()); return; }
            if (edge.getHealth() >= edge.getMaxHealth())
            { helper.fail("Fractional blast radius must reach the target at 4.3 blocks"); return; }
            for (var target : java.util.List.of(exposed, shielded, edge)) target.discard();
            for (int x = -2; x <= 2; ++x)
                for (int y = -1; y <= 3; ++y) level.removeBlock(base.offset(x, y, 2), false);
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void instantGrenadeDetonatesOnBlockImpact(GameTestHelper helper)
    {
        var level = helper.getLevel();
        var player = new net.minecraftforge.common.util.FakePlayer(level,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "instant-grenade-test"));
        var base = helper.absolutePos(new net.minecraft.core.BlockPos(1, 200, 1));
        player.setPos(base.getX() + 0.5D, base.getY(), base.getZ() + 0.5D);
        var wall = base.offset(0, 1, 3);
        level.setBlockAndUpdate(wall, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        var config = new com.google.gson.JsonObject();
        config.addProperty("isInstant", true); config.addProperty("fuseTime", 40); config.addProperty("radius", 4.0F);
        var grenade = new com.fiskmods.heroes.common.entity.projectile.EquipmentProjectileEntity(player,
                "fiskheroes:grenade", config,
                new net.minecraft.world.item.ItemStack(com.fiskmods.heroes.common.item.ModItems.GRENADE.get()), 0.0F);
        grenade.setDeltaMovement(0, 0, 1.75D); level.addFreshEntity(grenade);
        helper.runAfterDelay(5, () -> {
            if (!grenade.isRemoved()) { helper.fail("Instant grenade must detonate on impact, not bounce or wait for its fuse"); return; }
            level.removeBlock(wall, false); helper.succeed();
        });
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void smokePelletBurstsOnImpactWithoutHidingUnsuitedMobs(GameTestHelper helper)
    {
        var level = helper.getLevel();
        var player = new net.minecraftforge.common.util.FakePlayer(level,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "smoke-pellet-test"));
        var base = helper.absolutePos(new net.minecraft.core.BlockPos(1, 180, 1));
        player.setPos(base.getX() + 0.5D, base.getY(), base.getZ() + 0.5D);
        var wall = base.offset(0, 1, 3);
        level.setBlockAndUpdate(wall, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        var target = net.minecraft.world.entity.EntityType.COW.create(level);
        if (target == null) { helper.fail("Smoke test target missing"); return; }
        target.setNoAi(true); target.setNoGravity(true);
        target.setPos(base.getX() + 1.8D, base.getY() + 1.0D, base.getZ() + 2.5D);
        level.addFreshEntity(target);
        var pellet = new com.fiskmods.heroes.common.entity.projectile.EquipmentProjectileEntity(player,
                "fiskheroes:smoke_pellet", new com.google.gson.JsonObject(),
                new net.minecraft.world.item.ItemStack(com.fiskmods.heroes.common.item.ModItems.SMOKE_PELLET.get()), 0.0F);
        pellet.setDeltaMovement(0, 0, 1.25D); level.addFreshEntity(pellet);
        helper.runAfterDelay(6, () -> {
            if (!pellet.isRemoved()) { helper.fail("Smoke pellet must burst and disappear on impact"); return; }
            if (target.hasEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY))
            { helper.fail("Smoke stealth must not apply to unsuited mobs"); return; }
            target.discard(); level.removeBlock(wall, false); helper.succeed();
        });
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void stickyWebFollowsLegTargetAndExpiresAfterReload(GameTestHelper helper)
    {
        var level = helper.getLevel();
        var base = helper.absolutePos(new net.minecraft.core.BlockPos(1, 180, 1));
        var player = new net.minecraftforge.common.util.FakePlayer(level,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "sticky-web-test"));
        player.setPos(base.getX() + 0.5, base.getY(), base.getZ() + 0.5);
        var target = net.minecraft.world.entity.EntityType.COW.create(level);
        if (target == null) { helper.fail("Sticky web target missing"); return; }
        target.setNoAi(true); target.setNoGravity(true);
        target.setPos(player.getX(), player.getY(), player.getZ() + 3.0); level.addFreshEntity(target);
        var config = new com.google.gson.JsonObject(); config.addProperty("dissolveTime", 16);
        var web = new com.fiskmods.heroes.common.entity.projectile.EquipmentProjectileEntity(player,
                "fiskheroes:sticky_web", config, new net.minecraft.world.item.ItemStack(
                        com.fiskmods.heroes.common.item.ModItems.STICKY_WEB.get()), 0);
        web.setPos(player.getX(), player.getY() + 0.2, player.getZ());
        web.setDeltaMovement(0, 0, 3); level.addFreshEntity(web);
        helper.runAfterDelay(3, () -> {
            if (!web.isAttachedWeb() || web.webTargetId() != target.getId() || web.isRemoved())
            { helper.fail("Sticky web must attach to the living target"); return; }
            if (!target.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN)
                    || target.getHealth() != target.getMaxHealth())
            { helper.fail("A sticky web to the legs must slow, not damage"); return; }
            var tag = new net.minecraft.nbt.CompoundTag(); web.addAdditionalSaveData(tag); web.discard();
            var reloaded = new com.fiskmods.heroes.common.entity.projectile.EquipmentProjectileEntity(
                    com.fiskmods.heroes.common.entity.ModEntities.EQUIPMENT_PROJECTILE.get(), level);
            reloaded.readAdditionalSaveData(tag); reloaded.setPos(web.getX(), web.getY(), web.getZ());
            level.addFreshEntity(reloaded);
            target.setPos(target.getX() + 2.0, target.getY(), target.getZ());
            helper.runAfterDelay(3, () -> {
                if (!reloaded.isAttachedWeb() || reloaded.webTargetId() != target.getId()
                        || Math.abs(reloaded.getX() - target.getX()) > 1.0)
                { helper.fail("Reloaded web must resolve and follow its moving target"); return; }
                helper.runAfterDelay(18, () -> {
                    if (!reloaded.isRemoved()) { helper.fail("Sticky web did not dissolve"); return; }
                    target.discard(); helper.succeed();
                });
            });
        });
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void stickyWebAttachesToBlockAndDissolves(GameTestHelper helper)
    {
        var level = helper.getLevel();
        var base = helper.absolutePos(new net.minecraft.core.BlockPos(1, 180, 1));
        var player = new net.minecraftforge.common.util.FakePlayer(level,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "sticky-wall-test"));
        player.setPos(base.getX() + 0.5, base.getY(), base.getZ() + 0.5);
        var wall = base.offset(0, 1, 3);
        level.setBlockAndUpdate(wall, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        var config = new com.google.gson.JsonObject(); config.addProperty("dissolveTime", 12);
        var web = new com.fiskmods.heroes.common.entity.projectile.EquipmentProjectileEntity(player,
                "fiskheroes:sticky_web", config, new net.minecraft.world.item.ItemStack(
                        com.fiskmods.heroes.common.item.ModItems.STICKY_WEB.get()), 0);
        web.setDeltaMovement(0, 0, 3); level.addFreshEntity(web);
        helper.runAfterDelay(3, () -> {
            if (!web.isAttachedWeb() || web.webAnchor() != net.minecraft.core.Direction.NORTH.get3DDataValue())
            { helper.fail("Sticky web must attach to the struck block face"); return; }
            helper.runAfterDelay(15, () -> {
                if (!web.isRemoved()) { helper.fail("Block web failed to dissolve"); return; }
                level.removeBlock(wall, false); helper.succeed();
            });
        });
    }

    @GameTest(template = "test_empty")
    public static void gadgetDamageProfilesRespectHurtCooldownAndDropoff(GameTestHelper helper)
    {
        var level = helper.getLevel();
        var attacker = net.minecraft.world.entity.EntityType.ZOMBIE.create(level);
        var target = net.minecraft.world.entity.EntityType.IRON_GOLEM.create(level);
        if (attacker == null || target == null) { helper.fail("Missing damage test actors"); return; }
        attacker.setNoAi(true); target.setNoAi(true);
        var props = new com.google.gson.JsonObject();
        props.addProperty("DAMAGE_DROPOFF", 0.6F);
        props.addProperty("HIT_COOLDOWN", 0);
        var profile = new com.google.gson.JsonObject();
        profile.add("properties", props);
        var source = level.damageSources().mobAttack(attacker);
        com.fiskmods.heroes.common.hero.modifier.DamageGroups.applyProfileDamage(target, attacker, source, 8.0F, profile);
        com.fiskmods.heroes.common.hero.modifier.DamageGroups.applyProfileDamage(target, attacker, source, 8.0F, profile);
        if (Math.abs(target.getHealth() - 88.8F) > 0.01F)
        { helper.fail("Consecutive gadget hits must bypass cooldown but use pre-hit damage dropoff"); return; }
        target.setHealth(100.0F); target.invulnerableTime = 0;
        props.remove("DAMAGE_DROPOFF"); props.addProperty("HIT_COOLDOWN", 5);
        com.fiskmods.heroes.common.hero.modifier.DamageGroups.applyProfileDamage(target, attacker, source, 8.0F, profile);
        com.fiskmods.heroes.common.hero.modifier.DamageGroups.applyProfileDamage(target, attacker, source, 8.0F, profile);
        if (Math.abs(target.getHealth() - 92.0F) > 0.01F)
        { helper.fail("Nonzero profile cooldown must still block hits during its first five ticks"); return; }
        target.invulnerableTime = target.invulnerableDuration - 5;
        com.fiskmods.heroes.common.hero.modifier.DamageGroups.applyProfileDamage(target, attacker, source, 8.0F, profile);
        if (Math.abs(target.getHealth() - 84.0F) > 0.01F)
        { helper.fail("Profile cooldown must allow the next hit after five ticks"); return; }
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void rapidWebProjectilesEachDamageTheTarget(GameTestHelper helper)
    {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new net.minecraft.core.BlockPos(1, 180, 1));
        var player = new net.minecraftforge.common.util.FakePlayer(level,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "rapid-web-damage-test"));
        player.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        var target = net.minecraft.world.entity.EntityType.IRON_GOLEM.create(level);
        if (target == null) { helper.fail("Rapid web target missing"); return; }
        target.setNoAi(true); target.setNoGravity(true);
        target.setPos(player.getX(), player.getY(), player.getZ() + 3.0D);
        level.addFreshEntity(target);
        var props = new com.google.gson.JsonObject();
        props.addProperty("DAMAGE_DROPOFF", 0.2F); props.addProperty("HIT_COOLDOWN", 0);
        var profile = new com.google.gson.JsonObject();
        profile.addProperty("damage", 4.0F); profile.add("properties", props);
        var config = new com.google.gson.JsonObject(); config.add("damageProfile", profile);
        for (int i = 0; i < 5; ++i)
        {
            var web = new com.fiskmods.heroes.common.entity.projectile.EquipmentProjectileEntity(player,
                    "fiskheroes:rapid_webs", config, new net.minecraft.world.item.ItemStack(
                            com.fiskmods.heroes.common.item.ModItems.IMPACT_WEB.get()), 0);
            web.setPos(player.getX(), player.getY() + 1.5D, player.getZ());
            web.setDeltaMovement(0, 0, 4);
            level.addFreshEntity(web);
            web.tick();
        }
        if (Math.abs(target.getHealth() - 83.2F) > 0.05F)
        { helper.fail("Five rapid web projectiles must each hit, with profile damage dropoff"); return; }
        target.discard();
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void impactWebDamagesWithoutStickySlowdown(GameTestHelper helper)
    {
        var level = helper.getLevel();
        var base = helper.absolutePos(new net.minecraft.core.BlockPos(1, 180, 1));
        var player = new net.minecraftforge.common.util.FakePlayer(level,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "impact-web-test"));
        player.setPos(base.getX() + 0.5, base.getY(), base.getZ() + 0.5);
        var target = net.minecraft.world.entity.EntityType.COW.create(level);
        if (target == null) { helper.fail("Impact web target missing"); return; }
        target.setNoAi(true); target.setNoGravity(true);
        target.setPos(player.getX(), player.getY(), player.getZ() + 3.0); level.addFreshEntity(target);
        var profile = new com.google.gson.JsonObject(); profile.addProperty("damage", 5.0F);
        var config = new com.google.gson.JsonObject(); config.add("damageProfile", profile);
        var web = new com.fiskmods.heroes.common.entity.projectile.EquipmentProjectileEntity(player,
                "fiskheroes:impact_web", config, new net.minecraft.world.item.ItemStack(
                        com.fiskmods.heroes.common.item.ModItems.IMPACT_WEB.get()), 0);
        web.setPos(player.getX(), player.getY() + 0.5, player.getZ());
        web.setDeltaMovement(0, 0, 3); level.addFreshEntity(web);
        helper.runAfterDelay(3, () -> {
            if (!web.isRemoved() || target.getHealth() >= target.getMaxHealth()
                    || target.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN))
            { helper.fail("Impact web must damage without applying the sticky web slowdown"); return; }
            target.discard(); helper.succeed();
        });
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void stickyWebOnTorsoDoesNotSlow(GameTestHelper helper)
    {
        var level = helper.getLevel();
        var base = helper.absolutePos(new net.minecraft.core.BlockPos(1, 180, 1));
        var player = new net.minecraftforge.common.util.FakePlayer(level,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "sticky-torso-test"));
        player.setPos(base.getX() + 0.5, base.getY(), base.getZ() + 0.5);
        var target = net.minecraft.world.entity.EntityType.IRON_GOLEM.create(level);
        if (target == null) { helper.fail("Sticky web target missing"); return; }
        target.setNoAi(true); target.setNoGravity(true);
        target.setPos(player.getX(), player.getY(), player.getZ() + 3.0); level.addFreshEntity(target);
        var web = new com.fiskmods.heroes.common.entity.projectile.EquipmentProjectileEntity(player,
                "fiskheroes:sticky_web", new com.google.gson.JsonObject(), new net.minecraft.world.item.ItemStack(
                        com.fiskmods.heroes.common.item.ModItems.STICKY_WEB.get()), 0);
        web.setPos(player.getX(), player.getY() + 1.5, player.getZ());
        web.setDeltaMovement(0, 0, 3); level.addFreshEntity(web);
        helper.runAfterDelay(3, () -> {
            if (!web.isAttachedWeb() || target.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN))
            { helper.fail("A torso web must stick without the leg slowdown"); return; }
            if (web.getZ() >= target.getZ() - target.getBbWidth() * 0.5)
            { helper.fail("The web patch must be in front of the hit surface, not inside the target"); return; }
            web.discard(); target.discard(); helper.succeed();
        });
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void rapidWebsFireOverTimeAndCancelWhenSelectionChanges(GameTestHelper helper)
    {
        var level = helper.getLevel();
        var player = new net.minecraftforge.common.util.FakePlayer(level,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "rapid-web-test"));
        var hero = com.fiskmods.heroes.common.hero.Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:spider_man");
        if (hero == null) { helper.fail("Spider-Man missing"); return; }
        var slots = new net.minecraft.world.entity.EquipmentSlot[] {net.minecraft.world.entity.EquipmentSlot.HEAD,
                net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.LEGS,
                net.minecraft.world.entity.EquipmentSlot.FEET};
        for (int i = 0; i < 4; ++i)
            if (hero.getDefaultIteration().hasPieceOfSet(i)) player.setItemSlot(slots[i],
                    com.fiskmods.heroes.common.hero.ItemHeroArmor.create(hero.getDefaultIteration(),
                            com.fiskmods.heroes.common.item.ModItems.heroArmor[i]));
        com.fiskmods.heroes.common.hero.HeroTracker.update(player);
        var data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(player);
        var belt = com.fiskmods.heroes.common.hero.equipment.EquipmentHelper.getUtilityBelt(hero, player, data);
        if (belt == null) { helper.fail("Spider-Man web equipment missing"); return; }
        var names = new java.util.ArrayList<>(com.fiskmods.heroes.common.hero.equipment.EquipmentHelper.equipmentOptions(belt).keySet());
        int selected = names.indexOf("fiskheroes:rapid_webs");
        if (selected < 0) { helper.fail("Rapid webs missing"); return; }
        data.getData().set(com.fiskmods.heroes.common.data.var.Vars.UTILITY_BELT_TYPE, (byte) selected);
        var base = helper.absolutePos(new net.minecraft.core.BlockPos(1, 200, 1));
        player.setPos(base.getX() + 0.5, base.getY(), base.getZ() + 0.5);
        player.setYRot(0); player.setXRot(-25);
        if (!com.fiskmods.heroes.common.hero.equipment.EquipmentHelper.useUtilityBelt(player))
        { helper.fail("Failed to start rapid web burst"); return; }
        var bounds = player.getBoundingBox().inflate(120);
        java.util.function.IntSupplier count = () -> level.getEntitiesOfClass(
                com.fiskmods.heroes.common.entity.projectile.EquipmentProjectileEntity.class, bounds,
                e -> e.getOwner() == player).size();
        if (count.getAsInt() != 1 || com.fiskmods.heroes.common.hero.equipment.EquipmentHelper.useUtilityBelt(player))
        { helper.fail("Rapid webs must fire one shot initially and consume their single charge"); return; }
        helper.runAfterDelay(2, () -> {
            com.fiskmods.heroes.common.hero.equipment.EquipmentHelper.tickRapidWebs(player);
            if (count.getAsInt() != 2) { helper.fail("Second rapid web must fire after two ticks"); return; }
            data.getData().set(com.fiskmods.heroes.common.data.var.Vars.UTILITY_BELT_TYPE, (byte) 0);
            helper.runAfterDelay(2, () -> {
                com.fiskmods.heroes.common.hero.equipment.EquipmentHelper.tickRapidWebs(player);
                if (count.getAsInt() != 2) { helper.fail("Switching gadgets must cancel the rapid burst"); return; }
                for (var shot : level.getEntitiesOfClass(com.fiskmods.heroes.common.entity.projectile.EquipmentProjectileEntity.class,
                        bounds, e -> e.getOwner() == player)) shot.discard();
                com.fiskmods.heroes.common.hero.equipment.EquipmentHelper.clearRapidWebs(player);
                helper.succeed();
            });
        });
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void ricochetWebBouncesOnceThenSticks(GameTestHelper helper)
    {
        var level = helper.getLevel();
        var base = helper.absolutePos(new net.minecraft.core.BlockPos(1, 200, 1));
        var player = new net.minecraftforge.common.util.FakePlayer(level,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "ricochet-web-test"));
        player.setPos(base.getX() + 0.5, base.getY(), base.getZ() + 0.5);
        player.setYRot(0); player.setXRot(0);
        var first = base.offset(0, 1, 5);
        var second = base.offset(0, 1, -5);
        level.setBlockAndUpdate(first, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(second, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        var web = new com.fiskmods.heroes.common.entity.projectile.EquipmentProjectileEntity(player,
                "fiskheroes:ricochet_web", new com.google.gson.JsonObject(), new net.minecraft.world.item.ItemStack(
                        com.fiskmods.heroes.common.item.ModItems.RICOCHET_WEB.get()), 0);
        double launchSpeed = web.getDeltaMovement().length();
        if (launchSpeed < 5.8 || launchSpeed > 6.2)
        { helper.fail("Ricochet web must launch at approximately six blocks per tick: " + launchSpeed); return; }
        web.setDeltaMovement(0, 0, 6); level.addFreshEntity(web);
        helper.runAfterDelay(2, () -> {
            var tag = new net.minecraft.nbt.CompoundTag(); web.addAdditionalSaveData(tag);
            if (web.isRemoved() || web.isAttachedWeb() || tag.getByte("RicochetBounces") != 1
                    || web.getDeltaMovement().z >= 0)
            { helper.fail("Ricochet web must bounce off its first wall: pos=" + web.position() + " vel=" + web.getDeltaMovement()); return; }
            helper.runAfterDelay(4, () -> {
                if (web.isRemoved() || !web.isAttachedWeb()
                        || web.webAnchor() != net.minecraft.core.Direction.SOUTH.get3DDataValue())
                { helper.fail("Ricochet web must stick to its second wall: pos=" + web.position()); return; }
                web.discard(); level.removeBlock(first, false); level.removeBlock(second, false); helper.succeed();
            });
        });
    }

}
