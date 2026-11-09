package com.fiskmods.heroes.client.render;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.item.ItemDualSword;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Original 3D Tactical Tonfa mesh ported from ModelTonfa & RenderItemTonfa. */
public final class TacticalTonfaItemRenderer extends BlockEntityWithoutLevelRenderer
{
    private static final ResourceLocation TEXTURE = FiskHeroes.id("textures/models/tonfa.png");
    private static TacticalTonfaItemRenderer instance;
    private static final java.util.Set<String> POSE_DIAGNOSTICS = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final ModelPart handle;

    private TacticalTonfaItemRenderer()
    {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        this.handle = createModel().getChild("handle");
    }

    public static TacticalTonfaItemRenderer getInstance()
    {
        if (instance == null) instance = new TacticalTonfaItemRenderer();
        return instance;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
            MultiBufferSource buffers, int light, int overlay)
    {
        pose.pushPose();
        switch (context)
        {
            case NONE ->
            {
                // Suit placement already supplies the anchor and rotation.
                pose.translate(0.5D, 0.5D, 0.5D);
            }
            case GUI ->
            {
                pose.translate(0.5D, 0.55D, 0.5D);
                pose.mulPose(Axis.YP.rotationDegrees(45.0F));
                pose.mulPose(Axis.XP.rotationDegrees(-35.0F));
                pose.scale(1.25F, -1.25F, -1.25F);
            }
            case GROUND, FIXED ->
            {
                pose.translate(0.5D, 0.5D, 0.5D);
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
                pose.scale(1.0F, -1.0F, -1.0F);
            }
            case FIRST_PERSON_RIGHT_HAND, FIRST_PERSON_LEFT_HAND ->
            {
                // Vanilla's hand origin is much closer to the camera than the legacy
                // renderer's. Pull the grip inward and away so the full baton is visible.
                pose.translate(context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND ? 0.36D : 0.64D, 0.75D, 0.18D);
                pose.mulPose(Axis.YP.rotationDegrees(context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND ? 85.0F : -85.0F));
                pose.mulPose(Axis.XP.rotationDegrees(150.0F));
                applyHandPose(pose, context);
                pose.scale(0.8F, -0.8F, -0.8F);
            }
            case THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND ->
            {
                pose.translate(context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND ? 0.52D : 0.48D, 0.48D, 0.46D);
                // Modern ItemInHandLayer already rotates the item into the hand;
                // the legacy 85-degree yaw made both batons stick out sideways.
                pose.mulPose(Axis.XP.rotationDegrees(142.0F));
                applyHandPose(pose, context);
                pose.scale(1.2F, -1.2F, -1.2F);
            }
            default ->
            {
                pose.translate(0.5D, 0.5D, 0.5D);
                pose.scale(1.0F, -1.0F, -1.0F);
            }
        }
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        handle.render(pose, consumer, light, overlay);
        if (ItemDualSword.isDual(stack) && (context == ItemDisplayContext.GUI || context == ItemDisplayContext.GROUND || context == ItemDisplayContext.FIXED))
        {
            pose.translate(0.22D, 0.0D, 0.16D);
            handle.render(pose, consumer, light, overlay);
        }
        pose.popPose();
    }

    private static void applyHandPose(PoseStack pose, ItemDisplayContext context)
    {
        var wearer = SuitItemRenderContext.wearer();
        if (wearer != null)
        {
            boolean left = context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                    || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
            var data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(wearer);
            byte state = data != null ? data.getData().get(com.fiskmods.heroes.common.data.var.Vars.TONFA_STATE) : 0;
            boolean swingLeft = wearer.getMainArm() == net.minecraft.world.entity.HumanoidArm.LEFT;
            if (wearer.swingingArm == net.minecraft.world.InteractionHand.OFF_HAND) swingLeft = !swingLeft;
            boolean swinging = wearer.swinging && swingLeft == left;
            float progress = swinging ? wearer.getAttackAnim(Minecraft.getInstance().getFrameTime()) : 0.0F;
            float rotation = com.fiskmods.heroes.common.util.TonfaPoseMath.rotation(state, left, progress, swinging);
            pose.mulPose(Axis.YP.rotationDegrees(rotation));
            boolean toggled = (state & com.fiskmods.heroes.common.util.TonfaPoseMath.mask(left)) != 0;
            String key = wearer.getUUID() + "|" + context + "|" + toggled;
            if (POSE_DIAGNOSTICS.add(key))
                FiskHeroes.LOGGER.info("Tonfa hand pose: wearer={}, context={}, toggled={}",
                        wearer.getName().getString(), context, toggled);
        }
    }

    private static ModelPart createModel()
    {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition meshRoot = mesh.getRoot();
        PartDefinition p_handle = meshRoot.addOrReplaceChild("handle", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        p_handle.addOrReplaceChild("stick", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-0.5F, 0.0F, -7.5F, 1.0F, 1.0F, 11.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 32, 16).bakeRoot();
    }
}
