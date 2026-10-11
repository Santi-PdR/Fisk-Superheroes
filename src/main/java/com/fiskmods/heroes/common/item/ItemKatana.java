package com.fiskmods.heroes.common.item;

/** Katana item with the original model render; other dual swords keep their own item models. */
public final class ItemKatana extends ItemDualSword
{
    public ItemKatana(Properties properties)
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
                return com.fiskmods.heroes.client.render.KatanaItemRenderer.getInstance();
            }
        });
    }
}
