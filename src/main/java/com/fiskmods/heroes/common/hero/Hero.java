package com.fiskmods.heroes.common.hero;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import com.fiskmods.heroes.common.hero.power.PowerContainer;
import com.fiskmods.heroes.pack.ScriptFunction;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/**
 * A superhero defined by a hero pack. Heroes are immutable once loaded: the pack script builds an
 * instance through the scripting facade, which is then registered and only read from.
 */
public class Hero implements Comparable<Hero>
{
    public static final HeroRegistry REGISTRY = new HeroRegistry();

    public static final Comparator<Hero> COMPARATOR = Comparator.comparing(Hero::getLocalizedName)
            .thenComparing(Hero::getName);
    public static final Comparator<Hero> COMPARING_TIER = Comparator.comparing(Hero::getTier).thenComparing(COMPARATOR);

    private final ResourceLocation registryName;

    private String nameKey;
    private String versionKey;
    private String[] aliases = new String[0];
    private int tier;

    private final String[] armor = new String[4];
    private byte armorSignature;
    private int armorCount;

    private double defaultScale = 1.0;
    private int tierOverride = -1;
    private boolean hidden;
    private boolean maskToggle = true;

    private final Map<HeroAttribute, List<AttributeMod>> attributes = new EnumMap<>(HeroAttribute.class);
    private final Map<String, Map<HeroAttribute, AttributeMod>> attributeProfiles = new LinkedHashMap<>();
    private final Map<String, Boolean> profileInheritsDefaults = new HashMap<>();
    private final Map<String, Boolean> profileRevokesAugments = new HashMap<>();
    private ScriptFunction attributeProfileFunc;

    private final List<KeyBind> keyBinds = new ArrayList<>();
    private final Map<String, ScriptFunction> keyBindFuncs = new HashMap<>();
    private ScriptFunction keyBindEnabledFunc;
    private ScriptFunction modifierEnabledFunc;
    private ScriptFunction permissionFunc;
    private ScriptFunction propertyFunc;
    private ScriptFunction damageProfileFunc;
    private ScriptFunction tickHandler;

    private final Map<String, ScriptFunction> functions = new HashMap<>();
    private final Map<String, Map<String, Double>> damageProfiles = new LinkedHashMap<>();
    private final List<EquipmentEntry> equipment = new ArrayList<>();
    private final List<ResourceLocation> powers = new ArrayList<>();
    private final PowerContainer powerContainer = new PowerContainer();

    private final Map<String, ResourceLocation> soundEvents = new LinkedHashMap<>();
    private final Map<String, Map<Integer, String>> soundOverrides = new LinkedHashMap<>();

    private final Map<Integer, HeroIteration> iterations = new LinkedHashMap<>();
    private final Map<String, HeroIteration> iterationKeyMap = new HashMap<>();
    private HeroIteration defaultIteration;

    public Hero(ResourceLocation registryName)
    {
        this.registryName = registryName;
    }

    /* ------------------------------------------------------------------ */
    /* Registration / build                                                */
    /* ------------------------------------------------------------------ */

    void register(Map<String, HeroIteration.Candidate> candidates)
    {
        defaultIteration = new HeroIteration(this, -1, null, candidates != null ? candidates.get("$DEFAULT") : null);
        iterations.put(-1, defaultIteration);

        if (candidates != null)
        {
            for (Map.Entry<String, HeroIteration.Candidate> e : candidates.entrySet())
            {
                if ("$DEFAULT".equals(e.getKey()))
                {
                    continue;
                }

                HeroIteration iteration = new HeroIteration(this, iterations.size() - 1, e.getKey(), e.getValue());
                iterations.put(iteration.getId(), iteration);

                if (iteration.getKey() != null)
                {
                    iterationKeyMap.put(iteration.getKey(), iteration);
                }
            }
        }

        powerContainer.bake();
    }

    /* ------------------------------------------------------------------ */
    /* Pack script API                                                     */
    /* ------------------------------------------------------------------ */

    public void setName(String key)
    {
        nameKey = key;
    }

    public void setVersion(String key)
    {
        versionKey = key;
    }

    public void setAliases(String... values)
    {
        aliases = values;
    }

    public void setTier(int tier)
    {
        this.tier = tier;
    }

    public void setDefaultScale(double scale)
    {
        defaultScale = scale;
    }

    public void hide()
    {
        hidden = true;
    }

    public void setMaskToggle(boolean value)
    {
        maskToggle = value;
    }

    public void setHelmet(String armorType)
    {
        setArmor(0, armorType);
    }

    public void setChestplate(String armorType)
    {
        setArmor(1, armorType);
    }

    public void setLeggings(String armorType)
    {
        setArmor(2, armorType);
    }

    public void setBoots(String armorType)
    {
        setArmor(3, armorType);
    }

    public void setArmor(int slot, String armorType)
    {
        if (armorType != null && !armorType.isEmpty())
        {
            armor[slot] = armorType;
            armorSignature |= 1 << slot;
            armorCount++;
        }
    }

    public void addAttribute(String attribute, double amount, int operation)
    {
        HeroAttribute attr = HeroAttribute.byName(attribute);

        if (attr != null)
        {
            attributes.computeIfAbsent(attr, k -> new ArrayList<>()).add(new AttributeMod(amount, operation));
        }
    }

    public void addAttributeProfile(String name, Map<HeroAttribute, AttributeMod> modifiers, boolean inheritDefaults)
    {
        attributeProfiles.put(name, modifiers);
        profileInheritsDefaults.put(name, inheritDefaults);
    }

    public void setProfileRevokesAugments(String name, boolean revoke)
    {
        profileRevokesAugments.put(name, revoke);
    }

    /** Whether the given profile discards the augments the hero grants while it is active. */
    public boolean profileRevokesAugments(String name)
    {
        return profileRevokesAugments.containsKey(name) && profileRevokesAugments.get(name);
    }

    public boolean profileInheritsDefaults(String name)
    {
        return profileInheritsDefaults.getOrDefault(name, true);
    }

    public void setAttributeProfileFunc(ScriptFunction function)
    {
        attributeProfileFunc = function;
    }

    public void addKeyBind(String name, String keyName, int index)
    {
        keyBinds.add(new KeyBind(name, keyName, index));
        keyBinds.sort(Comparator.comparingInt(k -> k.index));
    }

    public void addKeyBindFunc(String name, ScriptFunction function)
    {
        keyBindFuncs.put(name, function);
    }

    public void setKeyBindEnabledFunc(ScriptFunction function)
    {
        keyBindEnabledFunc = function;
    }

    public void setModifierEnabledFunc(ScriptFunction function)
    {
        modifierEnabledFunc = function;
    }

    public void setPermissionFunc(ScriptFunction function)
    {
        permissionFunc = function;
    }

    public void setPropertyFunc(ScriptFunction function)
    {
        propertyFunc = function;
    }

    public void setDamageProfileFunc(ScriptFunction function)
    {
        damageProfileFunc = function;
    }

    public void setTickHandler(ScriptFunction function)
    {
        tickHandler = function;
    }

    public void addDamageProfile(String name, Map<String, Double> types)
    {
        damageProfiles.put(name, types);
    }

    public void supplyFunction(String name, ScriptFunction function)
    {
        functions.put(name, function);
    }

    public void addEquipment(ItemStack stack, boolean primary)
    {
        equipment.add(new EquipmentEntry(stack, primary));
    }

    public void addPowers(ResourceLocation... ids)
    {
        for (ResourceLocation id : ids)
        {
            if (!powers.contains(id))
            {
                powers.add(id);
            }
        }
    }

    public void addSoundEvent(String name, ResourceLocation sound)
    {
        soundEvents.put(name, sound);
    }

    public void addSoundOverrides(String name, Map<Integer, String> overrides)
    {
        soundOverrides.put(name, overrides);
    }

    /* ------------------------------------------------------------------ */
    /* Accessors                                                           */
    /* ------------------------------------------------------------------ */

    public ResourceLocation getRegistryName()
    {
        return registryName;
    }

    public String getName()
    {
        return registryName.toString();
    }

    public String getDomain()
    {
        return registryName.getNamespace();
    }

    public String getNameKey()
    {
        return nameKey != null ? nameKey : "hero." + registryName.getNamespace() + "." + registryName.getPath() + ".name";
    }

    public void setTierOverride(int tier)
    {
        tierOverride = tier;
    }

    /** Tier used when sorting suit pieces, or {@code -1} when the hero's own tier applies. */
    public int getTierOverride()
    {
        return tierOverride;
    }

    public String getVersionKey()
    {
        return versionKey;
    }

    public String getLocalizedName()
    {
        return Component.translatable(getNameKey()).getString();
    }

    public String[] getAliases()
    {
        return aliases;
    }

    public int getTier()
    {
        return tier;
    }

    public double getDefaultScale()
    {
        return defaultScale;
    }

    public boolean isHidden()
    {
        return hidden;
    }

    public boolean canToggleMask()
    {
        return maskToggle;
    }

    public String getArmorType(int slot)
    {
        return slot >= 0 && slot < armor.length ? armor[slot] : null;
    }

    public boolean hasPieceOfSet(int slot)
    {
        return (armorSignature & 1 << slot) == 1 << slot;
    }

    public int getArmorSignature()
    {
        return armorSignature;
    }

    public int getPiecesToSet()
    {
        return armorCount;
    }

    public int getFirstPieceOfSet()
    {
        for (int i = 0; i < 4; ++i)
        {
            if (hasPieceOfSet(i))
            {
                return i;
            }
        }

        return -1;
    }

    public int getCorePieceOfSet()
    {
        return hasPieceOfSet(1) ? 1 : getFirstPieceOfSet();
    }

    public Map<HeroAttribute, List<AttributeMod>> getAttributes()
    {
        return ImmutableMap.copyOf(attributes);
    }

    public List<AttributeMod> getAttributes(HeroAttribute attribute)
    {
        return attributes.getOrDefault(attribute, ImmutableList.of());
    }

    public Map<String, Map<HeroAttribute, AttributeMod>> getAttributeProfiles()
    {
        return ImmutableMap.copyOf(attributeProfiles);
    }

    public ScriptFunction getAttributeProfileFunc()
    {
        return attributeProfileFunc;
    }

    public List<KeyBind> getKeyBinds()
    {
        return ImmutableList.copyOf(keyBinds);
    }

    public boolean hasKeyBind(String name)
    {
        return keyBinds.stream().anyMatch(k -> k.name.equals(name));
    }

    public KeyBind getKeyBind(String name)
    {
        return keyBinds.stream().filter(k -> k.name.equals(name)).findFirst().orElse(null);
    }

    public int getKeyBinding(String name)
    {
        KeyBind key = getKeyBind(name);
        return key != null ? key.index : -1;
    }

    public boolean hasKeyBinding(int index)
    {
        return keyBinds.stream().anyMatch(k -> k.index == index);
    }

    public Set<String> getKeyBindsMatching(int index)
    {
        Set<String> set = new java.util.LinkedHashSet<>();

        for (KeyBind key : keyBinds)
        {
            if (key.index == index)
            {
                set.add(key.name);
            }
        }

        return set;
    }

    public Map<String, ScriptFunction> getKeyBindFuncs()
    {
        return keyBindFuncs;
    }

    public String getKeyName(String name)
    {
        KeyBind key = getKeyBind(name);
        return key != null ? key.keyName : null;
    }

    public boolean isKeyBindEnabled(Entity entity, String name)
    {
        return keyBindEnabledFunc == null || keyBindEnabledFunc.callBoolean(entity, name);
    }

    public boolean isModifierEnabled(Entity entity, String modifier)
    {
        return modifierEnabledFunc == null || modifierEnabledFunc.callBoolean(entity, modifier);
    }

    public boolean hasPermission(Entity entity, String permission)
    {
        return permissionFunc == null || permissionFunc.callBoolean(entity, permission);
    }

    public boolean hasProperty(Entity entity, String property)
    {
        return propertyFunc == null || propertyFunc.callBoolean(entity, property);
    }

    public String getDamageProfile(Entity entity)
    {
        return damageProfileFunc == null ? null : damageProfileFunc.callString(entity);
    }

    public Map<String, Double> getDamageProfile(String name)
    {
        return damageProfiles.get(name);
    }

    public Map<String, Map<String, Double>> getDamageProfiles()
    {
        return ImmutableMap.copyOf(damageProfiles);
    }

    public ScriptFunction getTickHandler()
    {
        return tickHandler;
    }

    public ScriptFunction getFunction(String name)
    {
        return functions.get(name);
    }

    public List<EquipmentEntry> getEquipment()
    {
        return ImmutableList.copyOf(equipment);
    }

    public List<ResourceLocation> getPowers()
    {
        return ImmutableList.copyOf(powers);
    }

    public PowerContainer getPowerContainer()
    {
        return powerContainer;
    }

    public Map<String, ResourceLocation> getSoundEvents()
    {
        return soundEvents;
    }

    public Map<String, Map<Integer, String>> getSoundOverrides()
    {
        return soundOverrides;
    }

    public Map<Integer, HeroIteration> getIterations()
    {
        return ImmutableMap.copyOf(iterations);
    }

    public HeroIteration getDefaultIteration()
    {
        return defaultIteration;
    }

    public HeroIteration getIteration(String key)
    {
        if (key == null || key.isEmpty())
        {
            return defaultIteration;
        }

        return iterationKeyMap.getOrDefault(key, defaultIteration);
    }

    public HeroIteration getIteration(int id)
    {
        return iterations.getOrDefault(id, defaultIteration);
    }

    public Optional<HeroIteration> findIteration(String key)
    {
        return Optional.ofNullable(iterationKeyMap.get(key));
    }

    public Component getFormattedName()
    {
        return Component.translatable(getNameKey());
    }

    @Override
    public int compareTo(Hero o)
    {
        return COMPARATOR.compare(this, o);
    }

    @Override
    public int hashCode()
    {
        return registryName.hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        return obj instanceof Hero && ((Hero) obj).registryName.equals(registryName);
    }

    @Override
    public String toString()
    {
        return getName();
    }

    /* ------------------------------------------------------------------ */

    /** A single hero attribute modifier, mirroring the (amount, operation) pairs of the pack scripts. */
    public record AttributeMod(double amount, int operation)
    {
    }

    /** An attribute modifier resolved from a profile, which may target any attribute. */
    public record ProfileMod(HeroAttribute attribute, double amount, int operation)
    {
    }

    /** A keybind declared by a hero: an ability name bound to one of the shared ability keys. */
    public record KeyBind(String name, String keyName, int index)
    {
    }

    /** An item offered as part of a suit's equipment. */
    public record EquipmentEntry(ItemStack stack, boolean primary)
    {
        public EquipmentEntry copy()
        {
            return new EquipmentEntry(stack.copy(), primary);
        }
    }

    /** Bridge used by the player data to resolve heroes without a hard static cycle. */
    public static final class HeroRegistryAccess
    {
        private HeroRegistryAccess()
        {
        }

        public static Hero getHero(ResourceLocation id)
        {
            return REGISTRY.getHero(id);
        }
    }
}
