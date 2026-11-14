package com.fiskmods.heroes.common.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Arrays;

/** Immutable texture-sheet animation, following vanilla's animation metadata defaults. */
public final class AnimatedSpriteFrames
{
    private final int columns, rows;
    private final int[] indices, ends;
    private final int duration;

    private AnimatedSpriteFrames(int columns, int rows, int[] indices, int[] ends, int duration)
    {
        this.columns = columns; this.rows = rows; this.indices = indices; this.ends = ends; this.duration = duration;
    }

    public static AnimatedSpriteFrames single()
    {
        return new AnimatedSpriteFrames(1, 1, new int[] { 0 }, new int[] { 1 }, 1);
    }

    public static AnimatedSpriteFrames parse(JsonObject animation, int width, int height)
    {
        if (animation == null || width <= 0 || height <= 0) return single();
        int frameWidth = integer(animation.get("width"), -1);
        int frameHeight = integer(animation.get("height"), -1);
        if (frameWidth == -1 && frameHeight == -1) frameWidth = frameHeight = Math.min(width, height);
        else if (frameWidth == -1) frameWidth = width;
        else if (frameHeight == -1) frameHeight = height;
        if (frameWidth <= 0 || frameHeight <= 0 || width % frameWidth != 0 || height % frameHeight != 0) return single();
        int columns = width / frameWidth, rows = height / frameHeight;
        long countLong = (long) columns * rows;
        if (countLong <= 0 || countLong > 65536) return single();
        int count = (int) countLong;
        int defaultTime = Math.max(1, Math.min(1000000, integer(animation.get("frametime"), 1)));
        ArrayList<Integer> frames = new ArrayList<>(), ends = new ArrayList<>();
        JsonArray source = animation.has("frames") && animation.get("frames").isJsonArray()
                ? animation.getAsJsonArray("frames") : null;
        if (source == null || source.isEmpty())
        {
            if ((long) count * defaultTime > Integer.MAX_VALUE) return single();
            for (int i = 0; i < count; ++i) { frames.add(i); ends.add((i + 1) * defaultTime); }
        }
        else
        {
            int total = 0;
            for (JsonElement element : source)
            {
                JsonObject object = element.isJsonObject() ? element.getAsJsonObject() : null;
                int index = integer(object == null ? element : object.get("index"), -1);
                int time = object == null ? defaultTime : Math.max(1, Math.min(1000000, integer(object.get("time"), defaultTime)));
                if (index < 0 || index >= count || total > Integer.MAX_VALUE - time) continue;
                total += time;
                frames.add(index); ends.add(total);
            }
        }
        if (frames.isEmpty()) return single();
        return new AnimatedSpriteFrames(columns, rows, frames.stream().mapToInt(Integer::intValue).toArray(),
                ends.stream().mapToInt(Integer::intValue).toArray(), ends.get(ends.size() - 1));
    }

    private static int integer(JsonElement element, int fallback)
    {
        try { return element != null && element.isJsonPrimitive() ? element.getAsInt() : fallback; }
        catch (RuntimeException ignored) { return fallback; }
    }

    public int columns() { return columns; }
    public int rows() { return rows; }
    public int frameCount() { return indices.length; }
    public int totalTicks() { return duration; }
    public int frameAt(int tick)
    {
        int position = Math.floorMod(tick, duration) + 1;
        int found = Arrays.binarySearch(ends, position);
        return indices[found >= 0 ? found : -found - 1];
    }
    public float u0(int frame) { return (float) (frame % columns) / columns; }
    public float u1(int frame) { return (float) (frame % columns + 1) / columns; }
    public float v0(int frame) { return (float) (frame / columns) / rows; }
    public float v1(int frame) { return (float) (frame / columns + 1) / rows; }
}
