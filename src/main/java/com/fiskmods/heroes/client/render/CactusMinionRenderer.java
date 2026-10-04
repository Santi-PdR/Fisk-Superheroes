package com.fiskmods.heroes.client.render;

import com.fiskmods.heroes.common.entity.CactusMinionEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import com.mojang.blaze3d.vertex.PoseStack;

/** Renders the minion as the same stacked vanilla cactus blocks as the original renderer. */
public final class CactusMinionRenderer extends EntityRenderer<CactusMinionEntity>
{
    private final BlockRenderDispatcher blocks;
    private final BlockState cactus = Blocks.CACTUS.defaultBlockState();

    public CactusMinionRenderer(EntityRendererProvider.Context context)
    {
        super(context);
        blocks = context.getBlockRenderDispatcher();
        shadowRadius = 0.5F;
    }

    @Override
    public void render(CactusMinionEntity entity, float yaw, float partialTick, PoseStack pose,
            MultiBufferSource buffers, int packedLight)
    {
        pose.pushPose();
        pose.translate(-0.5D, 0.0D, -0.5D);
        for (int i = 0; i < entity.getCactusSize(); i++)
        {
            blocks.renderSingleBlock(cactus, pose, buffers, packedLight, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
            pose.translate(0.0D, 1.0D, 0.0D);
        }
        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffers, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(CactusMinionEntity entity)
    {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
