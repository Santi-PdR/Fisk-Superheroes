package com.fiskmods.heroes.client.render;

import com.fiskmods.heroes.FiskHeroes;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Short component beams in the actual animated suit-part coordinate system. */
final class ComponentBeamRenderer
{
    private record Component(Vec3 offset, Vec3 first, Vec3 direction, float width) { }
    private record Layout(String anchor, boolean mirror, Vec3 offset, Vec3 rotation, int color, List<Component> beams) { }
    private static final Map<JsonObject, Layout> LAYOUTS = new IdentityHashMap<>();
    private static final java.util.Set<String> DIAGNOSTICS = new java.util.HashSet<>();
    private ComponentBeamRenderer() { }
    static void clear() { LAYOUTS.clear(); DIAGNOSTICS.clear(); }

    static void render(JsonObject effect, PoseStack pose, MultiBufferSource buffers, AbstractClientPlayer player,
            PlayerModel<?> model, HeroModelData suit, int slot, float opacity, Boolean firstRight, float partialTick)
    {
        if (!HeroSuitLayer.appliesToSlot(effect, slot) || !HeroSuitLayer.passesConditionals(effect, suit, player)) return;
        float amount = Mth.clamp(suit.evaluateRenderData(effect.get("length"), player, 0), 0, 1);
        if (!(amount > .001F) || opacity <= .001F) return;
        Layout layout = LAYOUTS.computeIfAbsent(effect, ComponentBeamRenderer::parse);
        if (layout.beams().isEmpty()) return;
        if (firstRight == null || (firstRight && layout.anchor().equalsIgnoreCase("rightArm"))
                || (!firstRight && layout.anchor().equalsIgnoreCase("leftArm")))
            draw(pose, buffers, player, model, layout, layout.anchor(), false, firstRight != null, amount, opacity, partialTick);
        if (layout.mirror())
        {
            String other = opposite(layout.anchor());
            if (firstRight == null || (firstRight && other.equalsIgnoreCase("rightArm"))
                    || (!firstRight && other.equalsIgnoreCase("leftArm")))
                draw(pose, buffers, player, model, layout, other, true, firstRight != null, amount, opacity, partialTick);
        }
        if (Boolean.getBoolean("fiskheroes.ciParticleDiagnostics")
                && DIAGNOSTICS.add(suit.getHero() + "|" + (firstRight != null)))
            FiskHeroes.LOGGER.info("Component beam rendered: hero={}, firstPerson={}, amount={}, beams={}",
                    suit.getHero(), firstRight != null, amount, layout.beams().size());
    }

    private static void draw(PoseStack pose, MultiBufferSource buffers, AbstractClientPlayer player, PlayerModel<?> model,
            Layout layout, String anchorName, boolean mirror, boolean first, float amount, float opacity, float partialTick)
    {
        ModelPart part = BoosterFlameRenderer.HeroSuitAnchor.get(model, anchorName);
        if (part == null) return;
        pose.pushPose();
        part.translateAndRotate(pose);
        pose.scale(1F / 16F, 1F / 16F, 1F / 16F);
        Vec3 p = layout.offset(), r = layout.rotation();
        pose.translate(mirror ? p.x : -p.x, p.y, p.z);
        pose.mulPose(Axis.XP.rotationDegrees((float) r.x));
        pose.mulPose(Axis.YP.rotationDegrees((float) (mirror ? -r.y : r.y)));
        pose.mulPose(Axis.ZP.rotationDegrees((float) r.z));
        VertexConsumer vertex = buffers.getBuffer(RenderType.lightning());
        int i = 0;
        for (Component beam : layout.beams())
        {
            Vec3 start = first ? beam.first() : beam.offset();
            Vec3 delta = beam.direction().scale(amount);
            Vec3 forward = delta.normalize();
            Vec3 side = forward.cross(new Vec3(0, 1, 0));
            if (side.lengthSqr() < 1e-8) side = new Vec3(1, 0, 0);
            else side = side.normalize();
            Vec3 up = forward.cross(side).normalize();
            Vec3 previous = start;
            for (int n = 1; n <= 3; ++n)
            {
                double wobble = n == 3 ? 0 : Math.sin((player.tickCount + partialTick) * .9 + i * 4.7 + n * 3) * .15;
                Vec3 next = start.add(delta.scale(n / 3D)).add(side.scale(wobble));
                float width = Math.max(.1F, beam.width() * .5F);
                ribbon(vertex, pose.last(), previous, next, side.scale(width * 2.5), layout.color(), (int)(opacity * 75));
                ribbon(vertex, pose.last(), previous, next, up.scale(width * 2.5), layout.color(), (int)(opacity * 75));
                ribbon(vertex, pose.last(), previous, next, side.scale(width), 0xD9F5FF, (int)(opacity * 220));
                ribbon(vertex, pose.last(), previous, next, up.scale(width), 0xD9F5FF, (int)(opacity * 220));
                previous = next;
            }
            ++i;
        }
        pose.popPose();
    }

    private static void ribbon(VertexConsumer vertex, PoseStack.Pose pose, Vec3 a, Vec3 b, Vec3 side, int color, int alpha)
    {
        put(vertex, pose, a.subtract(side), color, alpha); put(vertex, pose, b.subtract(side), color, alpha);
        put(vertex, pose, b.add(side), color, alpha); put(vertex, pose, a.add(side), color, alpha);
    }
    private static void put(VertexConsumer vertex, PoseStack.Pose pose, Vec3 p, int color, int alpha)
    {
        vertex.vertex(pose.pose(), (float) p.x, (float) p.y, (float) p.z)
                .color(color >> 16 & 255, color >> 8 & 255, color & 255, Mth.clamp(alpha, 0, 255)).endVertex();
    }
    private static Layout parse(JsonObject json)
    {
        List<Component> beams = new ArrayList<>();
        if (json.has("componentBeams") && json.get("componentBeams").isJsonArray())
            for (JsonElement e : json.getAsJsonArray("componentBeams"))
            {
                if (!e.isJsonObject()) continue;
                JsonObject c = e.getAsJsonObject();
                if (!c.has("beam") || !c.get("beam").isJsonObject()) continue;
                JsonObject b = c.getAsJsonObject("beam");
                Vec3 offset = vector(b.get("offset"), Vec3.ZERO);
                Vec3 first = vector(b.get("firstPerson"), offset);
                JsonArray size = b.has("size") && b.get("size").isJsonArray() ? b.getAsJsonArray("size") : null;
                beams.add(new Component(offset, first, vector(c.get("direction"), Vec3.ZERO), number(size, 0, 1)));
            }
        int color = 0xFFFFFF;
        try { if (json.has("color")) color = (int) Long.decode(json.get("color").getAsString()).longValue(); }
        catch (RuntimeException ignored) { }
        return new Layout(json.has("anchor") ? json.get("anchor").getAsString() : "rightArm",
                json.has("mirror") && json.get("mirror").getAsBoolean(), vector(json.get("offset"), Vec3.ZERO),
                vector(json.get("rotation"), Vec3.ZERO), color, List.copyOf(beams));
    }
    private static Vec3 vector(JsonElement e, Vec3 fallback)
    {
        if (e == null || !e.isJsonArray() || e.getAsJsonArray().size() < 3) return fallback;
        JsonArray a = e.getAsJsonArray(); return new Vec3(number(a, 0, 0), number(a, 1, 0), number(a, 2, 0));
    }
    private static float number(JsonArray a, int i, float fallback)
    {
        try { return a != null && a.size() > i ? a.get(i).getAsFloat() : fallback; }
        catch (RuntimeException ignored) { return fallback; }
    }
    private static String opposite(String anchor)
    {
        return switch (anchor.toLowerCase(java.util.Locale.ROOT))
        {
            case "rightarm" -> "leftArm"; case "leftarm" -> "rightArm";
            case "rightleg" -> "leftLeg"; case "leftleg" -> "rightLeg"; default -> anchor;
        };
    }
}
