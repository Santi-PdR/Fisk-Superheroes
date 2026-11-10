package com.fiskmods.heroes.common.item;

/** Desert Eagle variant that installs its original 3D model renderer on the client. */
public final class ItemDesertEagle extends ItemGun
{
    public ItemDesertEagle(int magazineSize, int cooldownTicks, int reloadTicks,
            double range, float damage, Properties properties)
    {
        super(magazineSize, cooldownTicks, reloadTicks, range, damage, properties);
    }

    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer)
    {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions()
        {
            @Override
            public boolean applyForgeHandTransform(com.mojang.blaze3d.vertex.PoseStack pose,
                    net.minecraft.client.player.LocalPlayer player, net.minecraft.world.entity.HumanoidArm arm,
                    net.minecraft.world.item.ItemStack stack, float partialTick, float equip, float swing)
            {
                com.fiskmods.heroes.client.render.DesertEagleAimPose.apply(pose, player, arm, partialTick);
                return false; // Preserve vanilla equip and swing transforms after our shared translation.
            }

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer()
            {
                return com.fiskmods.heroes.client.render.DesertEagleItemRenderer.getInstance();
            }
        });
    }
}
