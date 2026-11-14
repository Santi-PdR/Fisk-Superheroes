package com.fiskmods.heroes.common.hero.modifier;

import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.hero.power.Modifier;
import com.fiskmods.heroes.common.hero.power.ModifierEntry;
import com.fiskmods.heroes.common.hero.power.PowerProperty;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.phys.AABB;

/** The original Martian fire weakness also inflicts effects near fire, lava or burning entities. */
public final class ModifierFireWeakness extends Modifier
{
    public ModifierFireWeakness(ResourceLocation id)
    {
        super(id);
        addProperty(PowerProperty.RADIUS, 2.5F);
        addProperty(PowerProperty.DURATION, 100);
    }

    @Override
    public void tick(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        if (entity.level().isClientSide) return;
        int duration = entry.getInt(entity, PowerProperty.DURATION);
        if (duration <= 0) return;
        // Checking the area five times a second avoids hundreds of block lookups per
        // player per tick, while a burning wearer is handled immediately every tick.
        boolean exposed = entity.isOnFire();
        if (!exposed && entity.tickCount % 5 == 0)
        {
            float radius = Mth.clamp(entry.getFloat(entity, PowerProperty.RADIUS), 0.0F, 16.0F);
            exposed = radius > 0.0F && isExposed(entity, radius);
        }
        if (!exposed) return;
        if (!entity.hasEffect(MobEffects.CONFUSION)) AbilityData.playSound(entity, entry, "INFLICT");
        entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, duration, 0));
        entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 3));
        entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 2));
    }

    private static boolean isExposed(LivingEntity entity, float radius)
    {
        AABB box = entity.getBoundingBox().inflate(radius);
        for (BlockPos pos : BlockPos.betweenClosed(Mth.floor(box.minX), Mth.floor(box.minY), Mth.floor(box.minZ),
                Mth.floor(box.maxX), Mth.floor(box.maxY), Mth.floor(box.maxZ)))
        {
            // Do not load chunks merely to inspect a neighboring fire.
            if (!entity.level().hasChunkAt(pos)) continue;
            var state = entity.level().getBlockState(pos);
            if (state.is(BlockTags.FIRE) || state.getFluidState().is(FluidTags.LAVA)) return true;
        }
        return !entity.level().getEntities(entity, box,
                target -> target.isAlive() && !target.isSpectator() && !(target instanceof HangingEntity) && target.isOnFire()).isEmpty();
    }
}
