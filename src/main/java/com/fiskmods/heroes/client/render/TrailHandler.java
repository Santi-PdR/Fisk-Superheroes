package com.fiskmods.heroes.client.render;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
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
    private static final Map<UUID, Deque<FlickerBurst>> FLICKERS = new HashMap<>();

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

    public static List<FlickerBurst> getFlickers(UUID player)
    {
        Deque<FlickerBurst> bursts = FLICKERS.get(player);
        return bursts != null ? List.copyOf(bursts) : List.of();
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
        FLICKERS.remove(player);
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event)
    {
        STATES.clear();
        FLICKERS.clear();
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END || !event.player.level().isClientSide) return;
        Player player = event.player;
        HeroTracker.update(player);
        HeroIteration iteration = HeroTracker.getHero(player);
        HeroModelData model = HeroModelRegistry.get(iteration);
        TrailDefinition configured = model != null ? model.getTrail(player, false) : null;
        TrailDefinition active = model != null ? model.getTrail(player) : null;
        tick(player, active);
        tickFlicker(player, configured, active != null, iteration != null);
    }

    private static void tickFlicker(Player player, @Nullable TrailDefinition definition, boolean active, boolean wearingHero)
    {
        Deque<FlickerBurst> bursts = FLICKERS.get(player.getUUID());
        if (bursts != null)
        {
            for (FlickerBurst burst : bursts) ++burst.age;
            bursts.removeIf(burst -> burst.age >= 4);
            if (bursts.isEmpty()) FLICKERS.remove(player.getUUID());
        }

        if (definition == null || definition.flicker() == null) return;
        var flicker = definition.flicker();
        boolean confined = flicker.has("confined") && flicker.get("confined").getAsBoolean();
        if (!active && (confined || !wearingHero)) return;
        float frequency = flicker.has("frequency") ? flicker.get("frequency").getAsFloat() : 0.4F;
        if (Math.random() >= frequency) return;

        int density = Math.max(0, flicker.has("density") ? flicker.get("density").getAsInt() : 16);
        float spread = flicker.has("spread") ? flicker.get("spread").getAsFloat() : 1.0F;
        float length = flicker.has("length") ? flicker.get("length").getAsFloat() : 0.1F;
        float scale = com.fiskmods.heroes.common.data.var.Vars.getScale(player);
        float width = player.getBbWidth();
        float height = player.getBbHeight();
        Random random = new Random();
        int color = flickerColor(definition, player);
        float opacity = flicker.has("opacity") ? flicker.get("opacity").getAsFloat() : 1.0F;
        for (int i = 0; i < density; ++i)
        {
            double x;
            double y;
            double z;
            do
            {
                x = player.getRandom().nextFloat() * 2.0F - 1.0F;
                y = player.getRandom().nextFloat() * 2.0F - 1.0F;
                z = player.getRandom().nextFloat() * 2.0F - 1.0F;
            }
            while (x * x + y * y + z * z < 1.0D);

            Vec3 offset = new Vec3(x * width / 4.0D * spread,
                    y * spread * scale * height + height / 2.0D,
                    z * width / 4.0D * spread);
            LightningNode lightning = createLightning(random, length * scale, scale, color, 0);
            FLICKERS.computeIfAbsent(player.getUUID(), ignored -> new ArrayDeque<>())
                    .addLast(new FlickerBurst(offset, lightning, color, opacity));
        }
    }

    private static int flickerColor(TrailDefinition trail, Player player)
    {
        if (trail.id().getPath().startsWith("builtin/lightning_rgb_"))
        {
            int step = 1;
            try { step = Integer.parseInt(trail.id().getPath().substring("builtin/lightning_rgb_".length())); }
            catch (NumberFormatException ignored) {}
            float hue = (player.tickCount * step / 360.0F) % 1.0F;
            return java.awt.Color.HSBtoRGB(hue, 1.0F, 1.0F) & 0xFFFFFF;
        }
        var flicker = trail.flicker();
        String color = trail.resolveConstant(flicker.has("color") ? flicker.get("color").getAsString() : null);
        try { return color != null ? Long.decode(color).intValue() & 0xFFFFFF : 0xFFFFFF; }
        catch (NumberFormatException ignored) { return 0xFFFFFF; }
    }

    private static LightningNode createLightning(Random random, float length, float scale, int color, int branch)
    {
        float nodeLength = random.nextFloat() * length;
        LightningNode node = new LightningNode(nodeLength, scale, random.nextFloat() * 360.0F,
                random.nextFloat() * 360.0F, random.nextFloat() * 360.0F, color);
        branchLightning(node, random, length, scale, branch);
        return node;
    }

    private static void branchLightning(LightningNode parent, Random random, float length, float scale, int branch)
    {
        LightningNode child = new LightningNode(random.nextFloat() * length, scale,
                random.nextFloat() * random.nextFloat() * 90.0F,
                random.nextFloat() * random.nextFloat() * 90.0F,
                random.nextFloat() * random.nextFloat() * 90.0F, parent.color);
        parent.children.add(child);
        if (branch < 10 && random.nextDouble() < 1.0D - branch * 0.1D)
        {
            branchLightning(child, random, length, scale, branch + 1);
        }
        if (random.nextDouble() < 0.1D)
        {
            branchLightning(parent, random, length, scale, 7);
        }
    }

    public static final class FlickerBurst
    {
        private final Vec3 offset;
        private final LightningNode lightning;
        private final int color;
        private final float opacity;
        private int age;

        private FlickerBurst(Vec3 offset, LightningNode lightning, int color, float opacity)
        {
            this.offset = offset;
            this.lightning = lightning;
            this.color = color;
            this.opacity = opacity;
        }

        public Vec3 offset() { return offset; }
        public LightningNode lightning() { return lightning; }
        public int color() { return color; }
        public float opacity() { return opacity; }
        public int age() { return age; }
    }

    public static final class LightningNode
    {
        private final float length;
        private final float scale;
        private final float rotateX;
        private final float rotateY;
        private final float rotateZ;
        private final int color;
        private final List<LightningNode> children = new ArrayList<>();

        private LightningNode(float length, float scale, float rotateX, float rotateY, float rotateZ, int color)
        {
            this.length = length;
            this.scale = scale;
            this.rotateX = rotateX;
            this.rotateY = rotateY;
            this.rotateZ = rotateZ;
            this.color = color;
        }

        public float length() { return length; }
        public float scale() { return scale; }
        public float rotateX() { return rotateX; }
        public float rotateY() { return rotateY; }
        public float rotateZ() { return rotateZ; }
        public int color() { return color; }
        public List<LightningNode> children() { return List.copyOf(children); }
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
