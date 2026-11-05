package com.fiskmods.heroes.common.block;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.hero.Hero;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Loads and caches the 67 Suit Fabricator recipes from
 * {@code assets/fiskheroes/data/fabricator/recipes/*.json}.
 */
public final class FabricatorRecipes
{
    public record Recipe(boolean cosmic, Map<Item, Integer> items)
    {
    }

    private static final Map<ResourceLocation, Recipe> RECIPES = new LinkedHashMap<>();
    private static boolean loaded;

    private FabricatorRecipes()
    {
    }

    public static synchronized void ensureLoaded()
    {
        if (loaded && !RECIPES.isEmpty()) return;
        RECIPES.clear();
        for (Hero hero : Hero.REGISTRY.getValues())
        {
            ResourceLocation id = hero.getRegistryName();
            String resourcePath = "/assets/" + id.getNamespace() + "/data/fabricator/recipes/" + id.getPath() + ".json";
            try (InputStream stream = FiskHeroes.class.getResourceAsStream(resourcePath))
            {
                if (stream == null) continue;
                JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
                boolean cosmic = root.has("isCosmic") && root.get("isCosmic").getAsBoolean();
                Map<Item, Integer> map = new LinkedHashMap<>();
                if (root.has("items") && root.get("items").isJsonObject())
                {
                    for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("items").entrySet())
                    {
                        ResourceLocation itemId = ResourceLocation.tryParse(entry.getKey());
                        if (itemId == null) continue;
                        Item item = ForgeRegistries.ITEMS.getValue(itemId);
                        if (item != null && item != Items.AIR)
                        {
                            map.put(item, entry.getValue().getAsInt());
                        }
                    }
                }
                if (!map.isEmpty())
                {
                    RECIPES.put(id, new Recipe(cosmic, Collections.unmodifiableMap(map)));
                }
            }
            catch (Exception e)
            {
                FiskHeroes.LOGGER.warn("Failed to load fabricator recipe for {}: {}", id, e.getMessage());
            }
        }
        loaded = true;
    }

    @Nullable
    public static Recipe get(Hero hero)
    {
        if (hero == null) return null;
        ensureLoaded();
        return RECIPES.get(hero.getRegistryName());
    }

    public static List<Hero> getCraftableHeroes()
    {
        ensureLoaded();
        List<Hero> list = new ArrayList<>();
        for (Hero hero : Hero.REGISTRY.getSortedHeroes())
        {
            if (!hero.isHidden() && hero.getPiecesToSet() > 0 && RECIPES.containsKey(hero.getRegistryName()))
            {
                list.add(hero);
            }
        }
        return list;
    }
}
