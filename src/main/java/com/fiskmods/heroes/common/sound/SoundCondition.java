package com.fiskmods.heroes.common.sound;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.world.entity.Entity;

/**
 * The {@code condition} of a sound definition.
 * <p>
 * It is written either as a single value or as an object with two of them:
 * <pre>
 * "condition": "fiskheroes:shield_blocking"
 * "condition": { "start": "entity.isSprinting()", "continue": "... &amp;&amp; entity.getData('fiskheroes:speeding')" }
 * </pre>
 * {@code start} decides whether the sound may begin, {@code continue} whether it keeps playing.
 */
public class SoundCondition
{
    public static final SoundCondition ALWAYS = new SoundCondition(SoundValue.unset(), SoundValue.unset());

    private final SoundValue start;
    private final SoundValue resume;

    public SoundCondition(SoundValue start, SoundValue resume)
    {
        this.start = start;
        this.resume = resume;
    }

    public static SoundCondition read(JsonElement json)
    {
        if (json == null || json.isJsonNull())
        {
            return ALWAYS;
        }

        if (json.isJsonObject())
        {
            JsonObject object = json.getAsJsonObject();
            return new SoundCondition(SoundValue.parse(object.get("start")), SoundValue.parse(object.get("continue")));
        }

        SoundValue value = SoundValue.parse(json);
        return value.isSet() ? new SoundCondition(value, value) : ALWAYS;
    }

    public boolean shouldStart(Entity entity)
    {
        return !start.isSet() || start.getBoolean(entity, true);
    }

    public boolean shouldContinue(Entity entity)
    {
        return !resume.isSet() || resume.getBoolean(entity, true);
    }
}
