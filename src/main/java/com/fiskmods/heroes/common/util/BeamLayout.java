package com.fiskmods.heroes.common.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.Vec3;

/** Immutable beam constellation, including mirrored pack-defined anchors and view offsets. */
public final class BeamLayout
{
    public record Beam(String anchor, Vec3 offset, Vec3 firstPerson, float width, float height) { }
    private final List<Beam> beams;
    private BeamLayout(List<Beam> beams) { this.beams = List.copyOf(beams); }
    public List<Beam> beams() { return beams; }

    public static BeamLayout parse(JsonObject effect, boolean heatFallback)
    {
        List<Beam> beams = new ArrayList<>();
        String anchor = effect != null && effect.has("anchor") ? effect.get("anchor").getAsString() : "head";
        JsonArray items = effect != null && effect.has("beams") && effect.get("beams").isJsonArray()
                ? effect.getAsJsonArray("beams") : null;
        if (items != null)
        {
            for (JsonElement item : items)
            {
                if (!item.isJsonObject()) continue;
                JsonObject object = item.getAsJsonObject();
                String part = object.has("anchor") ? object.get("anchor").getAsString() : anchor;
                Vec3 offset = vector(object.get("offset"), Vec3.ZERO);
                Vec3 firstPerson = vector(object.get("firstPerson"), offset);
                JsonArray size = object.has("size") && object.get("size").isJsonArray() ? object.getAsJsonArray("size") : null;
                float width = number(size, 0, 1.76F), height = number(size, 1, width);
                beams.add(new Beam(part, offset, firstPerson, width, height));
            }
        }
        if (beams.isEmpty())
        {
            if (heatFallback)
            {
                beams.add(new Beam("head", new Vec3(2.2, -3.3, -4), new Vec3(2.2, 0, 2), 1, .5F));
                beams.add(new Beam("head", new Vec3(-2.2, -3.3, -4), new Vec3(-2.2, 0, 2), 1, .5F));
            }
            else beams.add(new Beam(anchor, Vec3.ZERO, Vec3.ZERO, 1.76F, 1.0F));
        }
        else if (effect != null && effect.has("mirror") && effect.get("mirror").getAsBoolean())
        {
            int count = beams.size();
            for (int i = 0; i < count; ++i)
            {
                Beam source = beams.get(i);
                beams.add(new Beam(opposite(source.anchor()), flip(source.offset()), flip(source.firstPerson()), source.width(), source.height()));
            }
        }
        return new BeamLayout(beams);
    }

    private static Vec3 flip(Vec3 vector) { return new Vec3(-vector.x, vector.y, vector.z); }
    private static String opposite(String anchor)
    {
        return switch (anchor.toLowerCase(java.util.Locale.ROOT))
        {
            case "rightarm" -> "leftArm";
            case "leftarm" -> "rightArm";
            case "rightleg" -> "leftLeg";
            case "leftleg" -> "rightLeg";
            default -> anchor;
        };
    }
    private static Vec3 vector(JsonElement element, Vec3 fallback)
    {
        if (element == null || !element.isJsonArray() || element.getAsJsonArray().size() < 3) return fallback;
        JsonArray values = element.getAsJsonArray();
        return new Vec3(number(values, 0, 0), number(values, 1, 0), number(values, 2, 0));
    }
    private static float number(JsonArray array, int index, float fallback)
    {
        try { return array != null && index < array.size() ? array.get(index).getAsFloat() : fallback; }
        catch (RuntimeException ignored) { return fallback; }
    }
}
