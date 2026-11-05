package com.fiskmods.heroes.common.block;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

import com.fiskmods.heroes.common.item.ModItems;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Alloy Smeltery recipe registry ported directly from 1.7.10 AlloyRecipes & SHRecipes.addMaterialRecipes().
 */
public final class AlloyRecipes
{
    public record Entry(Item inputA, int countA, Item inputB, int countB, Item result, int resultCount)
    {
        public boolean matches(ItemStack first, ItemStack second)
        {
            return (matchesOrdered(first, second) || matchesOrdered(second, first));
        }

        public boolean matchesOrdered(ItemStack first, ItemStack second)
        {
            return !first.isEmpty() && !second.isEmpty()
                    && first.is(inputA) && first.getCount() >= countA
                    && second.is(inputB) && second.getCount() >= countB;
        }

        public void consume(ItemStack first, ItemStack second)
        {
            if (matchesOrdered(first, second))
            {
                first.shrink(countA);
                second.shrink(countB);
            }
            else if (matchesOrdered(second, first))
            {
                second.shrink(countA);
                first.shrink(countB);
            }
        }

        public ItemStack assemble()
        {
            return new ItemStack(result, resultCount);
        }
    }

    private static List<Entry> entries;

    private AlloyRecipes()
    {
    }

    public static List<Entry> getAll()
    {
        if (entries == null)
        {
            entries = List.of(
                    new Entry(Items.GOLD_INGOT, 1, ModItems.TITANIUM_INGOT.get(), 1, ModItems.GOLD_TITANIUM_INGOT.get(), 1),
                    new Entry(ModItems.RAW_VIBRANIUM.get(), 1, Items.IRON_INGOT, 1, ModItems.VIBRANIUM_INGOT.get(), 1),
                    new Entry(ModItems.RAW_DWARF_STAR.get(), 2, Items.GOLD_INGOT, 1, ModItems.DWARF_STAR_ALLOY.get(), 2),
                    new Entry(ModItems.RAW_TUTRITE.get(), 2, Items.EMERALD, 1, ModItems.CRYSTALLINE_TUTRITE.get(), 1),
                    new Entry(ModItems.RAW_TUTRITE.get(), 2, ModItems.OLIVINE.get(), 1, ModItems.CRYSTALLINE_TUTRITE.get(), 1),
                    new Entry(ModItems.OLIVINE_DUST.get(), 4, Items.QUARTZ, 1, ModItems.OLIVINE.get(), 1)
            );
        }
        return entries;
    }

    @Nullable
    public static Entry find(ItemStack first, ItemStack second)
    {
        if (first.isEmpty() || second.isEmpty()) return null;
        for (Entry entry : getAll())
        {
            if (entry.matches(first, second)) return entry;
        }
        return null;
    }

    public static boolean isValidIngredient(ItemStack stack)
    {
        if (stack.isEmpty()) return false;
        for (Entry e : getAll())
        {
            if (stack.is(e.inputA()) || stack.is(e.inputB())) return true;
        }
        return false;
    }
}
