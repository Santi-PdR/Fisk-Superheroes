package com.fiskmods.heroes.common.recipe;

import com.fiskmods.heroes.common.item.ItemQuiver;
import com.fiskmods.heroes.common.item.ModItems;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/** Quiver plus hopper, preserving the quiver's contents and enchantments. */
public final class AutomaticQuiverRecipe extends CustomRecipe
{
    public AutomaticQuiverRecipe(ResourceLocation id, CraftingBookCategory category) { super(id, category); }

    @Override public boolean matches(CraftingContainer inventory, Level level)
    {
        boolean quiver = false, hopper = false;
        for (int i = 0; i < inventory.getContainerSize(); ++i)
        {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.is(ModItems.QUIVER.get()) && !ItemQuiver.isAutomatic(stack) && !quiver) quiver = true;
            else if (stack.is(Items.HOPPER) && !hopper) hopper = true;
            else return false;
        }
        return quiver && hopper;
    }

    @Override public ItemStack assemble(CraftingContainer inventory, RegistryAccess registry)
    {
        for (int i = 0; i < inventory.getContainerSize(); ++i)
            if (inventory.getItem(i).is(ModItems.QUIVER.get())) return ItemQuiver.makeAutomatic(inventory.getItem(i));
        return ItemStack.EMPTY;
    }

    @Override public boolean canCraftInDimensions(int width, int height) { return width * height >= 2; }
    @Override public RecipeSerializer<?> getSerializer() { return ModRecipes.AUTOMATIC_QUIVER.get(); }
}
