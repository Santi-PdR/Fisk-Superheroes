package com.fiskmods.heroes.common.recipe;

import com.fiskmods.heroes.FiskHeroes;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModRecipes
{
    public static final DeferredRegister<RecipeSerializer<?>> REGISTRY = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, FiskHeroes.MODID);
    public static final RegistryObject<RecipeSerializer<AutomaticQuiverRecipe>> AUTOMATIC_QUIVER = REGISTRY.register("automatic_quiver",
            () -> new SimpleCraftingRecipeSerializer<>(AutomaticQuiverRecipe::new));
    private ModRecipes() {}
}
