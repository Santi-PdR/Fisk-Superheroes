package com.fiskmods.heroes.common.item;

/** Chronos's Temporal Rifle with scoped zoom, explosive energy bolts, and 3D ModelChronosRifle. */
public final class ItemChronosRifle extends ItemGun
{
    public ItemChronosRifle(int magazineSize, int cooldownTicks, int reloadTicks, double range, float damage, Properties properties)
    {
        super(magazineSize, cooldownTicks, reloadTicks, range, damage, false, properties);
    }

    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer)
    {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions()
        {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer()
            {
                return com.fiskmods.heroes.client.render.ChronosRifleItemRenderer.getInstance();
            }
        });
    }
}
