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
    private static UUID uuid(LivingEntity entity, HeroAttribute attribute, String source)
    {
        return UUID.nameUUIDFromBytes(("fiskheroes:" + entity.getUUID() + ":" + attribute.getName() + ":" + source).getBytes(StandardCharsets.UTF_8));
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

            UUID id = uuid(entity, attribute, "hero");
            AttributeModifier existing = instance.getModifier(id);
            Hero.AttributeMod mod = values.get(attribute);
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
    }

    public static void clearModifiers(LivingEntity entity)
    {
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
                AttributeModifier modifier = instance.getModifier(uuid(entity, attribute, "hero"));

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

                    for (Map.Entry<HeroAttribute, Hero.AttributeMod> e : profileMods.entrySet())
                    {
                        Hero.AttributeMod current = map.get(e.getKey());
                        Hero.AttributeMod add = e.getValue();

                        map.put(e.getKey(), current == null ? add
                                : new Hero.AttributeMod(current.amount() + add.amount(), add.operation()));
                    }
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
