package com.fiskmods.heroes.common.hero.modifier;

import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.entity.projectile.CactusSpikeEntity;
import com.fiskmods.heroes.common.hero.power.Modifier;
import com.fiskmods.heroes.common.hero.power.ModifierEntry;
import com.fiskmods.heroes.common.hero.power.PowerProperty;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** Señor Cactus's radial spike key and separate aimed burst. */
public final class ModifierSpikeBurst extends Modifier
{
    public ModifierSpikeBurst(ResourceLocation id) { super(id); }

    public static boolean canShoot(Player player)
    {
        return player.getAbilities().instabuild || player.getFoodData().getFoodLevel() >= 12;
    }

    /** The SHOOT_SPIKES key emits 72 projectiles in a full circle, not a single hitscan. */
    @Override
    public void onActivate(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        if (!(entity instanceof Player player) || !(player.level() instanceof ServerLevel level) || !canShoot(player)) return;
        JsonElement profile = entry.get(player, PowerProperty.DAMAGE_PROFILE);
        for (int i = 0; i < 72; ++i)
        {
            CactusSpikeEntity spike = new CactusSpikeEntity(player);
            spike.setDamageProfile(profile);
            spike.shootFromRotation(player, player.getXRot(), i * 5.0F, 0.0F, 4.5F, 0.0F);
            level.addFreshEntity(spike);
        }
        if (!player.getAbilities().instabuild) player.causeFoodExhaustion(6.0F);
        level.playSound(null, player.blockPosition(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    /** Right click while aiming fires an eleven-spike spread in the look direction. */
    public static boolean fireAimed(Player player, ModifierEntry entry)
    {
        if (!(player.level() instanceof ServerLevel level) || !canShoot(player)
                || player.isShiftKeyDown() || !player.getMainHandItem().isEmpty()) return false;
        JsonElement profile = entry.get(player, PowerProperty.DAMAGE_PROFILE);
        for (int i = 0; i < 11; ++i)
        {
            CactusSpikeEntity spike = new CactusSpikeEntity(player);
            spike.setDamageProfile(profile);
            spike.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 4.5F, 0.0F);
            if (i != 0)
                spike.setDeltaMovement(spike.getDeltaMovement().add(
                        (level.random.nextDouble() - .5D) * .9D,
                        (level.random.nextDouble() - .5D) * .9D,
                        (level.random.nextDouble() - .5D) * .9D));
            level.addFreshEntity(spike);
        }
        if (!player.getAbilities().instabuild) player.causeFoodExhaustion(1.0F);
        AbilityData.playSound(player, entry, "SHOOT");
        return true;
    }
}
