package com.fiskmods.heroes.client.render;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.item.ItemQuiver;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Draws the carried quiver using the texture selected by the active hero's archery effect. */
final class QuiverSuitRenderer
{
    private static final ResourceLocation DEFAULT_TEXTURE = new ResourceLocation(FiskHeroes.MODID,
            "textures/heroes/quiver/quiver.png");
    private static final ModelPart QUIVER = createModel();

    private QuiverSuitRenderer()
    {
    }

    static void render(PoseStack pose, MultiBufferSource buffer, int light,
            AbstractClientPlayer player, PlayerModel<AbstractClientPlayer> playerModel,
            HeroModelData model, int armorSlot)
    {
        ItemStack quiver = ItemQuiver.findQuiver(player);
        if (quiver.isEmpty()) return;

        String textureKey = null;
        JsonObject archery = model.getCustom().get("fiskheroes:archery");
        if (archery != null)
        {
            JsonElement texture = archery.get("textureQuiver");
            if (texture != null && texture.isJsonPrimitive() && texture.getAsJsonPrimitive().isString())
            {
                textureKey = texture.getAsString();
            }
        }

        ResourceLocation texture = textureKey != null ? model.resolveCustomTexture(textureKey, player, armorSlot) : null;
        if (texture == null) texture = DEFAULT_TEXTURE;

        pose.pushPose();
        playerModel.body.translateAndRotate(pose);
        pose.translate(-3.75D / 16.0D, 0.0D, 3.51D / 16.0D);
        pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-30.0F));

        RenderType type = RenderType.entityTranslucent(texture);
        VertexConsumer consumer = ItemRenderer.getFoilBuffer(buffer, type, false, quiver.isEnchanted());
        QUIVER.render(pose, consumer, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }

    private static ModelPart createModel()
    {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("quiver", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-2.0F, 0.5F, -1.5F, 4.0F, 11.0F, 3.0F),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64).bakeRoot().getChild("quiver");
    }
}
