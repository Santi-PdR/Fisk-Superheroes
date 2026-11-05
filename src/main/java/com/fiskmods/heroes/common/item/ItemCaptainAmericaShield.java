package com.fiskmods.heroes.common.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ShieldItem;

/** Captain America's Vibranium shield with its original curved 3D ModelCapsShield renderer. */
public final class ItemCaptainAmericaShield extends ShieldItem
{
    public ItemCaptainAmericaShield(Properties properties)
    {
        super(properties.durability(1561).rarity(Rarity.RARE));
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair)
    {
        return repair.is(ModItems.VIBRANIUM_INGOT.get()) || repair.is(ModItems.VIBRANIUM_DISC.get());
    }

    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer)
    {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions()
        {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer()
            {
                return com.fiskmods.heroes.client.render.CaptainAmericaShieldItemRenderer.getInstance();
            }
        });
    }
}
