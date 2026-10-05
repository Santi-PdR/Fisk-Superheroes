package com.fiskmods.heroes.client.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fiskmods.heroes.FiskHeroes;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.phys.Vec3;

/** Client cache for the original line and circle shapes referenced by suit effects. */
public final class ShapeEffectRegistry extends SimplePreparableReloadListener<Map<ResourceLocation, ShapeEffectRegistry.Shape>>
{
    public static final ShapeEffectRegistry INSTANCE = new ShapeEffectRegistry();
    private static final Map<ResourceLocation, Shape> SHAPES = new HashMap<>();

    public static Shape get(ResourceLocation id)
    {
        return SHAPES.get(id);
    }

    @Override
    protected Map<ResourceLocation, Shape> prepare(ResourceManager manager, ProfilerFiller profiler)
    {
        Map<ResourceLocation, Shape> parsed = new HashMap<>();
        for (Map.Entry<ResourceLocation, Resource> entry : manager.listResources("models/shapes",
                path -> path.getPath().endsWith(".json")).entrySet())
        {
            try (var reader = entry.getValue().openAsReader())
            {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                ResourceLocation id = new ResourceLocation(entry.getKey().getNamespace(),
                        entry.getKey().getPath().substring("models/shapes/".length(), entry.getKey().getPath().length() - 5));
                Shape shape = parse(json);
                if (shape != null) parsed.put(id, shape);
            }
            catch (Exception e)
            {
                FiskHeroes.LOGGER.error("Failed to read suit effect shape {}", entry.getKey(), e);
            }
        }
        return parsed;
    }

    @Override
    protected void apply(Map<ResourceLocation, Shape> prepared, ResourceManager manager, ProfilerFiller profiler)
    {
        SHAPES.clear();
        SHAPES.putAll(prepared);
        FiskHeroes.LOGGER.info("Loaded {} suit effect shapes", SHAPES.size());
    }

    private static Shape parse(JsonObject json)
    {
        String format = json.has("dataFormat") ? json.get("dataFormat").getAsString() : "LINES";
        if (!json.has("shapes") || !json.get("shapes").isJsonArray()) return null;
        List<Path> paths = new ArrayList<>();
        for (JsonElement item : json.getAsJsonArray("shapes"))
        {
            if (format.equalsIgnoreCase("CIRCLES"))
            {
                JsonElement data = item.isJsonObject() ? item.getAsJsonObject().get("data") : item;
                if (data == null || !data.isJsonArray() || data.getAsJsonArray().size() < 2) continue;
                JsonArray pair = data.getAsJsonArray();
                int divisions = Math.max(3, Math.min(256, pair.get(0).getAsInt() * 8));
                double radius = pair.get(1).getAsDouble();
                double[] rotation = item.isJsonObject() ? vector(item.getAsJsonObject().get("rotation")) : new double[3];
                double maxAngle = json.has("maxAngle") ? json.get("maxAngle").getAsDouble() : 360.0D;
                List<Vec3> points = new ArrayList<>(divisions + 1);
                for (int i = 0; i <= divisions; ++i)
                {
                    double angle = Math.toRadians(maxAngle * i / divisions);
                    points.add(rotate(new Vec3(Math.cos(angle) * radius, Math.sin(angle) * radius, 0.0D), rotation));
                }
                paths.add(new Path(List.copyOf(points)));
            }
            else
            {
                JsonElement data = item.isJsonObject() ? item.getAsJsonObject().get("data") : item;
                if (data == null || !data.isJsonArray()) continue;
                JsonArray values = data.getAsJsonArray();
                // LINES shapes store one flattened XYZ path per entry in the outer array.
                // WIREFRAME and object-backed shapes store one flattened path in "data".
                if (values.size() > 0 && values.get(0).isJsonArray())
                {
                    for (JsonElement pathData : values)
                    {
                        if (!pathData.isJsonArray()) continue;
                        List<Vec3> points = readPoints(pathData.getAsJsonArray());
                        if (points.size() > 1) paths.add(new Path(List.copyOf(points)));
                    }
                    continue;
                }
                if (values.size() < 6) continue;
                List<Vec3> points = readPoints(values);
                if (format.equalsIgnoreCase("WIREFRAME") && points.size() % 8 == 0)
                {
                    int[][] edges = { {0,1}, {0,2}, {0,4}, {1,3}, {1,5}, {2,3}, {2,6}, {3,7}, {4,5}, {4,6}, {5,7}, {6,7} };
                    for (int base = 0; base < points.size(); base += 8)
                    {
                        for (int[] edge : edges)
                        {
                            paths.add(new Path(List.of(points.get(base + edge[0]), points.get(base + edge[1]))));
                        }
                    }
                }
                else if (points.size() > 1) paths.add(new Path(List.copyOf(points)));
            }
        }
        return paths.isEmpty() ? null : new Shape(List.copyOf(paths));
    }

    private static List<Vec3> readPoints(JsonArray values)
    {
        List<Vec3> points = new ArrayList<>(values.size() / 3);
        for (int i = 0; i + 2 < values.size(); i += 3)
        {
            points.add(new Vec3(values.get(i).getAsDouble(), values.get(i + 1).getAsDouble(), values.get(i + 2).getAsDouble()));
        }
        return points;
    }

    private static double[] vector(JsonElement value)
    {
        double[] result = new double[3];
        if (value != null && value.isJsonArray())
        {
            JsonArray array = value.getAsJsonArray();
            for (int i = 0; i < Math.min(3, array.size()); ++i) result[i] = array.get(i).getAsDouble();
        }
        return result;
    }

    private static Vec3 rotate(Vec3 point, double[] degrees)
    {
        double x = Math.toRadians(degrees[0]);
        double y = Math.toRadians(degrees[1]);
        double z = Math.toRadians(degrees[2]);
        double cy = Math.cos(y), sy = Math.sin(y);
        double cx = Math.cos(x), sx = Math.sin(x);
        double cz = Math.cos(z), sz = Math.sin(z);
        double px = point.x * cy + point.z * sy;
        double pz = -point.x * sy + point.z * cy;
        double py = point.y * cx - pz * sx;
        pz = point.y * sx + pz * cx;
        return new Vec3(px * cz - py * sz, px * sz + py * cz, pz);
    }

    public record Shape(List<Path> paths) {}
    public record Path(List<Vec3> points) {}
}
