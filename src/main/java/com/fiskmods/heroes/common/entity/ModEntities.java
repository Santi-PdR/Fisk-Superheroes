package com.fiskmods.heroes.common.entity;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.entity.arrow.TrickArrowEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities
{
    public static final DeferredRegister<EntityType<?>> REGISTRY = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, FiskHeroes.MODID);
    public static final RegistryObject<EntityType<TrickArrowEntity>> TRICK_ARROW = REGISTRY.register("trick_arrow",
            () -> EntityType.Builder.<TrickArrowEntity>of(TrickArrowEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(64).updateInterval(20).build("fiskheroes:trick_arrow"));

    private ModEntities()
    {
    }
}
