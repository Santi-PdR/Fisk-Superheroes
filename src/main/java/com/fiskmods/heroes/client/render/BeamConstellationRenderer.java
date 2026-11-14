package com.fiskmods.heroes.client.render;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.data.var.Vars;
import com.fiskmods.heroes.common.util.BeamLayout;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Shared model-defined beam constellation for heat vision, projection and charged beams. */
final class BeamConstellationRenderer
{
    private static final Map<JsonObject, BeamLayout> LAYOUTS = new IdentityHashMap<>();
    private static final BeamLayout DEFAULT_HEAT = BeamLayout.parse(null, true);
    private static final BeamLayout DEFAULT_OTHER = BeamLayout.parse(null, false);
    private static final java.util.Set<String> DIAGNOSTICS = new java.util.HashSet<>();
    private BeamConstellationRenderer() { }
    static void clear() { LAYOUTS.clear(); DIAGNOSTICS.clear(); }

    static void render(PoseStack pose, MultiBufferSource.BufferSource buffers, Vec3 camera, Player player,
            JsonObject effect, double length, float activity, float partialTick, boolean heat, int color)
    {
        BeamLayout layout = effect == null ? (heat ? DEFAULT_HEAT : DEFAULT_OTHER)
                : LAYOUTS.computeIfAbsent(effect, value -> BeamLayout.parse(value, heat));
        boolean first = player == Minecraft.getInstance().player && Minecraft.getInstance().options.getCameraType().isFirstPerson();
        Vec3 direction = player.getViewVector(partialTick).normalize();
        Vec3 right = direction.cross(new Vec3(0, 1, 0));
        right = right.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : right.normalize();
        Vec3 up = right.cross(direction).normalize();
        Vec3 eye = player.getEyePosition(partialTick);
        Vec3 end = eye.add(direction.scale(length)).subtract(camera);
        float scale = Vars.getScale(player);
        int alpha = Mth.clamp(Math.round(activity * 230.0F), 0, 230);
        VertexConsumer consumer = buffers.getBuffer(RenderType.lightning());
        for (BeamLayout.Beam beam : layout.beams())
        {
            Vec3 offset = first ? beam.firstPerson() : beam.offset();
            Vec3 origin = first ? eye : anchor(player, partialTick, beam.anchor(), right, scale);
            Vec3 start = origin.add(right.scale(offset.x * scale / 16.0D))
                    .add(up.scale(-offset.y * scale / 16.0D))
                    .add(direction.scale(-offset.z * scale / 16.0D)).subtract(camera);
            float width = Mth.clamp(beam.width() * scale / 32.0F, 0.006F, 0.35F);
            float height = Mth.clamp(beam.height() * scale / 32.0F, 0.006F, 0.35F);
            ribbon(consumer, pose.last(), start, end, right.scale(width), color, alpha);
            ribbon(consumer, pose.last(), start, end, up.scale(height), color, alpha);
        }
        if (Boolean.getBoolean("fiskheroes.ciParticleDiagnostics"))
        {
            var data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(player);
            var hero = data != null ? data.getHero() : null;
            String key = (hero == null ? "none" : hero.getRegistryName()) + "|" + heat + "|" + first;
            if (DIAGNOSTICS.add(key))
                FiskHeroes.LOGGER.info("Beam constellation rendered: hero={}, heat={}, firstPerson={}, beams={}",
                        hero == null ? "none" : hero.getRegistryName(), heat, first, layout.beams().size());
        }
    }

    private static Vec3 anchor(Player player, float partialTick, String anchor, Vec3 right, float scale)
    {
        Vec3 base = player.getPosition(partialTick);
        return switch (anchor.toLowerCase(java.util.Locale.ROOT))
        {
            case "rightarm" -> base.add(0, 1.375D * scale, 0).add(right.scale(.3125D * scale));
            case "leftarm" -> base.add(0, 1.375D * scale, 0).add(right.scale(-.3125D * scale));
            case "rightleg" -> base.add(0, .75D * scale, 0).add(right.scale(.125D * scale));
            case "leftleg" -> base.add(0, .75D * scale, 0).add(right.scale(-.125D * scale));
            default -> base.add(0, 1.5D * scale, 0);
        };
    }

    private static void ribbon(VertexConsumer consumer, PoseStack.Pose pose, Vec3 start, Vec3 end, Vec3 side, int color, int alpha)
    {
        vertex(consumer, pose, start.subtract(side), color, alpha);
        vertex(consumer, pose, end.subtract(side), color, alpha);
        vertex(consumer, pose, end.add(side), color, alpha);
        vertex(consumer, pose, start.add(side), color, alpha);
    }
    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, Vec3 point, int color, int alpha)
    {
        consumer.vertex(pose.pose(), (float) point.x, (float) point.y, (float) point.z)
                .color(color >> 16 & 255, color >> 8 & 255, color & 255, alpha).endVertex();
    }
}
