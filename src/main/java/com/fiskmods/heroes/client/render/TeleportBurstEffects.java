package com.fiskmods.heroes.client.render;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.hero.Hero;
import com.fiskmods.heroes.common.network.PacketTeleportBurst;
import com.fiskmods.heroes.common.util.CloudColors;
import com.fiskmods.heroes.common.util.TeleportBurstMath;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.resources.ResourceLocation;
import java.util.Random;

/** Resolves the hero's own teleport cloud on the client, including inherited pack models. */
public final class TeleportBurstEffects
{
    private TeleportBurstEffects() { }

    public static void spawn(PacketTeleportBurst packet)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.options.particles().get() == ParticleStatus.MINIMAL
                || mc.gameRenderer.getMainCamera().getPosition().distanceToSqr(packet.center()) > 1024.0D) return;
        Hero hero = Hero.REGISTRY.getHero(packet.hero());
        HeroModelData model = hero == null ? null : HeroModelRegistry.get(hero.getIteration(packet.iteration()));
        JsonObject effect = model == null ? null : model.getCustom().get("fiskheroes:teleportation");
        if (effect == null || !effect.has("particles") || !effect.get("particles").isJsonPrimitive()) return;
        ResourceLocation id = ResourceLocation.tryParse(effect.get("particles").getAsString());
        JsonObject cloud = id == null ? null : ParticleEmitterRenderer.cloud(id);
        if (cloud == null) return;
        boolean firstPerson = mc.player != null && mc.player.getId() == packet.entityId() && mc.options.getCameraType().isFirstPerson();
        Random random = new Random();
        int n = TeleportBurstMath.latitudeCount(firstPerson), emitted = 0;
        for (int i = 0; i < n; ++i)
        {
            for (int j = 0; j < n * 2; ++j)
            {
                if (mc.options.particles().get() == ParticleStatus.DECREASED && mc.level.random.nextInt(3) == 0) continue;
                var offset = TeleportBurstMath.offset(i, j, firstPerson, packet.width(), packet.height());
                mc.particleEngine.add(new TeleportBurstParticle(mc.level, packet.center(), offset,
                        packet.scale(), CloudColors.sample(cloud, random)));
                ++emitted;
            }
        }
        if (Boolean.getBoolean("fiskheroes.ciParticleDiagnostics"))
            FiskHeroes.LOGGER.info("Teleport burst emitted: endpoint={}, firstPerson={}, center={}, count={}, cloud={}",
                    packet.destination() ? "destination" : "origin", firstPerson, packet.center(), emitted, id);
    }
}
