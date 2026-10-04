package com.fiskmods.heroes.client.render;

import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.entity.projectile.EnergyBoltEntity;
import com.fiskmods.heroes.common.hero.HeroIteration;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Glowing bolt mesh tinted by the shooter's original energy_bolt color property. */
public final class EnergyBoltRenderer extends EntityRenderer<EnergyBoltEntity>
{
    public EnergyBoltRenderer(EntityRendererProvider.Context context)
    {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(EnergyBoltEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource buffers, int packedLight)
    {
        Vec3 forward = entity.getDeltaMovement().normalize();
        if (forward.lengthSqr() < 1.0E-6D) forward = new Vec3(0.0D, 0.0D, 1.0D);
        Vec3 right = forward.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (right.lengthSqr() < 1.0E-6D) right = new Vec3(1.0D, 0.0D, 0.0D);
        else right = right.normalize();
        Vec3 up = right.cross(forward).normalize();
        Vec3 tail = forward.scale(-0.22D);
        Vec3 tip = forward.scale(0.22D);
        int color = color(entity);
        VertexConsumer consumer = buffers.getBuffer(RenderType.lightning());
        PoseStack.Pose pose = poseStack.last();
        drawRibbon(consumer, pose, tail, tip, right.scale(0.055D), color, 245);
        drawRibbon(consumer, pose, tail, tip, up.scale(0.055D), color, 245);
        drawRibbon(consumer, pose, tail.scale(0.55D), tip.scale(0.7D), right.scale(0.024D), 0xFFFFFF, 255);
        super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);
    }

    private static int color(EnergyBoltEntity entity)
    {
        if (entity.getOwner() instanceof Player player)
        {
            var data = SHDataCapabilities.getPlayer(player);
            HeroIteration iteration = data != null ? data.getHero() : null;
            HeroModelData model = HeroModelRegistry.get(iteration);
            if (model != null)
            {
                var effect = model.getCustom().get("fiskheroes:energy_bolt");
                if (effect != null && effect.has("color"))
                {
                    try
                    {
                        return (int) Long.decode(effect.get("color").getAsString()).longValue() & 0xFFFFFF;
                    }
                    catch (NumberFormatException ignored)
                    {
                        // Fall back to the pack's default energy bolt color.
                    }
                }
            }
        }
        return 0x6EFF00;
    }

    private static void drawRibbon(VertexConsumer consumer, PoseStack.Pose pose, Vec3 start,
            Vec3 end, Vec3 side, int color, int alpha)
    {
        int red = color >> 16 & 255;
        int green = color >> 8 & 255;
        int blue = color & 255;
        vertex(consumer, pose, start.subtract(side), red, green, blue, alpha);
        vertex(consumer, pose, end.subtract(side), red, green, blue, alpha);
        vertex(consumer, pose, end.add(side), red, green, blue, alpha);
        vertex(consumer, pose, start.add(side), red, green, blue, alpha);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, Vec3 point,
            int red, int green, int blue, int alpha)
    {
        consumer.vertex(pose.pose(), (float) point.x, (float) point.y, (float) point.z)
                .color(red, green, blue, Mth.clamp(alpha, 0, 255)).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(EnergyBoltEntity entity)
    {
        return new ResourceLocation("minecraft", "textures/atlas/blocks.png");
    }
}
