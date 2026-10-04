package com.fiskmods.heroes.client.render;

import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.data.var.Vars;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Renders the active web tether with the original rope texture. */
@Mod.EventBusSubscriber(modid = com.fiskmods.heroes.FiskHeroes.MODID, value = Dist.CLIENT)
public final class WebRopeRenderer
{
    private static final ResourceLocation TEXTURE = new ResourceLocation("fiskheroes", "textures/models/web_rope.png");
    private static final double RADIUS = 0.018D;

    private WebRopeRenderer()
    {
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event)
    {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;

        RenderType renderType = RenderType.entityCutoutNoCull(TEXTURE);
        var buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer consumer = buffers.getBuffer(renderType);
        PoseStack.Pose pose = event.getPoseStack().last();
        boolean rendered = false;

        for (Player player : level.players())
        {
            SHPlayerData data = SHDataCapabilities.getPlayer(player);
            if (data == null || !data.getData().get(Vars.WEB_RAPPEL)) continue;

            Vec3 start = player.getPosition(event.getPartialTick()).add(0.0D, player.getBbHeight() * 0.78D, 0.0D);
            Vec3 end = new Vec3(data.getData().get(Vars.WEB_ANCHOR_X),
                    data.getData().get(Vars.WEB_ANCHOR_Y), data.getData().get(Vars.WEB_ANCHOR_Z));
            if (start.distanceToSqr(end) < 0.04D) continue;

            drawRope(consumer, pose, start, end);
            rendered = true;
        }

        if (rendered) buffers.endBatch(renderType);
    }

    private static void drawRope(VertexConsumer consumer, PoseStack.Pose pose, Vec3 start, Vec3 end)
    {
        double sag = Math.min(0.75D, start.distanceTo(end) * 0.018D);
        Vec3 control = start.add(end).scale(0.5D).add(0.0D, -sag, 0.0D);
        Vec3 previous = start;
        double textureV = 0.0D;
        int segments = Math.max(16, Math.min(256, (int) Math.ceil(start.distanceTo(end) * 3.0D)));

        for (int i = 1; i <= segments; ++i)
        {
            double t = (double) i / segments;
            double oneMinusT = 1.0D - t;
            Vec3 current = start.scale(oneMinusT * oneMinusT)
                    .add(control.scale(2.0D * oneMinusT * t))
                    .add(end.scale(t * t));
            double segmentLength = previous.distanceTo(current);
            drawSegment(consumer, pose, previous, current, textureV, segmentLength);
            textureV = (textureV + segmentLength * 2.0D) % 1.0D;
            previous = current;
        }
    }

    private static void drawSegment(VertexConsumer consumer, PoseStack.Pose pose, Vec3 start, Vec3 end,
            double textureV, double length)
    {
        Vec3 direction = end.subtract(start).normalize();
        Vec3 reference = Math.abs(direction.y) > 0.96D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 side = direction.cross(reference).normalize().scale(RADIUS);
        Vec3 up = direction.cross(side).normalize().scale(RADIUS);
        Vec3[] corners = { side.add(up), side.scale(-1.0D).add(up), side.scale(-1.0D).subtract(up), side.subtract(up) };
        double maxV = textureV + length * 2.0D;
        for (int face = 0; face < 4; ++face)
        {
            int nextFace = (face + 1) & 3;
            Vec3 normal = corners[face].add(corners[nextFace]).normalize();
            double minU = face * 0.25D;
            double maxU = (face + 1) * 0.25D;
            vertex(consumer, pose, start.add(corners[face]), normal, minU, textureV);
            vertex(consumer, pose, start.add(corners[nextFace]), normal, maxU, textureV);
            vertex(consumer, pose, end.add(corners[nextFace]), normal, maxU, maxV);
            vertex(consumer, pose, end.add(corners[face]), normal, minU, maxV);
        }
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, Vec3 point, Vec3 normal, double u, double v)
    {
        consumer.vertex(pose.pose(), (float) point.x, (float) point.y, (float) point.z)
                .color(255, 255, 255, 255)
                .uv((float) u, (float) v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(pose.normal(), (float) normal.x, (float) normal.y, (float) normal.z)
                .endVertex();
    }
}
