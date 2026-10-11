package com.fiskmods.heroes.client.render;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Ports the original articulated ModelRetractableShield meshes used by suit render effects. */
final class ShieldSuitRenderer
{
    private static final Mesh SMALL = new Mesh(2, 6, 2);
    private static final Mesh LARGE = new Mesh(3, 11, 6);

    private ShieldSuitRenderer() {}

    static void render(JsonObject effect, PoseStack pose, MultiBufferSource buffers, int packedLight,
            net.minecraft.world.entity.Entity entity, HeroModelData model, int slot,
            ModelPart anchor, ModelPart oppositeAnchor, ModelPart bodyAnchor, float suitOpacity)
    {
        if (anchor == null) return;
        float amount = model.evaluateRenderData(effect.get("data"), entity, 0.0F);
        float activity = Mth.clamp(amount * 5.0F, 0.0F, 1.0F);
        if (activity <= 0.0F) return;

        float[] offset = vector(effect.get("offset"));
        float[] rotation = vector(effect.get("rotation"));
        float[] curve = vector2(effect.get("curve"));
        float opacity = Mth.clamp(effect.has("opacity") ? effect.get("opacity").getAsFloat() : 1.0F, 0.0F, 1.0F)
                * suitOpacity;
        boolean mirror = effect.has("mirror") && effect.get("mirror").getAsBoolean();
        Mesh mesh = effect.has("large") && effect.get("large").getAsBoolean() ? LARGE : SMALL;
        mesh.setAngles(amount, curve[0], curve[1]);

        for (int pass = 0; pass < 2; ++pass)
        {
            String key = textureForPass(effect.get("texture"), pass);
            if (key == null || key.equals("null")) continue;
            ResourceLocation texture = model.resolveCustomTexture(key, entity, slot);
            if (texture == null) continue;
            RenderType type = pass == 0 ? RenderType.entityTranslucent(texture) : RenderType.eyes(texture);
            VertexConsumer consumer = buffers.getBuffer(type);
            int light = pass == 0 ? packedLight : net.minecraft.client.renderer.LightTexture.FULL_BRIGHT;

            if (mirror && anchor == bodyAnchor)
            {
                renderAttached(mesh, anchor, pose, consumer, light, opacity,
                        -lerp(2.9F, offset[0], activity), lerp(5.0F, offset[1], activity), offset[2] * activity,
                        rotation[0], rotation[1], rotation[2], false);
                renderAttached(mesh, anchor, pose, consumer, light, opacity,
                        lerp(2.9F, offset[0], activity), lerp(5.0F, offset[1], activity), offset[2] * activity,
                        rotation[0], -rotation[1], rotation[2], true);
            }
            else
            {
                renderAttached(mesh, anchor, pose, consumer, light, opacity,
                        -lerp(2.9F, offset[0], activity), lerp(5.0F, offset[1], activity), offset[2] * activity,
                        rotation[0] * activity, rotation[1] * activity, rotation[2] * activity, false);

                if (mirror && oppositeAnchor != null)
                {
                    pose.pushPose();
                    try
                    {
                        oppositeAnchor.translateAndRotate(pose);
                        pose.translate(-2.9F * (1.0F - activity) / 16.0F, 0.0D, 0.0D);
                        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
                        pose.translate(-offset[0] * activity / 16.0F, lerp(5.0F, offset[1], activity) / 16.0F,
                                offset[2] * activity / 16.0F);
                        rotate(pose, rotation[0] * activity, rotation[1] * activity, rotation[2] * activity);
                        mesh.render(pose, consumer, light, opacity);
                    }
                    finally { pose.popPose(); }
                }
            }
        }
    }

    private static void renderAttached(Mesh mesh, ModelPart anchor, PoseStack pose, VertexConsumer consumer,
            int light, float opacity, float x, float y, float z, float rx, float ry, float rz, boolean flipZ)
    {
        pose.pushPose();
        try
        {
            anchor.translateAndRotate(pose);
            pose.translate(x / 16.0F, y / 16.0F, z / 16.0F);
            if (flipZ) pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(180.0F));
            rotate(pose, rx, ry, rz);
            mesh.render(pose, consumer, light, opacity);
        }
        finally { pose.popPose(); }
    }

    private static void rotate(PoseStack pose, float x, float y, float z)
    {
        pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(x));
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(y));
        pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(z));
    }

    private static String textureForPass(JsonElement textures, int pass)
    {
        if (textures == null || textures.isJsonNull()) return null;
        if (textures.isJsonArray())
        {
            JsonArray array = textures.getAsJsonArray();
            return pass < array.size() && array.get(pass).isJsonPrimitive() ? array.get(pass).getAsString() : null;
        }
        return pass == 0 && textures.isJsonPrimitive() ? textures.getAsString() : null;
    }

    private static float[] vector(JsonElement element)
    {
        float[] result = new float[3];
        if (element != null && element.isJsonArray())
        {
            JsonArray array = element.getAsJsonArray();
            for (int i = 0; i < Math.min(3, array.size()); ++i) result[i] = array.get(i).getAsFloat();
        }
        return result;
    }

    private static float[] vector2(JsonElement element)
    {
        float[] result = new float[2];
        if (element != null && element.isJsonArray())
        {
            JsonArray array = element.getAsJsonArray();
            for (int i = 0; i < Math.min(2, array.size()); ++i) result[i] = (float) Math.toRadians(array.get(i).getAsDouble());
        }
        return result;
    }

    private static float lerp(float start, float end, float delta) { return start + (end - start) * delta; }

    private static final class Mesh
    {
        private final int segments, center, width, textureWidth, textureHeight;
        private final Shape[] spine;
        private final Shape[][][] sides;

        Mesh(int segments, int spineLength, int spineCenter)
        {
            this.segments = segments;
            center = spineCenter - 1;
            width = segments * 4 + 2;
            int tw = 32, th = 16;
            while (tw < width * 2) tw *= 2;
            while (th < spineLength * 2) th *= 2;
            textureWidth = tw; textureHeight = th;
            spine = new Shape[spineLength];
            sides = new Shape[spineLength][2][segments];

            for (int i = 0; i < spineLength; ++i)
            {
                int y = i < center ? -2 : 0;
                spine[i] = new Shape(-1, y, 2, 2, segments * 2, i * 2);
                if (i > center) spine[i - 1].children.add(spine[i]);
                else if (i > 0) spine[i].children.add(spine[i - 1]);
                for (int side = 0; side < 2; ++side)
                {
                    Shape previous = spine[i];
                    int direction = side * 2 - 1;
                    for (int segment = 0; segment < segments; ++segment)
                    {
                        Shape part = new Shape(-side * 2, y, 2, 2,
                                segments * 2 + direction * (segment + 1) * 2, i * 2);
                        previous.children.add(part);
                        previous = part;
                        sides[i][side][segment] = part;
                    }
                }
            }
        }

        void setAngles(float amount, float curveX, float curveZ)
        {
            int max = Math.max(spine.length - center, center);
            for (int i = 0; i < spine.length; ++i)
            {
                float open = Mth.clamp(i < center ? amount * max - center + i + 1.0F
                        : amount * max - i + center, 0.0F, 1.0F);
                float closed = 1.0F - open;
                if (i != center)
                {
                    int direction = i < center ? -1 : 1;
                    spine[i].ty = 2.0F * direction * open + (i == center - 1 ? 2.0F : 0.0F);
                    spine[i].rx = 0.0F;
                    if (i != center - 1) spine[i].rz = -curveX * direction * open;
                }
                for (int side = 0; side < 2; ++side)
                {
                    int direction = side * 2 - 1;
                    for (int segment = 0; segment < segments; ++segment)
                    {
                        Shape part = sides[i][side][segment];
                        part.tz = (segment == 0 ? direction : 0.0F) - 2.0F * direction * open;
                        part.tx = 0.1F * closed;
                        part.ry = -curveZ * direction * open;
                    }
                }
            }
        }

        void render(PoseStack pose, VertexConsumer consumer, int light, float opacity)
        {
            renderShape(spine[center], pose, consumer, light, opacity);
        }

        private void renderShape(Shape shape, PoseStack pose, VertexConsumer consumer, int light, float opacity)
        {
            pose.pushPose();
            pose.translate(shape.tx / 16.0F, shape.ty / 16.0F, shape.tz / 16.0F);
            pose.mulPose(com.mojang.math.Axis.ZP.rotation(shape.rz));
            pose.mulPose(com.mojang.math.Axis.YP.rotation(shape.ry));
            pose.mulPose(com.mojang.math.Axis.XP.rotation(shape.rx));
            float y = shape.y, z = shape.x, w = shape.w, h = shape.h;
            float u = shape.u, v = shape.v;
            emit(consumer, pose, 0, y, z + w, u, v, light, opacity);
            emit(consumer, pose, 0, y + h, z + w, u, v + h, light, opacity);
            emit(consumer, pose, 0, y + h, z, u + w, v + h, light, opacity);
            emit(consumer, pose, 0, y, z, u + w, v, light, opacity);
            emit(consumer, pose, 0, y + h, z, u + width + w, v + h, light, opacity);
            emit(consumer, pose, 0, y + h, z + w, u + width, v + h, light, opacity);
            emit(consumer, pose, 0, y, z + w, u + width, v, light, opacity);
            emit(consumer, pose, 0, y, z, u + width + w, v, light, opacity);
            for (Shape child : shape.children) renderShape(child, pose, consumer, light, opacity);
            pose.popPose();
        }

        private void emit(VertexConsumer consumer, PoseStack pose, float x, float y, float z,
                float u, float v, int light, float opacity)
        {
            consumer.vertex(pose.last().pose(), x / 16.0F, y / 16.0F, z / 16.0F)
                    .color(255, 255, 255, Math.round(opacity * 255.0F))
                    .uv(u / textureWidth, v / textureHeight).overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(light).normal(pose.last().normal(), 1.0F, 0.0F, 0.0F).endVertex();
        }
    }

    private static final class Shape
    {
        final float x,y,w,h,u,v;
        final List<Shape> children = new ArrayList<>();
        float tx,ty,tz,rx,ry,rz;
        Shape(float x,float y,float w,float h,float u,float v)
        { this.x=x;this.y=y;this.w=w;this.h=h;this.u=u;this.v=v; }
    }

}
