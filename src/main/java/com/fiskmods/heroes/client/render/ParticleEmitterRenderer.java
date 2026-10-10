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

    private ParticleEmitterRenderer() { }

    static void clear()
    {
        EMITTERS.clear();
        CLOUDS.clear();
        CONDITIONS.clear();
        LAST_TICK.clear();
    }

    static void tick(Player player, PlayerModel<?> model, HeroModelData suit, float suitScale)
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
        emitTelekinesisChain(player, model, suit, suitScale);
        boolean firstPerson = player == mc.player && mc.options.getCameraType().isFirstPerson();
        for (Map.Entry<String, JsonObject> entry : suit.getCustom().entrySet())
        {
            if (!entry.getKey().startsWith("fiskheroes:particles") || !passesConditionals(entry.getValue(), suit, player)) continue;
            if (!entry.getValue().has("particleType")) continue;
            ResourceLocation id = ResourceLocation.tryParse(entry.getValue().get("particleType").getAsString());
            if (id == null) continue;
            for (Emitter emitter : EMITTERS.computeIfAbsent(id, ParticleEmitterRenderer::load))
            {
                if (!emitter.condition(player, firstPerson)) continue;
                double maxDistance = emitter.maxDistance;
                if (mc.player != null && mc.player.distanceToSqr(player) > maxDistance * maxDistance) continue;
                emit(player, model, suitScale, emitter, firstPerson);
            }
        }
    }

    /** Executes the original particle_cloud and teleportation cloud directives. */
    private static void emitCloud(Player player, HeroModelData suit)
    {
        JsonObject effect = suit.getCustom().get("fiskheroes:particle_cloud");
        if (effect != null && passesConditionals(effect, suit, player)) emitCloud(player, effect);

        effect = suit.getCustom().get("fiskheroes:teleportation");
        SHPlayerData data = SHDataCapabilities.getPlayer(player);
        if (effect != null && data != null && data.getData().get(Vars.TELEPORT_TIMER) > 0.0F)
        {
            emitCloud(player, effect);
        }

        // The Monitor binds its telekinesis cloud directly to the modifier state instead of
        // declaring it as particle_cloud. Keep it active only while the server-synced grab power
        // is held, matching the original renderer helper's condition.
        effect = suit.getCustom().get("fiskheroes:telekinesis");
        if (effect != null && data != null && data.getData().get(Vars.TELEKINESIS))
        {
            emitCloud(player, effect);
        }
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
        float[] color = cloudColor(definition, random);
        var particle = new DustParticleOptions(new Vector3f(color[0], color[1], color[2]), 1.0F);
        Vec3 velocity = player.getDeltaMovement().scale(motionFactor);
        Level level = player.level();
        for (int i = 0; i < amount; ++i)
        {
            level.addParticle(particle,
                    player.getX() + randomOffset(random, spread),
                    player.getY() + player.getBbHeight() * 0.5D + randomOffset(random, spread),
                    player.getZ() + randomOffset(random, spread),
                    velocity.x, velocity.y, velocity.z);
        }
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

    /** Samples the pack's base/rand/start/end RGB values using its shared-channel rules. */
    private static float[] cloudColor(JsonObject definition, Random random)
    {
        float[] randoms = { random.nextFloat(), random.nextFloat(), random.nextFloat() };
        JsonObject channels = definition.has("color") && definition.get("color").isJsonObject()
                ? definition.getAsJsonObject("color") : new JsonObject();
        JsonElement[] specs = new JsonElement[3];
        int[] links = { 0, 1, 2 };
        String[] names = { "red", "green", "blue" };
        for (var entry : channels.entrySet())
        {
            String[] shared = entry.getKey().split(",");
            int first = channelIndex(shared[0].trim());
            if (first < 0) continue;
            for (String name : shared)
            {
                int channel = channelIndex(name.trim());
                if (channel >= 0)
                {
                    specs[channel] = entry.getValue();
                    links[channel] = first;
                }
            }
        }
        float[] result = new float[3];
        for (int i = 0; i < result.length; ++i)
        {
            result[i] = Mth.clamp(sampleCloudChannel(specs[i], i, links[i], specs, links, randoms), 0.0F, 1.0F);
        }
        return result;
    }

    private static float sampleCloudChannel(JsonElement spec, int channel, int link,
            JsonElement[] specs, int[] links, float[] randoms)
    {
        if (spec == null || spec.isJsonNull()) return 0.5F;
        if (spec.isJsonPrimitive() && spec.getAsJsonPrimitive().isNumber()) return spec.getAsFloat();
        if (spec.isJsonPrimitive() && spec.getAsJsonPrimitive().isString())
        {
            java.util.regex.Matcher matcher = java.util.regex.Pattern
                    .compile("(red|green|blue)\\s*([+*/-])\\s*([0-9.]+)").matcher(spec.getAsString());
            if (!matcher.matches()) return 0.5F;
            int source = channelIndex(matcher.group(1));
            float value = source >= 0 && source != channel
                    ? sampleCloudChannel(specs[source], source, links[source], specs, links, randoms)
                    : channelBase(spec, link, randoms);
            float operand = Float.parseFloat(matcher.group(3));
            return switch (matcher.group(2))
            {
                case "+" -> value + operand;
                case "-" -> value - operand;
                case "*" -> value * operand;
                case "/" -> operand == 0.0F ? value : value / operand;
                default -> value;
            };
        }
        if (!spec.isJsonObject()) return 0.5F;
        JsonObject object = spec.getAsJsonObject();
        if (object.has("start") || object.has("end"))
        {
            float start = sampleCloudEndpoint(object.get("start"), link, randoms);
            float end = sampleCloudEndpoint(object.get("end"), link, randoms);
            return start + (end - start) * 0.5F;
        }
        return sampleCloudEndpoint(spec, link, randoms);
    }

    private static float channelBase(JsonElement spec, int link, float[] randoms)
    {
        if (spec != null && spec.isJsonObject()) return sampleCloudEndpoint(spec, link, randoms);
        return 0.5F;
    }

    private static float sampleCloudEndpoint(JsonElement endpoint, int link, float[] randoms)
    {
        if (endpoint == null || endpoint.isJsonNull()) return 0.5F;
        if (endpoint.isJsonPrimitive() && endpoint.getAsJsonPrimitive().isNumber()) return endpoint.getAsFloat();
        if (endpoint.isJsonObject())
        {
            JsonObject values = endpoint.getAsJsonObject();
            float base = values.has("base") ? values.get("base").getAsFloat() : 0.0F;
            float range = values.has("rand") ? values.get("rand").getAsFloat() : 0.0F;
            return base + range * randoms[Math.max(0, Math.min(2, link))];
        }
        return 0.5F;
    }

    private static int channelIndex(String name)
    {
        return switch (name)
        {
            case "red" -> 0;
            case "green" -> 1;
            case "blue" -> 2;
            default -> -1;
        };
    }

    private static double randomOffset(Random random, double spread)
    {
        return (random.nextDouble() * 2.0D - 1.0D) * spread;
    }

    /** Recreates the original Obsidian shadow-smoke chain between the caster and grabbed target. */
    private static void emitTelekinesisChain(Player player, PlayerModel<?> model, HeroModelData suit, float scale)
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
        Vec3 offset = effect.has(firstPerson ? "firstPerson" : "offset")
                ? vector(effect.get(firstPerson ? "firstPerson" : "offset")) : Vec3.ZERO;
        ModelPart part = anchor(model, anchorName);
        Vec3 local = anchorOffset(part, offset, scale);
        Vec3 startOffset = orient(local, player.yBodyRot);
        Vec3 start = new Vec3(player.getX() + startOffset.x,
                player.getY() + 1.5D * scale - startOffset.y, player.getZ() + startOffset.z);
        Vec3 end = target.position().add(0.0D, target.getBbHeight() * 0.55D, 0.0D);
        Vec3 delta = end.subtract(start);
        Vec3 bend = new Vec3(0.0D, Math.min(1.5D, delta.length() * 0.12D), 0.0D);
        Vec3 control1 = start.add(delta.scale(0.33D)).add(bend);
        Vec3 control2 = start.add(delta.scale(0.66D)).add(bend);

        // A low particle budget keeps this attached effect light even with several players nearby.
        for (int i = 1; i <= 5; ++i)
        {
            double t = i / 6.0D;
            double inv = 1.0D - t;
            Vec3 point = start.scale(inv * inv * inv)
                    .add(control1.scale(3.0D * inv * inv * t))
                    .add(control2.scale(3.0D * inv * t * t))
                    .add(end.scale(t * t * t));
            player.level().addParticle(ParticleTypes.SMOKE, point.x, point.y, point.z,
                    0.0D, 0.005D, 0.0D);
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

    private static void emit(Player player, PlayerModel<?> model, float scale, Emitter emitter, boolean firstPerson)
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
            Vec3 sideOrigin = anchor != null ? orient(anchorOffset(anchorForSide(model, emitter.anchor, side), mirroredOffset, scale), yaw)
                    : orient(new Vec3(mirroredOffset.x * scale, mirroredOffset.y * scale, mirroredOffset.z * scale), yaw);
            Vec3 sideMotion = rgb ? baseMotion : orient(new Vec3(side == 0 ? -baseMotion.x : baseMotion.x,
                    baseMotion.y, baseMotion.z), yaw).scale(scale);
            Vec3 randOffset = emitter.randOffset(firstPerson);
            Vec3 randMotion = emitter.randMotion(firstPerson);
            for (int i = 0; i < amount; ++i)
            {
                double x = player.getX() + sideOrigin.x + random(randOffset.x * scale);
                double y = player.getY() + 1.5D * scale - sideOrigin.y + random(randOffset.y * scale);
                double z = player.getZ() + sideOrigin.z + random(randOffset.z * scale);
                ParticleOptions options = emitter.options(sideMotion, randMotion);
                level.addParticle(options, x, y, z,
                        rgb ? 0.0D : sideMotion.x + random(randMotion.x) * scale,
                        rgb ? 0.0D : sideMotion.y + random(randMotion.y) * scale,
                        rgb ? 0.0D : sideMotion.z + random(randMotion.z) * scale);
            }
        }
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

    private static Vec3 anchorOffset(ModelPart anchor, Vec3 offset, float scale)
    {
        PoseStack pose = new PoseStack();
        pose.scale(-scale, scale, -scale);
        if (anchor != null)
        {
            anchor.translateAndRotate(pose);
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
        }
        Vector3f point = new Vector3f((float) offset.x, (float) offset.y, (float) offset.z);
        pose.last().pose().transformPosition(point);
        return new Vec3(point.x, point.y, point.z);
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
                case "CRYO_SMOKE", "FREEZE_SMOKE" -> ParticleTypes.SNOWFLAKE;
                case "MYSTERIO_SMOKE" -> ParticleTypes.PORTAL;
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
