package com.fiskmods.heroes.pack.js;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.script.ScriptEngine;

import com.fiskmods.heroes.common.hero.Hero;
import com.fiskmods.heroes.common.hero.HeroAttribute;
import com.fiskmods.heroes.common.hero.ItemHeroArmor;
import com.fiskmods.heroes.common.item.ModItems;
import com.fiskmods.heroes.pack.ScriptFunction;
import com.fiskmods.heroes.pack.js.JSContext;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * The {@code hero} object handed to a pack script's {@code init(hero)} function. This is the port
 * of the original's {@code Heroes} scripting facade: every method maps onto a hero builder call.
 */
public class JSHero
{
    private final Hero hero;
    private final ScriptEngine engine;
    private final Map<String, Map<HeroAttribute, Hero.AttributeMod>> pendingProfiles = new LinkedHashMap<>();
    private final Map<String, Boolean> pendingInherits = new LinkedHashMap<>();
    private Object currentProfile;

    public JSHero(Hero hero, ScriptEngine engine)
    {
        this.hero = hero;
        this.engine = engine;
    }

    /* --- Identity --- */

    public void setName(String key)
    {
        hero.setName(key);
    }

    public void setVersion(String key)
    {
        hero.setVersion(key);
    }

    public void setAliases(String... aliases)
    {
        hero.setAliases(aliases);
    }

    public void setTier(int tier)
    {
        hero.setTier(tier);
    }

    /** Original packs supply a function of the entity; numeric overrides remain supported. */
    public void setTierOverride(Object value)
    {
        if (value instanceof Number number)
        {
            hero.setTierOverride(number.intValue());
            hero.setTierOverrideFunction(null);
        }
        else
        {
            hero.setTierOverride(-1);
            hero.setTierOverrideFunction(JSContext.wrap(value));
        }
    }

    public void setDefaultScale(double scale)
    {
        hero.setDefaultScale(scale);
    }

    public void hide()
    {
        hero.hide();
    }

    public void setMaskToggle(boolean value)
    {
        hero.setMaskToggle(value);
    }

    /* --- Armour --- */

    public void setHelmet(String armorType)
    {
        hero.setHelmet(armorType);
    }

    public void setChestplate(String armorType)
    {
        hero.setChestplate(armorType);
    }

    public void setLeggings(String armorType)
    {
        hero.setLeggings(armorType);
    }

    public void setBoots(String armorType)
    {
        hero.setBoots(armorType);
    }

    public void setArmor(int slot, String armorType)
    {
        hero.setArmor(slot, armorType);
    }

    /* --- Attributes --- */

    public void addAttribute(String attribute, double amount, int operation)
    {
        hero.addAttribute(attribute, amount, operation);
    }

    public void addAttribute(String attribute, double amount)
    {
        hero.addAttribute(attribute, amount, 0);
    }

    /** {@code hero.addAttributeProfile("SUPER_BOOST", profile => { ... })} */
    public void addAttributeProfile(String name, Object function)
    {
        Map<HeroAttribute, Hero.AttributeMod> map = new LinkedHashMap<>();
        boolean inherited = false;

        com.fiskmods.heroes.pack.ScriptFunction fn = JSContext.wrap(function);

        if (fn != null)
        {
            JSProfileBuilder builder = new JSProfileBuilder(hero, name, map);
            fn.call(builder);
            inherited = builder.inherits;
            hero.setProfileRevokesAugments(name, builder.revokesAugments);
        }

        hero.addAttributeProfile(name, map, inherited);
    }

    public void setAttributeProfile(Object function)
    {
        ScriptFunction wrapper = JSContext.wrap(function);

        if (wrapper != null)
        {
            hero.setAttributeProfileFunc(wrapper);
        }
    }

    /* --- Keybinds --- */

    public void addKeyBind(String name, String keyName, int index)
    {
        hero.addKeyBind(name, keyName, index);
    }

    public void addKeyBindFunc(String name, Object function)
    {
        ScriptFunction wrapper = JSContext.wrap(function);

        if (wrapper != null)
        {
            hero.addKeyBindFunc(name, wrapper);
        }
    }

    /** Original packs may declare a key name and key index together with the callback. */
    public void addKeyBindFunc(String name, Object function, String keyName, int index)
    {
        addKeyBind(name, keyName, index);
        addKeyBindFunc(name, function);
    }

    public void setKeyBindEnabled(Object function)
    {
        ScriptFunction wrapper = JSContext.wrap(function);

        if (wrapper != null)
        {
            hero.setKeyBindEnabledFunc(wrapper);
        }
    }

    public void setModifierEnabled(Object function)
    {
        ScriptFunction wrapper = JSContext.wrap(function);

        if (wrapper != null)
        {
            hero.setModifierEnabledFunc(wrapper);
        }
    }

    public void setHasPermission(Object function)
    {
        ScriptFunction wrapper = JSContext.wrap(function);

        if (wrapper != null)
        {
            hero.setPermissionFunc(wrapper);
        }
    }

    public void setHasProperty(Object function)
    {
        ScriptFunction wrapper = JSContext.wrap(function);

        if (wrapper != null)
        {
            hero.setPropertyFunc(wrapper);
        }
    }

    public void setDamageProfile(Object function)
    {
        ScriptFunction wrapper = JSContext.wrap(function);

        if (wrapper != null)
        {
            hero.setDamageProfileFunc(wrapper);
        }
    }

    public void addDamageProfile(String name, Object types)
    {
        Map<String, Double> map = new LinkedHashMap<>();
        Map<String, Object> definition = JSContext.asMap(types);
        Object typeValues = definition.get("types");
        Map<String, Object> values = typeValues == null ? definition : JSContext.asMap(typeValues);

        for (Map.Entry<String, Object> entry : values.entrySet())
        {
            if (entry.getValue() instanceof Number n)
            {
                map.put(entry.getKey().toUpperCase(java.util.Locale.ROOT), n.doubleValue());
            }
        }

        hero.addDamageProfile(name, map);
    }

    public void setTickHandler(Object function)
    {
        ScriptFunction wrapper = JSContext.wrap(function);

        if (wrapper != null)
        {
            hero.setTickHandler(wrapper);
        }
    }

    public void supplyFunction(String name, Object function)
    {
        ScriptFunction wrapper = JSContext.wrap(function);

        if (wrapper != null)
        {
            hero.supplyFunction(name, wrapper);
        }
    }

    /* --- Equipment / powers --- */

    public void addPowers(String... powers)
    {
        for (String power : powers)
        {
            hero.addPowers(com.fiskmods.heroes.FiskHeroes.id(power));
        }
    }

    public void addEquipment(String item, boolean primary)
    {
        ItemStack stack = createStack(item);

        if (!stack.isEmpty())
        {
            hero.addEquipment(stack, primary);
        }
    }

    public void addEquipment(String item)
    {
        addEquipment(item, false);
    }

    public void addPrimaryEquipment(String item, boolean primary)
    {
        addPrimaryEquipment(item, primary, null);
    }

    /** Original packs may attach an item predicate to a primary equipment candidate. */
    public void addPrimaryEquipment(String item, boolean primary, Object predicate)
    {
        ItemStack stack = createStack(item);
        if (!stack.isEmpty())
        {
            hero.addPrimaryEquipment(stack, primary, JSContext.wrap(predicate));
        }
    }

    public void addSoundEvent(String name, Object sounds)
    {
        for (ResourceLocation id : soundIds(sounds)) hero.addSoundEvent(name, id);
    }

    public void addSoundOverrides(String name, Object overrides)
    {
        // The original format nests suit triggers under "suit". Power-target
        // overrides are separate and are not interpreted by this suit dispatcher.
        Object suit = JSContext.asMap(overrides).get("suit");
        for (Map.Entry<String, Object> entry : JSContext.asMap(suit).entrySet())
            hero.addSuitSoundOverride(name, entry.getKey(), soundIds(entry.getValue()));
    }

    private static java.util.List<ResourceLocation> soundIds(Object sounds)
    {
        java.util.List<ResourceLocation> result = new java.util.ArrayList<>();
        if (sounds instanceof String sound)
        {
            ResourceLocation id = ResourceLocation.tryParse(sound);
            if (id != null) result.add(id);
        }
        else if (sounds instanceof java.util.List<?> list)
        {
            for (Object value : list) result.addAll(soundIds(value));
        }
        else if (sounds instanceof Object[] array)
        {
            for (Object value : array) result.addAll(soundIds(value));
        }
        else if (sounds instanceof Map<?, ?>)
        {
            // Nashorn arrays are ScriptObjectMirror maps with indexed entries.
            for (Object value : JSContext.asMap(sounds).values()) result.addAll(soundIds(value));
        }
        return result;
    }

    /* --- Helpers --- */

    private static ItemStack createStack(String name)
    {
        if (name == null || name.isBlank())
        {
            return ItemStack.EMPTY;
        }

        // Fisk's 1.7.10 packs use keys in the form namespace:item@metadata{nbt}.
        // ResourceLocation.tryParse rejects that entire legacy key, which used to silently drop
        // every equipment declaration carrying a metadata value or an NBT variant.
        String itemKey = name.trim();
        net.minecraft.nbt.CompoundTag tag = null;
        int tagStart = itemKey.indexOf('{');
        if (tagStart >= 0)
        {
            if (!itemKey.endsWith("}"))
            {
                return ItemStack.EMPTY;
            }
            try
            {
                tag = net.minecraft.nbt.TagParser.parseTag(itemKey.substring(tagStart));
            }
            catch (com.mojang.brigadier.exceptions.CommandSyntaxException exception)
            {
                return ItemStack.EMPTY;
            }
            itemKey = itemKey.substring(0, tagStart);
        }

        int metadata = 0;
        int metadataStart = itemKey.lastIndexOf('@');
        if (metadataStart >= 0)
        {
            try
            {
                metadata = Integer.parseInt(itemKey.substring(metadataStart + 1));
            }
            catch (NumberFormatException exception)
            {
                return ItemStack.EMPTY;
            }
            itemKey = itemKey.substring(0, metadataStart);
        }

        ResourceLocation id = ResourceLocation.tryParse(itemKey);

        if (id == null)
        {
            return ItemStack.EMPTY;
        }

        var item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(id);
        if (item == null)
        {
            return ItemStack.EMPTY;
        }

        ItemStack stack = new ItemStack(item);
        if (metadata != 0)
        {
            stack.setDamageValue(metadata);
        }
        if (tag != null)
        {
            stack.setTag(tag);
        }
        return stack;
    }

    /** Builder passed to {@code addAttributeProfile} scripts. */
    public static class JSProfileBuilder
    {
        private final Hero hero;
        private final String profile;
        private final Map<HeroAttribute, Hero.AttributeMod> map;
        private boolean inherits = false;
        private boolean revokesAugments;

        JSProfileBuilder(Hero hero, String profile, Map<HeroAttribute, Hero.AttributeMod> map)
        {
            this.hero = hero;
            this.profile = profile;
            this.map = map;
        }

        public void revokeAugments()
        {
            // Augments granted by the hero are dropped while this profile is active
            revokesAugments = true;
        }

        public void inheritDefaults()
        {
            inherits = true;
        }

        public void resetDefaults()
        {
            inherits = false;
        }

        public void addAttribute(String attribute, double amount, int operation)
        {
            HeroAttribute attr = HeroAttribute.byName(attribute);

            if (attr != null)
            {
                map.put(attr, new Hero.AttributeMod(amount, operation));
            }
        }

        public void addAttribute(String attribute, double amount)
        {
            addAttribute(attribute, amount, 0);
        }
    }
}
