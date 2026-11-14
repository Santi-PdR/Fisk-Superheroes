package com.fiskmods.heroes.common.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Random;
import java.util.regex.Pattern;

/** One particle's sampled cloud colors, retaining shared random channels and both endpoints. */
public final class CloudColors
{
    private static final Pattern EXPRESSION = Pattern.compile("(red|green|blue)\\s*([+*/-])\\s*([0-9.]+)");
    private final float[] start;
    private final float[] end;

    private CloudColors(float[] start, float[] end) { this.start = start; this.end = end; }

    public static CloudColors sample(JsonObject cloud, Random random)
    {
        float[] randoms = {random.nextFloat(), random.nextFloat(), random.nextFloat()};
        JsonObject colors = cloud.has("color") && cloud.get("color").isJsonObject()
                ? cloud.getAsJsonObject("color") : new JsonObject();
        float[] start = new float[3], end = new float[3];
        if (colors.has("start") && colors.has("end") && colors.get("start").isJsonPrimitive()
                && colors.get("end").isJsonPrimitive())
        {
            Integer a = CloudColorMath.hexColor(colors.get("start").getAsString());
            Integer b = CloudColorMath.hexColor(colors.get("end").getAsString());
            if (a != null && b != null)
            {
                for (int i = 0; i < 3; ++i)
                {
                    start[i] = ((a >> (16 - i * 8)) & 255) / 255.0F;
                    end[i] = ((b >> (16 - i * 8)) & 255) / 255.0F;
                }
                return new CloudColors(start, end);
            }
        }
        JsonElement[] specs = new JsonElement[3];
        int[] links = {0, 1, 2};
        for (var entry : colors.entrySet())
        {
            String[] names = entry.getKey().split(",");
            int first = 3;
            for (String name : names)
            {
                int i = channel(name.trim());
                if (i >= 0) first = Math.min(first, i);
            }
            if (first == 3) continue;
            for (String name : names)
            {
                int i = channel(name.trim());
                if (i >= 0) { specs[i] = entry.getValue(); links[i] = first; }
            }
        }
        for (int i = 0; i < 3; ++i)
        {
            start[i] = value(i, false, specs, links, randoms, 0);
            end[i] = value(i, true, specs, links, randoms, 0);
        }
        return new CloudColors(start, end);
    }

    private static float value(int index, boolean end, JsonElement[] specs, int[] links, float[] randoms, int depth)
    {
        if (depth >= 3) return 0.5F; // A malformed pack can contain cyclic channel references.
        JsonElement spec = specs[index];
        if (spec == null || spec.isJsonNull()) return 0.5F;
        if (spec.isJsonPrimitive() && spec.getAsJsonPrimitive().isNumber()) return spec.getAsFloat();
        if (spec.isJsonPrimitive() && spec.getAsJsonPrimitive().isString())
        {
            var matcher = EXPRESSION.matcher(spec.getAsString());
            if (!matcher.matches()) return 0.5F;
            int source = channel(matcher.group(1));
            float base = source < 0 ? 0.5F : value(source, end, specs, links, randoms, depth + 1);
            float operand = Float.parseFloat(matcher.group(3));
            return switch (matcher.group(2))
            {
                case "+" -> base + operand;
                case "-" -> base - operand;
                case "*" -> base * operand;
                case "/" -> operand == 0 ? base : base / operand;
                default -> base;
            };
        }
        if (!spec.isJsonObject()) return 0.5F;
        JsonObject object = spec.getAsJsonObject();
        if (object.has("start") && object.has("end")) spec = object.get(end ? "end" : "start");
        if (spec.isJsonPrimitive() && spec.getAsJsonPrimitive().isNumber()) return spec.getAsFloat();
        if (!spec.isJsonObject()) return 0.5F;
        object = spec.getAsJsonObject();
        float base = object.has("base") ? object.get("base").getAsFloat() : 0.0F;
        float range = object.has("rand") ? object.get("rand").getAsFloat() : 0.0F;
        return base + range * randoms[links[index]];
    }

    private static int channel(String name)
    {
        return switch (name) { case "red" -> 0; case "green" -> 1; case "blue" -> 2; default -> -1; };
    }

    /** Original curveCrests(age/lifetime * 2) is sin(pi * age/lifetime). */
    public float channel(int index, float progress)
    {
        return atFactor(index, crest(progress));
    }

    public static float crest(float progress)
    {
        return (float) Math.sin(Math.PI * Math.max(0.0F, Math.min(1.0F, progress)));
    }

    public float atFactor(int index, float factor)
    {
        return Math.max(0.0F, Math.min(1.0F, start[index] + (end[index] - start[index]) * factor));
    }
}
