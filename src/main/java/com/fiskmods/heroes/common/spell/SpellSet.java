package com.fiskmods.heroes.common.spell;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

/** Parsed spell list belonging to a hero's spellcasting modifier. */
public final class SpellSet
{
    public static final SpellSet EMPTY = new SpellSet(List.of());

    private final List<SpellDefinition> spells;

    private SpellSet(List<SpellDefinition> spells)
    {
        this.spells = List.copyOf(spells);
    }

    public List<SpellDefinition> getSpells()
    {
        return Collections.unmodifiableList(spells);
    }

    public SpellDefinition get(int index)
    {
        return index >= 0 && index < spells.size() ? spells.get(index) : null;
    }

    public int size()
    {
        return spells.size();
    }

    public JsonObject toJson()
    {
        JsonObject json = new JsonObject();
        for (SpellDefinition spell : spells)
        {
            json.add(spell.id().toString(), spell.properties().deepCopy());
        }
        return json;
    }

    /** Parses the original power JSON shape: {@code {"fiskheroes:spell": {...}}}. */
    public static SpellSet parse(JsonElement json)
    {
        if (json == null || !json.isJsonObject())
        {
            return EMPTY;
        }

        List<SpellDefinition> parsed = new ArrayList<>();
        JsonObject object = json.getAsJsonObject();

        for (Map.Entry<String, JsonElement> entry : object.entrySet())
        {
            ResourceLocation id = ResourceLocation.tryParse(entry.getKey());
            if (id == null || !entry.getValue().isJsonObject())
            {
                com.fiskmods.heroes.FiskHeroes.LOGGER.warn("Ignoring invalid spell definition '{}'", entry.getKey());
                continue;
            }

            JsonObject definition = entry.getValue().getAsJsonObject();
            String sequence = definition.has("sequence") && definition.get("sequence").isJsonPrimitive()
                    ? definition.get("sequence").getAsString() : "";
            int cooldown = definition.has("cooldown") && definition.get("cooldown").isJsonPrimitive()
                    ? definition.get("cooldown").getAsInt() : 0;
            parsed.add(new SpellDefinition(id, sequence, cooldown, definition));
        }

        return parsed.isEmpty() ? EMPTY : new SpellSet(parsed);
    }
}
