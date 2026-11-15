package com.fiskmods.heroes.client.render;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.entity.projectile.ThrownShieldEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** The original projectile draws the shield model in its flight orientation, not as a billboard. */
public final class ThrownShieldRenderer extends EntityRenderer<ThrownShieldEntity>
{
    private static final ResourceLocation TEXTURE = FiskHeroes.id("textures/models/captain_americas_shield.png");
    private static final java.util.Set<ThrownShieldEntity> LOGGED =
            java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());

    public ThrownShieldRenderer(EntityRendererProvider.Context context)
    {
        super(context);
    }

    @Override
    public void render(ThrownShieldEntity entity, float yaw, float partialTick, PoseStack pose,
            MultiBufferSource buffers, int light)
    {
        if (Boolean.getBoolean("fiskheroes.ciShieldDiagnostics") && LOGGED.add(entity))
            FiskHeroes.LOGGER.info("Thrown shield model rendered: entity={}", entity.getId());
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot()) - 90.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTick, entity.xRotO, entity.getXRot())));
        pose.mulPose(Axis.XP.rotationDegrees(180.0F));
        pose.mulPose(Axis.YP.rotationDegrees(entity.getSpin(partialTick)));
        // A projectile is not part of a wearer's item render; do not inherit a
        // surrounding suit livery when an entity is rendered in a nested pass.
        SuitItemRenderContext.withTexture(null, () -> CaptainAmericaShieldItemRenderer.getInstance().renderShieldModel(
                entity.getItem(), net.minecraft.world.item.ItemDisplayContext.NONE, pose, buffers, light, OverlayTexture.NO_OVERLAY, false));
        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(ThrownShieldEntity entity)
    {
        return TEXTURE;
    }
}
