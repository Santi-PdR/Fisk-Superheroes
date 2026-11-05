package com.fiskmods.heroes.common.block;

import com.fiskmods.heroes.FiskHeroes;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** BlockEntityType registry for Fisk's Superheroes progression machines. */
public final class ModBlockEntities
{
    public static final DeferredRegister<BlockEntityType<?>> REGISTRY =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, FiskHeroes.MODID);

    public static final RegistryObject<BlockEntityType<AlloySmelteryBlockEntity>> ALLOY_SMELTERY =
            REGISTRY.register("alloy_smeltery", () -> BlockEntityType.Builder.of(
                    AlloySmelteryBlockEntity::new,
                    ModBlocks.ALLOY_SMELTERY.get()).build(null));

    public static final RegistryObject<BlockEntityType<SuitFabricatorBlockEntity>> SUIT_FABRICATOR =
            REGISTRY.register("suit_fabricator", () -> BlockEntityType.Builder.of(
                    SuitFabricatorBlockEntity::new,
                    ModBlocks.SUIT_FABRICATOR.get(),
                    ModBlocks.COSMIC_SUIT_FABRICATOR.get()).build(null));

    private ModBlockEntities()
    {
    }
}
