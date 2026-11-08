package com.fiskmods.heroes.client.render;

import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.data.var.Vars;
import com.fiskmods.heroes.common.util.TentacleVisualMath;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.List;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Renders the four original Tabula arm and claw models declared by Doctor Octopus's suit. */
final class TentacleSuitRenderer
{
    private static final java.util.Set<String> ACTION_DIAGNOSTICS = new java.util.HashSet<>();
    private record ArmGeometry(Vec3 source, Vec3 control, Vec3 end, int links, float grab, float strike) { }

    private TentacleSuitRenderer()
    {
    }

    static void render(JsonObject effect, PoseStack pose, MultiBufferSource buffer, int light,
            AbstractClientPlayer player, PlayerModel<AbstractClientPlayer> playerModel, HeroModelData model, int slot)
    {
        if (slot != 1) return;
        SHPlayerData data = SHDataCapabilities.getPlayer(player);
        if (data == null) return;

        float extension = data.getData().getInterpolated(Vars.TENTACLE_EXTEND_TIMER, MinecraftPartialTicks.get());
        extension = Math.max(0.0F, Math.min(1.0F, extension / 20.0F));
        if (extension <= 0.0F) return;

        JsonObject segment = object(effect.get("segment"));
        JsonObject head = object(effect.get("head"));
        JsonArray arms = array(effect.get("tentacles"));
        ModelPart anchor = playerModel.body;
        if (segment == null || arms == null || anchor == null || !segment.has("modelType")) return;

        int action = data.getData().get(Vars.TENTACLE_VISUAL_ACTION);
        int actionIndex = data.getData().get(Vars.TENTACLE_VISUAL_INDEX);
        float partial = MinecraftPartialTicks.get();
        float progress = data.getData().getPrevious(Vars.TENTACLE_VISUAL_ACTION) == action
                ? data.getData().getInterpolated(Vars.TENTACLE_VISUAL_TIMER, partial)
                : data.getData().get(Vars.TENTACLE_VISUAL_TIMER);
        float length = effect.has("segmentLength") ? effect.get("segmentLength").getAsFloat() : 1.8F;
        int links = effect.has("segmentLinks") ? effect.get("segmentLinks").getAsInt() : 16;
        Vec3 target = action != TentacleVisualMath.NONE ? aimedTarget(player, partial) : Vec3.ZERO;
        ArmGeometry[] geometry = new ArmGeometry[arms.size()];
        for (int index = 0; index < arms.size(); ++index)
        {
            JsonObject arm = object(arms.get(index));
            if (arm == null) continue;
            int state = index == actionIndex ? action : TentacleVisualMath.NONE;
            geometry[index] = geometry(player, arm, index, extension, length, links, state, progress, target, partial);
        }
        if (action != TentacleVisualMath.NONE && ACTION_DIAGNOSTICS.add(player.getUUID() + ":" + action))
            com.fiskmods.heroes.FiskHeroes.LOGGER.info("Tentacle action pose rendered: player={}, action={}, arm={}",
                    player.getGameProfile().getName(), action, actionIndex);

        for (int pass = 0; pass < 2; ++pass)
        {
            String textureKey = texture(segment.get("texture"), pass);
            ResourceLocation texture = model.resolveCustomTexture(textureKey, player, slot);
            if (texture == null) continue;
            RenderType type = pass == 0 ? RenderType.entityTranslucent(texture) : RenderType.eyes(texture);
            VertexConsumer vertex = buffer.getBuffer(type);

            pose.pushPose();
            anchor.translateAndRotate(pose);
            for (int index = 0; index < arms.size(); ++index)
            {
                if (geometry[index] == null) continue;
                renderArm(pose, vertex, light, segment.get("modelType").getAsString(), geometry[index]);
            }
            pose.popPose();

            if (pass == 0 && head != null)
            {
                renderClaws(head, geometry, model, player, slot, playerModel, pose, buffer, light, extension);
            }
        }
    }

    /** Converts a block-clipped view ray to body-model coordinates for the visual lunge. */
    private static Vec3 aimedTarget(AbstractClientPlayer player, float partial)
    {
        var iteration = com.fiskmods.heroes.common.hero.HeroTracker.getWornSuit(player);
        var entry = iteration != null ? iteration.getHero().getPowerContainer().getEntry(
                com.fiskmods.heroes.FiskHeroes.id("tentacles")) : null;
        float range = entry != null ? net.minecraft.util.Mth.clamp(entry.getFloat(
                com.fiskmods.heroes.common.hero.power.PowerProperty.RANGE), 0.0F, 8.0F) : 8.0F;
        Vec3 eye = player.getEyePosition(partial);
        Vec3 end = eye.add(player.getViewVector(partial).scale(range));
        var hit = player.level().clip(new net.minecraft.world.level.ClipContext(eye, end,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE, player));
        if (hit.getType() != net.minecraft.world.phys.HitResult.Type.MISS) end = hit.getLocation();
        final Vec3 clippedEnd = end;
        var search = player.getBoundingBox().expandTowards(clippedEnd.subtract(eye)).inflate(1.0D);
        end = player.level().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, search,
                target -> target != player && target.isAlive() && target.isPickable()).stream()
                .map(target -> {
                    var bounds = target.getBoundingBox().inflate(0.55D);
                    return bounds.contains(eye) ? eye : bounds.clip(eye, clippedEnd).orElse(null);
                })
                .filter(java.util.Objects::nonNull)
                .min(java.util.Comparator.comparingDouble(eye::distanceToSqr)).orElse(clippedEnd);
        Vec3 playerPos = player.getPosition(partial);
        float bodyYaw = net.minecraft.util.Mth.rotLerp(partial, player.yBodyRotO, player.yBodyRot);
        Vec3 relative = end.subtract(playerPos).yRot((float) -Math.toRadians(180.0F - bodyYaw));
        // Invert PlayerRenderer's yaw, (-1,-1,1) model scale and 1.501 origin offset.
        return new Vec3(-relative.x / 0.9375D, 1.501D - relative.y / 0.9375D, relative.z / 0.9375D);
    }

    private static ArmGeometry geometry(AbstractClientPlayer player, JsonObject arm, int index,
            float extension, float segmentLength, int configuredLinks, int action, float progress, Vec3 target, float partial)
    {
        Vec3 pixelOffset = vector(arm.get("offset"));
        Vec3 pixelDirection = vector(arm.get("direction"));
        Vec3 source = new Vec3(-pixelOffset.x / 16.0D, -pixelOffset.y / 16.0D, -pixelOffset.z / 16.0D);
        Vec3 direction = new Vec3(-pixelDirection.x, -pixelDirection.y, -pixelDirection.z).normalize();
        double restLength = Math.max(0.1D, segmentLength) * extension;
        double sway = Math.sin((player.tickCount + partial) * 0.075D + index * 1.3D) * 0.06D * extension;
        Vec3 end = source.add(direction.scale(restLength)).add(0.0D, sway, 0.0D);
        float reach = TentacleVisualMath.reach(action, progress) * extension;
        end = end.scale(1.0D - reach).add(target.scale(reach));
        float strike = TentacleVisualMath.strike(action, progress);
        if (action == TentacleVisualMath.CHARGE) end = end.add(0.0D, -0.22D * strike, 0.18D * strike);
        Vec3 control = source.add(direction.scale(0.55D * extension)).add(0.0D, -0.14D * extension + sway, 0.0D);
        control = control.scale(1.0D - reach * 0.5D).add(source.add(end).scale(0.5D * reach * 0.5D));
        int baseLinks = Math.max(4, Math.min(64, configuredLinks));
        int links = Math.max(baseLinks, Math.min(80,
                (int) Math.ceil(baseLinks * source.distanceTo(end) / Math.max(0.1D, segmentLength))));
        return new ArmGeometry(source, control, end, links, TentacleVisualMath.grab(action, progress), strike);
    }

    private static void renderArm(PoseStack pose, VertexConsumer vertex, int light, String modelType, ArmGeometry arm)
    {
        for (int link = 0; link < arm.links(); ++link)
        {
            double t = (link + 0.5D) / arm.links();
            Vec3 point = bezier(arm.source(), arm.control(), arm.end(), t);
            Vec3 tangent = bezier(arm.source(), arm.control(), arm.end(), Math.min(1.0D, t + 0.01D))
                    .subtract(bezier(arm.source(), arm.control(), arm.end(), Math.max(0.0D, t - 0.01D))).normalize();
            if (tangent.lengthSqr() < 1.0E-10D) continue;
            pose.pushPose();
            pose.translate(point.x, point.y, point.z);
            faceUp(pose, tangent);
            // ModelPart already converts Tabula pixel coordinates to blocks.
            TabulaModelCache.render(modelType, pose, vertex, light, 1.0F, 1.0F, 1.0F, 1.0F);
            pose.popPose();
        }
    }

    private static void renderClaws(JsonObject head, ArmGeometry[] arms, HeroModelData model,
            AbstractClientPlayer player, int slot, PlayerModel<AbstractClientPlayer> playerModel,
            PoseStack pose, MultiBufferSource buffer, int light, float extension)
    {
        String modelType = head.has("modelType") ? head.get("modelType").getAsString() : null;
        if (modelType == null) return;
        boolean ock = modelType.equals("fiskheroes:ock_claw") && head.has("animations")
                && head.get("animations").isJsonArray();
        List<String> keys = textureKeys(head.get("texture"));
        for (int index = 0; index < arms.length; ++index)
        {
            ArmGeometry arm = arms[index];
            if (arm == null) continue;
            Vec3 tangent = arm.end().subtract(arm.control()).normalize();
            if (tangent.lengthSqr() < 1.0E-10D) continue;
            final int armIndex = index;
            for (int pass = 0; pass < keys.size(); ++pass)
            {
                ResourceLocation texture = model.resolveCustomTexture(keys.get(pass), player, slot);
                if (texture == null) continue;
                VertexConsumer vertex = buffer.getBuffer(pass == 0
                        ? RenderType.entityTranslucent(texture) : RenderType.eyes(texture));
                pose.pushPose();
                playerModel.body.translateAndRotate(pose);
                pose.translate(arm.end().x, arm.end().y, arm.end().z);
                faceUp(pose, tangent);
                // ModelPart.render performs the pixel-to-block conversion itself.
                if (ock) TabulaModelCache.renderAnimated(modelType, pose, vertex, light, 1.0F, 1.0F, 1.0F, 1.0F,
                        claw -> OckClawAnimation.apply(claw, armIndex, extension, arm.grab(), arm.strike()));
                else TabulaModelCache.render(modelType, pose, vertex, light, 1.0F, 1.0F, 1.0F, 1.0F);
                pose.popPose();
            }
        }
    }

    private static void faceUp(PoseStack pose, Vec3 direction)
    {
        Vector3f up = new Vector3f(0.0F, 1.0F, 0.0F);
        Vector3f target = new Vector3f((float) direction.x, (float) direction.y, (float) direction.z).normalize();
        pose.mulPose(new Quaternionf().rotationTo(up, target));
    }

    private static Vec3 bezier(Vec3 start, Vec3 control, Vec3 end, double t)
    {
        double inverse = 1.0D - t;
        return start.scale(inverse * inverse).add(control.scale(2.0D * inverse * t)).add(end.scale(t * t));
    }

    private static String texture(JsonElement value, int pass)
    {
        if (value == null || value.isJsonNull()) return null;
        if (value.isJsonArray()) return pass < value.getAsJsonArray().size()
                ? value.getAsJsonArray().get(pass).getAsString() : null;
        return pass == 0 ? value.getAsString() : null;
    }

    private static List<String> textureKeys(JsonElement value)
    {
        if (value == null || value.isJsonNull()) return List.of();
        if (value.isJsonArray())
        {
            java.util.ArrayList<String> result = new java.util.ArrayList<>();
            for (JsonElement element : value.getAsJsonArray()) result.add(element.getAsString());
            return result;
        }
        return List.of(value.getAsString());
    }

    private static JsonArray array(JsonElement value)
    {
        return value != null && value.isJsonArray() ? value.getAsJsonArray() : null;
    }

    private static JsonObject object(JsonElement value)
    {
        return value != null && value.isJsonObject() ? value.getAsJsonObject() : null;
    }

    private static Vec3 vector(JsonElement value)
    {
        if (value == null || !value.isJsonArray() || value.getAsJsonArray().size() < 3) return Vec3.ZERO;
        JsonArray array = value.getAsJsonArray();
        return new Vec3(array.get(0).getAsDouble(), array.get(1).getAsDouble(), array.get(2).getAsDouble());
    }

    /** Partial tick access without threading the value through every item helper. */
    private static final class MinecraftPartialTicks
    {
        private static float get()
        {
            return net.minecraft.client.Minecraft.getInstance().getFrameTime();
        }
    }
}
