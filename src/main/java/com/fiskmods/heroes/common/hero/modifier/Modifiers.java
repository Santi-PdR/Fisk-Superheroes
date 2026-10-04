package com.fiskmods.heroes.common.hero.modifier;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.data.var.Vars;
import com.fiskmods.heroes.common.hero.power.Modifier;
import com.fiskmods.heroes.common.hero.power.ModifierRegistry;
import com.fiskmods.heroes.common.hero.power.PowerProperty;
import com.fiskmods.heroes.common.spell.ModifierSpellcasting;

import net.minecraft.resources.ResourceLocation;

/**
 * Every modifier implemented by the mod. The ids match the ones used by the original mod, so the
 * unmodified hero packs load and behave the same.
 */
public final class Modifiers
{
    private Modifiers()
    {
    }

    private static Modifier create(String id)
    {
        return new Modifier(new ResourceLocation(FiskHeroes.MODID, id));
    }

    /* ---------------------------------------------------------------- */
    /* Registry                                                          */
    /* ---------------------------------------------------------------- */

    public static Modifier FLIGHT;
    public static Modifier CONTROLLED_FLIGHT;
    public static Modifier PROPELLED_FLIGHT;
    public static Modifier SUPER_SPEED;
    public static Modifier SLOW_MOTION;
    public static Modifier WATER_BREATHING;
    public static Modifier LEAPING;
    public static Modifier REGENERATION;
    public static Modifier HEALING_FACTOR;
    public static Modifier FIRE_IMMUNITY;
    public static Modifier FIRE_RESISTANCE;
    public static Modifier FIRE_WEAKNESS;
    public static Modifier COLD_RESISTANCE;
    public static Modifier COLD_WEAKNESS;
    public static Modifier PROJECTILE_IMMUNITY;
    public static Modifier BULLET_IMMUNITY;
    public static Modifier BULLET_RESISTANCE;
    public static Modifier ARROW_CATCHING;
    public static Modifier DAMAGE_IMMUNITY;
    public static Modifier DAMAGE_RESISTANCE;
    public static Modifier DAMAGE_WEAKNESS;
    public static Modifier DAMAGE_BONUS;
    public static Modifier POTION_IMMUNITY;
    public static Modifier POTION_RETENTION;
    public static Modifier METAL_SKIN;
    public static Modifier INVISIBILITY;
    public static Modifier INTANGIBILITY;
    public static Modifier TRANSFORMATION;
    public static Modifier COOLDOWN;
    public static Modifier SHIELD;
    public static Modifier BLADE;
    public static Modifier ENERGY_PROJECTION;
    public static Modifier CHARGED_BEAM;
    public static Modifier ENERGY_BLAST;
    public static Modifier ENERGY_BOLT;
    public static Modifier ENERGY_MANIPULATION;
    public static Modifier HEAT_VISION;
    public static Modifier REPULSOR_BLAST;
    public static Modifier SIZE_MANIPULATION;
    public static Modifier GLIDING;
    public static Modifier TELEPORTATION;
    public static Modifier WEB_SWINGING;
    public static Modifier WEB_ZIP;
    public static Modifier HOVER;
    public static Modifier SENTRY_MODE;
    public static Modifier SHAPESHIFTING;
    public static Modifier SHADOWFORM;
    public static Modifier TELEKINESIS;
    public static Modifier GRAVITY_MANIPULATION;
    public static Modifier FROST_WALKING;
    public static Modifier FLAME_BLAST;
    public static Modifier FIREBALL;
    public static Modifier CRYOKINESIS;
    public static Modifier CRYO_CHARGE;
    public static Modifier CRYOBALL;
    public static Modifier ICICLES;
    public static Modifier LIGHTNING_CAST;
    public static Modifier ARCHERY;
    public static Modifier EQUIPMENT;
    public static Modifier THORNS;
    public static Modifier SPIKES;
    public static Modifier SONIC_WAVES;
    public static Modifier CANARY_CRY;
    public static Modifier CHARGED_PUNCH;
    public static Modifier EARTHQUAKE;
    public static Modifier GROUND_SMASH;
    public static Modifier SHIELD_THROWING;
    public static Modifier SPEED_DISINTEGRATION;
    public static Modifier WALL_CRAWLING;
    public static Modifier SPIDER_SENSE;
    public static Modifier TENTACLES;
    public static Modifier ETERNIUM_WEAKNESS;
    public static Modifier SPELLCASTING;
    public static Modifier SHADOWDOME;
    public static Modifier PHASING;
    public static Modifier INTANGIBLE;

    public static void registerAll()
    {
        ModifierRegistry registry = ModifierRegistry.INSTANCE;

        FLIGHT = registry.register(new ModifierFlight(new ResourceLocation(FiskHeroes.MODID, "flight"))
                .addProperty(PowerProperty.SPEED, 0.1F));
        CONTROLLED_FLIGHT = registry.register(new ModifierControlledFlight(new ResourceLocation(FiskHeroes.MODID, "controlled_flight"))
                .addProperty(PowerProperty.SPEED, 0.1F)
                .addProperty(PowerProperty.BOOST_SPEED, 0.2F)
                .addProperty(PowerProperty.CAN_BOOST, true)
                .addProperty(PowerProperty.CAN_ROLL, false)
                .addProperty(PowerProperty.DIVE_SPEED_RETENTION, 0.0F)
                .addProperty(PowerProperty.KNOCKBACK, 0.4F)).setSoundState(Vars.FLYING);
        PROPELLED_FLIGHT = registry.register(new ModifierControlledFlight(new ResourceLocation(FiskHeroes.MODID, "propelled_flight"))
                .addProperty(PowerProperty.SPEED, 0.1F)
                .addProperty(PowerProperty.CAN_BOOST, true)).setSoundState(Vars.JETPACKING);
        SUPER_SPEED = registry.register(new ModifierSuperSpeed(new ResourceLocation(FiskHeroes.MODID, "super_speed"))
                .addProperty(PowerProperty.SPEED, 1.0F)
                .addProperty(PowerProperty.CAN_USE_TREADMILL, true)).setSoundState(Vars.SPEEDING);
        SLOW_MOTION = registry.register(new ModifierSlowMotion(new ResourceLocation(FiskHeroes.MODID, "slow_motion"))
                .addProperty(PowerProperty.SPEED, 1.0F)).setSoundState(Vars.SLOW_MOTION);
        WATER_BREATHING = registry.register(new ModifierWaterBreathing(new ResourceLocation(FiskHeroes.MODID, "water_breathing")));
        LEAPING = registry.register(new ModifierLeaping(new ResourceLocation(FiskHeroes.MODID, "leaping"))
                .addProperty(PowerProperty.CAN_JUMP_ACTIVATE, true));
        REGENERATION = registry.register(new ModifierRegeneration(new ResourceLocation(FiskHeroes.MODID, "regeneration"))
                .addProperty(PowerProperty.FACTOR, 1.0F));
        HEALING_FACTOR = registry.register(new ModifierRegeneration(new ResourceLocation(FiskHeroes.MODID, "healing_factor"))
                .addProperty(PowerProperty.FACTOR, 1.0F));
        FIRE_IMMUNITY = registry.register(new ModifierImmunity(new ResourceLocation(FiskHeroes.MODID, "fire_immunity"), DamageGroup.FIRE));
        FIRE_RESISTANCE = registry.register(new ModifierResistance(new ResourceLocation(FiskHeroes.MODID, "fire_resistance"), DamageGroup.FIRE)
                .addProperty(PowerProperty.FACTOR, 0.5F));
        FIRE_WEAKNESS = registry.register(new ModifierWeakness(new ResourceLocation(FiskHeroes.MODID, "fire_weakness"), DamageGroup.FIRE)
                .addProperty(PowerProperty.FACTOR, 2.0F));
        COLD_RESISTANCE = registry.register(new ModifierResistance(new ResourceLocation(FiskHeroes.MODID, "cold_resistance"), DamageGroup.COLD)
                .addProperty(PowerProperty.FACTOR, 0.5F));
        COLD_WEAKNESS = registry.register(new ModifierWeakness(new ResourceLocation(FiskHeroes.MODID, "cold_weakness"), DamageGroup.COLD)
                .addProperty(PowerProperty.FACTOR, 2.0F));
        PROJECTILE_IMMUNITY = registry.register(new ModifierProjectileImmunity(new ResourceLocation(FiskHeroes.MODID, "projectile_immunity"))
                .addProperty(PowerProperty.IS_ABSOLUTE, false));
        BULLET_IMMUNITY = registry.register(new ModifierProjectileImmunity(new ResourceLocation(FiskHeroes.MODID, "bullet_immunity"))
                .addProperty(PowerProperty.IS_ABSOLUTE, true));
        BULLET_RESISTANCE = registry.register(new ModifierResistance(new ResourceLocation(FiskHeroes.MODID, "bullet_resistance"), DamageGroup.PROJECTILE)
                .addProperty(PowerProperty.FACTOR, 0.5F));
        ARROW_CATCHING = registry.register(new ModifierArrowCatching(new ResourceLocation(FiskHeroes.MODID, "arrow_catching"))
                .addProperty(PowerProperty.IS_ABSOLUTE, false));
        DAMAGE_IMMUNITY = registry.register(new ModifierDamageImmunity(new ResourceLocation(FiskHeroes.MODID, "damage_immunity")));
        DAMAGE_RESISTANCE = registry.register(new ModifierDamageResistance(new ResourceLocation(FiskHeroes.MODID, "damage_resistance"))
                .addProperty(PowerProperty.FACTOR, 0.5F));
        DAMAGE_WEAKNESS = registry.register(new ModifierDamageResistance(new ResourceLocation(FiskHeroes.MODID, "damage_weakness"))
                .addProperty(PowerProperty.FACTOR, 2.0F));
        DAMAGE_BONUS = registry.register(new ModifierDamageBonus(new ResourceLocation(FiskHeroes.MODID, "damage_bonus"))
                .addProperty(PowerProperty.AMOUNT, 0.0F)
                .addProperty(PowerProperty.DAMAGE_BONUS));
        POTION_IMMUNITY = registry.register(new ModifierPotionImmunity(new ResourceLocation(FiskHeroes.MODID, "potion_immunity")));
        POTION_RETENTION = registry.register(new ModifierPotionRetention(new ResourceLocation(FiskHeroes.MODID, "potion_retention"))
                .addProperty(PowerProperty.POTION_EFFECTS)
                .hideFromHud());
        METAL_SKIN = registry.register(new ModifierMetalSkin(new ResourceLocation(FiskHeroes.MODID, "metal_skin"))
                .addProperty(PowerProperty.FACTOR, 0.5F));
        INVISIBILITY = registry.register(new ModifierInvisibility(new ResourceLocation(FiskHeroes.MODID, "invisibility"))
                .addProperty(PowerProperty.IS_TOGGLE, true)).setSoundState(Vars.INVISIBLE);
        INTANGIBILITY = registry.register(new ModifierIntangibility(new ResourceLocation(FiskHeroes.MODID, "intangibility"))
                .addProperty(PowerProperty.IS_TOGGLE, true)
                .addProperty(PowerProperty.IS_ABSOLUTE, false)).setSoundState(Vars.INTANGIBLE);
        INTANGIBLE = INTANGIBILITY;
        TRANSFORMATION = registry.register(new ModifierTransformation(new ResourceLocation(FiskHeroes.MODID, "transformation"))
                .addProperty(PowerProperty.KEY, "")
                .addProperty(PowerProperty.TRANSFORMATION)
                .addProperty(PowerProperty.IS_TOGGLE, true));
        COOLDOWN = registry.register(new ModifierCooldown(new ResourceLocation(FiskHeroes.MODID, "cooldown")));
        SHIELD = registry.register(new ModifierShield(new ResourceLocation(FiskHeroes.MODID, "shield"))
                .addProperty(PowerProperty.SHIELD)
                .addProperty(PowerProperty.COVERAGE)
                .addProperty(PowerProperty.KNOCKBACK, 0.2F)
                .addProperty(PowerProperty.IS_TOGGLE, true)).setSoundState(Vars.SHIELD);
        BLADE = registry.register(new ModifierBlade(new ResourceLocation(FiskHeroes.MODID, "blade"))
                .addProperty(PowerProperty.IS_TOGGLE, true)).setSoundState(Vars.BLADE);
        ENERGY_PROJECTION = registry.register(new ModifierEnergyProjection(new ResourceLocation(FiskHeroes.MODID, "energy_projection"))
                .addProperty(PowerProperty.RANGE, 10.0F)).setSoundState(Vars.ENERGY_PROJECTION);
        CHARGED_BEAM = registry.register(new ModifierChargedBeam(new ResourceLocation(FiskHeroes.MODID, "charged_beam"))
                .addProperty(PowerProperty.CHARGE_TIME, 20)
                .addProperty(PowerProperty.DURATION, 40)
                .addProperty(PowerProperty.COOLDOWN_TIME, 40)
                .addProperty(PowerProperty.RANGE, 32.0F)
                .addProperty(PowerProperty.RADIUS, 0.1F)
                .addProperty(PowerProperty.IS_TOGGLE, false)).setSoundState(Vars.BEAM_CHARGING);
        ENERGY_BLAST = registry.register(new ModifierEnergyProjection(new ResourceLocation(FiskHeroes.MODID, "energy_blast"))
                .addProperty(PowerProperty.RANGE, 32.0F)
                .addProperty(PowerProperty.COOLDOWN_TIME, 30)
                .addProperty(PowerProperty.IS_EXPLOSIVE, true));
        ENERGY_BOLT = registry.register(new ModifierEnergyProjection(new ResourceLocation(FiskHeroes.MODID, "energy_bolt"))
                .addProperty(PowerProperty.RANGE, 32.0F)
                .addProperty(PowerProperty.COOLDOWN_TIME, 15)
                .addProperty(PowerProperty.IS_EXPLOSIVE, true));
        ENERGY_MANIPULATION = registry.register(new ModifierEnergyManipulation(new ResourceLocation(FiskHeroes.MODID, "energy_manipulation"))
                .addProperty(PowerProperty.CHARGE_TIME, 15)
                .addProperty(PowerProperty.IS_TOGGLE, false)
                .addProperty(PowerProperty.KEY, ModifierEnergyManipulation.KEY)).setSoundState(Vars.ENERGY_CHARGING);
        HEAT_VISION = registry.register(new ModifierHeatVision(new ResourceLocation(FiskHeroes.MODID, "heat_vision"))
                .addProperty(PowerProperty.RANGE, 32.0F)
                .addProperty(PowerProperty.COOLDOWN_TIME, 0)).setSoundState(Vars.HEAT_VISION);
        REPULSOR_BLAST = registry.register(new ModifierEnergyProjection(new ResourceLocation(FiskHeroes.MODID, "repulsor_blast"))
                .addProperty(PowerProperty.RANGE, 24.0F)
                .addProperty(PowerProperty.COOLDOWN_TIME, 10));
        SIZE_MANIPULATION = registry.register(new ModifierSizeManipulation(new ResourceLocation(FiskHeroes.MODID, "size_manipulation"))
                .addProperty(PowerProperty.MIN_SIZE, 0.0F)
                .addProperty(PowerProperty.MAX_SIZE, 1.0F)
                .addProperty(PowerProperty.IS_TOGGLE, true));
        GLIDING = registry.register(new ModifierGliding(new ResourceLocation(FiskHeroes.MODID, "gliding"))
                .addProperty(PowerProperty.IS_TOGGLE, true)).setSoundState(Vars.GLIDING);
        TELEPORTATION = registry.register(new ModifierTeleportation(new ResourceLocation(FiskHeroes.MODID, "teleportation"))
                .addProperty(PowerProperty.RANGE, 24.0F)
                .addProperty(PowerProperty.COOLDOWN_TIME, 20));
        WEB_SWINGING = registry.register(new ModifierGrapple(new ResourceLocation(FiskHeroes.MODID, "web_swinging"))
                .addProperty(PowerProperty.RANGE, 32.0F)
                .addProperty(PowerProperty.SPEED, 1.0F));
        WEB_ZIP = registry.register(new ModifierGrapple(new ResourceLocation(FiskHeroes.MODID, "web_zip"))
                .addProperty(PowerProperty.RANGE, 32.0F)
                .addProperty(PowerProperty.SPEED, 1.0F));
        HOVER = registry.register(new ModifierHover(new ResourceLocation(FiskHeroes.MODID, "hover"))).setSoundState(Vars.HOVERING);
        SENTRY_MODE = registry.register(new ModifierTransformation(new ResourceLocation(FiskHeroes.MODID, "sentry_mode"))
                .addProperty(PowerProperty.IS_TOGGLE, true)).setSoundState(Vars.SUIT_OPEN);
        SHAPESHIFTING = registry.register(new ModifierTransformation(new ResourceLocation(FiskHeroes.MODID, "shapeshifting"))
                .addProperty(PowerProperty.IS_TOGGLE, true));
        SHADOWFORM = registry.register(new ModifierShadowform(new ResourceLocation(FiskHeroes.MODID, "shadowform"))
                .addProperty(PowerProperty.IS_TOGGLE, true)).setSoundState(Vars.SHADOWFORM);
        TELEKINESIS = registry.register(new ModifierTelekinesis(new ResourceLocation(FiskHeroes.MODID, "telekinesis"))
                .addProperty(PowerProperty.RANGE, 16.0F)
                .addProperty(PowerProperty.IS_TOGGLE, false)).setSoundState(Vars.TELEKINESIS);
        GRAVITY_MANIPULATION = registry.register(new ModifierGravityManipulation(new ResourceLocation(FiskHeroes.MODID, "gravity_manipulation"))).setSoundState(Vars.GRAVITY_MANIP);
        FROST_WALKING = registry.register(new ModifierFrostWalking(new ResourceLocation(FiskHeroes.MODID, "frost_walking")));
        FLAME_BLAST = registry.register(new ModifierEnergyProjection(new ResourceLocation(FiskHeroes.MODID, "flame_blast"))
                .addProperty(PowerProperty.RANGE, 24.0F)
                .addProperty(PowerProperty.COOLDOWN_TIME, 20));
        FIREBALL = registry.register(new ModifierEnergyProjection(new ResourceLocation(FiskHeroes.MODID, "fireball"))
                .addProperty(PowerProperty.RANGE, 32.0F)
                .addProperty(PowerProperty.COOLDOWN_TIME, 30));
        CRYOKINESIS = registry.register(new ModifierEnergyProjection(new ResourceLocation(FiskHeroes.MODID, "cryokinesis"))
                .addProperty(PowerProperty.RANGE, 24.0F)
                .addProperty(PowerProperty.COOLDOWN_TIME, 20));
        CRYO_CHARGE = registry.register(new ModifierCryoCharge(new ResourceLocation(FiskHeroes.MODID, "cryo_charge"))
                .addProperty(PowerProperty.IS_TOGGLE, false)
                .addProperty(PowerProperty.KEY, ModifierCryoCharge.KEY)).setSoundState(Vars.CRYO_CHARGING);
        CRYOBALL = registry.register(new ModifierEnergyProjection(new ResourceLocation(FiskHeroes.MODID, "cryoball"))
                .addProperty(PowerProperty.RADIUS, 1.0F)
                .addProperty(PowerProperty.COOLDOWN_TIME, 20));
        ICICLES = registry.register(new ModifierEnergyProjection(new ResourceLocation(FiskHeroes.MODID, "icicles"))
                .addProperty(PowerProperty.COOLDOWN_TIME, 20));
        LIGHTNING_CAST = registry.register(new ModifierEnergyProjection(new ResourceLocation(FiskHeroes.MODID, "lightning_cast"))
                .addProperty(PowerProperty.RANGE, 32.0F)
                .addProperty(PowerProperty.COOLDOWN_TIME, 60));
        ARCHERY = registry.register(new ModifierArchery(new ResourceLocation(FiskHeroes.MODID, "archery"))
                .addProperty(PowerProperty.KEY, ModifierArchery.KEY_HORIZONTAL)
                .addProperty(PowerProperty.RADIUS, 2.0F)
                .addProperty(PowerProperty.IS_TOGGLE, false));
        EQUIPMENT = registry.register(new ModifierEquipment(new ResourceLocation(FiskHeroes.MODID, "equipment"))
                .addProperty(PowerProperty.KEY, "UTILITY_BELT"));
        THORNS = registry.register(new ModifierThorns(new ResourceLocation(FiskHeroes.MODID, "thorns"))
                .addProperty(PowerProperty.AMOUNT, 2.0F));
        SPIKES = registry.register(new ModifierSpikeBurst(new ResourceLocation(FiskHeroes.MODID, "spike_burst"))
                .addProperty(PowerProperty.AMOUNT, 4.0F)
                .addProperty(PowerProperty.RANGE, 24.0F)
                .addProperty(PowerProperty.COOLDOWN_TIME, 5));
        SONIC_WAVES = registry.register(new ModifierSonicWaves(new ResourceLocation(FiskHeroes.MODID, "sonic_waves"))
                .addProperty(PowerProperty.RANGE, 16.0F)
                .addProperty(PowerProperty.COOLDOWN_TIME, 0)).setSoundState(Vars.SONIC_WAVES);
        CANARY_CRY = registry.register(new ModifierEnergyProjection(new ResourceLocation(FiskHeroes.MODID, "canary_cry"))
                .addProperty(PowerProperty.RANGE, 16.0F)
                .addProperty(PowerProperty.COOLDOWN_TIME, 60));
        CHARGED_PUNCH = registry.register(new ModifierChargedPunch(new ResourceLocation(FiskHeroes.MODID, "charged_punch"))
                .addProperty(PowerProperty.CHARGE_TIME, 20)
                .addProperty(PowerProperty.IS_TOGGLE, true)).setSoundState(Vars.PUNCHMODE);
        EARTHQUAKE = registry.register(new ModifierGriefing(new ResourceLocation(FiskHeroes.MODID, "earthquake"))
                .addProperty(PowerProperty.RADIUS, 20.0F)
                .addProperty(PowerProperty.KNOCKBACK, 1.0F)
                .addProperty(PowerProperty.DURATION, 100)
                .addProperty(PowerProperty.COOLDOWN_TIME, 160));
        GROUND_SMASH = registry.register(new ModifierGriefing(new ResourceLocation(FiskHeroes.MODID, "ground_smash"))
                .addProperty(PowerProperty.RADIUS, 8.0F)
                .addProperty(PowerProperty.KNOCKBACK, 0.8F)
                .addProperty(PowerProperty.COOLDOWN_TIME, 100));
        // Shield throwing is triggered by the shield item while this key is held; treating it as
        // an energy projection fires an unrelated hitscan beam whenever the key is pressed.
        SHIELD_THROWING = registry.register(new Modifier(new ResourceLocation(FiskHeroes.MODID, "shield_throwing"))
                .addProperty(PowerProperty.KEY, "SHIELD_THROW")
                .addProperty(PowerProperty.RANGE, 24.0F)
                .addProperty(PowerProperty.COOLDOWN_TIME, 10));
        SPEED_DISINTEGRATION = registry.register(new ModifierSpeedDisintegration(new ResourceLocation(FiskHeroes.MODID, "speed_disintegration")));
        WALL_CRAWLING = registry.register(new ModifierWallCrawling(new ResourceLocation(FiskHeroes.MODID, "wall_crawling")));
        SPIDER_SENSE = registry.register(new ModifierSpiderSense(new ResourceLocation(FiskHeroes.MODID, "spider_sense")));
        TENTACLES = registry.register(new ModifierTentacles(new ResourceLocation(FiskHeroes.MODID, "tentacles"))
                .addProperty(PowerProperty.RANGE, 8.0F));
        ETERNIUM_WEAKNESS = registry.register(new ModifierEterniumWeakness(new ResourceLocation(FiskHeroes.MODID, "eternium_weakness"))
                .addProperty(PowerProperty.RADIUS, 3.0F)
                .addProperty(PowerProperty.DURATION, 120));
        SPELLCASTING = registry.register(new ModifierSpellcasting(new ResourceLocation(FiskHeroes.MODID, "spellcasting")));
        SHADOWDOME = registry.register(new ModifierShadowDome(new ResourceLocation(FiskHeroes.MODID, "shadowdome"))
                .addProperty(PowerProperty.CHARGE_TIME, 40)
                .addProperty(PowerProperty.DURATION, 1200)
                .addProperty(PowerProperty.RADIUS, 24.0F));
        PHASING = INTANGIBILITY;
        registry.register(new ModifierShapeShifting(new ResourceLocation(FiskHeroes.MODID, "shape_shifting")));
        UNREGISTERED_PLACEHOLDERS();
    }

    /**
     * Modifiers which exist in the packs but only alter other systems (visuals or unused
     * properties). They are registered so packs load and abilities keep their ids.
     */
    private static void UNREGISTERED_PLACEHOLDERS()
    {
        for (String id : new String[] {
                "cactus_physiology", "cactus_recruitment", "energy_manipulation",
                "heat_vision", "hover", "lightning_cast", "regeneration_dry", "regeneration_wet",
                "sentry_mode", "transformation", "web_zip", "cryokinesis", "flame_blast",
                "fireball", "icicles", "frost_walking", "gravity_manipulation", "telekinesis", "thorns",
                "sonic_waves", "charged_punch", "earthquake", "ground_smash", "shield_throwing",
                "speed_disintegration", "wall_crawling", "absolute_intangibility", "cosmic_physiology",
                "cold_weakness", "cosmic_empowerment", "archangel_physiology",
                "energy_blast", "energy_bolt", "repulsor_blast", "charged_beam", "spike_burst",
                "tentacle_strike", "archery", "canary_cry", "phasing", "super_boost"
        })
        {
            if (ModifierRegistry.INSTANCE.get(new ResourceLocation(FiskHeroes.MODID, id)) == null)
            {
                ModifierRegistry.INSTANCE.register(create(id).hideFromHud());
            }
        }
    }

    /** Damage categories used by the resistance/immunity modifiers. */
    public enum DamageGroup
    {
        FIRE,
        COLD,
        PROJECTILE,
        ELECTRICITY,
        COSMIC,
        ENERGY,
        EXPLOSION,
        FALL,
        MAGIC
    }
}
