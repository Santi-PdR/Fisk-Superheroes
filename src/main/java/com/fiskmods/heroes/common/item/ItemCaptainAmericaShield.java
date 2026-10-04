package com.fiskmods.heroes.common.item;

import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ShieldItem;

/** Captain America's shield uses the vanilla shield block action and its original durability. */
public final class ItemCaptainAmericaShield extends ShieldItem
{
    public ItemCaptainAmericaShield(Properties properties)
    {
        super(properties.durability(1561).rarity(Rarity.RARE));
    }
}
