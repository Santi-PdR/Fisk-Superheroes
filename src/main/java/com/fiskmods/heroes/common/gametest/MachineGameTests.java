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
