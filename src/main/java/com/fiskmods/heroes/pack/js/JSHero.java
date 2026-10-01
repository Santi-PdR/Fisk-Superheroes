package com.fiskmods.heroes.pack.js;

import java.util.LinkedHashMap;
import java.util.Map;

import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;

import com.fiskmods.heroes.common.hero.Hero;
import com.fiskmods.heroes.common.hero.HeroAttribute;
import com.fiskmods.heroes.common.hero.ItemHeroArmor;
import com.fiskmods.heroes.common.item.ModItems;
import com.fiskmods.heroes.pack.ScriptFunction;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * The {@code hero} object handed to a pack script's {@code init(hero)} function. This is the port
 * of the original's {@code Heroes} scripting facade: every method maps onto a hero builder call.
 */
public class JSHero
{
    private final Hero hero;
    private final Scriptable scope;
    private final Map<String, Map<HeroAttribute, Hero.AttributeMod>> pendingProfiles = new LinkedHashMap<>();
    private final Map<String, Boolean> pendingInherits = new LinkedHashMap<>();
    private Scriptable currentProfile;

    public JSHero(Hero hero, Scriptable scope)
    {
        this.hero = hero;
        this.scope = scope;
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

        if (function instanceof Function fn)
        {
            JSProfileBuilder builder = new JSProfileBuilder(map);
            JSContext.call(scope, fn, builder);
            inherited = builder.inherits;
        }

        hero.addAttributeProfile(name, map, inherited);
    }

    public void setAttributeProfile(Object function)
    {
        ScriptFunction wrapper = JSContext.wrap(scope, function);

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
        ScriptFunction wrapper = JSContext.wrap(scope, function);

        if (wrapper != null)
        {
            hero.addKeyBindFunc(name, wrapper);
        }
    }

    public void setKeyBindEnabled(Object function)
    {
        ScriptFunction wrapper = JSContext.wrap(scope, function);

        if (wrapper != null)
        {
            hero.setKeyBindEnabledFunc(wrapper);
        }
    }

    public void setModifierEnabled(Object function)
    {
        ScriptFunction wrapper = JSContext.wrap(scope, function);

        if (wrapper != null)
        {
            hero.setModifierEnabledFunc(wrapper);
        }
    }

    public void setHasPermission(Object function)
    {
        ScriptFunction wrapper = JSContext.wrap(scope, function);

        if (wrapper != null)
        {
            hero.setPermissionFunc(wrapper);
        }
    }

    public void setHasProperty(Object function)
    {
        ScriptFunction wrapper = JSContext.wrap(scope, function);

        if (wrapper != null)
        {
            hero.setPropertyFunc(wrapper);
        }
    }

    public void setDamageProfile(Object function)
    {
        ScriptFunction wrapper = JSContext.wrap(scope, function);

        if (wrapper != null)
        {
            hero.setDamageProfileFunc(wrapper);
        }
    }

    public void addDamageProfile(String name, Object types)
    {
        Map<String, Double> map = new LinkedHashMap<>();

        if (types instanceof org.mozilla.javascript.Scriptable scriptable)
        {
            for (Object id : scriptable.getIds())
            {
                String key = String.valueOf(id);
                Object value = scriptable.get(key, scriptable);
                map.put(key, value instanceof Number ? ((Number) value).doubleValue() : 0.0D);
            }
        }

        hero.addDamageProfile(name, map);
    }

    public void setTickHandler(Object function)
    {
        ScriptFunction wrapper = JSContext.wrap(scope, function);

        if (wrapper != null)
        {
            hero.setTickHandler(wrapper);
        }
    }

    public void supplyFunction(String name, Object function)
    {
        ScriptFunction wrapper = JSContext.wrap(scope, function);

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

        if (overrides instanceof org.mozilla.javascript.Scriptable scriptable)
        {
            for (Object id : scriptable.getIds())
            {
                Object value = scriptable.get(String.valueOf(id), scriptable);

                if (value instanceof String string)
                {
                    map.put(Integer.parseInt(String.valueOf(id)), string);
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
        private final Map<HeroAttribute, Hero.AttributeMod> map;
        private boolean inherits = true;

        JSProfileBuilder(Map<HeroAttribute, Hero.AttributeMod> map)
        {
            this.map = map;
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
