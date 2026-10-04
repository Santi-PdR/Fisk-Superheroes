package com.fiskmods.heroes.client.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.client.texture.TextureResolver;
import com.fiskmods.heroes.common.hero.HeroIteration;
import com.fiskmods.heroes.common.hero.ItemHeroArmor;
import com.fiskmods.heroes.pack.ScriptFunction;
import com.fiskmods.heroes.pack.js.JSExpressions;
import com.fiskmods.heroes.pack.js.JSEntity;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * A hero's suit model, loaded from {@code assets/<domain>/models/heroes/<hero>.json}.
 * <p>
 * The format is the one the original mod shipped:
 * <ul>
 * <li>{@code resources} - named texture sources (a sprite, or a {@code .tx.json} definition),</li>
 * <li>{@code texture} / {@code lights} - condition trees resolved per entity, keyed by
 * {@code vars:NAME} with a {@code default} fallback,</li>
 * <li>{@code vars} - script expressions for the condition names the model uses,</li>
 * <li>{@code showModel} - which player model parts each armour piece draws,</li>
 * <li>{@code fixHatLayer}, {@code custom} and {@code animations} - renderer behaviour.</li>
 * </ul>
 * Model files may declare a {@code parent}; the parent's values are inherited (the child wins).
 */
public class HeroModelData
{
    private final ResourceLocation hero;
    private String parent;
    private final Map<String, String> resources = new LinkedHashMap<>();
    private final Map<String, Set<String>> showModel = new LinkedHashMap<>();
    private final List<String> fixHatLayer = new ArrayList<>();
    private final Map<String, JsonObject> custom = new LinkedHashMap<>();
    private final Map<String, JsonObject> animations = new LinkedHashMap<>();
    private final Map<String, String> vars = new LinkedHashMap<>();
    private final Map<String, String> itemIcons = new LinkedHashMap<>();
    private final Map<String, ScriptFunction> renderExpressions = new HashMap<>();

    private JsonElement texture;
    private JsonElement lights;
    private final Map<Integer, JsonElement> renderLayerTextures = new LinkedHashMap<>();
    private final Map<Integer, JsonElement> renderLayerLights = new LinkedHashMap<>();
    private JsonElement defaultTexture;
    private JsonElement defaultLights;

    public HeroModelData(ResourceLocation hero)
    {
        this.hero = hero;
    }

    public ResourceLocation getHero()
    {
        return hero;
    }

    public String getParent()
    {
        return parent;
    }

    public Map<String, JsonObject> getCustom()
    {
        return custom;
    }

    /** Resolves this hero's active data-driven speed trail, if its conditions are met. */
    @Nullable
    public TrailDefinition getTrail(Entity entity)
    {
        return getTrail(entity, true);
    }

    /** Resolves the configured resource without applying its movement conditionals. */
    @Nullable
    public TrailDefinition getTrail(Entity entity, boolean testConditionals)
    {
        JsonObject effect = custom.get("fiskheroes:trail");
        if (effect == null || !effect.has("type")) return null;

        if (testConditionals && effect.has("conditionals") && effect.get("conditionals").isJsonArray())
        {
            for (JsonElement condition : effect.getAsJsonArray("conditionals"))
            {
                if (!condition.isJsonPrimitive()) return null;
                String expression = condition.getAsString();
                if (expression.startsWith("vars:") && !evaluate(expression.substring("vars:".length()), entity)) return null;
            }
        }

        String type = effect.get("type").getAsString();
        if (type.startsWith("builtin/lightning_rgb_"))
        {
            ResourceLocation builtinId = new ResourceLocation(FiskHeroes.MODID, type);
            TrailDefinition base = TrailRegistry.get(FiskHeroes.id("velocity_nine"));
            return base != null ? base.withId(builtinId) : null;
        }
        ResourceLocation id = ResourceLocation.tryParse(type);
        return id != null ? TrailRegistry.get(id) : null;
    }

    public Map<String, JsonObject> getAnimations()
    {
        return animations;
    }

    public List<String> getFixHatLayer()
    {
        return fixHatLayer;
    }

    public Map<String, String> getItemIcons()
    {
        return itemIcons;
    }

    /* --- Parsing --- */

    public static HeroModelData parse(ResourceLocation hero, JsonObject json)
    {
        HeroModelData data = new HeroModelData(hero);

        if (json.has("parent"))
        {
            data.parent = json.get("parent").getAsString();
        }

        if (json.has("resources"))
        {
            for (Map.Entry<String, JsonElement> e : json.getAsJsonObject("resources").entrySet())
            {
                data.resources.put(e.getKey(), e.getValue().getAsString());
            }
        }

        if (json.has("vars"))
        {
            for (Map.Entry<String, JsonElement> e : json.getAsJsonObject("vars").entrySet())
            {
                data.vars.put(e.getKey().toUpperCase(Locale.ROOT), e.getValue().getAsString());
            }
        }

        if (json.has("texture"))
        {
            data.texture = json.get("texture");
            readRenderLayers(data.texture, data.renderLayerTextures, data);
        }

        if (json.has("lights"))
        {
            data.lights = json.get("lights");
            readRenderLayers(data.lights, data.renderLayerLights, data);
        }

        if (json.has("showModel"))
        {
            for (Map.Entry<String, JsonElement> e : json.getAsJsonObject("showModel").entrySet())
            {
                Set<String> slots = new java.util.LinkedHashSet<>();

                for (JsonElement element : e.getValue().getAsJsonArray())
                {
                    slots.add(element.getAsString().toUpperCase(Locale.ROOT));
                }

                data.showModel.put(e.getKey().toLowerCase(Locale.ROOT), slots);
            }
        }

        if (json.has("fixHatLayer"))
        {
            for (JsonElement element : json.getAsJsonArray("fixHatLayer"))
            {
                data.fixHatLayer.add(element.getAsString().toUpperCase(Locale.ROOT));
            }
        }

        if (json.has("custom"))
        {
            for (Map.Entry<String, JsonElement> e : json.getAsJsonObject("custom").entrySet())
            {
                if (e.getValue().isJsonObject())
                {
                    data.custom.put(e.getKey(), e.getValue().getAsJsonObject());
                }
            }
        }

        if (json.has("animations"))
        {
            for (Map.Entry<String, JsonElement> e : json.getAsJsonObject("animations").entrySet())
            {
                if (e.getValue().isJsonObject())
                {
                    data.animations.put(e.getKey(), e.getValue().getAsJsonObject());
                }
            }
        }

        if (json.has("itemIcons"))
        {
            for (Map.Entry<String, JsonElement> e : json.getAsJsonObject("itemIcons").entrySet())
            {
                data.itemIcons.put(e.getKey().toUpperCase(Locale.ROOT), e.getValue().getAsString());
            }
        }

        return data;
    }

    private static void readRenderLayers(JsonElement tree, Map<Integer, JsonElement> output, HeroModelData data)
    {
        if (tree == null || !tree.isJsonObject())
        {
            return;
        }

        JsonObject json = tree.getAsJsonObject();

        if (json.has("renderLayer") && json.get("renderLayer").isJsonObject())
        {
            for (Map.Entry<String, JsonElement> e : json.getAsJsonObject("renderLayer").entrySet())
            {
                int slot = slot(e.getKey());

                if (slot != -1)
                {
                    output.put(slot, e.getValue());
                }
            }
        }

        if (json.has("default"))
        {
            if (tree == data.lights)
            {
                data.defaultLights = json.get("default");
            }
            else
            {
                data.defaultTexture = json.get("default");
            }
        }
    }

    /** Merges a parent model's values into this one; the child always wins. */
    public void inherit(HeroModelData parent)
    {
        parent.resources.forEach(resources::putIfAbsent);
        parent.vars.forEach(vars::putIfAbsent);
        parent.showModel.forEach((part, slots) -> showModel.computeIfAbsent(part, k -> new java.util.LinkedHashSet<>()).addAll(slots));
        parent.custom.forEach(custom::putIfAbsent);
        parent.animations.forEach(animations::putIfAbsent);
        parent.itemIcons.forEach(itemIcons::putIfAbsent);
        parent.fixHatLayer.forEach(slot -> fixHatLayer.add(slot));
        parent.renderLayerTextures.forEach(renderLayerTextures::putIfAbsent);
        parent.renderLayerLights.forEach(renderLayerLights::putIfAbsent);

        if (defaultTexture == null)
        {
            defaultTexture = parent.defaultTexture;
        }

        if (defaultLights == null)
        {
            defaultLights = parent.defaultLights;
        }

        // Parent trees feed the child's own tree
        texture = merge(texture, parent.texture);
        lights = merge(lights, parent.lights);
    }

    private static JsonElement merge(JsonElement child, JsonElement parent)
    {
        if (child == null)
        {
            return parent;
        }

        if (parent == null || !child.isJsonObject() || !parent.isJsonObject())
        {
            return child;
        }

        JsonObject result = new JsonObject();
        parent.getAsJsonObject().entrySet().forEach(e -> result.add(e.getKey(), e.getValue()));
        child.getAsJsonObject().entrySet().forEach(e -> result.add(e.getKey(), e.getValue()));
        return result;
    }

    /* --- Resolution --- */

    /** The texture a piece draws for an entity, or null when the piece has none. */
    @Nullable
    public ResourceLocation getTexture(int slot, Entity entity)
    {
        String key = resolve(slot == -1 ? defaultTexture : renderLayerTextures.getOrDefault(slot, defaultTexture), entity);
        return resolveResource(key, entity, slot);
    }

    /** The emissive (glowing) texture a piece draws for an entity, or null. */
    @Nullable
    public ResourceLocation getLights(int slot, Entity entity)
    {
        JsonElement tree = renderLayerLights.getOrDefault(slot, defaultLights);
        String key = resolve(tree, entity);
        return resolveResource(key, entity, slot);
    }

    /** Model texture trees name entries in {@code resources}; resolve that indirection first. */
    @Nullable
    private ResourceLocation resolveResource(@Nullable String key, Entity entity, int slot)
    {
        if (key == null)
        {
            return null;
        }

        return TextureResolver.resolve(resources.getOrDefault(key, key), entity, slot);
    }

    /** Resolves a texture alias used by one of this model's custom render effects. */
    @Nullable
    public ResourceLocation resolveCustomTexture(@Nullable String key, Entity entity, int slot)
    {
        return resolveResource(key, entity, slot);
    }

    /** Evaluates the number/string forms accepted by the original render effect data field. */
    public float evaluateRenderData(@Nullable JsonElement value, Entity entity, float defaultValue)
    {
        if (value == null || value.isJsonNull())
        {
            return defaultValue;
        }
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber())
        {
            return value.getAsFloat();
        }
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString())
        {
            return defaultValue;
        }

        String expression = value.getAsString();
        if (expression.matches("^[a-zA-Z0-9_]+:[a-zA-Z0-9_./-]+$"))
        {
            Object data = JSEntity.read(entity, expression);
            if (data == null)
            {
                return defaultValue;
            }
            if (data instanceof Number number)
            {
                net.minecraft.world.entity.player.Player player = entity instanceof net.minecraft.world.entity.player.Player p ? p : null;
                com.fiskmods.heroes.common.data.var.DataVar<?> variable = com.fiskmods.heroes.common.data.DataRegistry.INSTANCE.get(expression);
                if (player != null && variable != null && variable.getType() == com.fiskmods.heroes.common.data.DataType.FLOAT_INTERP)
                {
                    return new JSEntity(entity).getInterpolatedData(expression);
                }
                return number.floatValue();
            }
            return defaultValue;
        }

        ScriptFunction function = renderExpressions.computeIfAbsent(expression, JSExpressions::compile);
        if (function == null)
        {
            return defaultValue;
        }
        Object result = function.call(new JSEntity(entity));
        return result instanceof Number number ? number.floatValue() : defaultValue;
    }

    /** Whether the given armour slot has an emissive layer at all. */
    public boolean hasLights(int slot)
    {
        JsonElement tree = renderLayerLights.getOrDefault(slot, defaultLights);

        if (tree == null)
        {
            return false;
        }

        if (tree.isJsonPrimitive())
        {
            return !"null".equals(tree.getAsString());
        }

        return true;
    }

    /**
     * Walks a condition tree and returns the resource name it selects for the entity.
     * <p>
     * Leaves may be a resource name or {@code "null"} (nothing to draw); conditions are
     * {@code vars:NAME} nodes whose {@code true}/{@code false} branches are followed.
     */
    @Nullable
    public String resolve(JsonElement node, Entity entity)
    {
        if (node == null)
        {
            return null;
        }

        if (node.isJsonPrimitive())
        {
            String value = node.getAsString();
            return "null".equals(value) ? null : value;
        }

        if (!node.isJsonObject())
        {
            return null;
        }

        for (Map.Entry<String, JsonElement> e : node.getAsJsonObject().entrySet())
        {
            String key = e.getKey();

            if (key.startsWith("vars:"))
            {
                if (evaluate(key.substring("vars:".length()), entity))
                {
                    String value = resolve(e.getValue(), entity);

                    if (value != null)
                    {
                        return value;
                    }
                }
                else if (e.getValue().isJsonObject() && e.getValue().getAsJsonObject().has("false"))
                {
                    String value = resolve(e.getValue().getAsJsonObject().get("false"), entity);

                    if (value != null)
                    {
                        return value;
                    }
                }
            }
            else if (e.getValue().isJsonObject())
            {
                String selected = selectorValue(key, entity);
                JsonElement branch = selected != null ? e.getValue().getAsJsonObject().get(selected) : null;

                if (branch != null)
                {
                    String value = resolve(branch, entity);

                    if (value != null)
                    {
                        return value;
                    }
                }
            }
        }

        if (node.getAsJsonObject().has("default"))
        {
            String value = resolve(node.getAsJsonObject().get("default"), entity);

            if (value != null)
            {
                return value;
            }
        }

        // A direct true/false pair without a name (used by some render layers)
        JsonObject json = node.getAsJsonObject();

        if (json.has("true"))
        {
            return resolve(json.get("true"), entity);
        }

        return null;
    }

    /** Resolves the data-backed selectors used by the original texture trees. */
    @Nullable
    private static String selectorValue(String selector, Entity entity)
    {
        if (!(entity instanceof LivingEntity living))
        {
            return null;
        }

        return switch (selector)
        {
            case "heldItem" -> net.minecraftforge.registries.ForgeRegistries.ITEMS
                    .getKey(living.getMainHandItem().getItem()).toString();
            case "wornHelmet" -> suitType(living.getItemBySlot(EquipmentSlot.HEAD));
            case "wornChestplate" -> suitType(living.getItemBySlot(EquipmentSlot.CHEST));
            case "wornLeggings" -> suitType(living.getItemBySlot(EquipmentSlot.LEGS));
            case "wornBoots" -> suitType(living.getItemBySlot(EquipmentSlot.FEET));
            default -> null;
        };
    }

    private static String suitType(ItemStack stack)
    {
        HeroIteration iteration = ItemHeroArmor.getHero(stack);
        return iteration != null ? iteration.getFullName() : "null";
    }

    /** Evaluates a {@code vars:NAME} condition: a model variable, a built-in, or true. */
    public boolean evaluate(String name, Entity entity)
    {
        String script = vars.get(name.toUpperCase(Locale.ROOT));

        if (script != null)
        {
            ScriptFunction function = JSExpressions.compile(script);

            if (function != null)
            {
                Object result = function.call(new JSEntity(entity));
                return result instanceof Boolean b ? b : result instanceof Number n && n.doubleValue() != 0.0D;
            }

            return false;
        }

        return BUILT_IN.getOrDefault(name.toUpperCase(Locale.ROOT), false) && builtIn(name, entity);
    }

    /**
     * Conditions the renderer itself provides. They describe the state of whatever is wearing the
     * suit; in the port only the worn case exists, so the "display" states are false.
     */
    private static final Map<String, Boolean> BUILT_IN = Map.of(
            "MASK_OPEN", true,
            "SUIT_OPEN", true,
            "SPEEDING", true,
            "FLYING", true,
            "GLIDING", true,
            "SHIELD", true,
            "BLADE", true,
            "DISPLAY", true,
            "OPEN", true,
            "ANIM", true);

    private static boolean builtIn(String name, Entity entity)
    {
        String key = switch (name.toUpperCase(Locale.ROOT))
        {
            case "MASK_OPEN" -> "fiskheroes:mask_open";
            case "SUIT_OPEN" -> "fiskheroes:suit_open";
            case "SPEEDING" -> "fiskheroes:speeding";
            case "FLYING" -> "fiskheroes:flying";
            case "GLIDING" -> "fiskheroes:gliding";
            case "SHIELD" -> "fiskheroes:shield";
            case "BLADE" -> "fiskheroes:blade";
            default -> null;
        };

        if (key == null)
        {
            // DISPLAY / OPEN / ANIM only apply to the suit stands of the original, which the port
            // does not have: the suit is always worn, never displayed.
            return false;
        }

        Object value = JSEntity.read(entity, key);
        return value instanceof Boolean b ? b : value instanceof Number n && n.doubleValue() != 0.0D;
    }

    /** Whether the given player model part is drawn by the given armour slot. */
    public boolean showsModelPart(int slot, String part)
    {
        Set<String> slots = showModel.get(part.toLowerCase(Locale.ROOT));

        if (slots == null)
        {
            return true;
        }

        if (slots.isEmpty())
        {
            return false;
        }

        return slots.contains(slotName(slot));
    }

    public boolean shouldFixHatLayer(int slot)
    {
        return fixHatLayer.contains(slotName(slot));
    }

    public Map<String, String> getResources()
    {
        return resources;
    }

    private static String slotName(int slot)
    {
        return switch (slot)
        {
            case 0 -> "HELMET";
            case 1 -> "CHESTPLATE";
            case 2 -> "LEGGINGS";
            default -> "BOOTS";
        };
    }

    static int slot(String name)
    {
        return switch (name.toUpperCase(Locale.ROOT))
        {
            case "HELMET", "HEAD" -> 0;
            case "CHESTPLATE", "CHEST" -> 1;
            case "LEGGINGS", "LEGS" -> 2;
            case "BOOTS", "FEET" -> 3;
            default -> -1;
        };
    }

    public static HeroModelData load(@Nullable String name)
    {
        if (name == null)
        {
            return null;
        }

        ResourceLocation id = name.indexOf(':') == -1 ? new ResourceLocation(FiskHeroes.MODID, name) : ResourceLocation.tryParse(name);
        return id != null ? HeroModelRegistry.get(id) : null;
    }
}
