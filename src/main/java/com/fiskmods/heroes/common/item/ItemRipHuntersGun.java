package com.fiskmods.heroes.common.item;

/** Rip Hunter's laser revolver with its original 3D ModelRipHuntersGun & blue lights layer. */
public final class ItemRipHuntersGun extends ItemGun
{
    public ItemRipHuntersGun(int magazineSize, int cooldownTicks, int reloadTicks, double range, float damage, Properties properties)
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
                return com.fiskmods.heroes.client.render.RipHuntersGunItemRenderer.getInstance();
            }
        });
    }
}
