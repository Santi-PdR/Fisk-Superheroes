package com.fiskmods.heroes.client.render;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.data.var.Vars;
import com.fiskmods.heroes.common.item.ItemCompoundBow;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Original three-dimensional bow model with animated limbs and strings. */
public final class CompoundBowItemRenderer extends BlockEntityWithoutLevelRenderer
{
    private static final ResourceLocation TEXTURE = FiskHeroes.id("textures/models/compound_bow.png");
    private static final java.util.Set<String> LOGGED = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private static CompoundBowItemRenderer instance;
    private final ModelPart base, bottom, top;

    private CompoundBowItemRenderer()
    {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        var model = CompoundBowModel.create();
        base = model.getChild("base1"); bottom = model.getChild("archBottom1"); top = model.getChild("archTop1");
    }

    public static CompoundBowItemRenderer getInstance()
    {
        if (instance == null) instance = new CompoundBowItemRenderer();
        return instance;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
            MultiBufferSource buffers, int light, int overlay)
    {
        var wearer = SuitItemRenderContext.wearer();
        boolean drawing = wearer != null && wearer.isUsingItem() && wearer.getUseItem() == stack && !ItemCompoundBow.isBroken(stack);
        float draw = drawing ? Mth.clamp((stack.getUseDuration() - wearer.getUseItemRemainingTicks()
                + Minecraft.getInstance().getFrameTime()) / ItemCompoundBow.getDrawDuration(wearer), 0, 1) : 0;
        float arches = Mth.sin(draw * 1.5F) / 1.5F;
        float horizontal = 0;
        if (wearer != null)
        {
            var data = SHDataCapabilities.getPlayer(wearer);
            if (data != null) horizontal = Mth.lerp(Minecraft.getInstance().getFrameTime(),
                    data.getData().get(Vars.PREV_HORIZONTAL_BOW_TIMER), data.getData().get(Vars.HORIZONTAL_BOW_TIMER));
        }
        if (Boolean.getBoolean("fiskheroes.ciBowDiagnostics") && wearer != null)
        {
            String key = wearer.getUUID() + "|" + context + "|" + drawing + "|" + (horizontal > .5F) + "|" + ItemCompoundBow.isBroken(stack);
            if (LOGGED.add(key)) FiskHeroes.LOGGER.info("Compound bow 3D model rendered: wearer={}, context={}, drawing={}, horizontal={}, broken={}",
                    wearer.getName().getString(), context, drawing, horizontal > .5F, ItemCompoundBow.isBroken(stack));
        }
        pose.pushPose();
        switch (context)
        {
            case FIRST_PERSON_RIGHT_HAND, FIRST_PERSON_LEFT_HAND ->
            {
                boolean left = context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
                pose.translate(left ? .25 : .75, .5, .5);
                pose.mulPose(Axis.XP.rotationDegrees(-5));
                pose.mulPose(Axis.ZP.rotationDegrees(left ? -230 : 230));
                pose.mulPose(Axis.YP.rotationDegrees(left ? -100 : 100));
                pose.mulPose(Axis.XP.rotationDegrees(-5));
                pose.scale(.375F, -.375F, -.375F);
                pose.translate(-.2F * horizontal, -.4F * horizontal, -.1F * horizontal);
                pose.mulPose(Axis.ZP.rotationDegrees(70 * horizontal));
                pose.mulPose(Axis.YP.rotationDegrees(-10 * horizontal));
                if (drawing)
                {
                    pose.translate(.2F * horizontal, .3F * horizontal, -.2F * horizontal);
                    pose.mulPose(Axis.ZP.rotationDegrees(20 * horizontal));
                    pose.mulPose(Axis.XP.rotationDegrees(-10 * horizontal));
                }
            }
            case THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND ->
            {
                pose.translate(.5, .5, .5);
                pose.mulPose(Axis.YP.rotationDegrees(90));
                pose.mulPose(Axis.XP.rotationDegrees(140));
                pose.translate(-.035, .4, -.68);
                pose.mulPose(Axis.XP.rotationDegrees(180));
                pose.scale(-.4F, .4F, .4F);
                pose.translate(-.3F * horizontal, -.1F * horizontal, 0);
                pose.mulPose(Axis.ZP.rotationDegrees(50 * horizontal));
            }
            default ->
            {
                pose.translate(.5, .5, .5);
                pose.mulPose(Axis.YP.rotationDegrees(-45));
                pose.scale(.35F, -.35F, -.35F);
            }
        }
        bottom.xRot = .1308997F - arches;
        top.xRot = .18151425F - arches;
        var consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(SuitItemRenderContext.textureOr(TEXTURE)));
        bottom.render(pose, consumer, light, overlay);
        base.render(pose, consumer, light, overlay);
        top.render(pose, consumer, light, overlay);
        if (!ItemCompoundBow.isBroken(stack)) drawStrings(pose, buffers, arches);
        pose.popPose();
    }

    private static void drawStrings(PoseStack pose, MultiBufferSource buffers, float draw)
    {
        float y1 = .91F - .55F * draw, y2 = -2.3F + .55F * draw;
        float z = .625F + Mth.sin(1.075F + draw) - .9F;
        float centerY = (y1 + y2) / 2 - .15F, centerZ = z + draw * 1.3F;
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        line(lines, pose.last(), 0, .15F, .17F, 0, y1, z);
        line(lines, pose.last(), 0, -1.63125F, .05F, 0, y2, z);
        line(lines, pose.last(), 0, y1, z, 0, centerY, centerZ);
        line(lines, pose.last(), 0, centerY, centerZ, 0, y2, z);
    }

    private static void line(VertexConsumer out, PoseStack.Pose pose, float x1, float y1, float z1, float x2, float y2, float z2)
    {
        float dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        float length = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        if (length == 0) return;
        out.vertex(pose.pose(), x1, y1, z1).color(20, 20, 20, 255).normal(pose.normal(), dx / length, dy / length, dz / length).endVertex();
        out.vertex(pose.pose(), x2, y2, z2).color(20, 20, 20, 255).normal(pose.normal(), dx / length, dy / length, dz / length).endVertex();
    }
}
