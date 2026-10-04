package com.fiskmods.heroes.client.render;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.hero.HeroIteration;
import com.fiskmods.heroes.common.hero.HeroTracker;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Client-side movement history used by the original speed-trail effects. */
@Mod.EventBusSubscriber(modid = FiskHeroes.MODID, value = Dist.CLIENT)
public final class TrailHandler
{
    private static final Map<UUID, TrailState> STATES = new HashMap<>();

    private TrailHandler() {}

    public static void tick(Player player, @Nullable TrailDefinition definition)
    {
        TrailState state = STATES.get(player.getUUID());
        if (definition == null)
        {
            if (state != null)
            {
                state.age();
                if (state.samples.isEmpty()) STATES.remove(player.getUUID());
            }
            return;
        }

        if (state == null || !state.definition.id().equals(definition.id()))
        {
            state = new TrailState(definition);
            STATES.put(player.getUUID(), state);
        }
        state.tick(player.position(), player.getBbWidth());
    }

    public static List<Sample> getSamples(UUID player)
    {
        TrailState state = STATES.get(player);
        return state != null ? List.copyOf(state.samples) : List.of();
    }

    @Nullable
    public static TrailDefinition getDefinition(UUID player)
    {
        TrailState state = STATES.get(player);
        return state != null && !state.samples.isEmpty() ? state.definition : null;
    }

    public static void clear(UUID player)
    {
        STATES.remove(player);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END || !event.player.level().isClientSide) return;
        Player player = event.player;
        HeroTracker.update(player);
        HeroIteration iteration = HeroTracker.getHero(player);
        HeroModelData model = iteration != null ? HeroModelRegistry.get(iteration.getHero().getRegistryName()) : null;
        tick(player, model != null ? model.getTrail(player) : null);
    }

    public record Sample(Vec3 position, float[] lightningFactor, int age)
    {
        public Sample
        {
            lightningFactor = lightningFactor.clone();
        }

        @Override
        public float[] lightningFactor()
        {
            return lightningFactor.clone();
        }
    }

    private static final class TrailState
    {
        private final TrailDefinition definition;
        private final Deque<Sample> samples = new ArrayDeque<>();
        private Vec3 lastPosition;
        private double distance;

        private TrailState(TrailDefinition definition)
        {
            this.definition = definition;
        }

        private void tick(Vec3 position, float width)
        {
            age();
            if (lastPosition != null)
            {
                distance += position.distanceTo(lastPosition);
                if (distance >= width * 1.1D)
                {
                    add(position, width);
                    distance = 0.0D;
                }
            }
            lastPosition = position;
        }

        private void add(Vec3 position, float width)
        {
            int density = definition.lightning() != null
                    ? Math.max(0, definition.lightning().has("density") ? definition.lightning().get("density").getAsInt() : 6) : 0;
            float[] factors = new float[density * 2 + 1];
            for (int i = 0; i < factors.length; ++i)
            {
                factors[i] = (float) ((Math.random() - 0.5D) * width);
            }
            samples.addLast(new Sample(position, factors, 0));
        }

        private void age()
        {
            if (samples.isEmpty()) return;
            int fade = definition.fade();
            if (definition.particles() != null && definition.particles().has("fade"))
            {
                fade = Math.max(fade, definition.particles().get("fade").getAsInt());
            }
            List<Sample> aged = new ArrayList<>(samples.size());
            for (Sample sample : samples)
            {
                if (sample.age() + 1 < fade)
                {
                    aged.add(new Sample(sample.position(), sample.lightningFactor(), sample.age() + 1));
                }
            }
            samples.clear();
            samples.addAll(aged);
        }
    }
}
