package com.fiskmods.heroes.common.util;

import java.util.Map;
import java.util.WeakHashMap;

/** Runs once per object's own tick without retaining unloaded entities. Not thread-safe. */
public final class WeakTickGate<T>
{
    private final Map<T, Integer> lastTicks = new WeakHashMap<>();

    public boolean enter(T object, int tick)
    {
        Integer previous = lastTicks.put(object, tick);
        return previous == null || previous != tick;
    }

    public void clear()
    {
        lastTicks.clear();
    }
}
