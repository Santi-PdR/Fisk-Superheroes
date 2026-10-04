package com.fiskmods.heroes.common.hero;

import java.util.Collection;
import java.util.Iterator;
import java.util.function.Predicate;
import java.util.stream.Stream;

import net.minecraft.world.item.ItemStack;

/** Declared weapon choices for a hero, including each pack's eligibility predicate. */
public final class WeaponList implements Iterable<ItemStack>
{
    private final Entry[] entries;

    public WeaponList(Collection<Candidate> candidates)
    {
        entries = new Entry[candidates.size()];
        int i = 0;
        for (Candidate candidate : candidates)
        {
            entries[i] = new Entry(candidate.stack().copy(), i, candidate.included(), candidate.predicate());
            ++i;
        }
    }

    public boolean isEmpty() { return entries.length == 0; }
    public int size() { return entries.length; }

    public Entry getEntry(int index)
    {
        return index >= 0 && index < entries.length ? entries[index] : null;
    }

    public ItemStack get(int index)
    {
        Entry entry = getEntry(index);
        return entry != null ? entry.value() : ItemStack.EMPTY;
    }

    public boolean isValid(int index, ItemStack other)
    {
        Entry entry = getEntry(index);
        return entry != null && entry.isValid(other);
    }

    public int indexOf(ItemStack stack)
    {
        for (int i = 0; i < entries.length; ++i)
        {
            if (entries[i].isValid(stack)) return i;
        }
        return -1;
    }

    public Stream<Entry> entries() { return Stream.of(entries); }
    public Stream<ItemStack> stream() { return entries().map(Entry::value); }
    @Override public Iterator<ItemStack> iterator() { return stream().iterator(); }

    public record Candidate(ItemStack stack, Predicate<ItemStack> predicate, boolean included)
    {
        public Candidate
        {
            stack = stack.copy();
            predicate = predicate != null ? predicate : stack1 -> true;
        }
    }

    public record Entry(ItemStack value, int index, boolean included, Predicate<ItemStack> predicate)
    {
        public boolean isValid(ItemStack other)
        {
            if (other == null || other.isEmpty() || value.isEmpty()) return false;
            boolean sameVariant = other.getItem() == value.getItem()
                    && (value.isDamageableItem() || other.getDamageValue() == value.getDamageValue());
            return (other == value || sameVariant) && predicate.test(other);
        }
    }
}
