package com.fiskmods.heroes.common.config;

import org.apache.commons.lang3.tuple.Pair;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Server/common configuration. The original mod used a plain properties file; the options are kept
 * the same but exposed through Forge's config system.
 */
public class SHConfig
{
    public static final ForgeConfigSpec SPEC;

    /* Suits */
    public static ForgeConfigSpec.BooleanValue REQUIRE_FULL_SUIT;
    public static ForgeConfigSpec.BooleanValue SUIT_PIECES_TAKE_DAMAGE;
    public static ForgeConfigSpec.BooleanValue FORCE_SLOW_LANDING;
    public static ForgeConfigSpec.BooleanValue ALLOW_VANITY_SUITS;

    /* Powers */
    public static ForgeConfigSpec.BooleanValue ALLOW_FLIGHT;
    public static ForgeConfigSpec.BooleanValue ALLOW_GRIEFING;
    public static ForgeConfigSpec.BooleanValue ALLOW_SUPER_SPEED;
    public static ForgeConfigSpec.DoubleValue ABILITY_COOLDOWN_MULTIPLIER;
    public static ForgeConfigSpec.DoubleValue DAMAGE_MULTIPLIER;

    /* Heroes */
    public static ForgeConfigSpec.BooleanValue LOAD_EXTERNAL_PACKS;
    public static ForgeConfigSpec.BooleanValue LOG_PACK_LOADING;

    static
    {
        Pair<SHConfig, ForgeConfigSpec> pair = new ForgeConfigSpec.Builder().configure(SHConfig::new);
        SPEC = pair.getRight();
    }

    private SHConfig(ForgeConfigSpec.Builder builder)
    {
        builder.comment("Suit options").push("suits");
        REQUIRE_FULL_SUIT = builder.comment("Whether the full suit must be worn for the hero's powers to apply")
                .define("requireFullSuit", false);
        SUIT_PIECES_TAKE_DAMAGE = builder.comment("Whether suit pieces are damaged when the wearer is hit")
                .define("suitPiecesTakeDamage", false);
        FORCE_SLOW_LANDING = builder.comment("Whether superhero landings are forced to be slow")
                .define("forceSlowLanding", false);
        ALLOW_VANITY_SUITS = builder.comment("Whether vanity iterations of suits can be equipped")
                .define("allowVanitySuits", true);
        builder.pop();

        builder.comment("Power options").push("powers");
        ALLOW_FLIGHT = builder.comment("Whether powered flight is enabled")
                .define("allowFlight", true);
        ALLOW_GRIEFING = builder.comment("Whether abilities may break blocks")
                .define("allowGriefing", true);
        ALLOW_SUPER_SPEED = builder.comment("Whether super speed is enabled")
                .define("allowSuperSpeed", true);
        ABILITY_COOLDOWN_MULTIPLIER = builder.comment("Multiplier applied to every ability cooldown")
                .defineInRange("abilityCooldownMultiplier", 1.0D, 0.0D, 100.0D);
        DAMAGE_MULTIPLIER = builder.comment("Multiplier applied to hero melee damage")
                .defineInRange("damageMultiplier", 1.0D, 0.0D, 100.0D);
        builder.pop();

        builder.comment("Hero pack options").push("heroes");
        LOAD_EXTERNAL_PACKS = builder.comment("Whether packs in config/fiskheroes/heropacks are loaded")
                .define("loadExternalPacks", true);
        LOG_PACK_LOADING = builder.comment("Whether every loaded hero is logged")
                .define("logPackLoading", true);
        builder.pop();
    }

    public static double cooldown(double value)
    {
        return value * ABILITY_COOLDOWN_MULTIPLIER.get();
    }
}
