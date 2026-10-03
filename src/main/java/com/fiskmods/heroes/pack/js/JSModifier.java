package com.fiskmods.heroes.pack.js;

import com.fiskmods.heroes.common.hero.power.ModifierEntry;

/** The modifier view exposed to hero-pack predicates. */
public final class JSModifier
{
    private final ModifierEntry entry;

    public JSModifier(ModifierEntry entry)
    {
        this.entry = entry;
    }

    /** Fully qualified modifier name, e.g. {@code fiskheroes:controlled_flight}. */
    public String name()
    {
        return entry.getModifier().getId().toString();
    }

    /** Pack-specific modifier card id, e.g. {@code boosted} or {@code springboard}. */
    public String id()
    {
        String card = entry.getCard();
        int separator = card != null ? card.indexOf('|') : -1;
        return separator >= 0 ? card.substring(separator + 1) : "";
    }
}
