package com.fiskmods.heroes.client.render;

import com.fiskmods.heroes.common.spell.EarthCrackEntity;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** The Earth Crack controller is invisible; its replicated target and block particles are visible. */
public final class EarthCrackRenderer extends EntityRenderer<EarthCrackEntity>
{
    private static final ResourceLocation TEXTURE = new ResourceLocation("minecraft", "textures/atlas/blocks.png");

    public EarthCrackRenderer(EntityRendererProvider.Context context)
    {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public ResourceLocation getTextureLocation(EarthCrackEntity entity)
    {
        return TEXTURE;
    }
}
