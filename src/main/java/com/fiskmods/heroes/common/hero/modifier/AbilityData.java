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

        // Sound entries may be a single id or a list of variants; one is picked at random, as the
        // original dispatcher did.
        String id;

        if (sound.isJsonArray())
        {
            var list = sound.getAsJsonArray();

            if (list.isEmpty())
            {
                return;
            }

            id = list.get(entity.getRandom().nextInt(list.size())).getAsString();
        }
        else
        {
            id = sound.getAsString();
        }

        ResourceLocation location = ResourceLocation.tryParse(id);

        if (location != null)
        {
            com.fiskmods.heroes.common.sound.SHSounds.play(entity, location, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }
}

/** Transformation abilities (nanites, steel, shapeshifting): a timed toggle with cooldown. */
