package com.fiskmods.heroes.common.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;

/** Rupture's Scythe ported from 1.7.10 ItemRuptureScythe (800 durability + 3D ModelRuptureScythe). */
public final class ItemRuptureScythe extends SwordItem
{
    public ItemRuptureScythe(Properties properties)
    {
        super(Tiers.IRON, 6, -2.8F, properties.durability(800));
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair)
    {
        return repair.is(ModItems.TITANIUM_INGOT.get()) || super.isValidRepairItem(toRepair, repair);
    }

    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer)
    {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions()
        {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer()
            {
                return com.fiskmods.heroes.client.render.RuptureScytheItemRenderer.getInstance();
            }
        });
    }
}
