package com.fiskmods.heroes.common.spell;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

/** Data for one spell supplied by a hero power. */
public record SpellDefinition(ResourceLocation id, String sequence, int cooldown, JsonObject properties)
{
    public SpellDefinition
    {
        String input = sequence == null ? "" : sequence.toLowerCase(java.util.Locale.ROOT);
        sequence = input.chars().filter(c -> c == 'w' || c == 'a' || c == 's' || c == 'd')
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();
        cooldown = Math.max(0, cooldown);
        properties = properties == null ? new JsonObject() : properties.deepCopy();
    }

    public String getNameKey()
    {
        return "spell." + id.getNamespace() + "." + id.getPath() + ".name";
    }

    /** Icon order preserved from SpellType registration in the 1.7.10 reference. */
    public int getIconIndex()
    {
        return switch (id.getPath())
        {
            case "earth_swallowing" -> 0;
            case "atmospheric" -> 1;
            case "duplication" -> 2;
            case "whip" -> 3;
            case "blindness" -> 4;
            case "drones" -> 5;
            default -> -1;
        };
    }
}
