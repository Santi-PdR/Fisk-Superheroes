package com.fiskmods.heroes.client.render;

import com.fiskmods.heroes.common.entity.arrow.TrickArrowEntity;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class TrickArrowRenderer extends ArrowRenderer<TrickArrowEntity>
{
    private static final ResourceLocation TEXTURE = new ResourceLocation("minecraft", "textures/entity/projectiles/arrow.png");

    public TrickArrowRenderer(EntityRendererProvider.Context context)
    {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(TrickArrowEntity entity)
    {
        return TEXTURE;
    }
}
