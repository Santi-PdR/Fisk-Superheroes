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
    private net.minecraft.world.entity.projectile.AbstractArrow displayedArrow;
    private net.minecraft.world.entity.player.Player displayedOwner;
    private ItemStack displayedAmmo = ItemStack.EMPTY;

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
        if (context == ItemDisplayContext.GUI || context == ItemDisplayContext.GROUND || context == ItemDisplayContext.FIXED)
        {
            // The original custom bow renderer handled equipped views only. Inventories,
            // item frames and dropped items use the flat icon, including the broken variant.
            var minecraft = Minecraft.getInstance();
            var manager = minecraft.getModelManager();
            boolean broken = ItemCompoundBow.isBroken(stack);
            var model = manager.getModel(FiskHeroes.id(broken
                    ? "item/compound_bow_inventory_broken" : "item/compound_bow_inventory"));
            if (Boolean.getBoolean("fiskheroes.ciBowDiagnostics") && LOGGED.add("icon|" + context + "|" + broken))
                FiskHeroes.LOGGER.info("Compound bow icon rendered: context={}, broken={}, missing={}",
                        context, broken, model == manager.getMissingModel());
            pose.pushPose();
            // The outer custom model has already applied ItemRenderer's -0.5 centering;
            // undo it before the generated model applies its own transform and centering.
            pose.translate(.5, .5, .5);
            minecraft.getItemRenderer().render(stack, context, false, pose, buffers, light, overlay, model);
            pose.popPose();
            return;
        }
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
                // The 1.20 item-in-hand renderer already applies the vanilla drawing pose.
                // Counter its edgeward placement so the compound bow stays visible on screen.
                if (drawing) pose.translate(left ? .38 : -.38, .28, 0);
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
        var consumer = net.minecraft.client.renderer.entity.ItemRenderer.getFoilBufferDirect(buffers,
                RenderType.entityCutoutNoCull(SuitItemRenderContext.textureOr(TEXTURE)), true, stack.hasFoil());
        bottom.render(pose, consumer, light, overlay);
        base.render(pose, consumer, light, overlay);
        top.render(pose, consumer, light, overlay);
        if (!ItemCompoundBow.isBroken(stack)) drawStrings(pose, buffers, arches);
        if (drawing && wearer instanceof net.minecraft.world.entity.player.Player player)
            drawArrow(player, pose, buffers, light, arches);
        else if (displayedOwner != null && !displayedOwner.isUsingItem())
        {
            displayedArrow = null; displayedOwner = null; displayedAmmo = ItemStack.EMPTY;
        }
        pose.popPose();
    }

    /** Reuse the actual projectile renderer and selected arrow's payload while the bow is drawn. */
    private void drawArrow(net.minecraft.world.entity.player.Player player, PoseStack pose, MultiBufferSource buffers,
            int light, float draw)
    {
        ItemStack ammo = ItemCompoundBow.getArrowForDisplay(player);
        if (ammo.isEmpty()) return;
        if (displayedArrow == null || displayedOwner != player || displayedArrow.level() != player.level()
                || !ItemStack.isSameItemSameTags(displayedAmmo, ammo))
        {
            displayedOwner = player;
            displayedAmmo = ammo.copyWithCount(1);
            if (!(ammo.getItem() instanceof net.minecraft.world.item.ArrowItem arrowItem)) return;
            // Each ArrowItem supplies its own entity and renderer (e.g. vanilla spectral arrows).
            displayedArrow = arrowItem.createArrow(player.level(), ammo, player);
            displayedArrow.setYRot(0); displayedArrow.yRotO = 0;
            displayedArrow.setXRot(0); displayedArrow.xRotO = 0;
        }
        float y1 = .91F - .55F * draw, y2 = -2.3F + .55F * draw;
        float z = .625F + Mth.sin(1.075F + draw) - .9F;
        pose.pushPose();
        pose.translate(0, (y1 + y2) / 2 - .15F, z + draw * 1.3F);
        pose.scale(2.3F, 2.3F, 2.3F);
        var renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(displayedArrow);
        renderer.render(displayedArrow, 0, Minecraft.getInstance().getFrameTime(), pose, buffers, light);
        pose.popPose();
        String type = displayedArrow instanceof com.fiskmods.heroes.common.entity.arrow.TrickArrowEntity trick
                ? trick.getArrowType() : net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(ammo.getItem()).toString();
        if (Boolean.getBoolean("fiskheroes.ciBowDiagnostics") && LOGGED.add("arrow|" + player.getUUID() + "|" + type))
            FiskHeroes.LOGGER.info("Compound bow nocked arrow rendered: type={}, wearer={}",
                    type, player.getName().getString());
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
