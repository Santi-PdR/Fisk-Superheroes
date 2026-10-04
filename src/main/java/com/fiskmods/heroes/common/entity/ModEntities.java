package com.fiskmods.heroes.common.entity;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.entity.arrow.TrickArrowEntity;
import com.fiskmods.heroes.common.spell.IllusionDroneEntity;
import com.fiskmods.heroes.common.spell.SpellDuplicateEntity;
import com.fiskmods.heroes.common.spell.EarthCrackEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = FiskHeroes.MODID, bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD)
public final class ModEntities
{
    public static final DeferredRegister<EntityType<?>> REGISTRY = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, FiskHeroes.MODID);
    public static final RegistryObject<EntityType<TrickArrowEntity>> TRICK_ARROW = REGISTRY.register("trick_arrow",
            () -> EntityType.Builder.<TrickArrowEntity>of(TrickArrowEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(64).updateInterval(20).build("fiskheroes:trick_arrow"));
    public static final RegistryObject<EntityType<CactusMinionEntity>> CACTUS_MINION = REGISTRY.register("cactus_minion",
            () -> EntityType.Builder.<CactusMinionEntity>of(CactusMinionEntity::new, MobCategory.CREATURE)
                    .sized(1.0F, 1.0F).clientTrackingRange(80).updateInterval(1).build("fiskheroes:cactus_minion"));
    public static final RegistryObject<EntityType<EarthCrackEntity>> EARTH_CRACK = REGISTRY.register("earth_crack",
            () -> EntityType.Builder.<EarthCrackEntity>of(EarthCrackEntity::new, MobCategory.MISC)
                    .sized(0.1F, 0.1F).clientTrackingRange(64).updateInterval(1).fireImmune()
                    .build("fiskheroes:earth_crack"));
    public static final RegistryObject<EntityType<SpellDuplicateEntity>> SPELL_DUPLICATE = REGISTRY.register("spell_duplicate",
            () -> EntityType.Builder.<SpellDuplicateEntity>of(SpellDuplicateEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F).clientTrackingRange(64).updateInterval(1).fireImmune()
                    .build("fiskheroes:spell_duplicate"));
    public static final RegistryObject<EntityType<IllusionDroneEntity>> ILLUSION_DRONE = REGISTRY.register("illusion_drone",
            () -> EntityType.Builder.<IllusionDroneEntity>of(IllusionDroneEntity::new, MobCategory.MISC)
                    .sized(1.0F, 0.75F).clientTrackingRange(80).updateInterval(1).fireImmune()
                    .build("fiskheroes:illusion_drone"));

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void registerAttributes(net.minecraftforge.event.entity.EntityAttributeCreationEvent event)
    {
        event.put(CACTUS_MINION.get(), CactusMinionEntity.createAttributes().build());
        event.put(SPELL_DUPLICATE.get(), SpellDuplicateEntity.createAttributes().build());
        event.put(ILLUSION_DRONE.get(), IllusionDroneEntity.createAttributes().build());
    }

    private ModEntities()
    {
    }
}
