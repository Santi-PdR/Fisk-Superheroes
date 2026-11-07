package com.fiskmods.heroes.common.sound;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/** Converts the original 1.7 sound archive's event table to modern resource-pack syntax. */
public final class SoundManifest
{
    private SoundManifest() {}

    public static JsonObject normalize(JsonObject original)
    {
        JsonObject result = original.deepCopy();
        for (var event : result.entrySet())
        {
            if (!event.getValue().isJsonObject()) continue;
            JsonObject definition = event.getValue().getAsJsonObject();
            if (!definition.has("sounds") || !definition.get("sounds").isJsonArray()) continue;
            JsonArray oldSounds = definition.getAsJsonArray("sounds");
            JsonArray sounds = new JsonArray();
            for (JsonElement element : oldSounds)
            {
                JsonObject sound;
                if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString())
                {
                    sound = new JsonObject();
                    sound.add("name", new JsonPrimitive(element.getAsString()));
                }
                else if (element.isJsonObject()) sound = element.getAsJsonObject().deepCopy();
                else continue;
                if (!sound.has("name") || !sound.get("name").isJsonPrimitive()) continue;
                String name = sound.get("name").getAsString();
                if (name.startsWith("minecraft:"))
                {
                    String replacement = modernVanillaEvent(name.substring("minecraft:".length()));
                    if (replacement == null) continue;
                    sound.addProperty("name", "minecraft:" + replacement);
                    sound.addProperty("type", "event");
                }
                else if (name.indexOf(':') < 0) sound.addProperty("name", "fiskheroes:" + name);
                sounds.add(sound);
            }
            definition.add("sounds", sounds);
        }
        return result;
    }

    private static String modernVanillaEvent(String path)
    {
        if (path.equals("mob/ghast/fireball4")) return "entity.ghast.shoot";
        if (path.equals("mob/sheep/shear")) return "entity.sheep.shear";
        if (path.startsWith("mob/irongolem/walk")) return "entity.iron_golem.step";
        if (path.startsWith("mob/zombie/metal")) return "entity.zombie.attack_iron_door";
        if (path.startsWith("random/explode")) return "entity.generic.explode";
        if (path.equals("random/fizz")) return "block.fire.extinguish";
        if (path.equals("random/fuse")) return "entity.tnt.primed";
        if (path.startsWith("random/glass")) return "block.glass.break";
        return null;
    }
}
