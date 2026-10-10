package com.fiskmods.heroes.client.render;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.spell.IllusionDroneEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** Renders the original 128x64 drone model and its separate emissive light texture. */
public final class IllusionDroneRenderer extends MobRenderer<IllusionDroneEntity, IllusionDroneModel>
{
    private static final ResourceLocation TEXTURE = FiskHeroes.id("textures/entity/illusion_drone.png");
    private static final ResourceLocation LIGHTS = FiskHeroes.id("textures/entity/illusion_drone_lights.png");

    public IllusionDroneRenderer(EntityRendererProvider.Context context)
    {
        super(context, new IllusionDroneModel(context.bakeLayer(IllusionDroneModel.LAYER)), 1.0F);
        addLayer(new EmissiveLightsLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(IllusionDroneEntity entity)
    {
        return TEXTURE;
    }

    private static final class EmissiveLightsLayer extends RenderLayer<IllusionDroneEntity, IllusionDroneModel>
    {
        private EmissiveLightsLayer(RenderLayerParent<IllusionDroneEntity, IllusionDroneModel> parent)
        {
            super(parent);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, IllusionDroneEntity entity,
                float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch)
        {
            IllusionDroneModel model = getParentModel();
            model.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            VertexConsumer vertex = buffers.getBuffer(RenderType.eyes(LIGHTS));
            model.renderToBuffer(pose, vertex, net.minecraft.client.renderer.LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        }
    }
}
