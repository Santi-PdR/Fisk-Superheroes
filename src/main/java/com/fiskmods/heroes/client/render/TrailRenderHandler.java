package com.fiskmods.heroes.client.render;

import java.awt.Color;
import java.util.List;
import java.util.UUID;

import com.fiskmods.heroes.FiskHeroes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Renders the lightning aspect between the movement samples captured by {@link TrailHandler}. */
@Mod.EventBusSubscriber(modid = FiskHeroes.MODID, value = Dist.CLIENT)
public final class TrailRenderHandler
{
    private TrailRenderHandler() {}

    @SubscribeEvent
    public static void renderPlayer(RenderPlayerEvent.Post event)
    {
        Player player = event.getEntity();
        HeroIterationHolder holder = activeTrail(player);
        if (holder == null || holder.definition.lightning() == null) return;

        TrailDefinition trail = holder.definition;
        var lightning = trail.lightning();
        int density = Math.max(1, lightning.has("density") ? lightning.get("density").getAsInt() : 6);
        double differ = lightning.has("differ") ? lightning.get("differ").getAsDouble() : 0.435D;
        float opacity = lightning.has("opacity") ? lightning.get("opacity").getAsFloat() : 1.0F;
        int color = resolveColor(trail, player, event.getPartialTick(), lightning.has("color") ? lightning.get("color").getAsString() : null);
        float red = ((color >> 16) & 255) / 255.0F;
        float green = ((color >> 8) & 255) / 255.0F;
        float blue = (color & 255) / 255.0F;
        List<TrailHandler.Sample> samples = TrailHandler.getSamples(player.getUUID());
        if (samples.isEmpty()) return;

        double px = Mth.lerp(event.getPartialTick(), player.xo, player.getX());
        double py = Mth.lerp(event.getPartialTick(), player.yo, player.getY());
        double pz = Mth.lerp(event.getPartialTick(), player.zo, player.getZ());
        Vec3 current = new Vec3(px, py, pz);
        float height = player.getBbHeight();
        float scale = height / 1.8F;
        float lightningOffset = (float) (differ * scale);
        float sideScale = (float) (differ / 0.435D * player.getBbWidth() / 0.6D);
        VertexConsumer consumer = event.getMultiBufferSource().getBuffer(RenderType.lightning());
        PoseStack.Pose pose = event.getPoseStack().last();

        for (int strand = 0; strand < density; ++strand)
        {
            double yOffset = (double) strand * height / density;
            for (int i = 0; i < samples.size(); ++i)
            {
                TrailHandler.Sample startSample = samples.get(i);
                Vec3 start = lightningPoint(startSample, strand, density, sideScale, yOffset, lightningOffset);
                Vec3 end;
                float alphaEnd;
                if (i + 1 < samples.size())
                {
                    TrailHandler.Sample endSample = samples.get(i + 1);
                    end = lightningPoint(endSample, strand, density, sideScale, yOffset, lightningOffset);
                    alphaEnd = fade(endSample, trail, event.getPartialTick()) * opacity;
                }
                else
                {
                    end = current.add(0.0D, yOffset, 0.0D);
                    alphaEnd = 1.0F;
                }
                float alphaStart = fade(startSample, trail, event.getPartialTick()) * opacity;
                if (start.distanceToSqr(end) > 1.0E-5D)
                {
                    drawBolt(consumer, pose, local(start.subtract(current), player.getYRot()),
                            local(end.subtract(current), player.getYRot()), red, green, blue,
                            Math.min(alphaStart, alphaEnd));
                }
            }
        }
    }

    @SubscribeEvent
    public static void renderTrailParticles(RenderPlayerEvent.Post event)
    {
        Player player = event.getEntity();
        HeroIterationHolder holder = activeTrail(player);
        if (holder == null || holder.definition.particles() == null) return;

        TrailDefinition trail = holder.definition;
        var particles = trail.particles();
        String textureName = trail.resolveConstant(particles.has("texture") ? particles.get("texture").getAsString() : null);
        ResourceLocation texture = textureName != null ? ResourceLocation.tryParse(textureName) : null;
        if (texture == null) return;

        int density = Math.max(1, particles.has("density") ? particles.get("density").getAsInt() : 6);
        float differ = particles.has("differ") ? particles.get("differ").getAsFloat() : 0.435F;
        float opacity = particles.has("opacity") ? particles.get("opacity").getAsFloat() : 1.0F;
        float spriteScale = particles.has("scale") ? particles.get("scale").getAsFloat() : 1.0F;
        int fade = Math.max(trail.fade(), particles.has("fade") ? particles.get("fade").getAsInt() : 10);
        List<TrailHandler.Sample> samples = TrailHandler.getSamples(player.getUUID());
        if (samples.isEmpty()) return;

        double px = Mth.lerp(event.getPartialTick(), player.xo, player.getX());
        double py = Mth.lerp(event.getPartialTick(), player.yo, player.getY());
        double pz = Mth.lerp(event.getPartialTick(), player.zo, player.getZ());
        Vec3 current = new Vec3(px, py, pz);
        float height = player.getBbHeight();
        float scale = height / 1.8F;
        float sideScale = player.getBbWidth() / 0.6F;
        Vec3 cameraRight = fromVector(Minecraft.getInstance().gameRenderer.getMainCamera().getLeftVector()).scale(-1.0D);
        Vec3 cameraUp = fromVector(Minecraft.getInstance().gameRenderer.getMainCamera().getUpVector());
        VertexConsumer consumer = event.getMultiBufferSource().getBuffer(RenderType.entityTranslucent(texture));
        PoseStack.Pose pose = event.getPoseStack().last();

        for (TrailHandler.Sample sample : samples)
        {
            for (int i = 0; i < density; ++i)
            {
                double[] drift = sample.particleOffset(i, event.getPartialTick());
                float randomHeight = sample.particleFactor(i) * differ * scale;
                double spread = differ / 0.435D * sideScale;
                Vec3 center = sample.position().add(
                        sample.particleFactor(i) * spread + drift[0] * sideScale,
                        drift[1] * sideScale + (double) i * height / density + randomHeight,
                        sample.particleFactor(i + density) * spread + drift[2] * sideScale);
                Vec3 relative = center.subtract(current);
                float size = spriteScale * scale * 2.0F;
                float alpha = Mth.clamp(1.0F - (sample.age() + event.getPartialTick()) / fade, 0.0F, 1.0F) * opacity;
                if (alpha <= 0.0F) continue;

                Vec3 horizontal = cameraRight.scale(size);
                Vec3 vertical = cameraUp.scale(size);
                Vec3 a = local(relative.subtract(horizontal).subtract(vertical), player.getYRot());
                Vec3 b = local(relative.add(horizontal).subtract(vertical), player.getYRot());
                Vec3 c = local(relative.add(horizontal).add(vertical), player.getYRot());
                Vec3 d = local(relative.subtract(horizontal).add(vertical), player.getYRot());

                texturedVertex(consumer, pose, a, 0, 1, alpha, event.getPackedLight());
                texturedVertex(consumer, pose, b, 1, 1, alpha, event.getPackedLight());
                texturedVertex(consumer, pose, c, 1, 0, alpha, event.getPackedLight());
                texturedVertex(consumer, pose, d, 0, 0, alpha, event.getPackedLight());
            }
        }
    }

    private static HeroIterationHolder activeTrail(Player player)
    {
        var iteration = com.fiskmods.heroes.common.hero.HeroTracker.getHero(player);
        if (iteration == null) return null;
        HeroModelData model = HeroModelRegistry.get(iteration.getHero().getRegistryName());
        TrailDefinition trail = model != null ? model.getTrail(player) : null;
        if (trail == null) trail = TrailHandler.getDefinition(player.getUUID());
        return trail != null ? new HeroIterationHolder(trail) : null;
    }

    private static Vec3 lightningPoint(TrailHandler.Sample sample, int strand, int density,
            float sideScale, double yOffset, float lightningOffset)
    {
        float[] factors = sample.lightningFactor();
        if (factors.length == 0) return sample.position().add(0.0D, yOffset, 0.0D);
        float x = factors[strand % factors.length] * sideScale;
        float z = factors[(strand + density) % factors.length] * sideScale;
        float y = factors[Math.min(strand, factors.length - 1)] * lightningOffset;
        return sample.position().add(x, yOffset + y, z);
    }

    private static float fade(TrailHandler.Sample sample, TrailDefinition trail, float partialTick)
    {
        return Mth.clamp(1.0F - (sample.age() + partialTick) / trail.fade(), 0.0F, 1.0F);
    }

    private static int resolveColor(TrailDefinition trail, Player player, float partialTick, String source)
    {
        String value = trail.resolveConstant(source);
        if (value == null) return 0xFFFFFF;
        if (trail.id().getPath().startsWith("builtin/lightning_rgb_"))
        {
            int step = 1;
            try { step = Integer.parseInt(trail.id().getPath().substring("builtin/lightning_rgb_".length())); }
            catch (NumberFormatException ignored) {}
            float hue = ((player.tickCount + partialTick) * step / 360.0F) % 1.0F;
            return Color.HSBtoRGB(hue, 1.0F, 1.0F) & 0xFFFFFF;
        }
        try
        {
            return Long.decode(value).intValue() & 0xFFFFFF;
        }
        catch (NumberFormatException ignored)
        {
            return 0xFFFFFF;
        }
    }

    /** Transforms a world-relative delta into the player's post-render local axes. */
    private static Vec3 local(Vec3 delta, float yaw)
    {
        double radians = Math.toRadians(yaw);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return new Vec3(cos * delta.x + sin * delta.z, -delta.y, sin * delta.x - cos * delta.z);
    }

    private static Vec3 fromVector(org.joml.Vector3f vector)
    {
        return new Vec3(vector.x(), vector.y(), vector.z());
    }

    private static void texturedVertex(VertexConsumer consumer, PoseStack.Pose pose, Vec3 point,
            float u, float v, float alpha, int packedLight)
    {
        consumer.vertex(pose.pose(), (float) point.x, (float) point.y, (float) point.z)
                .color(1.0F, 1.0F, 1.0F, alpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(pose.normal(), 0.0F, 1.0F, 0.0F)
                .endVertex();
    }

    private static void drawBolt(VertexConsumer consumer, PoseStack.Pose pose, Vec3 start, Vec3 end,
            float red, float green, float blue, float alpha)
    {
        Vec3 direction = end.subtract(start).normalize();
        Vec3 side = direction.cross(new Vec3(0, 1, 0));
        if (side.lengthSqr() < 1.0E-5D) side = direction.cross(new Vec3(1, 0, 0));
        side = side.normalize().scale(0.025D);
        Vec3 up = direction.cross(side).normalize().scale(0.025D);
        Vec3[] offsets = { side.add(up), side.subtract(up), side.scale(-1).subtract(up), side.scale(-1).add(up) };

        // Six faces keep the lightning visible from either side, like the original beam renderer.
        for (int i = 0; i < 4; ++i)
        {
            Vec3 a = offsets[i];
            Vec3 b = offsets[(i + 1) % 4];
            vertex(consumer, pose, start.add(a), red, green, blue, alpha);
            vertex(consumer, pose, end.add(a), red, green, blue, alpha);
            vertex(consumer, pose, end.add(b), red, green, blue, alpha);
            vertex(consumer, pose, start.add(b), red, green, blue, alpha);
        }
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, Vec3 point,
            float red, float green, float blue, float alpha)
    {
        consumer.vertex(pose.pose(), (float) point.x, (float) point.y, (float) point.z)
                .color(red, green, blue, alpha).endVertex();
    }

    private record HeroIterationHolder(TrailDefinition definition) {}
}
