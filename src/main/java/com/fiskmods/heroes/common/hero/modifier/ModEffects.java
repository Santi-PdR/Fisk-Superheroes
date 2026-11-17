package com.fiskmods.heroes.common.hero.modifier;

import com.fiskmods.heroes.FiskHeroes;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Custom effect identifiers used by the original hero scripts as transient status flags. */
public final class ModEffects
{
    public static final DeferredRegister<MobEffect> REGISTRY =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, FiskHeroes.MODID);

    /** The original custom status effect's active-state signal, read by Shazam's pack scripts. */
    public static final RegistryObject<MobEffect> ETERNIUM = REGISTRY.register("eternium",
            () -> new MobEffect(MobEffectCategory.HARMFUL, 0x49D7D0) { });
    /** Temporary original-pack status flags carried by phantom and tutridium arrows. */
    public static final RegistryObject<MobEffect> PHASE_SUPPRESSANT = REGISTRY.register("disable_phasing",
            () -> new MobEffect(MobEffectCategory.HARMFUL, 0xA9A9B8) { });
    public static final RegistryObject<MobEffect> TUTRIDIUM = REGISTRY.register("tutridium",
            () -> new MobEffect(MobEffectCategory.HARMFUL, 0x7B4BA8) { });

    public static final RegistryObject<MobEffect> VELOCITY_NINE = REGISTRY.register("velocity_nine",
            () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0x610000) {
                @Override public String getDescriptionId() { return "statusEffect.fiskheroes.velocity_nine"; }
            });
    public static final RegistryObject<MobEffect> SPEED_SICKNESS = REGISTRY.register("speed_sickness",
            () -> new MobEffect(MobEffectCategory.HARMFUL, 0x66575D) {
                @Override public String getDescriptionId() { return "statusEffect.fiskheroes.speed_sickness"; }
            });

    private ModEffects()
    {
    }
}
