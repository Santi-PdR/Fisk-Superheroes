package com.fiskmods.heroes.client.render;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.data.var.Vars;
import com.fiskmods.heroes.common.hero.HeroIteration;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Renders the model-declared translucent forcefield while its shield key is held. */
@Mod.EventBusSubscriber(modid = FiskHeroes.MODID, value = Dist.CLIENT)
public final class ForcefieldRenderHandler
{
    private ForcefieldRenderHandler()
    {
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event)
    {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES)
        {
            return;
        }

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null)
        {
            return;
        }

        var buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        boolean rendered = false;
        for (Player player : level.players())
        {
            SHPlayerData data = SHDataCapabilities.getPlayer(player);
            if (data == null)
            {
                continue;
            }

            HeroIteration iteration = data.getHero();
            HeroModelData model = HeroModelRegistry.get(iteration);
            if (model == null)
            {
                continue;
            }

            var effect = model.getCustom().get("fiskheroes:forcefield");
            if (effect == null)
            {
                continue;
            }

            float progress = Mth.clamp(model.evaluateRenderData(effect.get("data"), player, 0.0F), 0.0F, 1.0F);
            if (progress <= 0.0F)
            {
                continue;
            }

            int color = 0xFFD3A8;
            if (effect.has("color"))
            {
                try
                {
                    color = (int) Long.decode(effect.get("color").getAsString()).longValue();
                }
                catch (NumberFormatException ignored)
                {
                    // Keep the model format's default forcefield tint for malformed colors.
                }
            }

            int slices = 36;
            int stacks = 18;
            if (effect.has("shape") && effect.get("shape").isJsonArray()
                    && effect.getAsJsonArray("shape").size() >= 2)
            {
                slices = Mth.clamp(effect.getAsJsonArray("shape").get(0).getAsInt(), 8, 128);
                stacks = Mth.clamp(effect.getAsJsonArray("shape").get(1).getAsInt(), 4, 64);
            }

            Vec3 camera = event.getCamera().getPosition();
            Vec3 position = player.getPosition(event.getPartialTick()).subtract(camera);
            PoseStack poseStack = event.getPoseStack();
            poseStack.pushPose();
            poseStack.translate(position.x, position.y + player.getBbHeight() * 0.5D, position.z);

            if (effect.has("translation") && effect.get("translation").isJsonArray()
                    && effect.getAsJsonArray("translation").size() >= 3)
            {
                poseStack.translate(effect.getAsJsonArray("translation").get(0).getAsFloat() / 16.0F,
                        effect.getAsJsonArray("translation").get(1).getAsFloat() / 16.0F,
                        effect.getAsJsonArray("translation").get(2).getAsFloat() / 16.0F);
            }

            float scaleX = 1.25F;
            float scaleY = 1.25F;
            float scaleZ = 1.25F;
            if (effect.has("scale") && effect.get("scale").isJsonArray()
                    && effect.getAsJsonArray("scale").size() >= 3)
            {
                scaleX = effect.getAsJsonArray("scale").get(0).getAsFloat();
                scaleY = effect.getAsJsonArray("scale").get(1).getAsFloat();
                scaleZ = effect.getAsJsonArray("scale").get(2).getAsFloat();
            }
            poseStack.scale(scaleX, scaleY, scaleZ);

            VertexConsumer consumer = buffers.getBuffer(RenderType.lightning());
            drawSphere(consumer, poseStack.last(), color, progress, slices, stacks);
            poseStack.popPose();
            rendered = true;
        }

        if (rendered)
        {
            buffers.endBatch(RenderType.lightning());
        }
    }

    private static void drawSphere(VertexConsumer consumer, PoseStack.Pose pose, int color,
            float progress, int slices, int stacks)
    {
        int red = color >> 16 & 255;
        int green = color >> 8 & 255;
        int blue = color & 255;
        int alpha = Mth.clamp(Math.round(progress * 255.0F), 0, 255);

        for (int stack = 0; stack < stacks; ++stack)
        {
            double top = Math.PI * stack / stacks;
            double bottom = Math.PI * (stack + 1) / stacks;
            for (int slice = 0; slice < slices; ++slice)
            {
                double left = Math.PI * 2.0D * slice / slices;
                double right = Math.PI * 2.0D * (slice + 1) / slices;
                vertex(consumer, pose, point(top, left), red, green, blue, alpha);
                vertex(consumer, pose, point(bottom, left), red, green, blue, alpha);
                vertex(consumer, pose, point(bottom, right), red, green, blue, alpha);
                vertex(consumer, pose, point(top, right), red, green, blue, alpha);
            }
        }
    }

    private static Vec3 point(double latitude, double longitude)
    {
        return new Vec3(Math.sin(latitude) * Math.cos(longitude), Math.cos(latitude),
                Math.sin(latitude) * Math.sin(longitude));
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, Vec3 point,
            int red, int green, int blue, int alpha)
    {
        consumer.vertex(pose.pose(), (float) point.x, (float) point.y, (float) point.z)
                .color(red, green, blue, alpha)
                .endVertex();
    }
}
