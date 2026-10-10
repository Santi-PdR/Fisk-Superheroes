package com.fiskmods.heroes.client.render;

import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.data.var.Vars;
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
        if (segment == null || arms == null || anchor == null) return;

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
                JsonElement element = arms.get(index);
                if (!element.isJsonObject()) continue;
                JsonObject arm = element.getAsJsonObject();
                Vec3 offset = vector(arm.get("offset"));
                Vec3 direction = vector(arm.get("direction"));
                renderArm(pose, vertex, light, segment.get("modelType").getAsString(), offset,
                        direction, extension, effect.has("segmentLinks") ? effect.get("segmentLinks").getAsInt() : 16);
            }
            pose.popPose();

            if (pass == 0 && head != null)
            {
                renderClaws(head, arms, model, player, slot, playerModel, pose, buffer, light, extension);
            }
        }
    }

    private static void renderArm(PoseStack pose, VertexConsumer vertex, int light, String modelType,
            Vec3 pixelOffset, Vec3 pixelDirection, float extension, int configuredLinks)
    {
        Vec3 source = new Vec3(-pixelOffset.x / 16.0D, -pixelOffset.y / 16.0D, -pixelOffset.z / 16.0D);
        Vec3 direction = new Vec3(-pixelDirection.x, -pixelDirection.y, -pixelDirection.z).normalize();
        double length = 1.8D * extension;
        Vec3 control = source.add(direction.scale(0.55D)).add(0.0D, -0.14D * extension, 0.0D);
        Vec3 end = source.add(direction.scale(length));
        int links = Math.max(4, configuredLinks);

        for (int link = 0; link < links; ++link)
        {
            double t = (link + 0.5D) / links;
            Vec3 point = bezier(source, control, end, t);
            Vec3 tangent = bezier(source, control, end, Math.min(1.0D, t + 0.01D))
                    .subtract(bezier(source, control, end, Math.max(0.0D, t - 0.01D))).normalize();
            pose.pushPose();
            pose.translate(point.x, point.y, point.z);
            faceUp(pose, tangent);
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            TabulaModelCache.render(modelType, pose, vertex, light, 1.0F, 1.0F, 1.0F, 1.0F);
            pose.popPose();
        }
    }

    private static void renderClaws(JsonObject head, JsonArray arms, HeroModelData model,
            AbstractClientPlayer player, int slot, PlayerModel<AbstractClientPlayer> playerModel,
            PoseStack pose, MultiBufferSource buffer, int light, float extension)
    {
        String modelType = head.has("modelType") ? head.get("modelType").getAsString() : null;
        if (modelType == null) return;
        List<String> keys = textureKeys(head.get("texture"));
        for (int index = 0; index < arms.size(); ++index)
        {
            JsonObject arm = object(arms.get(index));
            if (arm == null) continue;
            Vec3 source = vector(arm.get("offset"));
            Vec3 direction = vector(arm.get("direction"));
            Vec3 unit = new Vec3(-direction.x, -direction.y, -direction.z).normalize();
            Vec3 point = new Vec3(-source.x / 16.0D, -source.y / 16.0D, -source.z / 16.0D)
                    .add(unit.scale(1.8D * extension));

            for (int pass = 0; pass < keys.size(); ++pass)
            {
                ResourceLocation texture = model.resolveCustomTexture(keys.get(pass), player, slot);
                if (texture == null) continue;
                VertexConsumer vertex = buffer.getBuffer(pass == 0
                        ? RenderType.entityTranslucent(texture) : RenderType.eyes(texture));
                pose.pushPose();
                playerModel.body.translateAndRotate(pose);
                pose.translate(point.x, point.y, point.z);
                faceUp(pose, unit);
                pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
                TabulaModelCache.render(modelType, pose, vertex, light, 1.0F, 1.0F, 1.0F, 1.0F);
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
