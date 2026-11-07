package com.fiskmods.heroes.common.hero.modifier;
import java.util.ArrayList;
import java.util.List;
import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.data.var.DataVar;
import com.fiskmods.heroes.common.data.var.Vars;
import com.fiskmods.heroes.common.hero.power.Modifier;
import com.fiskmods.heroes.common.hero.power.ModifierEntry;
import com.fiskmods.heroes.common.hero.power.PowerProperty;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public final class AbilityData
{
    /** Resolves a data variable referenced by a power JSON entry such as {@code "toggleData"}. */
    static DataVar<Boolean> toggle(JsonObject json)
    {
        return resolve(json, "toggleData");
    }

    static DataVar<Float> timer(JsonObject json)
    {
        return resolve(json, "timerData");
    }

    static DataVar<Float> cooldown(JsonObject json)
    {
        return resolve(json, "cooldownData");
    }

    @SuppressWarnings("unchecked")
    private static <T> DataVar<T> resolve(JsonObject json, String key)
    {
        if (json == null || !json.has(key))
        {
            return null;
        }

        return (DataVar<T>) com.fiskmods.heroes.common.data.DataRegistry.INSTANCE.get(json.get(key).getAsString());
    }

    static JsonObject object(JsonElement element)
    {
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    public static void playSound(LivingEntity entity, ModifierEntry entry, String trigger)
    {
        JsonElement sounds = entry.get(PowerProperty.SOUND_EVENTS);

        if (sounds == null || !sounds.isJsonObject())
        {
            return;
        }

        JsonElement sound = sounds.getAsJsonObject().get(trigger);

        if (sound == null)
        {
            return;
        }

        // Arrays are layered sounds (often an enable cue plus one or more loops), not alternatives.
        if (sound.isJsonArray())
        {
            for (JsonElement element : sound.getAsJsonArray()) playSoundElement(entity, element);
        }
        else playSoundElement(entity, sound);
    }

    private static void playSoundElement(LivingEntity entity, JsonElement sound)
    {
        if (!sound.isJsonPrimitive() || !sound.getAsJsonPrimitive().isString()) return;
        ResourceLocation id = ResourceLocation.tryParse(sound.getAsString());
        if (id != null) com.fiskmods.heroes.common.sound.SHSounds.play(entity, id, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    /** Stop any looping cues for a trigger when its state or suit goes away. */
    public static void stopSoundLoops(LivingEntity entity, ModifierEntry entry, String trigger)
    {
        JsonElement sounds = entry.get(PowerProperty.SOUND_EVENTS);
        if (sounds == null || !sounds.isJsonObject()) return;
        JsonElement sound = sounds.getAsJsonObject().get(trigger);
        if (sound == null) return;
        if (sound.isJsonArray())
            for (JsonElement element : sound.getAsJsonArray()) stopLoopElement(entity, element);
        else stopLoopElement(entity, sound);
    }

    private static void stopLoopElement(LivingEntity entity, JsonElement sound)
    {
        if (!sound.isJsonPrimitive() || !sound.getAsJsonPrimitive().isString()) return;
        ResourceLocation id = ResourceLocation.tryParse(sound.getAsString());
        var definition = id == null ? null : com.fiskmods.heroes.common.sound.SHSounds.getDefinition(id);
        if (definition != null && definition.isLoop()) com.fiskmods.heroes.common.sound.SHSounds.stopLoop(entity, id);
    }
}

/** Transformation abilities (nanites, steel, shapeshifting): a timed toggle with cooldown. */
