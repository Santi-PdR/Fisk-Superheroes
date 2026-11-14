package com.fiskmods.heroes.common.hero.augment;

import java.util.ArrayList;
import java.util.List;

import com.fiskmods.heroes.common.hero.HeroAttribute;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** Original hero-pack augment ranges, in JSON order. Each index occupies a nibble in Upgrades. */
public final class AugmentSet
{
    public static final AugmentSet EMPTY = new AugmentSet(List.of());
    public static final int MAX_ENTRIES = 6;
    private final List<Entry> entries;

    public record Entry(HeroAttribute attribute, int index, int min, int max)
    {
        public int capacity() { return max - min; }
    }

    private AugmentSet(List<Entry> entries) { this.entries = List.copyOf(entries); }
    public List<Entry> entries() { return entries; }
    public int size() { return entries.size(); }
    public Entry get(int index) { return index >= 0 && index < entries.size() ? entries.get(index) : null; }
    public Entry get(HeroAttribute attribute)
    {
        for (Entry entry : entries) if (entry.attribute() == attribute) return entry;
        return null;
    }

    /** Reject invalid definitions instead of interpreting them as different packed indices. */
    public static AugmentSet parse(JsonObject root)
    {
        if (!root.has("attributes") || !root.get("attributes").isJsonObject()) return EMPTY;
        List<Entry> entries = new ArrayList<>();
        var seen = java.util.EnumSet.noneOf(HeroAttribute.class);
        for (var item : root.getAsJsonObject("attributes").entrySet())
        {
            HeroAttribute attribute = HeroAttribute.byName(item.getKey());
            if (attribute == null) throw new IllegalArgumentException("Unknown augment attribute " + item.getKey());
            if (!seen.add(attribute)) throw new IllegalArgumentException("Duplicate augment attribute " + item.getKey());
            if (entries.size() >= MAX_ENTRIES) throw new IllegalArgumentException("At most six augment attributes are supported");
            JsonElement value = item.getValue();
            int min, max;
            if (value.isJsonArray() && value.getAsJsonArray().size() == 2)
            {
                min = value.getAsJsonArray().get(0).getAsInt();
                max = value.getAsJsonArray().get(1).getAsInt();
            }
            else if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber())
            {
                min = 0;
                max = value.getAsInt();
            }
            else throw new IllegalArgumentException("Invalid augment range for " + attribute);
            // The five original upgrade items represent transitions 0->1 through 4->5.
            if (min < 0 || max < min || max > 5)
                throw new IllegalArgumentException("Invalid augment bounds " + min + ".." + max);
            entries.add(new Entry(attribute, entries.size(), min, max));
        }
        return new AugmentSet(entries);
    }

    public int level(int packed, Entry entry)
    {
        return Math.min(entry.capacity(), (packed >>> (entry.index() * 4)) & 15);
    }

    public boolean isMaxed(int packed)
    {
        if (entries.isEmpty()) return false;
        for (Entry entry : entries) if (level(packed, entry) < entry.capacity()) return false;
        return true;
    }

    /** The original functions modify the suit attribute's amount, not its final vanilla value. */
    public static double apply(HeroAttribute attribute, double value, int operation, int level)
    {
        if (level <= 0) return value;
        boolean additive = operation == 0;
        return switch (attribute)
        {
            case DAMAGE_REDUCTION -> additive ? value : 1.0D - (1.0D - value) / (1.0D + level * .1D);
            case BASE_SPEED_LEVELS -> additive ? value + level * .6D : value;
            case PUNCH_DAMAGE -> value + level * (additive ? 1.0D : .2D);
            case WEAPON_DAMAGE -> value + level * (additive ? .5D : .1D);
            case IMPACT_DAMAGE, ARROW_DAMAGE -> value + level * (additive ? 1.0D : .1D);
            case BOW_DRAWBACK -> additive ? value : value + level * .05D;
            case REACH_DISTANCE -> value + level * (additive ? .5D : .1D);
            case BASE_SPEED, SPRINT_SPEED -> additive ? value : value + level * .1D;
            case JUMP_HEIGHT -> value + level * (additive ? .5D : .2D);
            case FALL_RESISTANCE -> additive ? value + level : value;
            case KNOCKBACK -> value + level * (additive ? .5D : .2D);
            case MAX_HEALTH -> value + level * (additive ? 1.0D : .1D);
            case STEP_HEIGHT -> additive ? value + level * .2D : value;
        };
    }

    public static int defaultOperation(HeroAttribute attribute)
    {
        return switch (attribute)
        {
            case DAMAGE_REDUCTION, IMPACT_DAMAGE, ARROW_DAMAGE, BOW_DRAWBACK, BASE_SPEED, SPRINT_SPEED -> 1;
            default -> 0;
        };
    }
}
