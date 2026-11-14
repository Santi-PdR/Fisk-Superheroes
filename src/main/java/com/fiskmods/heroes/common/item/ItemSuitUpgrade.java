package com.fiskmods.heroes.common.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** The five 1.7 metadata variants are separate registered items on modern Minecraft. */
public final class ItemSuitUpgrade extends Item
{
    private final int level;
    public ItemSuitUpgrade(int level, Properties properties)
    {
        super(properties);
        if (level < 0 || level > 4) throw new IllegalArgumentException("upgrade level");
        this.level = level;
    }
    public int level() { return level; }
    @Override public Component getName(ItemStack stack)
    {
        return Component.translatable("item.suit_upgrade_" + level + ".name");
    }
}
