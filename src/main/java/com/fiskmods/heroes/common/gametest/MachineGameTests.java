package com.fiskmods.heroes.common.gametest;

import java.util.Map;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.block.AlloySmelteryBlockEntity;
import com.fiskmods.heroes.common.block.FabricatorRecipes;
import com.fiskmods.heroes.common.block.ModBlocks;
import com.fiskmods.heroes.common.block.SuitFabricatorBlockEntity;
import com.fiskmods.heroes.common.hero.Hero;
import com.fiskmods.heroes.common.hero.ItemHeroArmor;
import com.fiskmods.heroes.common.item.ItemFlashRing;
import com.fiskmods.heroes.common.item.ItemSuitDrive;
import com.fiskmods.heroes.common.item.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Small server-side regression tests. They run with runGameTestServer, not during ordinary play. */
@GameTestHolder(FiskHeroes.MODID)
@PrefixGameTestTemplate(false)
public final class MachineGameTests
{
    private MachineGameTests() {}

    @GameTest(template = "test_empty")
    public static void suitIteratorChangesVersionsWithoutPartialOrUnknownChanges(GameTestHelper helper)
    {
        var hero = Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:captain_america");
        var other = Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:the_flash");
        if (hero == null || other == null || hero.findIteration("aou").isEmpty())
        { helper.fail("Suit iterations missing"); return; }
        var player = helper.makeMockPlayer();
        var menu = new com.fiskmods.heroes.common.block.SuitIteratorMenu(1, player.getInventory(), BlockPos.ZERO);
        for (int i = 0; i < 4; ++i)
        {
            var piece = ItemHeroArmor.create(hero.getDefaultIteration(), ModItems.heroArmor[i]);
            piece.getOrCreateTag().putString("IteratorTestTag", "preserved");
            menu.armorInput.setItem(i, piece);
        }
        menu.applyIteration("aou");
        var target = hero.findIteration("aou").orElseThrow();
        for (int i = 0; i < 4; ++i)
        {
            var piece = menu.armorInput.getItem(i);
            if (ItemHeroArmor.getHero(piece) != target || !"preserved".equals(piece.getTag().getString("IteratorTestTag")))
            { helper.fail("Every piece must change to the selected version without losing its data"); return; }
        }
        menu.applyIteration("not_a_real_version");
        if (ItemHeroArmor.getHero(menu.armorInput.getItem(0)) != target)
        { helper.fail("Unknown version must not silently select the default"); return; }
        menu.armorInput.setItem(0, ItemHeroArmor.create(other.getDefaultIteration(), ModItems.heroArmor[0]));
        menu.applyIteration("");
        if (ItemHeroArmor.getHero(menu.armorInput.getItem(1)) != target)
        { helper.fail("Mixed heroes must not change part of a suit"); return; }
        menu.armorInput.setItem(0, ItemHeroArmor.create(target, ModItems.heroArmor[0]));
        menu.applyIteration("");
        for (int i = 0; i < 4; ++i)
            if (ItemHeroArmor.getHero(menu.armorInput.getItem(i)) != hero.getDefaultIteration())
            { helper.fail("Empty version key must restore the default for all pieces"); return; }
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void redstoneIteratorSkipsVanityAndRejectsMixedOrInvalidPieces(GameTestHelper helper)
    {
        var hero = Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:captain_america");
        var other = Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:the_flash");
        var rip = Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:rip_hunter");
        if (hero == null || other == null || rip == null || hero.findIteration("aou").isEmpty()
                || hero.findIteration("canada").isEmpty() || hero.findIteration("museum").isEmpty())
        { helper.fail("Required iterations missing"); return; }
        var aou = hero.findIteration("aou").orElseThrow();
        var museum = hero.findIteration("museum").orElseThrow();
        var slots = new net.minecraft.world.entity.EquipmentSlot[] {
                net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.entity.EquipmentSlot.CHEST,
                net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET };
        BlockPos relative = new BlockPos(1, 5, 1);
        helper.setBlock(relative, ModBlocks.SUIT_ITERATOR.get());
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(relative);
        var stand = net.minecraft.world.entity.EntityType.ARMOR_STAND.create(level);
        if (stand == null) { helper.fail("Could not create armor stand"); return; }
        stand.setPos(pos.getX() + 0.5D, pos.getY() + 1, pos.getZ() + 0.5D);
        stand.setNoGravity(true);
        for (int i = 0; i < 4; ++i)
        {
            var stack = ItemHeroArmor.create(aou, ModItems.heroArmor[i]);
            stack.getOrCreateTag().putString("IteratorTestTag", "preserved");
            stand.setItemSlot(slots[i], stack);
        }
        level.addFreshEntity(stand);
        // A rising edge after aou should skip the vanity-only canada and select museum.
        helper.setBlock(relative.west(), net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK);
        for (int i = 0; i < 4; ++i)
            if (ItemHeroArmor.getHero(stand.getItemBySlot(slots[i])) != museum
                    || !"preserved".equals(stand.getItemBySlot(slots[i]).getTag().getString("IteratorTestTag")))
            { helper.fail("Redstone must skip vanity and preserve every piece's data"); return; }
        var block = (com.fiskmods.heroes.common.block.SuitIteratorBlock) ModBlocks.SUIT_ITERATOR.get();
        block.neighborChanged(level.getBlockState(pos), level, pos, net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK,
                pos.west(), false);
        if (ItemHeroArmor.getHero(stand.getItemBySlot(slots[0])) != museum)
        { helper.fail("A held redstone signal must not cycle twice"); return; }
        helper.setBlock(relative.west(), net.minecraft.world.level.block.Blocks.AIR);
        stand.setItemSlot(slots[0], ItemHeroArmor.create(other.getDefaultIteration(), ModItems.heroArmor[0]));
        helper.setBlock(relative.west(), net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK);
        if (ItemHeroArmor.getHero(stand.getItemBySlot(slots[1])) != museum)
        { helper.fail("Mixed hero suits must not be partially changed"); return; }
        helper.setBlock(relative.west(), net.minecraft.world.level.block.Blocks.AIR);
        // Rip Hunter has no helmet in either version. A malformed helmet must never be
        // converted to an iteration that does not support the equipped piece.
        stand.setItemSlot(slots[0], ItemHeroArmor.create(rip.getDefaultIteration(), ModItems.heroArmor[0]));
        stand.setItemSlot(slots[1], ItemHeroArmor.create(rip.getDefaultIteration(), ModItems.heroArmor[1]));
        stand.setItemSlot(slots[2], ItemStack.EMPTY);
        stand.setItemSlot(slots[3], ItemStack.EMPTY);
        helper.setBlock(relative.west(), net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK);
        if (ItemHeroArmor.getHero(stand.getItemBySlot(slots[0])) != rip.getDefaultIteration()
                || ItemHeroArmor.getHero(stand.getItemBySlot(slots[1])) != rip.getDefaultIteration())
        { helper.fail("Incompatible iterations must not partially change the suit"); return; }
        stand.discard();
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void alloySmelteryConsumesIngredientsAndFuel(GameTestHelper helper)
    {
        BlockPos relative = new BlockPos(1, 1, 1);
        helper.setBlock(relative, ModBlocks.ALLOY_SMELTERY.get());
        BlockPos pos = helper.absolutePos(relative);
        AlloySmelteryBlockEntity be = (AlloySmelteryBlockEntity) helper.getLevel().getBlockEntity(pos);
        be.setItem(0, new ItemStack(Items.GOLD_INGOT, 2));
        be.setItem(1, new ItemStack(ModItems.TITANIUM_INGOT.get(), 2));
        be.setItem(3, new ItemStack(Items.BLAZE_POWDER));
        for (int i = 0; i < AlloySmelteryBlockEntity.MAX_BURN_TIME; ++i)
        {
            AlloySmelteryBlockEntity.serverTick(helper.getLevel(), pos, helper.getLevel().getBlockState(pos), be);
        }
        if (!be.getItem(2).is(ModItems.GOLD_TITANIUM_INGOT.get()) || be.getItem(2).getCount() != 1
                || be.getItem(0).getCount() != 1 || be.getItem(1).getCount() != 1
                || !be.getItem(3).isEmpty() || be.dataAccess.get(1) != 7)
        {
            helper.fail("Smelting must consume one of each ingredient and one fuel charge");
        }
        else helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void alloySmelteryDoesNotConsumeWhenOutputBlocked(GameTestHelper helper)
    {
        BlockPos relative = new BlockPos(1, 1, 1);
        helper.setBlock(relative, ModBlocks.ALLOY_SMELTERY.get());
        BlockPos pos = helper.absolutePos(relative);
        AlloySmelteryBlockEntity be = (AlloySmelteryBlockEntity) helper.getLevel().getBlockEntity(pos);
        be.setItem(0, new ItemStack(Items.GOLD_INGOT));
        be.setItem(1, new ItemStack(ModItems.TITANIUM_INGOT.get()));
        be.setItem(2, new ItemStack(Items.DIRT));
        be.setItem(3, new ItemStack(Items.BLAZE_POWDER));
        for (int i = 0; i < 220; ++i)
        {
            AlloySmelteryBlockEntity.serverTick(helper.getLevel(), pos, helper.getLevel().getBlockState(pos), be);
        }
        if (be.getItem(0).getCount() != 1 || be.getItem(1).getCount() != 1
                || be.getItem(3).getCount() != 1 || !be.getItem(2).is(Items.DIRT))
        {
            helper.fail("A blocked output must not consume ingredients or fuel");
        }
        else helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void fabricatorRejectsMissingIngredients(GameTestHelper helper)
    {
        Hero hero = FabricatorRecipes.getCraftableHeroes().stream().findFirst().orElse(null);
        if (hero == null)
        {
            helper.fail("No fabricator recipe loaded");
            return;
        }
        BlockPos relative = new BlockPos(1, 1, 1);
        helper.setBlock(relative, ModBlocks.COSMIC_SUIT_FABRICATOR.get());
        BlockPos pos = helper.absolutePos(relative);
        SuitFabricatorBlockEntity be = (SuitFabricatorBlockEntity) helper.getLevel().getBlockEntity(pos);
        be.setItem(10, ItemSuitDrive.createForHero(hero));
        be.setItem(11, new ItemStack(ModItems.SUIT_CORE.get()));
        if (be.canPrint() || be.startPrint() || be.getItem(11).isEmpty() || be.isPrinting())
        {
            helper.fail("Missing ingredients must not start printing or consume the core");
        }
        else helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void fabricatorPrintsAndRetainsHero(GameTestHelper helper)
    {
        Hero hero = FabricatorRecipes.getCraftableHeroes().stream()
                .filter(h -> FabricatorRecipes.get(h).items().values().stream().mapToInt(n -> (n + 63) / 64).sum() <= 10)
                .findFirst().orElse(null);
        if (hero == null)
        {
            helper.fail("No fabricator recipe loaded");
            return;
        }
        BlockPos relative = new BlockPos(1, 1, 1);
        helper.setBlock(relative, ModBlocks.COSMIC_SUIT_FABRICATOR.get());
        BlockPos pos = helper.absolutePos(relative);
        SuitFabricatorBlockEntity be = (SuitFabricatorBlockEntity) helper.getLevel().getBlockEntity(pos);
        be.setItem(10, ItemSuitDrive.createForHero(hero));
        be.setItem(11, new ItemStack(ModItems.SUIT_CORE.get()));
        int slot = 0;
        for (Map.Entry<Item, Integer> ingredient : FabricatorRecipes.get(hero).items().entrySet())
        {
            int remaining = ingredient.getValue();
            while (remaining > 0)
            {
                int count = Math.min(64, remaining);
                be.setItem(slot++, new ItemStack(ingredient.getKey(), count));
                remaining -= count;
            }
        }
        if (!be.startPrint() || !be.isPrinting() || !be.getItem(11).isEmpty())
        {
            helper.fail("Valid recipe must start printing and consume its core");
            return;
        }
        for (int i = 0; i < SuitFabricatorBlockEntity.MAX_PRINT_TIME; ++i)
        {
            SuitFabricatorBlockEntity.serverTick(helper.getLevel(), pos, helper.getLevel().getBlockState(pos), be);
            if (i == SuitFabricatorBlockEntity.MAX_PRINT_TIME / 2 - 1)
            {
                SuitFabricatorBlockEntity restored = new SuitFabricatorBlockEntity(pos, helper.getLevel().getBlockState(pos));
                restored.load(be.saveWithoutMetadata());
                if (!restored.isPrinting() || restored.getPrintTime() != SuitFabricatorBlockEntity.MAX_PRINT_TIME / 2
                        || restored.getTargetIteration() == null || restored.getTargetIteration().getHero() != hero)
                {
                    helper.fail("Fabricator must preserve print progress and hero when saved");
                    return;
                }
            }
        }
        if (be.isPrinting() || !be.hasFinishedOutput())
        {
            helper.fail("Fabricator did not finish printing");
            return;
        }
        for (int i = 0; i < 4; ++i)
        {
            ItemStack piece = be.getItem(12 + i);
            if (hero.getDefaultIteration().hasPieceOfSet(i)
                    && (ItemHeroArmor.getHero(piece) == null || ItemHeroArmor.getHero(piece).getHero() != hero))
            {
                helper.fail("Printed armor lost its hero");
                return;
            }
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void augmentorEnforcesLevelsAndPreservesSuit(GameTestHelper helper)
    {
        Hero hero = Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:iron_man");
        if (hero == null || hero.getAugmentSet().size() != 5)
        { helper.fail("Iron Man's original augment data did not load"); return; }
        var set = hero.getAugmentSet();
        if (set.get(0).attribute() != com.fiskmods.heroes.common.hero.HeroAttribute.DAMAGE_REDUCTION
                || set.get(1).attribute() != com.fiskmods.heroes.common.hero.HeroAttribute.PUNCH_DAMAGE
                || set.get(1).min() != 3 || set.get(1).max() != 5)
        { helper.fail("Augment order or range does not match the pack"); return; }
        BlockPos rel = new BlockPos(1, 1, 1);
        helper.setBlock(rel, ModBlocks.SUIT_AUGMENTOR.get());
        BlockPos pos = helper.absolutePos(rel);
        var player = helper.makeMockPlayer();
        player.setPos(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5);
        var menu = new com.fiskmods.heroes.common.block.SuitAugmentorMenu(1, player.getInventory(), pos);
        var core = ItemHeroArmor.create(hero.getDefaultIteration(), ModItems.heroArmor[1]);
        core.getOrCreateTag().putString("Keep", "yes");
        menu.input.setItem(0, core);
        menu.input.setItem(1, new ItemStack(ModItems.SUIT_UPGRADE.get(), 2));
        if (menu.clickMenuButton(player, -1) || menu.clickMenuButton(player, 9)
                || menu.clickMenuButton(player, 1) || menu.input.getItem(1).getCount() != 2)
        { helper.fail("Invalid index or incorrect material must never consume an upgrade"); return; }
        menu.input.setItem(1, new ItemStack(ModItems.SUIT_UPGRADE_VIBRANIUM.get(), 2));
        if (!menu.clickMenuButton(player, 1) || set.level(ItemHeroArmor.getUpgrades(core), set.get(1)) != 1
                || menu.input.getItem(1).getCount() != 1 || menu.clickMenuButton(player, 1))
        { helper.fail("The first punch upgrade must consume exactly one tier IV item"); return; }
        menu.input.setItem(1, new ItemStack(ModItems.SUIT_UPGRADE_DWARF_STAR.get(), 2));
        if (!menu.clickMenuButton(player, 1) || menu.clickMenuButton(player, 1)
                || menu.input.getItem(1).getCount() != 1 || !"yes".equals(core.getTag().getString("Keep")))
        { helper.fail("The last upgrade must stop at max and preserve suit tags"); return; }
        var copy = ItemStack.of(core.save(new net.minecraft.nbt.CompoundTag()));
        if (ItemHeroArmor.getUpgrades(copy) != 0x20 || set.level(ItemHeroArmor.getUpgrades(copy), set.get(1)) != 2)
        { helper.fail("Packed upgrades must round-trip in the original four-bit format"); return; }
        player.setPos(pos.getX() + 100, pos.getY(), pos.getZ());
        menu.input.setItem(1, new ItemStack(ModItems.SUIT_UPGRADE_GOLD_TITANIUM.get()));
        if (menu.clickMenuButton(player, 0) || menu.input.getItem(1).isEmpty())
        { helper.fail("Remote menu requests must be rejected"); return; }
        menu.input.setItem(0, ItemHeroArmor.create(hero.getDefaultIteration(), ModItems.heroArmor[0]));
        if (menu.canUpgrade(0)) { helper.fail("A non-core suit piece must be rejected"); return; }
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void augmentedAttributesApplyAndDoNotLeak(GameTestHelper helper)
    {
        Hero hero = Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:iron_man");
        var player = helper.makeMockPlayer();
        var data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(player);
        if (hero == null || data == null || hero.getAugmentSet().size() != 5)
        { helper.fail("Missing Iron Man or augment data"); return; }
        var core = ItemHeroArmor.create(hero.getDefaultIteration(), ModItems.heroArmor[1]);
        // One damage-reduction level and two punch levels.
        ItemHeroArmor.setUpgrades(core, 0x21);
        player.getInventory().armor.set(2, core);
        data.setHero(hero.getDefaultIteration());
        var attrs = com.fiskmods.heroes.common.hero.attribute.SHAttributes.collect(player, hero);
        var punch = attrs.get(com.fiskmods.heroes.common.hero.HeroAttribute.PUNCH_DAMAGE);
        var reduction = attrs.get(com.fiskmods.heroes.common.hero.HeroAttribute.DAMAGE_REDUCTION);
        if (punch == null || Math.abs(punch.amount() - 10) > .001 || reduction == null
                || Math.abs(reduction.amount() - (1 - (1 - .948) / 1.1)) > .001)
        { helper.fail("Original augment formulas were not applied to equipped core"); return; }
        com.fiskmods.heroes.common.hero.attribute.SHAttributes.applyModifiers(player);
        var damage = new net.minecraftforge.event.entity.living.LivingHurtEvent(player, player.damageSources().playerAttack(player), 11F);
        com.fiskmods.heroes.common.event.CommonEventHandler.onLivingHurt(damage);
        if (Math.abs(damage.getAmount() - .52F) > .001)
        { helper.fail("Tier and augment must combine before vanilla armor"); return; }
        var bypass = new net.minecraftforge.event.entity.living.LivingHurtEvent(player, player.damageSources().fellOutOfWorld(), 11F);
        com.fiskmods.heroes.common.event.CommonEventHandler.onLivingHurt(bypass);
        if (Math.abs(bypass.getAmount() - 11F) > .001)
        { helper.fail("Void damage must bypass suit protection"); return; }
        player.getInventory().armor.set(2, ItemStack.EMPTY);
        com.fiskmods.heroes.common.hero.attribute.SHAttributes.applyModifiers(player);
        if (Math.abs(player.getAttributeValue(com.fiskmods.heroes.common.hero.attribute.SHAttributes.PUNCH_DAMAGE.get()) - 9.0) > .001
                || Math.abs(player.getAttributeValue(com.fiskmods.heroes.common.hero.attribute.SHAttributes.DAMAGE_REDUCTION.get()) - .948) > .001)
        { helper.fail("Removing the core must remove its augment modifiers"); return; }
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void upgradeRecipesRequireThePreviousTier(GameTestHelper helper)
    {
        var player = new net.minecraftforge.common.util.FakePlayer(helper.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "augment-recipe-test"));
        var grid = new net.minecraft.world.inventory.TransientCraftingContainer(player.inventoryMenu, 3, 3);
        Item[] upgrades = { ModItems.SUIT_UPGRADE.get(), ModItems.SUIT_UPGRADE_GOLD.get(),
                ModItems.SUIT_UPGRADE_GOLD_TITANIUM.get(), ModItems.SUIT_UPGRADE_VIBRANIUM.get(),
                ModItems.SUIT_UPGRADE_DWARF_STAR.get() };
        Item[] materials = { Items.IRON_INGOT, Items.GOLD_INGOT, ModItems.GOLD_TITANIUM_INGOT.get(),
                ModItems.VIBRANIUM_INGOT.get(), ModItems.DWARF_STAR_ALLOY.get() };
        String[] ids = { "suit_upgrade", "suit_upgrade_gold", "suit_upgrade_gold_titanium",
                "suit_upgrade_vibranium", "suit_upgrade_dwarf_star" };
        for (int tier = 0; tier < ids.length; ++tier)
        {
            var recipe = helper.getLevel().getRecipeManager().byKey(FiskHeroes.id(ids[tier])).orElse(null);
            if (!(recipe instanceof net.minecraft.world.item.crafting.CraftingRecipe crafting))
            { helper.fail("Missing upgrade recipe " + ids[tier]); return; }
            for (int slot = 0; slot < 9; ++slot) grid.setItem(slot, ItemStack.EMPTY);
            for (int slot : new int[] {0, 1, 2, 6, 7, 8}) grid.setItem(slot, new ItemStack(materials[tier]));
            grid.setItem(4, new ItemStack(ModItems.TUTRIDIUM_GEM.get()));
            if (tier > 0)
            {
                grid.setItem(3, new ItemStack(upgrades[tier - 1]));
                grid.setItem(5, new ItemStack(upgrades[tier - 1]));
            }
            if (!crafting.matches(grid, helper.getLevel())
                    || !crafting.assemble(grid, helper.getLevel().registryAccess()).is(upgrades[tier]))
            { helper.fail("Upgrade recipe failed for tier " + tier); return; }
            if (tier > 0)
            {
                grid.setItem(5, new ItemStack(upgrades[tier]));
                if (crafting.matches(grid, helper.getLevel()))
                { helper.fail("Upgrade recipe accepted the wrong previous tier"); return; }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void augmentDefinitionsAndGameplayAreConnected(GameTestHelper helper)
    {
        long loaded = Hero.REGISTRY.getHeroes().stream()
                .filter(hero -> hero.getRegistryName().getNamespace().equals("fiskheroes"))
                .filter(hero -> hero.getAugmentSet().size() > 0).count();
        if (loaded < 67) { helper.fail("Expected 67 built-in augment definitions, got " + loaded); return; }
        Hero hero = Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:green_arrow");
        var player = helper.makeMockPlayer();
        var data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(player);
        if (hero == null || data == null || hero.getAugmentSet().size() != 6)
        { helper.fail("Missing Green Arrow augments"); return; }
        var core = ItemHeroArmor.create(hero.getDefaultIteration(), ModItems.heroArmor[1]);
        player.getInventory().armor.set(2, core);
        data.setHero(hero.getDefaultIteration());
        float before = com.fiskmods.heroes.common.item.ItemCompoundBow.getDrawDuration(player);
        // Two levels each on jump, fall resistance, bow draw and sprint (indices 2..5).
        ItemHeroArmor.setUpgrades(core, 0x222200);
        float after = com.fiskmods.heroes.common.item.ItemCompoundBow.getDrawDuration(player);
        if (Math.abs(before - 12) > .001 || Math.abs(after - 9) > .001)
        { helper.fail("Bow drawback augment must shorten the actual draw time"); return; }
        com.fiskmods.heroes.common.hero.attribute.SHAttributes.applyModifiers(player);
        var fall = new net.minecraftforge.event.entity.living.LivingFallEvent(player, 20F, 1F);
        com.fiskmods.heroes.common.event.CommonEventHandler.onLivingFall(fall);
        if (Math.abs(fall.getDistance() - 13F) > .001)
        { helper.fail("Fall resistance augment must change the actual fall distance"); return; }
        player.setDeltaMovement(0, 0, 0);
        com.fiskmods.heroes.common.hero.ability.ModifierHandler.onJump(player);
        if (Math.abs(player.getDeltaMovement().y - .3D) > .001)
        { helper.fail("Jump augment must change the actual jump impulse"); return; }
        player.setSprinting(true);
        com.fiskmods.heroes.common.hero.attribute.SHAttributes.applyModifiers(player);
        var speed = player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        player.setSprinting(false);
        com.fiskmods.heroes.common.hero.attribute.SHAttributes.applyModifiers(player);
        var walking = player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        if (speed <= walking * 1.3D)
        { helper.fail("Sprint augment must affect only sprinting movement"); return; }
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void naniteInactiveProfileRevokesSuitAugments(GameTestHelper helper)
    {
        Hero hero = Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:iron_man_mk50");
        var player = helper.makeMockPlayer();
        var data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(player);
        if (hero == null || data == null || hero.getAugmentSet().size() != 5)
        { helper.fail("Missing Mk50 or augment definition"); return; }
        var core = ItemHeroArmor.create(hero.getDefaultIteration(), ModItems.heroArmor[1]);
        ItemHeroArmor.setUpgrades(core, 0x11);
        player.getInventory().armor.set(2, core);
        data.setHero(hero.getDefaultIteration());
        var js = new com.fiskmods.heroes.pack.js.JSEntity(player);
        var punch = com.fiskmods.heroes.common.hero.HeroAttribute.PUNCH_DAMAGE;
        var reduction = com.fiskmods.heroes.common.hero.HeroAttribute.DAMAGE_REDUCTION;
        js.setData("fiskheroes:dyn/nanites", false);
        var inactive = com.fiskmods.heroes.common.hero.attribute.SHAttributes.collect(player, hero);
        if (inactive.containsKey(punch) || inactive.get(reduction).amount() != 0)
        { helper.fail("Inactive nanites must revoke both damage and punch augments"); return; }
        js.setData("fiskheroes:dyn/nanites", true);
        var active = com.fiskmods.heroes.common.hero.attribute.SHAttributes.collect(player, hero);
        if (Math.abs(active.get(punch).amount() - 9.5) > .001 || !active.containsKey(reduction))
        { helper.fail("Reactivating nanites must restore augment effects"); return; }
        data.getData().set(com.fiskmods.heroes.common.data.var.Vars.BLADE, true);
        var blade = com.fiskmods.heroes.common.hero.attribute.SHAttributes.collect(player, hero);
        if (Math.abs(blade.get(punch).amount() - 11.5) > .001)
        { helper.fail("An inherited blade profile must apply augments to its override"); return; }
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void augmentRecipesHaveRecipeBookAdvancements(GameTestHelper helper)
    {
        for (String recipe : new String[] { "suit_augmentor", "suit_upgrade", "suit_upgrade_gold",
                "suit_upgrade_gold_titanium", "suit_upgrade_vibranium", "suit_upgrade_dwarf_star" })
        {
            var advancement = helper.getLevel().getServer().getAdvancements().getAdvancement(
                    FiskHeroes.id("recipes/misc/" + recipe));
            if (advancement == null || !advancement.getCriteria().containsKey("has_material")
                    || !advancement.getCriteria().containsKey("has_recipe"))
            { helper.fail("Missing recipe-book unlock advancement for " + recipe); return; }
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void augmentedCoreSurvivesIteratorAndRing(GameTestHelper helper)
    {
        Hero captain = Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:captain_america");
        Hero flash = Hero.REGISTRY.getHeroIgnoreCase("fiskheroes:the_flash");
        if (captain == null || flash == null || captain.findIteration("aou").isEmpty())
        { helper.fail("Suit variants for augment persistence are missing"); return; }
        var player = helper.makeMockPlayer();
        var menu = new com.fiskmods.heroes.common.block.SuitIteratorMenu(1, player.getInventory(), BlockPos.ZERO);
        var core = ItemHeroArmor.create(captain.getDefaultIteration(), ModItems.heroArmor[1]);
        ItemHeroArmor.setUpgrades(core, 0x12345);
        menu.armorInput.setItem(1, core);
        menu.applyIteration("aou");
        if (ItemHeroArmor.getHero(core) != captain.findIteration("aou").orElseThrow()
                || ItemHeroArmor.getUpgrades(core) != 0x12345)
        { helper.fail("Changing a compatible iteration must preserve all augment nibbles"); return; }
        var flashCore = ItemHeroArmor.create(flash.getDefaultIteration(), ModItems.heroArmor[1]);
        ItemHeroArmor.setUpgrades(flashCore, 0x123456);
        var ring = new ItemStack(ModItems.FLASH_RING.get());
        ItemFlashRing.setContainedArmor(ring, ItemStack.EMPTY, flashCore, ItemStack.EMPTY, ItemStack.EMPTY);
        ring = ItemStack.of(ring.save(new net.minecraft.nbt.CompoundTag()));
        var stored = ItemFlashRing.getContainedArmor(ring);
        if (stored == null || ItemHeroArmor.getHero(stored[1]) != flash.getDefaultIteration()
                || ItemHeroArmor.getUpgrades(stored[1]) != 0x123456)
        { helper.fail("Ring serialization must preserve the upgraded suit core"); return; }
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void augmentParsingRejectsInvalidRanges(GameTestHelper helper)
    {
        var set = com.fiskmods.heroes.common.hero.augment.AugmentSet.parse(com.google.gson.JsonParser.parseString(
                "{\"attributes\":{\"PUNCH_DAMAGE\":2,\"FALL_RESISTANCE\":[1,4]}}").getAsJsonObject());
        if (set.size() != 2 || set.get(0).min() != 0 || set.get(0).max() != 2
                || set.level(0xF0, set.get(1)) != 3)
        { helper.fail("Scalar ranges or malformed stored levels were not handled safely"); return; }
        try
        {
            com.fiskmods.heroes.common.hero.augment.AugmentSet.parse(com.google.gson.JsonParser.parseString(
                    "{\"attributes\":{\"PUNCH_DAMAGE\":[4,8]}}").getAsJsonObject());
            helper.fail("Unsupported upgrade range should be rejected");
        }
        catch (IllegalArgumentException expected) { }
        try
        {
            com.fiskmods.heroes.common.hero.augment.AugmentSet.parse(com.google.gson.JsonParser.parseString(
                    "{\"attributes\":{\"PUNCH_DAMAGE\":3,\"punch_damage\":4}}").getAsJsonObject());
            helper.fail("A duplicate attribute must not create conflicting packed indices");
        }
        catch (IllegalArgumentException expected) { helper.succeed(); }
    }

    @GameTest(template = "test_empty")
    public static void miniatureArmorRoundTrip(GameTestHelper helper)
    {
        Hero hero = FabricatorRecipes.getCraftableHeroes().stream().findFirst().orElse(null);
        if (hero == null)
        {
            helper.fail("No hero loaded");
            return;
        }
        ItemStack empty = new ItemStack(ModItems.MINI_SUIT.get());
        if (ItemFlashRing.getContainedArmor(empty) != null)
        {
            helper.fail("An empty miniature must not contain armor");
            return;
        }
        ItemStack filled = ItemFlashRing.create(ModItems.MINI_SUIT.get(), hero.getDefaultIteration());
        ItemStack restored = ItemStack.of(filled.save(new net.minecraft.nbt.CompoundTag()));
        if (ItemFlashRing.getContainedHero(restored) == null
                || ItemFlashRing.getContainedHero(restored).getHero() != hero)
        {
            helper.fail("Miniature suit NBT must survive serialization");
        }
        else helper.succeed();
    }
}
