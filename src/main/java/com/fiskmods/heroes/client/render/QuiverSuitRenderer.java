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
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = FiskHeroes.MODID, value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public final class QuiverSuitRenderer
{
    private static final ResourceLocation DEFAULT_TEXTURE = new ResourceLocation(FiskHeroes.MODID,
            "textures/heroes/quiver/quiver.png");
    private static final ModelPart QUIVER = createModel();
    private static final double[][][] ARROW_OFFSETS = createArrowOffsets();
    private static net.minecraft.world.level.Level cacheLevel;
    private static final java.util.Map<java.util.UUID, String> LOGGED = new java.util.HashMap<>();
    private static final java.util.Map<java.util.UUID, String> LOGGED_CONTENTS = new java.util.HashMap<>();
    // Projectile renderers need a dummy owner for hero-specific arrow textures. Keep a small,
    // bounded cache instead of allocating a new entity for every shaft on every frame.
    private static final java.util.Map<java.util.UUID, com.fiskmods.heroes.common.entity.arrow.TrickArrowEntity> ARROWS =
            new java.util.LinkedHashMap<>(16, .75F, true)
            {
                @Override protected boolean removeEldestEntry(java.util.Map.Entry<java.util.UUID,
                        com.fiskmods.heroes.common.entity.arrow.TrickArrowEntity> eldest)
                { return size() > 64; }
            };

    private QuiverSuitRenderer()
    {
    }

    static void render(PoseStack pose, MultiBufferSource buffer, int light,
            AbstractClientPlayer player, PlayerModel<AbstractClientPlayer> playerModel,
            HeroModelData model, int armorSlot)
    {
        boolean equipped, foil;
        String contents;
        if (player.isLocalPlayer())
        {
            ItemStack quiver = ItemQuiver.findQuiver(player);
            equipped = !quiver.isEmpty();
            foil = equipped && quiver.isEnchanted();
            contents = ItemQuiver.visualCounts(quiver);
        }
        else
        {
            // A tracking client does not receive the remote player's whole inventory.
            // Its equipped-quiver data is published by the server instead.
            var data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(player);
            equipped = data != null && !data.getData().get(com.fiskmods.heroes.common.data.var.Vars.EQUIPPED_QUIVER).isEmpty();
            foil = equipped && data.getData().get(com.fiskmods.heroes.common.data.var.Vars.EQUIPPED_QUIVER_GLINT);
            contents = equipped ? data.getData().get(com.fiskmods.heroes.common.data.var.Vars.EQUIPPED_QUIVER_CONTENTS) : "00000";
        }
        // The original hides the back-mounted quiver while it is held in the main hand.
        boolean visible = equipped && !(player.getMainHandItem().getItem() instanceof ItemQuiver);
        if (Boolean.getBoolean("fiskheroes.ciBowDiagnostics"))
        {
            String state = equipped + "|" + foil + "|" + visible + "|" + (model != null);
            if (!state.equals(LOGGED.put(player.getUUID(), state)))
                FiskHeroes.LOGGER.info("Quiver appearance: wearer={}, equipped={}, foil={}, visible={}, hero={}",
                        player.getName().getString(), equipped, foil, visible, model != null);
        }
        if (!visible) return;

        String textureKey = null;
        JsonObject archery = model != null ? model.getCustom().get("fiskheroes:archery") : null;
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
        VertexConsumer consumer = ItemRenderer.getFoilBuffer(buffer, type, false, foil);
        QUIVER.render(pose, consumer, light, OverlayTexture.NO_OVERLAY);
        renderArrows(player, contents, pose, buffer, light);
        pose.popPose();
    }

    private static void renderArrows(AbstractClientPlayer player, String contents, PoseStack pose,
            MultiBufferSource buffer, int light)
    {
        int total = 0;
        for (int slot = 0; slot < 5; ++slot) total += ItemQuiver.visualCount(contents, slot);
        if (Boolean.getBoolean("fiskheroes.ciBowDiagnostics"))
        {
            if (!contents.equals(LOGGED_CONTENTS.put(player.getUUID(), contents)))
                FiskHeroes.LOGGER.info("Quiver shafts rendered: wearer={}, counts={}, total={}",
                        player.getName().getString(), contents, total);
        }
        if (total == 0) return;
        if (cacheLevel != player.level())
        {
            ARROWS.clear();
            cacheLevel = player.level();
        }
        var arrow = ARROWS.get(player.getUUID());
        if (arrow == null || arrow.level() != player.level() || arrow.getOwner() != player)
        {
            arrow = new com.fiskmods.heroes.common.entity.arrow.TrickArrowEntity(
                    com.fiskmods.heroes.common.entity.ModEntities.TRICK_ARROW.get(), player.level(), player, "normal");
            arrow.setYRot(0); arrow.yRotO = 0; arrow.setXRot(0); arrow.xRotO = 0;
            ARROWS.put(player.getUUID(), arrow);
        }
        var renderer = net.minecraft.client.Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(arrow);
        if (!(renderer instanceof TrickArrowRenderer arrowRenderer)) return;
        for (int slot = 0; slot < 5; ++slot)
        {
            for (int i = 0; i < ItemQuiver.visualCount(contents, slot); ++i)
            {
                double[] offset = ARROW_OFFSETS[slot][i];
                pose.pushPose();
                // The vanilla projectile renderer points down its local Z axis; turn it upright
                // in the back-mounted quiver and keep the original 0.7 model scale.
                pose.translate(offset[0], offset[1] - .08, offset[2]);
                pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90));
                pose.scale(.7F, .7F, .7F);
                arrowRenderer.renderQuiverShaft(arrow, net.minecraft.client.Minecraft.getInstance().getFrameTime(), pose, buffer, light);
                pose.popPose();
            }
        }
    }

    private static double[][][] createArrowOffsets()
    {
        double[][][] result = new double[5][4][3];
        for (int slot = 0; slot < 5; ++slot)
        {
            java.util.Random slotRandom = new java.util.Random(slot * 100000000L);
            double sx = (slotRandom.nextFloat() - .5) / 10;
            double sz = (slotRandom.nextFloat() - .5) / 10;
            for (int i = 0; i < 4; ++i)
            {
                java.util.Random random = new java.util.Random((i + 1) * 1000000000L);
                result[slot][i][0] = sx + (random.nextFloat() - .5) / 20;
                result[slot][i][1] = random.nextFloat() / 10 - .075;
                result[slot][i][2] = sz + (random.nextFloat() - .5) / 20;
            }
        }
        return result;
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void onLogout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event)
    {
        ARROWS.clear(); LOGGED.clear(); LOGGED_CONTENTS.clear(); cacheLevel = null;
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
