package com.fiskmods.heroes.common.entity.arrow;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;

/** The coremod redirects only vanilla's two arrow-source calls through this method. */
public final class ArrowDamageSources
{
    public static final net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType> BLAZE_ARROW =
            net.minecraft.resources.ResourceKey.create(Registries.DAMAGE_TYPE, com.fiskmods.heroes.FiskHeroes.id("blaze_arrow"));

    private ArrowDamageSources() { }

    public static DamageSource directHit(DamageSources vanilla, AbstractArrow arrow, Entity vanillaOwner)
    {
        if (arrow instanceof TrickArrowEntity trick)
        {
            if ("fireball".equals(trick.getArrowType())) return fireball(arrow);
            if ("blaze".equals(trick.getArrowType()))
            {
                var type = arrow.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(BLAZE_ARROW);
                return new DamageSource(type, arrow, vanillaOwner);
            }
        }
        return vanilla.arrow(arrow, vanillaOwner);
    }

    public static DamageSource fireball(AbstractArrow arrow)
    {
        var type = arrow.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(DamageTypes.FIREBALL);
        return new DamageSource(type, arrow, arrow.getOwner());
    }
}
