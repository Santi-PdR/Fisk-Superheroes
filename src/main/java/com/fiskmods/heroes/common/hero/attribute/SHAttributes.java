package com.fiskmods.heroes.common.hero.attribute;

import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.hero.Hero;
import com.fiskmods.heroes.common.hero.HeroAttribute;
import com.fiskmods.heroes.common.hero.HeroIteration;
import com.fiskmods.heroes.pack.ScriptFunction;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Hero armour attributes.
 * <p>
 * Attributes that line up with a vanilla/Forge attribute are applied as genuine attribute
 * modifiers (so they interoperate with everything else in the game); the remainder are registered
 * as custom attributes so they can still be read through the attribute system.
 * <p>
 * Pack scripts declare them as {@code hero.addAttribute("PUNCH_DAMAGE", 6.5, 0)} where the last
 * argument is the vanilla operation (0 = addition, 1 = multiply base, 2 = multiply total).
 */
public class SHAttributes
{
    public static final DeferredRegister<Attribute> REGISTRY = DeferredRegister.create(ForgeRegistries.ATTRIBUTES, FiskHeroes.MODID);

    public static final RegistryObject<Attribute> PUNCH_DAMAGE = REGISTRY.register("punch_damage", () -> new RangedAttribute("attribute.fiskheroes.punch_damage", 1.0D, 0.0D, 2048.0D).setSyncable(true));
    public static final RegistryObject<Attribute> WEAPON_DAMAGE = REGISTRY.register("weapon_damage", () -> new RangedAttribute("attribute.fiskheroes.weapon_damage", 1.0D, 0.0D, 2048.0D).setSyncable(true));
    public static final RegistryObject<Attribute> IMPACT_DAMAGE = REGISTRY.register("impact_damage", () -> new RangedAttribute("attribute.fiskheroes.impact_damage", 1.0D, 0.0D, 2048.0D).setSyncable(true));
    public static final RegistryObject<Attribute> ARROW_DAMAGE = REGISTRY.register("arrow_damage", () -> new RangedAttribute("attribute.fiskheroes.arrow_damage", 1.0D, 0.0D, 2048.0D).setSyncable(true));
    public static final RegistryObject<Attribute> BOW_DRAWBACK = REGISTRY.register("bow_drawback", () -> new RangedAttribute("attribute.fiskheroes.bow_drawback", 1.0D, 0.0D, 2048.0D).setSyncable(true));
    public static final RegistryObject<Attribute> REACH_DISTANCE = REGISTRY.register("reach_distance", () -> new RangedAttribute("attribute.fiskheroes.reach_distance", 0.0D, -2048.0D, 2048.0D).setSyncable(true));
    public static final RegistryObject<Attribute> JUMP_HEIGHT = REGISTRY.register("jump_height", () -> new RangedAttribute("attribute.fiskheroes.jump_height", 1.0D, 0.0D, 2048.0D).setSyncable(true));
    public static final RegistryObject<Attribute> FALL_RESISTANCE = REGISTRY.register("fall_resistance", () -> new RangedAttribute("attribute.fiskheroes.fall_resistance", 0.0D, 0.0D, 2048.0D).setSyncable(true));
    public static final RegistryObject<Attribute> KNOCKBACK = REGISTRY.register("knockback", () -> new RangedAttribute("attribute.fiskheroes.knockback", 0.0D, 0.0D, 2048.0D).setSyncable(true));
    public static final RegistryObject<Attribute> DAMAGE_REDUCTION = REGISTRY.register("damage_reduction", () -> new RangedAttribute("attribute.fiskheroes.damage_reduction", 0.0D, 0.0D, 1.0D).setSyncable(true));
    public static final RegistryObject<Attribute> BASE_SPEED_LEVELS = REGISTRY.register("base_speed_levels", () -> new RangedAttribute("attribute.fiskheroes.base_speed_levels", 0.0D, 0.0D, 2048.0D).setSyncable(true));

    // Original speedster movement modifier; stable across server ticks and removed on suit change.
    private static final UUID SPEEDSTER_SPEED = UUID.fromString("388b19ef-9685-4881-be63-04bc862dca2d");
    private static final UUID SPEEDSTER_STEP = UUID.fromString("953f86b5-f64b-4665-b3aa-837b2c1082da");
    private static Map<HeroAttribute, Attribute> mapping;

    private static Map<HeroAttribute, Attribute> mapping()
    {
        if (mapping == null)
        {
            mapping = new EnumMap<>(HeroAttribute.class);
            populate(mapping);
        }

        return mapping;
    }

    private static void populate(Map<HeroAttribute, Attribute> MAPPING)
    {
        MAPPING.put(HeroAttribute.PUNCH_DAMAGE, PUNCH_DAMAGE.get());
        MAPPING.put(HeroAttribute.WEAPON_DAMAGE, WEAPON_DAMAGE.get());
        MAPPING.put(HeroAttribute.IMPACT_DAMAGE, IMPACT_DAMAGE.get());
        MAPPING.put(HeroAttribute.ARROW_DAMAGE, ARROW_DAMAGE.get());
        MAPPING.put(HeroAttribute.BOW_DRAWBACK, BOW_DRAWBACK.get());
        MAPPING.put(HeroAttribute.REACH_DISTANCE, REACH_DISTANCE.get());
        MAPPING.put(HeroAttribute.JUMP_HEIGHT, JUMP_HEIGHT.get());
        MAPPING.put(HeroAttribute.FALL_RESISTANCE, FALL_RESISTANCE.get());
        MAPPING.put(HeroAttribute.KNOCKBACK, KNOCKBACK.get());
        MAPPING.put(HeroAttribute.DAMAGE_REDUCTION, DAMAGE_REDUCTION.get());
        MAPPING.put(HeroAttribute.BASE_SPEED_LEVELS, BASE_SPEED_LEVELS.get());
        MAPPING.put(HeroAttribute.BASE_SPEED, Attributes.MOVEMENT_SPEED);
        MAPPING.put(HeroAttribute.SPRINT_SPEED, Attributes.MOVEMENT_SPEED);
        MAPPING.put(HeroAttribute.MAX_HEALTH, Attributes.MAX_HEALTH);
        MAPPING.put(HeroAttribute.STEP_HEIGHT, ForgeMod.STEP_HEIGHT_ADDITION.get());
    }

    public static Attribute get(HeroAttribute attribute)
    {
        return mapping().get(attribute);
    }

    public static void onAttributeModification(EntityAttributeModificationEvent event)
    {
        for (Attribute attribute : mapping().values())
        {
            if (attribute != null && !event.has(EntityType.PLAYER, attribute))
            {
                event.add(EntityType.PLAYER, attribute);
            }
        }
    }

    /** Stable, deterministic modifier id for (entity, attribute, source). */
    private static UUID uuid(LivingEntity entity, HeroAttribute attribute, SHPlayerData data)
    {
        return data != null ? data.heroAttributeId(entity.getUUID(), attribute)
                : UUID.nameUUIDFromBytes(("fiskheroes:" + entity.getUUID() + ":" + attribute.getName() + ":hero").getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Recomputes and applies every hero attribute modifier of the given entity. Only players
     * wearing a suit are affected.
     */
    public static void applyModifiers(LivingEntity entity)
    {
        SHPlayerData data = entity instanceof Player player ? SHDataCapabilities.getPlayer(player) : null;

        if (data == null)
        {
            return;
        }

        HeroIteration iteration = data.getHero();

        if (iteration == null)
        {
            clearModifiers(entity);
            return;
        }

        Map<HeroAttribute, Hero.AttributeMod> values = collect(entity, iteration.getHero());

        for (HeroAttribute attribute : HeroAttribute.values())
        {
            Attribute vanilla = get(attribute);

            if (vanilla == null)
            {
                continue;
            }

            AttributeInstance instance = entity.getAttribute(vanilla);

            if (instance == null)
            {
                continue;
            }

            UUID id = uuid(entity, attribute, data);
            AttributeModifier existing = instance.getModifier(id);
            Hero.AttributeMod mod = attribute == HeroAttribute.SPRINT_SPEED && !entity.isSprinting()
                    ? null : values.get(attribute);
            double amount = mod != null ? mod.amount() : 0.0D;
            AttributeModifier.Operation operation = mod != null ? operation(mod.operation()) : AttributeModifier.Operation.ADDITION;

            if (Math.abs(amount) < 1.0E-5D)
            {
                if (existing != null)
                {
                    instance.removeModifier(existing);
                }
            }
            else if (existing == null || Math.abs(existing.getAmount() - amount) > 1.0E-5D || existing.getOperation() != operation)
            {
                if (existing != null)
                {
                    instance.removeModifier(existing);
                }

                instance.addTransientModifier(new AttributeModifier(id, "fiskheroes:" + attribute.getName(), amount, operation));
            }
        }
        updateSpeedsterModifier(entity, data);
    }

    /** Original speed-level curve, expressed as a vanilla movement-speed attribute bonus. */
    public static double speedsterBonus(int speedLevel)
    {
        if (speedLevel < 1) return 0.0D;
        double topSpeed = Math.max(speedLevel * 50.0D, 20.0D);
        return 0.0049467960817423D * (topSpeed - 20.0D);
    }

    private static void updateSpeedsterModifier(LivingEntity entity, SHPlayerData data)
    {
        var speed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;
        var entry = data.getHeroType() == null ? null : data.getHeroType().getPowerContainer().getEntries().stream()
                .filter(candidate -> candidate.getModifier() instanceof com.fiskmods.heroes.common.hero.modifier.ModifierSuperSpeed
                        && candidate.isEnabled() && candidate.isModifierEnabled(entity, data))
                .findFirst().orElse(null);
        if (entry != null && entity instanceof Player player)
        {
            int maximum = com.fiskmods.heroes.common.hero.modifier.ModifierSuperSpeed.maxSpeed(player, entry, data);
            int selected = data.getData().get(com.fiskmods.heroes.common.data.var.Vars.SPEED);
            int clamped = net.minecraft.util.Mth.clamp(selected, 1, maximum);
            if (selected != clamped) data.getData().set(com.fiskmods.heroes.common.data.var.Vars.SPEED, (byte) clamped);
        }
        boolean enabled = entry != null && data.getData().get(com.fiskmods.heroes.common.data.var.Vars.SPEEDING)
                && com.fiskmods.heroes.common.config.SHConfig.ALLOW_SUPER_SPEED.get();
        double amount = enabled ? speedsterBonus(data.getData().get(com.fiskmods.heroes.common.data.var.Vars.SPEED)) : 0.0D;
        AttributeModifier existing = speed.getModifier(SPEEDSTER_SPEED);
        if (existing != null && (amount <= 0.0D || Math.abs(existing.getAmount() - amount) > 1.0E-5D))
            speed.removeModifier(existing);
        if (amount > 0.0D && speed.getModifier(SPEEDSTER_SPEED) == null)
            speed.addTransientModifier(new AttributeModifier(SPEEDSTER_SPEED, "fiskheroes:speedster", amount, AttributeModifier.Operation.ADDITION));

        updateSpeedsterStep(entity, enabled);
    }

    /** Keeps at least one block of effective Forge step height without overriding other mods. */
    private static void updateSpeedsterStep(LivingEntity entity, boolean enabled)
    {
        AttributeInstance step = entity.getAttribute(ForgeMod.STEP_HEIGHT_ADDITION.get());
        if (step == null) return;

        AttributeModifier current = step.getModifier(SPEEDSTER_STEP);
        // Changes from other mods can alter the effective height while speed is active. Recalculate
        // our contribution only when needed, rather than replacing it and syncing every tick.
        if (current != null && (!enabled || Math.abs(entity.getStepHeight() - 1.0F) > 1.0E-4F))
        {
            step.removeModifier(current);
            current = null;
        }
        if (!enabled || current != null) return;

        // Forge uses maxUpStep + STEP_HEIGHT_ADDITION, clamped at zero. Compute from the
        // unclamped sum so a negative external addition can also be compensated correctly.
        double missing = 1.0D - (entity.maxUpStep() + step.getValue());
        if (missing <= 1.0E-5D) return;

        double multiplier = 1.0D;
        double baseMultiplier = 0.0D;
        for (AttributeModifier modifier : step.getModifiers(AttributeModifier.Operation.MULTIPLY_BASE))
            baseMultiplier += modifier.getAmount();
        multiplier += baseMultiplier;
        for (AttributeModifier modifier : step.getModifiers(AttributeModifier.Operation.MULTIPLY_TOTAL))
            multiplier *= 1.0D + modifier.getAmount();
        if (multiplier > 1.0E-8D && Double.isFinite(multiplier))
            step.addTransientModifier(new AttributeModifier(SPEEDSTER_STEP, "fiskheroes:speedster_step",
                    missing / multiplier, AttributeModifier.Operation.ADDITION));
    }

    public static void clearModifiers(LivingEntity entity)
    {
        com.fiskmods.heroes.common.hero.ability.MeleeCombat.clearAttackBonus(entity);
        AttributeInstance movement = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movement != null) movement.removeModifier(SPEEDSTER_SPEED);
        AttributeInstance step = entity.getAttribute(ForgeMod.STEP_HEIGHT_ADDITION.get());
        if (step != null) step.removeModifier(SPEEDSTER_STEP);
        SHPlayerData data = SHDataCapabilities.getPlayer(entity);
        for (HeroAttribute attribute : HeroAttribute.values())
        {
            Attribute vanilla = get(attribute);

            if (vanilla == null)
            {
                continue;
            }

            AttributeInstance instance = entity.getAttribute(vanilla);

            if (instance != null)
            {
                AttributeModifier modifier = instance.getModifier(uuid(entity, attribute, data));

                if (modifier != null)
                {
                    instance.removeModifier(modifier);
                }
            }
        }
    }

    private static AttributeModifier.Operation operation(int operation)
    {
        return switch (operation)
        {
            case 1 -> AttributeModifier.Operation.MULTIPLY_BASE;
            case 2 -> AttributeModifier.Operation.MULTIPLY_TOTAL;
            default -> AttributeModifier.Operation.ADDITION;
        };
    }

    /** Resolves the final value of every hero attribute for an entity, including active profiles. */
    public static Map<HeroAttribute, Hero.AttributeMod> collect(LivingEntity entity, Hero hero)
    {
        Map<HeroAttribute, Hero.AttributeMod> map = new EnumMap<>(HeroAttribute.class);

        for (Map.Entry<HeroAttribute, List<Hero.AttributeMod>> e : hero.getAttributes().entrySet())
        {
            map.put(e.getKey(), combine(e.getValue()));
        }

        ScriptFunction profileFunc = hero.getAttributeProfileFunc();

        if (profileFunc != null)
        {
            Object result = profileFunc.call(entity);
            String profile = result != null && !"null".equals(String.valueOf(result)) ? String.valueOf(result) : null;

            if (profile != null)
            {
                Map<HeroAttribute, Hero.AttributeMod> profileMods = hero.getAttributeProfiles().get(profile);

                if (profileMods != null)
                {
                    if (!hero.profileInheritsDefaults(profile))
                    {
                        map.clear();
                    }

                    // Original profiles are keyed by attribute: their own entry takes
                    // precedence over an inherited default, rather than adding to it.
                    map.putAll(profileMods);
                }
            }
        }

        return map;
    }

    private static Hero.AttributeMod combine(List<Hero.AttributeMod> list)
    {
        if (list.isEmpty())
        {
            return new Hero.AttributeMod(0, 0);
        }

        double amount = 0;
        int operation = list.get(0).operation();

        for (Hero.AttributeMod mod : list)
        {
            amount += mod.amount();
        }

        return new Hero.AttributeMod(amount, operation);
    }
}
