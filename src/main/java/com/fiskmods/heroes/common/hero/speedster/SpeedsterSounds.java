package com.fiskmods.heroes.common.hero.speedster;

import java.util.List;

import javax.annotation.Nullable;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.data.var.Vars;
import com.fiskmods.heroes.common.hero.modifier.AbilityData;
import com.fiskmods.heroes.common.hero.modifier.ModEffects;
import com.fiskmods.heroes.common.hero.power.ModifierEntry;
import com.fiskmods.heroes.common.sound.SHSounds;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;

/** Speed transitions use the original V9 dispatcher in preference to a suit's sound profile. */
public final class SpeedsterSounds
{
    private SpeedsterSounds() { }

    public static List<ResourceLocation> soundIds(LivingEntity entity, @Nullable ModifierEntry entry, String trigger)
    {
        if (entity.hasEffect(ModEffects.VELOCITY_NINE.get()))
            return switch (trigger)
            {
                // SHResourceHandler.loadV9Sounds in the original jar.
                case "MOVE" -> List.of(FiskHeroes.id("super_speed_move_loop_cw"));
                case "SPRINT" -> List.of(FiskHeroes.id("super_speed_sprint_zoom"));
                case "STOP" -> List.of(FiskHeroes.id("super_speed_stop_zoom"));
                default -> List.of();
            };
        return entry != null ? AbilityData.soundIds(entity, entry, trigger) : List.of();
    }

    public static void tick(LivingEntity entity, @Nullable ModifierEntry entry, SHPlayerData data)
    {
        if (entity.level().isClientSide) return;
        var state = data.getSpeedsterSounds();
        boolean speeding = data.getData().get(Vars.SPEEDING);
        boolean moving = data.getData().get(Vars.MOVING);
        List<ResourceLocation> moveIds = soundIds(entity, entry, "MOVE");
        // Switching profile mid-run must retire the original loop and start the new one.
        if (state.isMoving() && speeding && moving && !state.getMoveIds().equals(moveIds))
        {
            stopMoves(entity, state);
            state.setMoveIds(moveIds);
            play(entity, moveIds);
        }
        state.update(speeding, moving, data.getData().get(Vars.SPEED_SPRINTING), trigger -> {
            if ("STOP".equals(trigger)) stopMoves(entity, state);
            if ("MOVE".equals(trigger)) state.setMoveIds(moveIds);
            play(entity, soundIds(entity, entry, trigger));
        });
    }

    private static void play(LivingEntity entity, List<ResourceLocation> ids)
    {
        for (var id : ids) SHSounds.play(entity, id, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    public static void stopMoves(LivingEntity entity, SpeedsterSoundState state)
    {
        for (var id : state.getMoveIds())
        {
            var definition = SHSounds.getDefinition(id);
            if (definition != null && definition.isLoop()) SHSounds.stopLoop(entity, id);
        }
        state.setMoveIds(List.of());
    }
}
