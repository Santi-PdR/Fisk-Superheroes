package com.fiskmods.heroes.client.render;

import com.fiskmods.heroes.common.entity.arrow.TrickArrowEntity;
import com.fiskmods.heroes.common.hero.HeroIteration;
import com.fiskmods.heroes.common.hero.HeroTracker;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.world.entity.Entity;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class TrickArrowRenderer extends ArrowRenderer<TrickArrowEntity>
{
    private static final ResourceLocation TEXTURE = new ResourceLocation("fiskheroes", "textures/heroes/arrow/arrow.png");

    public TrickArrowRenderer(EntityRendererProvider.Context context)
    {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(TrickArrowEntity entity)
    {
        Entity shooter = entity.getOwner();
        HeroIteration iteration = shooter != null ? HeroTracker.getHero(shooter) : null;
        HeroModelData model = iteration != null ? HeroModelRegistry.get(iteration) : null;
        if (model != null)
        {
            JsonObject archery = model.getCustom().get("fiskheroes:archery");
            JsonElement configured = archery != null ? archery.get("textureArrow") : null;
            if (configured != null && configured.isJsonPrimitive() && configured.getAsJsonPrimitive().isString())
            {
                ResourceLocation texture = model.resolveCustomTexture(configured.getAsString(), shooter, -1);
                if (texture != null) return texture;
            }
        }
        return TEXTURE;
    }
}
