package com.fiskmods.heroes.client.render;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.data.var.Vars;
import com.fiskmods.heroes.pack.ScriptFunction;
import com.fiskmods.heroes.pack.js.JSEntity;
import com.fiskmods.heroes.pack.js.JSExpressions;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.packs.resources.Resource;
import org.joml.Vector3f;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/** Client side execution of the original data driven suit particle emitters. */
final class ParticleEmitterRenderer
{
    private static final Map<ResourceLocation, List<Emitter>> EMITTERS = new HashMap<>();
    private static final Map<ResourceLocation, JsonObject> CLOUDS = new HashMap<>();
    private static final Map<String, ScriptFunction> CONDITIONS = new HashMap<>();
    private static final Map<UUID, Integer> LAST_TICK = new HashMap<>();
    private static final java.util.Set<String> FIRST_PERSON_CLOUD_DIAGNOSTICS = new java.util.HashSet<>();
    private static final boolean PARTICLE_DIAGNOSTICS = Boolean.getBoolean("fiskheroes.ciParticleDiagnostics");
    private static final java.util.Set<String> EMITTER_DIAGNOSTICS = new java.util.HashSet<>();
    private static final java.util.Set<String> ANCHOR_DIAGNOSTICS = new java.util.HashSet<>();

    private ParticleEmitterRenderer() { }

    static void clear()
    {
        EMITTERS.clear();
        CLOUDS.clear();
        CONDITIONS.clear();
        LAST_TICK.clear();
        FIRST_PERSON_CLOUD_DIAGNOSTICS.clear();
        EMITTER_DIAGNOSTICS.clear();
        ANCHOR_DIAGNOSTICS.clear();
        LocalParticleEmitter.clear();
        TelekinesisChainParticle.clearDiagnostics();
        TeleportBurstParticle.clearDiagnostics();
    }

    static void tick(Player player, PlayerModel<?> model, HeroModelData suit, float suitScale, PoseStack renderPose)
    {
        if (LAST_TICK.getOrDefault(player.getUUID(), Integer.MIN_VALUE) == player.tickCount) return;
        LAST_TICK.put(player.getUUID(), player.tickCount);
        if (LAST_TICK.size() > 256)
        {
            int cutoff = player.tickCount - 80;
            LAST_TICK.entrySet().removeIf(entry -> entry.getValue() < cutoff);
        }

        Minecraft mc = Minecraft.getInstance();
        emitCloud(player, suit);
        emitTelekinesisChain(player, model, suit, suitScale, renderPose);
        boolean firstPerson = player == mc.player && mc.options.getCameraType().isFirstPerson();
        for (Map.Entry<String, JsonObject> entry : suit.getCustom().entrySet())
        {
            if (!entry.getKey().startsWith("fiskheroes:particles") || !passesConditionals(entry.getValue(), suit, player)) continue;
            // The original hides these emitters for miniature local players in first person.
            if (firstPerson && suitScale < 1.0F) continue;
            if (!entry.getValue().has("particleType")) continue;
            ResourceLocation id = ResourceLocation.tryParse(entry.getValue().get("particleType").getAsString());
            if (id == null) continue;
            for (Emitter emitter : EMITTERS.computeIfAbsent(id, ParticleEmitterRenderer::load))
            {
                if (!emitter.condition(player, firstPerson)) continue;
                // Retain the existing coarse range guard for vanilla-backed particles. Custom
                // smoke is checked at the actual spawn point against its pack distance below.
                if (!emitter.smokeType() && mc.player != null
                        && mc.player.distanceToSqr(player) > emitter.maxDistance * emitter.maxDistance) continue;
                emit(player, model, suitScale, emitter, firstPerson, renderPose);
            }
        }
    }

    /** Executes the original particle_cloud and teleportation cloud directives. */
    private static void emitCloud(Player player, HeroModelData suit)
    {
        JsonObject effect = suit.getCustom().get("fiskheroes:particle_cloud");
        if (effect != null && passesConditionals(effect, suit, player)) emitCloud(player, effect);

        SHPlayerData data = SHDataCapabilities.getPlayer(player);

        // The original telekinesis aura surrounds the grabbed entity, not the caster.
        effect = suit.getCustom().get("fiskheroes:telekinesis");
        if (effect != null && data != null && data.getData().get(Vars.TELEKINESIS))
        {
            Entity target = player.level().getEntity(data.getData().get(Vars.GRAB_ID));
            if (target != null && target.isAlive()) emitTelekinesisAura(target, effect);
        }
    }

    private static void emitTelekinesisAura(Entity target, JsonObject effect)
    {
        if (!effect.has("particles") || !effect.get("particles").isJsonPrimitive()) return;
        ResourceLocation id = ResourceLocation.tryParse(effect.get("particles").getAsString());
        JsonObject cloud = id != null ? CLOUDS.computeIfAbsent(id, ParticleEmitterRenderer::loadCloud) : null;
        Minecraft mc = Minecraft.getInstance();
        if (cloud == null || mc.level == null || mc.options.particles().get() == net.minecraft.client.ParticleStatus.MINIMAL
                || mc.gameRenderer.getMainCamera().getPosition().distanceToSqr(target.position()) > 1024.0D) return;

        Random pattern = new Random(253151369061L + (long) (target.tickCount / 6) * 7694536L);
        Random colorsRandom = new Random();
        int count = Math.min(64, 1 + (int) (Math.sqrt(target.getBbWidth() * target.getBbHeight()) * 10.0D));
        for (int i = 0; i < count; ++i)
        {
            if (mc.options.particles().get() == net.minecraft.client.ParticleStatus.DECREASED && mc.level.random.nextInt(3) == 0) continue;
            Vec3 rotation = randomUnit(pattern);
            Vec3 direction = randomUnit(pattern);
            var colors = com.fiskmods.heroes.common.util.CloudColors.sample(cloud, colorsRandom);
            mc.particleEngine.add(new TelekinesisAuraParticle(mc.level, target, rotation, direction, colors));
        }
        if (PARTICLE_DIAGNOSTICS && EMITTER_DIAGNOSTICS.add("aura:" + id))
            FiskHeroes.LOGGER.info("Telekinesis aura emitted around target: type={}, target={}, count={}, distance={}",
                    id, target.getType(), count, mc.player != null ? target.distanceTo(mc.player) : -1);
    }

    private static Vec3 randomUnit(Random random)
    {
        Vec3 vector = new Vec3(random.nextDouble() * 2.0D - 1.0D, random.nextDouble() * 2.0D - 1.0D,
                random.nextDouble() * 2.0D - 1.0D);
        return vector.lengthSqr() < 1.0E-8 ? new Vec3(1, 0, 0) : vector.normalize();
    }

    private static void emitCloud(Player player, JsonObject effect)
    {
        if (!effect.has("particles") || !effect.get("particles").isJsonPrimitive()) return;
        ResourceLocation id = ResourceLocation.tryParse(effect.get("particles").getAsString());
        if (id == null) return;
        JsonObject definition = CLOUDS.computeIfAbsent(id, ParticleEmitterRenderer::loadCloud);
        if (definition == null) return;

        Minecraft mc = Minecraft.getInstance();
        boolean firstPerson = player == mc.player && mc.options.getCameraType().isFirstPerson();
        int amount = firstPerson ? 1 : 4;
        double spread = firstPerson ? 1.2D : 0.2D;
        double motionFactor = firstPerson ? -0.5D : 1.0D;
        Random random = new Random();
        Vec3 velocity = player.getDeltaMovement().scale(motionFactor);
        for (int i = 0; i < amount; ++i)
        {
            Vec3 point = new Vec3(player.getX() + randomOffset(random, spread),
                    player.getY() + player.getBbHeight() * 0.5D + randomOffset(random, spread),
                    player.getZ() + randomOffset(random, spread));
            if (spawnCloudSmoke(point, velocity, com.fiskmods.heroes.common.util.CloudColors.sample(definition, random), firstPerson)
                    && firstPerson && FIRST_PERSON_CLOUD_DIAGNOSTICS.add(id.toString()))
                FiskHeroes.LOGGER.info("First-person particle cloud emitted: {}", id);
        }
    }

    /** Use the animated smoke sprite rather than a solid dust square, retaining pack color. */
    private static boolean spawnCloudSmoke(Vec3 point, Vec3 velocity, com.fiskmods.heroes.common.util.CloudColors colors, boolean firstPerson)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return false;
        double distance = mc.gameRenderer.getMainCamera().getPosition().distanceToSqr(point);
        // Vanilla applies the same 32-block cutoff and particle setting to normal particles.
        // Avoid spawning a billboard on top of the camera in first person.
        if (distance > 1024.0D || firstPerson && distance < 0.49D) return false;
        var setting = mc.options.particles().get();
        if (setting == net.minecraft.client.ParticleStatus.MINIMAL
                || setting == net.minecraft.client.ParticleStatus.DECREASED && mc.level.random.nextInt(3) == 0)
            return false;
        mc.particleEngine.add(new CloudSmokeParticle(mc.level, point, velocity, colors, firstPerson));
        return true;
    }

    static JsonObject cloud(ResourceLocation id)
    {
        return CLOUDS.computeIfAbsent(id, ParticleEmitterRenderer::loadCloud);
    }

    private static JsonObject loadCloud(ResourceLocation id)
    {
        ResourceLocation path = new ResourceLocation(id.getNamespace(), "models/clouds/" + id.getPath() + ".json");
        try
        {
            Resource resource = Minecraft.getInstance().getResourceManager().getResource(path).orElseThrow();
            try (InputStreamReader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8))
            {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        }
        catch (Exception e)
        {
            FiskHeroes.LOGGER.warn("Could not load particle cloud {}: {}", path, e.toString());
            return null;
        }
    }

    private static double randomOffset(Random random, double spread)
    {
        return (random.nextDouble() * 2.0D - 1.0D) * spread;
    }

    /** Recreates the original Obsidian shadow-smoke chain between the caster and grabbed target. */
    private static void emitTelekinesisChain(Player player, PlayerModel<?> model, HeroModelData suit, float scale,
            PoseStack renderPose)
    {
        if (!player.level().isClientSide) return;
        SHPlayerData data = SHDataCapabilities.getPlayer(player);
        if (data == null || !data.getData().get(Vars.TELEKINESIS)) return;

        JsonObject effect = suit.getCustom().get("fiskheroes:telekinesis_chain");
        if (effect == null) return;

        Entity target = player.level().getEntity(data.getData().get(Vars.GRAB_ID));
        if (target == null || !target.isAlive()) return;

        boolean firstPerson = player == Minecraft.getInstance().player
                && Minecraft.getInstance().options.getCameraType().isFirstPerson();
        String anchorName = effect.has("anchor") ? effect.get("anchor").getAsString() : "rightArm";
        String offsetKey = firstPerson && effect.has("firstPerson") ? "firstPerson" : "offset";
        Vec3 offset = effect.has(offsetKey) ? vector(effect.get(offsetKey)) : Vec3.ZERO;
        Vec3 start;
        if (firstPerson)
        {
            Vec3 local = com.fiskmods.heroes.common.util.ParticleEmitterMath.firstPersonChainOffset(
                    offset, scale, player.getYRot(), player.getXRot());
            start = new Vec3(player.getX(), player.getEyeY(), player.getZ()).add(local);
        }
        else if (renderPose != null)
        {
            renderPose.pushPose();
            ModelPart part = anchor(model, anchorName);
            if (part != null)
            {
                part.translateAndRotate(renderPose);
                renderPose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            }
            start = com.fiskmods.heroes.common.util.ParticleEmitterMath.worldFromRenderTransform(
                    renderPose.last().pose(), com.mojang.blaze3d.systems.RenderSystem.getInverseViewRotationMatrix(),
                    Minecraft.getInstance().gameRenderer.getMainCamera().getPosition(),
                    new Vec3(-offset.x, offset.y, offset.z));
            renderPose.popPose();
        }
        else return;
        if (PARTICLE_DIAGNOSTICS && EMITTER_DIAGNOSTICS.add("chain:" + firstPerson))
            FiskHeroes.LOGGER.info("Telekinesis chain origin: firstPerson={}, yaw={}, pitch={}, dx={}, dy={}, dz={}",
                    firstPerson, player.getYRot(), player.getXRot(), start.x - player.getX(),
                    start.y - player.getY(), start.z - player.getZ());
        Vec3 end = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
        ResourceLocation cloudId = effect.has("particles") && effect.get("particles").isJsonPrimitive()
                ? ResourceLocation.tryParse(effect.get("particles").getAsString()) : null;
        JsonObject cloud = cloudId != null ? CLOUDS.computeIfAbsent(cloudId, ParticleEmitterRenderer::loadCloud) : null;
        Minecraft mc = Minecraft.getInstance();
        if (cloud == null || mc.level == null || mc.options.particles().get() == net.minecraft.client.ParticleStatus.MINIMAL) return;
        // Original samples every 0.15 blocks from the caster's eye to the target.
        double length = player.getEyePosition().distanceTo(end);
        int count = com.fiskmods.heroes.common.util.ParticleEmitterMath.telekinesisChainCount(length);
        Random random = new Random();
        for (int i = 0; i < count; ++i)
        {
            if (mc.options.particles().get() == net.minecraft.client.ParticleStatus.DECREASED && mc.level.random.nextInt(3) == 0) continue;
            double progress = length > 1.0E-6D ? Math.min(1.0D, i * 0.15D / length) : 0.0D;
            Vec3 point = com.fiskmods.heroes.common.util.ParticleEmitterMath.telekinesisChainPoint(
                    start, player.getEyePosition(), player.getViewVector(1.0F), end, progress);
            if (point.distanceToSqr(mc.gameRenderer.getMainCamera().getPosition()) > 1024.0D) continue;
            mc.particleEngine.add(new TelekinesisChainParticle(mc.level, player, target, start, progress, com.fiskmods.heroes.common.util.CloudColors.sample(cloud, random)));
        }
    }

    private static Vec3 vector(JsonElement value)
    {
        if (value == null || !value.isJsonArray() || value.getAsJsonArray().size() < 3) return Vec3.ZERO;
        JsonArray array = value.getAsJsonArray();
        return new Vec3(array.get(0).getAsDouble(), array.get(1).getAsDouble(), array.get(2).getAsDouble());
    }

    private static List<Emitter> load(ResourceLocation id)
    {
        ResourceLocation path = new ResourceLocation(id.getNamespace(), "models/particles/emitters/" + id.getPath() + ".json");
        try
        {
            Resource resource = Minecraft.getInstance().getResourceManager().getResource(path).orElseThrow();
            try (InputStreamReader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8))
            {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                List<Emitter> result = new ArrayList<>();
                JsonArray particles = root.getAsJsonArray("particles");
                if (particles != null)
                {
                    for (JsonElement element : particles)
                    {
                        if (element.isJsonObject()) result.add(Emitter.parse(element.getAsJsonObject()));
                    }
                }
                return List.copyOf(result);
            }
        }
        catch (Exception e)
        {
            FiskHeroes.LOGGER.warn("Could not load particle emitter {}: {}", path, e.toString());
            return List.of();
        }
    }

    private static void emit(Player player, PlayerModel<?> model, float scale, Emitter emitter, boolean firstPerson,
            PoseStack renderPose)
    {
        int amount = Math.max(0, emitter.amount(firstPerson));
        if (amount == 0) return;
        Level level = player.level();
        ModelPart anchor = emitter.anchor != null ? anchor(model, emitter.anchor) : null;
        Vec3 baseOffset = emitter.offset(firstPerson);
        float yaw = firstPerson && emitter.getLockFpYaw(player, firstPerson) ? player.getYRot() : player.yBodyRot;
        Vec3 baseMotion = emitter.motion(firstPerson);
        boolean rgb = emitter.optionsRgb();
        boolean mirror = emitter.getMirror(player, firstPerson);
        int sides = mirror ? 2 : 1;

        for (int side = 0; side < sides; ++side)
        {
            Vec3 mirroredOffset = side == 0 ? new Vec3(-baseOffset.x, baseOffset.y, baseOffset.z) : baseOffset;
            Vec3 origin;
            if (firstPerson)
            {
                Vec3 offset = com.fiskmods.heroes.common.util.ParticleEmitterMath.firstPersonOffset(
                        baseOffset, anchor != null, side != 0, scale, yaw);
                origin = new Vec3(player.getX(), player.getEyeY(), player.getZ()).add(offset);
            }
            else if (renderPose != null)
            {
                // Capture the exact parent and model-part transforms used by the rendered suit,
                // including its body yaw, gliding rotations, interpolated position and scale.
                renderPose.pushPose();
                if (anchor != null)
                {
                    anchorForSide(model, emitter.anchor, side).translateAndRotate(renderPose);
                    renderPose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
                }
                origin = com.fiskmods.heroes.common.util.ParticleEmitterMath.worldFromRenderTransform(
                        renderPose.last().pose(), com.mojang.blaze3d.systems.RenderSystem.getInverseViewRotationMatrix(),
                        Minecraft.getInstance().gameRenderer.getMainCamera().getPosition(), mirroredOffset);
                renderPose.popPose();
                var camera = Minecraft.getInstance().gameRenderer.getMainCamera();
                float viewYaw = Mth.wrapDegrees(camera.getYRot());
                int yawQuadrant = Math.round(viewYaw / 90.0F);
                if (PARTICLE_DIAGNOSTICS && Math.abs(viewYaw - yawQuadrant * 90.0F) < 3.0F
                        && ANCHOR_DIAGNOSTICS.add(emitter.type + ":" + emitter.anchor + ":" + side + ":" + yawQuadrant))
                    FiskHeroes.LOGGER.info("Suit emitter anchor: type={}, anchor={}, side={}, yaw={}, viewYaw={}, viewPitch={}, dx={}, dy={}, dz={}",
                            emitter.type, emitter.anchor, side, Mth.wrapDegrees(player.yBodyRot), viewYaw, camera.getXRot(),
                            origin.x - player.getX(), origin.y - player.getY(), origin.z - player.getZ());
            }
            else return; // Third-person particles need the actual model render transform.
            Vec3 sideMotion = rgb ? baseMotion : orient(new Vec3(side == 0 ? -baseMotion.x : baseMotion.x,
                    baseMotion.y, baseMotion.z), yaw).scale(scale);
            Vec3 randOffset = emitter.randOffset(firstPerson);
            Vec3 randMotion = emitter.randMotion(firstPerson);
            for (int i = 0; i < amount; ++i)
            {
                double x = origin.x + random(randOffset.x * scale);
                double y = origin.y + random(randOffset.y * scale);
                double z = origin.z + random(randOffset.z * scale);
                double vx = rgb ? 0.0D : sideMotion.x + random(randMotion.x) * scale;
                double vy = rgb ? 0.0D : sideMotion.y + random(randMotion.y) * scale;
                double vz = rgb ? 0.0D : sideMotion.z + random(randMotion.z) * scale;
                if (emitter.smokeType())
                {
                    if (spawnEmitterSmoke(player, emitter, x, y, z, vx, vy, vz, sideMotion, randMotion, scale)
                            && PARTICLE_DIAGNOSTICS && EMITTER_DIAGNOSTICS.add(emitter.type + ":" + firstPerson))
                        FiskHeroes.LOGGER.info("Suit emitter smoke rendered: type={}, firstPerson={}", emitter.type, firstPerson);
                }
                else level.addParticle(emitter.options(sideMotion, randMotion), x, y, z, vx, vy, vz);
            }
        }
    }

    /** Approximate the original animated smoke using a tinted vanilla smoke sprite, not snowflakes or dust. */
    private static boolean spawnEmitterSmoke(Player player, Emitter emitter, double x, double y, double z,
            double vx, double vy, double vz, Vec3 color, Vec3 colorRandom, float scale)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !com.fiskmods.heroes.common.util.ParticleEmitterMath.inRange(
                new Vec3(x, y, z), mc.gameRenderer.getMainCamera().getPosition(), emitter.maxDistance,
                mc.options.renderDistance().get())) return false;
        var setting = mc.options.particles().get();
        if (setting == net.minecraft.client.ParticleStatus.MINIMAL
                || setting == net.minecraft.client.ParticleStatus.DECREASED && mc.level.random.nextInt(3) == 0) return false;
        boolean mysterio = emitter.type.equals("MYSTERIO_SMOKE");
        var particle = mc.particleEngine.createParticle(mysterio ? ParticleTypes.LARGE_SMOKE : ParticleTypes.SMOKE,
                x, y, z, vx, vy, vz);
        if (particle == null) return false;
        if (emitter.type.equals("ENERGY_SMOKE"))
            particle.setColor(Mth.clamp((float) (color.x + random(colorRandom.x)), 0F, 1F),
                    Mth.clamp((float) (color.y + random(colorRandom.y)), 0F, 1F),
                    Mth.clamp((float) (color.z + random(colorRandom.z)), 0F, 1F));
        else if (mysterio)
        {
            float gray = 0.9F + player.getRandom().nextFloat() * 0.1F;
            particle.setColor(gray, gray, gray);
        }
        else
        {
            particle.setColor(0.7F + player.getRandom().nextFloat() * 0.3F, 1F, 1F);
            // EntitySHSmokeFX adds this independent drift even when the emitter itself
            // specifies almost no motion. It lets cryogenic puffs spread around the hand.
            particle.setParticleSpeed(vx + random(0.05D), vy + random(0.05D), vz + random(0.05D));
        }
        particle.scale(scale * (mysterio ? 2F : 1F));
        return true;
    }

    private static ModelPart anchorForSide(PlayerModel<?> model, String name, int side)
    {
        if (side == 0)
        {
            return switch (name.toLowerCase(java.util.Locale.ROOT))
            {
                case "rightarm" -> model.rightArm;
                case "leftarm" -> model.leftArm;
                case "rightleg" -> model.rightLeg;
                case "leftleg" -> model.leftLeg;
                default -> anchor(model, name);
            };
        }
        return switch (name.toLowerCase(java.util.Locale.ROOT))
        {
            case "rightarm" -> model.leftArm;
            case "leftarm" -> model.rightArm;
            case "rightleg" -> model.leftLeg;
            case "leftleg" -> model.rightLeg;
            default -> anchor(model, name);
        };
    }

    private static Vec3 orient(Vec3 vector, float yaw)
    {
        double radians = Math.toRadians(-yaw);
        double sin = Math.sin(radians);
        double cos = Math.cos(radians);
        return new Vec3(vector.x * cos + vector.z * sin, vector.y, vector.z * cos - vector.x * sin);
    }

    private static double random(double amount)
    {
        return (Math.random() * 2.0D - 1.0D) * amount;
    }

    private static boolean passesConditionals(JsonObject effect, HeroModelData suit, Player player)
    {
        if (!effect.has("conditionals") || !effect.get("conditionals").isJsonArray()) return true;
        for (JsonElement entry : effect.getAsJsonArray("conditionals"))
        {
            if (entry.isJsonPrimitive() && entry.getAsString().startsWith("vars:")
                    && !suit.evaluate(entry.getAsString().substring("vars:".length()), player)) return false;
        }
        return true;
    }

    private static ModelPart anchor(PlayerModel<?> model, String name)
    {
        return switch (name.toLowerCase(java.util.Locale.ROOT))
        {
            case "head" -> model.head;
            case "rightarm" -> model.rightArm;
            case "leftarm" -> model.leftArm;
            case "rightleg" -> model.rightLeg;
            case "leftleg" -> model.leftLeg;
            case "hat", "headwear" -> model.hat;
            default -> model.body;
        };
    }

    private record Emitter(String type, String anchor, double maxDistance, JsonElement amount,
            JsonElement offset, JsonElement motion, JsonElement randOffset, JsonElement randMotion,
            JsonElement condition, JsonElement mirror, JsonElement lockFpYaw)
    {
        static Emitter parse(JsonObject json)
        {
            return new Emitter(json.has("type") ? json.get("type").getAsString() : "SMOKE",
                    json.has("anchor") ? json.get("anchor").getAsString() : null,
                    json.has("maxDistance") ? json.get("maxDistance").getAsDouble() : 64.0D,
                    json.get("amount"), json.get("offset"), json.get("motion"), json.get("randOffset"),
                    json.get("randMotion"), json.get("condition"), json.get("mirror"), json.get("lockFPYaw"));
        }

        int amount(boolean fp) { return (int) Math.round(number(amount, fp, 1.0F)); }
        Vec3 offset(boolean fp) { return vector(offset, fp); }
        Vec3 motion(boolean fp) { return vector(motion, fp); }
        Vec3 randOffset(boolean fp) { return vector(randOffset, fp); }
        Vec3 randMotion(boolean fp) { return vector(randMotion, fp); }
        boolean rgb() { return type.equals("ENERGY_SMOKE") || type.equals("FIREWORK_BACKGROUND"); }
        boolean smokeType()
        {
            return type.equals("CRYO_SMOKE") || type.equals("FREEZE_SMOKE")
                    || type.equals("MYSTERIO_SMOKE") || type.equals("ENERGY_SMOKE");
        }

        boolean condition(Player player, boolean fp)
        {
            if (condition == null) return true;
            if (condition.isJsonPrimitive() && condition.getAsJsonPrimitive().isBoolean()) return condition.getAsBoolean();
            if (!condition.isJsonPrimitive()) return true;
            return expression(condition.getAsString(), player, fp);
        }

        boolean getMirror(Player player, boolean fp) { return bool(mirror, player, fp, false); }
        boolean getLockFpYaw(Player player, boolean fp) { return bool(lockFpYaw, player, fp, false); }

        boolean optionsRgb() { return rgb(); }

        ParticleOptions options(Vec3 motion, Vec3 randomMotion)
        {
            if (rgb())
            {
                return new DustParticleOptions(new Vector3f(Mth.clamp((float) motion.x, 0.0F, 1.0F),
                        Mth.clamp((float) motion.y, 0.0F, 1.0F), Mth.clamp((float) motion.z, 0.0F, 1.0F)), 0.8F);
            }
            return particle(type);
        }

        private static ParticleOptions particle(String name)
        {
            return switch (name.toUpperCase(java.util.Locale.ROOT))
            {
                case "SMOKE", "SMOKE_SMALL" -> ParticleTypes.SMOKE;
                case "BIG_SMOKE", "THICK_SMOKE", "MINECRAFT:LARGESMOKE" -> ParticleTypes.LARGE_SMOKE;
                case "FLAME", "FLAME_BALL" -> ParticleTypes.FLAME;
                case "BLUE_FLAME" -> ParticleTypes.SOUL_FIRE_FLAME;
                case "SPARK", "BULLET_SPARK" -> ParticleTypes.ELECTRIC_SPARK;
                case "MINECRAFT:LARGEEXPLODE" -> ParticleTypes.EXPLOSION;
                case "MINECRAFT:FIREWORK" -> ParticleTypes.FIREWORK;
                default -> ParticleTypes.SMOKE;
            };
        }

        private static double number(JsonElement value, boolean fp, float fallback)
        {
            JsonElement resolved = select(value, fp);
            return resolved != null && resolved.isJsonPrimitive() ? resolved.getAsDouble() : fallback;
        }

        private static Vec3 vector(JsonElement value, boolean fp)
        {
            JsonElement resolved = select(value, fp);
            if (resolved == null || !resolved.isJsonArray() || resolved.getAsJsonArray().size() < 3) return Vec3.ZERO;
            JsonArray array = resolved.getAsJsonArray();
            return new Vec3(array.get(0).getAsDouble(), array.get(1).getAsDouble(), array.get(2).getAsDouble());
        }

        private static JsonElement select(JsonElement value, boolean fp)
        {
            if (value != null && value.isJsonObject())
            {
                JsonObject object = value.getAsJsonObject();
                return object.has(fp ? "fp" : "tp") ? object.get(fp ? "fp" : "tp") : null;
            }
            return value;
        }

        private static boolean bool(JsonElement value, Player player, boolean fp, boolean fallback)
        {
            if (value == null) return fallback;
            if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean()) return value.getAsBoolean();
            if (!value.isJsonPrimitive()) return fallback;
            return expression(value.getAsString(), player, fp);
        }

        private static boolean expression(String source, Player player, boolean fp)
        {
            if (source.equals("true")) return true;
            if (source.equals("false")) return false;
            String expression = source.replaceAll("\\bfirstPerson\\b", Boolean.toString(fp));
            ScriptFunction function = CONDITIONS.computeIfAbsent(expression, JSExpressions::compile);
            if (function == null) return false;
            Object result = function.call(new JSEntity(player));
            return result instanceof Boolean bool ? bool : result instanceof Number number && number.doubleValue() != 0.0D;
        }
    }
}
