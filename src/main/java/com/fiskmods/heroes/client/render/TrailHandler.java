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
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
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
        state.tick(player, player.position(), player.getBbWidth());
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
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event)
    {
        STATES.clear();
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

    public static final class Sample
    {
        private final Vec3 position;
        private final float[] lightningFactor;
        private final float[] particleFactor;
        private final float[][] particleMotion;
        private final float[][] particleOffset;
        private final float[][] previousParticleOffset;
        private final float particleSpeed;
        private final PoseSnapshot pose;
        private final float bodyYaw;
        private int age;

        private Sample(Player player, Vec3 position, float[] lightningFactor, TrailDefinition definition, float width)
        {
            this.position = position;
            this.lightningFactor = lightningFactor;
            this.pose = PoseSnapshot.capture(player);
            this.bodyYaw = player.yBodyRot;
            int density = definition.particles() != null && definition.particles().has("density")
                    ? Math.max(1, definition.particles().get("density").getAsInt()) : 0;
            particleFactor = new float[definition.particles() == null ? 0 : density * 2 + 1];
            particleMotion = new float[density][3];
            particleOffset = new float[density][3];
            previousParticleOffset = new float[density][3];
            particleSpeed = definition.particles() != null && definition.particles().has("speed")
                    ? definition.particles().get("speed").getAsFloat() : 1.0F;
            for (int i = 0; i < particleFactor.length; ++i)
            {
                particleFactor[i] = (float) ((Math.random() - 0.5D) * width);
            }
            float motion = definition.particles() != null && definition.particles().has("motion")
                    ? definition.particles().get("motion").getAsFloat() : 0.0F;
            for (int i = 0; i < particleMotion.length; ++i)
            {
                for (int axis = 0; axis < 3; ++axis)
                {
                    particleMotion[i][axis] = (float) (Math.random() * 2.0D - 1.0D) * motion;
                }
            }
        }

        public Vec3 position() { return position; }
        public int age() { return age; }
        public float[] lightningFactor() { return lightningFactor; }
        public int particleDensity() { return particleMotion.length; }
        public float particleFactor(int index) { return particleFactor[index % particleFactor.length]; }

        public double[] particleOffset(int index, float partialTick)
        {
            int i = Math.floorMod(index, particleOffset.length);
            return new double[] {
                    Mth.lerp(partialTick, previousParticleOffset[i][0], particleOffset[i][0]),
                    Mth.lerp(partialTick, previousParticleOffset[i][1], particleOffset[i][1]),
                    Mth.lerp(partialTick, previousParticleOffset[i][2], particleOffset[i][2])
            };
        }

        public PoseSnapshot pose() { return pose; }
        public float bodyYaw() { return bodyYaw; }

        private void tick()
        {
            ++age;
            for (int i = 0; i < particleMotion.length; ++i)
            {
                for (int axis = 0; axis < 3; ++axis)
                {
                    previousParticleOffset[i][axis] = particleOffset[i][axis];
                    particleOffset[i][axis] += particleMotion[i][axis];
                    particleMotion[i][axis] *= particleSpeed;
                }
            }
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

        private void tick(Player player, Vec3 position, float width)
        {
            age();
            if (lastPosition != null)
            {
                distance += position.distanceTo(lastPosition);
                if (distance >= width * 1.1D)
                {
                    add(player, position, width);
                    distance = 0.0D;
                }
            }
            lastPosition = position;
        }

        private void add(Player player, Vec3 position, float width)
        {
            int density = definition.lightning() != null
                    ? Math.max(0, definition.lightning().has("density") ? definition.lightning().get("density").getAsInt() : 6) : 0;
            float[] factors = new float[density * 2 + 1];
            for (int i = 0; i < factors.length; ++i)
            {
                factors[i] = (float) ((Math.random() - 0.5D) * width);
            }
            samples.addLast(new Sample(player, position, factors, definition, width));
        }

        private void age()
        {
            if (samples.isEmpty()) return;
            int fade = definition.fade();
            if (definition.particles() != null && definition.particles().has("fade"))
            {
                fade = Math.max(fade, definition.particles().get("fade").getAsInt());
            }
            List<Sample> expired = new ArrayList<>();
            for (Sample sample : samples)
            {
                sample.tick();
                if (sample.age() >= fade) expired.add(sample);
            }
            samples.removeAll(expired);
        }
    }

    public static final class PoseSnapshot
    {
        private final float[][] parts;

        private PoseSnapshot(float[][] parts)
        {
            this.parts = parts;
        }

        @Nullable
        private static PoseSnapshot capture(Player player)
        {
            Object renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player);
            if (!(renderer instanceof PlayerRenderer playerRenderer)) return null;
            PlayerModel<?> model = playerRenderer.getModel();
            return new PoseSnapshot(new float[][] { part(model.head), part(model.body), part(model.rightArm),
                    part(model.leftArm), part(model.rightLeg), part(model.leftLeg) });
        }

        private static float[] part(ModelPart part)
        {
            return new float[] { part.x, part.y, part.z, part.xRot, part.yRot, part.zRot,
                    part.xScale, part.yScale, part.zScale };
        }

        public void apply(PlayerModel<?> model)
        {
            ModelPart[] targets = { model.head, model.body, model.rightArm, model.leftArm, model.rightLeg, model.leftLeg };
            for (int i = 0; i < targets.length; ++i)
            {
                float[] values = parts[i];
                ModelPart part = targets[i];
                part.setPos(values[0], values[1], values[2]);
                part.setRotation(values[3], values[4], values[5]);
                part.xScale = values[6];
                part.yScale = values[7];
                part.zScale = values[8];
            }
        }

    }
}
