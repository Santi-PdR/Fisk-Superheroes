package com.fiskmods.heroes.client.render;

import com.fiskmods.heroes.common.spell.SpellDuplicateEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import java.util.UUID;

/** Renders a duplicate with its owner's player skin and current superhero suit textures. */
public final class SpellDuplicateRenderer extends MobRenderer<SpellDuplicateEntity, PlayerModel<SpellDuplicateEntity>>
{
    public SpellDuplicateRenderer(EntityRendererProvider.Context context)
    {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        addLayer(new SpellDuplicateSuitLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(SpellDuplicateEntity duplicate)
    {
        if (duplicate.getOwner() instanceof AbstractClientPlayer player)
        {
            return player.getSkinTextureLocation();
        }
        return DefaultPlayerSkin.getDefaultSkin(duplicate.getOwner() != null
                ? duplicate.getOwner().getUUID() : new UUID(0L, 0L));
    }
}
