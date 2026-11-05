package com.fiskmods.heroes.common.item;

/** Chokuto item with its original 3D model renderer and dual-wield support. */
public final class ItemChokuto extends ItemDualSword
{
    public ItemChokuto(Properties properties)
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
                return com.fiskmods.heroes.client.render.ChokutoItemRenderer.getInstance();
            }
        });
    }
}
