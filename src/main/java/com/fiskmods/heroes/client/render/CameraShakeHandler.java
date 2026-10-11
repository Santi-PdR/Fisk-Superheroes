package com.fiskmods.heroes.client.render;

import java.util.Random;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.hero.HeroIteration;
import com.fiskmods.heroes.common.hero.HeroTracker;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Applies the camera_shake render property used by the original hero renderers. */
@Mod.EventBusSubscriber(modid = FiskHeroes.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CameraShakeHandler
{
    private static final String PROPERTY = "fiskheroes:camera_shake";

    private CameraShakeHandler()
    {
    }

    @SubscribeEvent
    public static void computeCameraAngles(ViewportEvent.ComputeCameraAngles event)
    {
        Entity cameraEntity = event.getCamera().getEntity();
        if (!(cameraEntity instanceof Player player)) return;

        HeroIteration iteration = HeroTracker.getHero(player);
        HeroModelData model = iteration != null ? HeroModelRegistry.get(iteration) : null;
        JsonObject property = model != null ? model.getCustom().get(PROPERTY) : null;
        if (property == null || !property.has("shakes") || !property.get("shakes").isJsonArray()) return;

        float yaw = 0.0F;
        float pitch = 0.0F;
        float roll = 0.0F;
        float partialTick = Mth.clamp((float) event.getPartialTick(), 0.0F, 1.0F);
        double velocity = player.getDeltaMovement().length();
        JsonArray shakes = property.getAsJsonArray("shakes");

        for (int i = 0; i < shakes.size(); ++i)
        {
            JsonElement element = shakes.get(i);
            if (!element.isJsonObject()) continue;
            JsonObject shake = element.getAsJsonObject();
            float factor = model.evaluateRenderData(shake.get("factor"), player, 0.0F);
            float intensity = shake.has("intensity") && shake.get("intensity").isJsonPrimitive()
                    ? shake.get("intensity").getAsFloat() : 1.0F;
            if (!Float.isFinite(factor) || !Float.isFinite(intensity) || factor == 0.0F) continue;

            double amplitude = factor * Math.pow(velocity, intensity);
            if (!Double.isFinite(amplitude) || amplitude == 0.0D) continue;
            float scale = (float) amplitude;
            long entitySeed = player.getUUID().getMostSignificantBits() ^ player.getUUID().getLeastSignificantBits();
            float[] previous = sample(entitySeed, player.tickCount - 1L, i);
            float[] current = sample(entitySeed, player.tickCount, i);
            yaw += scale * Mth.lerp(partialTick, previous[0], current[0]);
            pitch += scale * Mth.lerp(partialTick, previous[1], current[1]);
            roll += scale * Mth.lerp(partialTick, previous[2], current[2]);
        }

        event.setYaw(event.getYaw() + yaw);
        event.setPitch(event.getPitch() + pitch);
        event.setRoll(event.getRoll() + roll);
    }

    private static float[] sample(long entitySeed, long tick, int shakeIndex)
    {
        long seed = entitySeed ^ (tick * 0x9E3779B97F4A7C15L) ^ (shakeIndex * 0xD1B54A32D192ED03L);
        Random random = new Random(seed);
        return new float[] {
                random.nextFloat() * 2.0F - 1.0F,
                random.nextFloat() * 2.0F - 1.0F,
                random.nextFloat() * 2.0F - 1.0F
        };
    }
}
