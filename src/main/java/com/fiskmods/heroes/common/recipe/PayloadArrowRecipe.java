package com.fiskmods.heroes.common.recipe;

import com.fiskmods.heroes.common.item.ItemTrickArrow;
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

/** Attaches the actual potion or rocket, including all its custom data, to a normal arrow. */
public final class PayloadArrowRecipe extends CustomRecipe
{
    private final boolean vial;

    public PayloadArrowRecipe(ResourceLocation id, CraftingBookCategory category, boolean vial)
    {
        super(id, category);
        this.vial = vial;
    }

    private boolean isPayload(ItemStack stack)
    {
        return vial ? stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION) : stack.is(Items.FIREWORK_ROCKET);
    }

    @Override public boolean matches(CraftingContainer grid, Level level)
    {
        boolean arrow = false, payload = false;
        for (int i = 0; i < grid.getContainerSize(); ++i)
        {
            ItemStack stack = grid.getItem(i);
            if (stack.isEmpty()) continue;
            if (!arrow && stack.is(ModItems.TRICK_ARROW.get()) && ItemTrickArrow.NORMAL.equals(ItemTrickArrow.getType(stack))) arrow = true;
            else if (!payload && isPayload(stack)) payload = true;
            else return false;
        }
        return arrow && payload;
    }

    @Override public ItemStack assemble(CraftingContainer grid, RegistryAccess registry)
    {
        for (int i = 0; i < grid.getContainerSize(); ++i)
        {
            ItemStack stack = grid.getItem(i);
            if (!isPayload(stack)) continue;
            ItemStack payload = stack.copy();
            payload.setCount(1);
            ItemStack result = ItemTrickArrow.createStack(vial ? "vial" : "firework");
            if (vial)
            {
                // The original accepts a drinkable potion and converts it to a splash payload.
                if (payload.is(Items.POTION))
                {
                    ItemStack splash = new ItemStack(Items.SPLASH_POTION);
                    if (payload.hasTag()) splash.setTag(payload.getTag().copy());
                    payload = splash;
                }
                ItemTrickArrow.setAttachedPotion(result, payload);
            }
            else ItemTrickArrow.setAttachedItem(result, payload);
            return result;
        }
        return ItemStack.EMPTY;
    }

    @Override public boolean canCraftInDimensions(int width, int height) { return width * height >= 2; }
    @Override public RecipeSerializer<?> getSerializer() { return vial ? ModRecipes.VIAL_ARROW.get() : ModRecipes.FIREWORK_ARROW.get(); }
}
