package com.fiskmods.heroes.client.render;

import com.fiskmods.heroes.FiskHeroes;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.phys.Vec3;

/** Loads the original .tbl Tabula models used by suit attachments and renders their cube hierarchy. */
final class TabulaModelCache
{
    private static final Map<ResourceLocation, ModelPart> MODELS = new HashMap<>();

    private TabulaModelCache()
    {
    }

    static void clear()
    {
        MODELS.clear();
    }

    static void render(String modelType, PoseStack pose, VertexConsumer vertex, int light,
            float red, float green, float blue, float alpha)
    {
        render(modelType, pose, vertex, light, red, green, blue, alpha, 0.0F);
    }

    static void render(String modelType, PoseStack pose, VertexConsumer vertex, int light,
            float red, float green, float blue, float alpha, float hatTip)
    {
        ResourceLocation id = ResourceLocation.tryParse(modelType);
        if (id == null) id = FiskHeroes.id(modelType);
        ModelPart model = MODELS.computeIfAbsent(id, TabulaModelCache::load);
        if (model != null)
        {
            model.getAllParts().forEach(ModelPart::resetPose);
            if (id.equals(FiskHeroes.id("sombrero")) && Float.isFinite(hatTip))
            {
                float progress = net.minecraft.util.Mth.clamp(hatTip, 0.0F, 1.0F);
                progress = net.minecraft.util.Mth.sin((float) Math.PI * (1.0F - progress));
                float rotation = progress * progress * progress * progress * progress * 0.17F;
                for (int ring = 1; ring <= 7; ++ring)
                {
                    try
                    {
                        model.getChild("ring" + ring + "_1_root_" + (ring - 1)).xRot += rotation;
                    }
                    catch (java.util.NoSuchElementException ignored)
                    {
                        // Some model revisions may omit a ring; animate the parts they provide.
                    }
                }
            }
            model.render(pose, vertex, light,
                    net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, red, green, blue, alpha);
        }
    }

    private static ModelPart load(ResourceLocation id)
    {
        ResourceLocation archive = new ResourceLocation(id.getNamespace(), "models/tabula/" + id.getPath() + ".tbl");
        try
        {
            Resource resource = Minecraft.getInstance().getResourceManager().getResource(archive)
                    .orElseThrow(() -> new java.io.FileNotFoundException(archive.toString()));
            try (InputStream input = resource.open(); ZipInputStream zip = new ZipInputStream(input))
            {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null)
                {
                    if (!entry.getName().equals("model.json")) continue;
                    JsonObject json = JsonParser.parseReader(new InputStreamReader(zip, StandardCharsets.UTF_8)).getAsJsonObject();
                    MeshDefinition mesh = new MeshDefinition();
                    PartDefinition root = mesh.getRoot();
                    if (json.has("cubes") && json.get("cubes").isJsonArray())
                    {
                        int index = 0;
                        for (JsonElement element : json.getAsJsonArray("cubes"))
                        {
                            if (element.isJsonObject()) addCubeTree(root, element.getAsJsonObject(), new Vec3(1.0D, 1.0D, 1.0D), "root_" + index++);
                        }
                    }
                    int width = integer(json, "textureWidth", 64);
                    int height = integer(json, "textureHeight", 32);
                    return LayerDefinition.create(mesh, width, height).bakeRoot();
                }
            }
        }
        catch (Exception e)
        {
            FiskHeroes.LOGGER.warn("Could not load Tabula suit model {}: {}", archive, e.toString());
        }
        return null;
    }

    private static void addCubeTree(PartDefinition parent, JsonObject cube, Vec3 parentScale, String fallbackName)
    {
        if (cube.has("hidden") && cube.get("hidden").getAsBoolean()) return;
        JsonArray dimensions = array(cube, "dimensions", 3);
        JsonArray offset = array(cube, "offset", 3);
        JsonArray position = array(cube, "position", 3);
        JsonArray rotation = array(cube, "rotation", 3);
        JsonArray scale = array(cube, "scale", 3);

        float sx = (float) parentScale.x * number(scale, 0, 1.0F);
        float sy = (float) parentScale.y * number(scale, 1, 1.0F);
        float sz = (float) parentScale.z * number(scale, 2, 1.0F);
        String name = cube.has("name") ? cube.get("name").getAsString() : fallbackName;
        String partName = (name + "_" + fallbackName).replaceAll("[^A-Za-z0-9_./-]", "_");

        CubeListBuilder geometry = CubeListBuilder.create();
        if (cube.has("txOffset") && cube.get("txOffset").isJsonArray())
        {
            JsonArray uv = cube.getAsJsonArray("txOffset");
            geometry.texOffs((int) number(uv, 0, 0.0F), (int) number(uv, 1, 0.0F));
        }
        geometry.mirror(cube.has("txMirror") && cube.get("txMirror").getAsBoolean());
        geometry.addBox(number(offset, 0, 0.0F) * sx, number(offset, 1, 0.0F) * sy,
                number(offset, 2, 0.0F) * sz, number(dimensions, 0, 0.0F) * sx,
                number(dimensions, 1, 0.0F) * sy, number(dimensions, 2, 0.0F) * sz,
                new CubeDeformation(number(cube, "mcScale", 0.0F) * (sx + sy + sz) / 3.0F));

        PartPose pose = PartPose.offsetAndRotation(number(position, 0, 0.0F) * (float) parentScale.x,
                number(position, 1, 0.0F) * (float) parentScale.y, number(position, 2, 0.0F) * (float) parentScale.z,
                (float) Math.toRadians(number(rotation, 0, 0.0F)),
                (float) Math.toRadians(number(rotation, 1, 0.0F)),
                (float) Math.toRadians(number(rotation, 2, 0.0F)));
        PartDefinition part = parent.addOrReplaceChild(partName, geometry, pose);
        if (cube.has("children") && cube.get("children").isJsonArray())
        {
            int index = 0;
            for (JsonElement child : cube.getAsJsonArray("children"))
            {
                if (child.isJsonObject()) addCubeTree(part, child.getAsJsonObject(), new Vec3(sx, sy, sz), name + "_" + index++);
            }
        }
    }

    private static JsonArray array(JsonObject object, String key, int minimumSize)
    {
        if (object.has(key) && object.get(key).isJsonArray() && object.getAsJsonArray(key).size() >= minimumSize)
        {
            return object.getAsJsonArray(key);
        }
        JsonArray fallback = new JsonArray();
        for (int i = 0; i < minimumSize; i++) fallback.add(0.0F);
        return fallback;
    }

    private static int integer(JsonObject object, String key, int fallback)
    {
        return object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsInt() : fallback;
    }

    private static float number(JsonObject object, String key, float fallback)
    {
        return object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsFloat() : fallback;
    }

    private static float number(JsonArray array, int index, float fallback)
    {
        return array != null && index < array.size() && array.get(index).isJsonPrimitive()
                ? array.get(index).getAsFloat() : fallback;
    }
}
