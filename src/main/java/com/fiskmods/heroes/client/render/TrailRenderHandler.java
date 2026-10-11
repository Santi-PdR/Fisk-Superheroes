package com.fiskmods.heroes.client.render;

import java.awt.Color;
import java.util.List;
import java.util.UUID;

import com.fiskmods.heroes.FiskHeroes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.mojang.math.Axis;

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
                    drawBolt(consumer, pose, start.subtract(current), end.subtract(current), red, green, blue,
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
                Vec3 a = relative.subtract(horizontal).subtract(vertical);
                Vec3 b = relative.add(horizontal).subtract(vertical);
                Vec3 c = relative.add(horizontal).add(vertical);
                Vec3 d = relative.subtract(horizontal).add(vertical);

                texturedVertex(consumer, pose, a, 0, 1, alpha, event.getPackedLight());
                texturedVertex(consumer, pose, b, 1, 1, alpha, event.getPackedLight());
                texturedVertex(consumer, pose, c, 1, 0, alpha, event.getPackedLight());
                texturedVertex(consumer, pose, d, 0, 0, alpha, event.getPackedLight());
            }
        }
    }

    @SubscribeEvent
    public static void renderTrailBlur(RenderPlayerEvent.Post event)
    {
        Player player = event.getEntity();
        if (player == Minecraft.getInstance().player && Minecraft.getInstance().options.getCameraType().isFirstPerson()) return;
        HeroIterationHolder holder = activeTrail(player);
        if (holder == null || holder.definition.blur() == null) return;

        var blur = holder.definition.blur();
        int color = resolveColor(holder.definition, player, event.getPartialTick(), blur.has("color") ? blur.get("color").getAsString() : null);
        float red = ((color >> 16) & 255) / 255.0F;
        float green = ((color >> 8) & 255) / 255.0F;
        float blue = (color & 255) / 255.0F;
        float opacity = blur.has("opacity") ? blur.get("opacity").getAsFloat() : 0.5F;
        float scale = player.getBbHeight() / 1.8F * 0.9375F;
        int fade = holder.definition.fade();
        List<TrailHandler.Sample> samples = TrailHandler.getSamples(player.getUUID());
        if (samples.isEmpty()) return;
        PlayerRenderer renderer = event.getRenderer();

        PlayerModel<?> model = renderer.getModel();
        ModelPart[] parts = { model.head, model.body, model.rightArm, model.leftArm, model.rightLeg, model.leftLeg };
        net.minecraft.client.model.geom.PartPose[] originalPoses = java.util.Arrays.stream(parts).map(ModelPart::storePose)
                .toArray(net.minecraft.client.model.geom.PartPose[]::new);
        boolean[] visible = { model.hat.visible, model.jacket.visible, model.leftSleeve.visible, model.rightSleeve.visible,
                model.leftPants.visible, model.rightPants.visible };
        model.hat.visible = false;
        model.jacket.visible = false;
        model.leftSleeve.visible = false;
        model.rightSleeve.visible = false;
        model.leftPants.visible = false;
        model.rightPants.visible = false;

        VertexConsumer consumer = event.getMultiBufferSource().getBuffer(RenderType.entityTranslucent(
                new ResourceLocation("minecraft", "textures/misc/white.png")));
        PoseStack poseStack = event.getPoseStack();
        double px = Mth.lerp(event.getPartialTick(), player.xo, player.getX());
        double py = Mth.lerp(event.getPartialTick(), player.yo, player.getY());
        double pz = Mth.lerp(event.getPartialTick(), player.zo, player.getZ());

        try
        {
            for (TrailHandler.Sample sample : samples)
            {
                TrailHandler.PoseSnapshot snapshot = sample.pose();
                if (snapshot == null) continue;
                snapshot.apply(model);

                float alpha = Mth.clamp(1.0F - (sample.age() + event.getPartialTick()) / fade, 0.0F, 1.0F) * opacity;
                if (alpha <= 0.0F) continue;

                poseStack.pushPose();
                poseStack.translate(sample.position().x - px, sample.position().y - py, sample.position().z - pz);
                poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - sample.bodyYaw()));
                poseStack.scale(-scale, -scale, scale);
                poseStack.translate(0.0D, -1.501D, 0.0D);
                try
                {
                    model.renderToBuffer(poseStack, consumer, event.getPackedLight(), OverlayTexture.NO_OVERLAY,
                            red, green, blue, alpha);
                }
                finally
                {
                    poseStack.popPose();
                }

                for (int i = 0; i < parts.length; ++i) parts[i].loadPose(originalPoses[i]);
            }
        }
        finally
        {
            for (int i = 0; i < parts.length; ++i) parts[i].loadPose(originalPoses[i]);
            model.hat.visible = visible[0];
            model.jacket.visible = visible[1];
            model.leftSleeve.visible = visible[2];
            model.rightSleeve.visible = visible[3];
            model.leftPants.visible = visible[4];
            model.rightPants.visible = visible[5];
        }
    }

    @SubscribeEvent
    public static void renderTrailFlickers(RenderPlayerEvent.Post event)
    {
        Player player = event.getEntity();
        if (player == Minecraft.getInstance().player && Minecraft.getInstance().options.getCameraType().isFirstPerson()) return;
        List<TrailHandler.FlickerBurst> bursts = TrailHandler.getFlickers(player.getUUID());
        if (bursts.isEmpty()) return;

        VertexConsumer consumer = event.getMultiBufferSource().getBuffer(RenderType.lightning());
        PoseStack poseStack = event.getPoseStack();
        float scale = player.getBbHeight() / 1.8F;
        for (TrailHandler.FlickerBurst burst : bursts)
        {
            float progress = Mth.clamp(1.0F - (burst.age() + event.getPartialTick()) / 4.0F, 0.0F, 1.0F);
            float alpha = progress * burst.opacity();
            if (alpha <= 0.0F) continue;

            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - player.yBodyRot));
            poseStack.scale(-1.0F, 1.0F, 1.0F);
            poseStack.translate(burst.offset().x, burst.offset().y - 0.21D * scale, burst.offset().z);
            try
            {
                renderLightningNode(poseStack, consumer, burst.lightning(), alpha);
            }
            finally
            {
                poseStack.popPose();
            }
        }
    }

    private static void renderLightningNode(PoseStack poseStack, VertexConsumer consumer,
            TrailHandler.LightningNode lightning, float alpha)
    {
        poseStack.pushPose();
        try
        {
            poseStack.mulPose(Axis.ZP.rotationDegrees(lightning.rotateZ()));
            poseStack.mulPose(Axis.YP.rotationDegrees(lightning.rotateY()));
            poseStack.mulPose(Axis.XP.rotationDegrees(lightning.rotateX()));

            int color = lightning.color();
            drawBolt(consumer, poseStack.last(), Vec3.ZERO, new Vec3(0.0D, lightning.length(), 0.0D),
                    ((color >> 16) & 255) / 255.0F, ((color >> 8) & 255) / 255.0F,
                    (color & 255) / 255.0F, alpha, 0.025D * lightning.scale());

            poseStack.translate(0.0D, lightning.length(), 0.0D);
            for (TrailHandler.LightningNode child : lightning.children())
            {
                renderLightningNode(poseStack, consumer, child, alpha);
            }
        }
        finally
        {
            poseStack.popPose();
        }
    }

    private static HeroIterationHolder activeTrail(Player player)
    {
        var iteration = com.fiskmods.heroes.common.hero.HeroTracker.getHero(player);
        if (iteration == null) return null;
        HeroModelData model = HeroModelRegistry.get(iteration);
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
        drawBolt(consumer, pose, start, end, red, green, blue, alpha, 0.025D);
    }

    private static void drawBolt(VertexConsumer consumer, PoseStack.Pose pose, Vec3 start, Vec3 end,
            float red, float green, float blue, float alpha, double width)
    {
        Vec3 direction = end.subtract(start).normalize();
        Vec3 side = direction.cross(new Vec3(0, 1, 0));
        if (side.lengthSqr() < 1.0E-5D) side = direction.cross(new Vec3(1, 0, 0));
        side = side.normalize().scale(width);
        Vec3 up = direction.cross(side).normalize().scale(width);
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
