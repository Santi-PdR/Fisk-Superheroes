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

    /** Overrides the tier used for suit piece ordering; {@code -1} clears the override. */
    public void setTierOverride(int tier)
    {
        hero.setTierOverride(tier);
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
        boolean inherited = true;

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

        for (Map.Entry<String, Object> entry : JSContext.asMap(types).entrySet())
        {
            map.put(entry.getKey(), entry.getValue() instanceof Number n ? n.doubleValue() : 0.0D);
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
        addEquipment(item, primary);
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

    public void addSoundEvent(String name, String sound)
    {
        ResourceLocation id = ResourceLocation.tryParse(sound);

        if (id != null)
        {
            hero.addSoundEvent(name, id);
        }
    }

    public void addSoundOverrides(String name, Object overrides)
    {
        Map<Integer, String> map = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : JSContext.asMap(overrides).entrySet())
        {
            if (entry.getValue() instanceof String string)
            {
                try
                {
                    map.put(Integer.parseInt(entry.getKey()), string);
                }
                catch (NumberFormatException e)
                {
                    // Not an indexed override; ignore
                }
            }
        }

        hero.addSoundOverrides(name, map);
    }

    /* --- Helpers --- */

    private static ItemStack createStack(String name)
    {
        ResourceLocation id = ResourceLocation.tryParse(name);

        if (id == null)
        {
            return ItemStack.EMPTY;
        }

        var item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(id);
        return item != null ? new ItemStack(item) : ItemStack.EMPTY;
    }

    /** Builder passed to {@code addAttributeProfile} scripts. */
    public static class JSProfileBuilder
    {
        private final Hero hero;
        private final String profile;
        private final Map<HeroAttribute, Hero.AttributeMod> map;
        private boolean inherits = true;
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
