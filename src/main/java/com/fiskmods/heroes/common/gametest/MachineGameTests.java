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
