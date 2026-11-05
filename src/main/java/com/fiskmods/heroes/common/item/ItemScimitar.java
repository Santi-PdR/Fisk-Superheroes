package com.fiskmods.heroes.common.item;

/** Scimitar item with its original 3D model renderer and dual-wield support. */
public final class ItemScimitar extends ItemDualSword
{
    public ItemScimitar(Properties properties)
    {
        super(properties);
    }

    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer)
    {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions()
        {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer()
            {
                return com.fiskmods.heroes.client.render.ScimitarItemRenderer.getInstance();
            }
        });
    }
}
