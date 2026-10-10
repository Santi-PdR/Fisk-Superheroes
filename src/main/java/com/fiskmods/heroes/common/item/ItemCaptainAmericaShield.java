package com.fiskmods.heroes.common.item;

import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ShieldItem;

/** Captain America's shield uses the vanilla block action; its throw input is handled client-side. */
public final class ItemCaptainAmericaShield extends ShieldItem
{
    public ItemCaptainAmericaShield(Properties properties)
    {
        super(properties.durability(1561).rarity(Rarity.RARE));
    }
}
