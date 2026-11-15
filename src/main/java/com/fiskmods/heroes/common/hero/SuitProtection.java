package com.fiskmods.heroes.common.hero;

import com.fiskmods.heroes.common.config.SHConfig;
import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.hero.attribute.SHAttributes;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;

/** Original tier protection, applied before vanilla armor and enchantments. */
public final class SuitProtection
{
    private static final net.minecraft.resources.ResourceLocation METAL_SKIN = new net.minecraft.resources.ResourceLocation("fiskheroes", "metal_skin");
    private static final net.minecraft.resources.ResourceLocation FIRE_IMMUNITY = new net.minecraft.resources.ResourceLocation("fiskheroes", "fire_immunity");

    private SuitProtection() {}

    public static double tierReduction(int tier)
    {
        if (tier < 1 || tier > 10) return 0.0D;
        return tierReduction(tier, SHConfig.DAMAGE_REDUCTION_BASELINE.get(), SHConfig.DAMAGE_REDUCTION_FACTOR.get());
    }

    /** Kept independent of config so the original formula and rounding can be tested. */
    public static double tierReduction(int tier, double baseline, double factor)
    {
        if (tier < 1 || tier > 10) return 0.0D;
        return Math.max(0.0D, Math.min(1.0D,
                Math.round((100.0D - (100.0D - baseline) * factor / Math.pow(factor, tier)) * 10.0D) / 1000.0D));
    }

    public static float damageMultiplier(LivingEntity entity, DamageSource source)
    {
        var data = SHDataCapabilities.getPlayer(entity);
        if (data == null || data.getHeroType() == null) return 1.0F;

        // MCP 1.7.10 mappings identify the original exceptions as onFire, lava
        // and magic: each receives half protection even if armor-bypassing.
        // Indirect magic receives full protection; other bypassing sources receive none.
        float sourceScale = 1.0F;
        if (source.is(DamageTypes.ON_FIRE) || source.is(DamageTypes.LAVA) || source.is(DamageTypes.MAGIC))
            sourceScale = 0.5F;
        else if (!source.is(DamageTypes.INDIRECT_MAGIC) && source.is(DamageTypeTags.BYPASSES_ARMOR))
            return 1.0F;

        double reduction = Math.max(0.0D, Math.min(1.0D, SHAttributes.protectionValue(entity, data.getHeroType(), data)));
        reduction *= sourceScale;
        if (isFireDamage(source))
        {
            for (var entry : data.getHeroType().getPowerContainer().getEntries())
            {
                if (entry.getModifier() == com.fiskmods.heroes.common.hero.modifier.Modifiers.FIRE_WEAKNESS
                        && entry.isEnabled() && entry.isModifierEnabled(entity, data))
                {
                    reduction *= 0.5D;
                }
            }
        }
        return (float) (1.0D - reduction);
    }

    /**
     * The legacy player's ISpecialArmor pass grants each worn hero armor item 1% absorption,
     * independently of whether a hero is active and even for armor-bypassing sources. All
     * pieces have the same priority, so their ratios add rather than multiply. In this port it
     * runs before modern vanilla armor; mixed armor therefore uses 1.20's armor/toughness rules.
     */
    public static float passiveArmorMultiplier(LivingEntity entity)
    {
        if (!(entity instanceof net.minecraft.world.entity.player.Player)) return 1.0F;
        int pieces = 0;
        for (var stack : entity.getArmorSlots())
            if (!stack.isEmpty() && stack.getItem() instanceof ItemHeroArmor) pieces++;
        return 1.0F - pieces * 0.01F;
    }

    /**
     * Number of filled HUD pixels using the legacy metal-skin heat texture. Fire immunity's
     * cooldown suppresses the overlay, as does disabling either modifier in the hero pack.
     * A client passes the current frame fraction so the transition stays smooth between ticks.
     */
    public static int metalHeatOverlayWidth(LivingEntity entity, com.fiskmods.heroes.common.data.SHPlayerData data,
            int filled, float partialTick)
    {
        filled = Math.max(0, Math.min(79, filled));
        if (filled == 0 || data == null || data.getHeroType() == null
                || data.getData().get(com.fiskmods.heroes.common.data.var.Vars.METAL_HEAT_COOLDOWN) > 0)
            return 0;
        var powers = data.getHeroType().getPowerContainer();
        var metal = powers.getEntry(METAL_SKIN);
        var fire = powers.getEntry(FIRE_IMMUNITY);
        if (metal == null || fire == null || !metal.isEnabled() || !fire.isEnabled()
                || !metal.isModifierEnabled(entity, data) || !fire.isModifierEnabled(entity, data)) return 0;
        float heat = data.getData().getInterpolated(com.fiskmods.heroes.common.data.var.Vars.METAL_HEAT,
                net.minecraft.util.Mth.clamp(partialTick, 0.0F, 1.0F));
        if (!Float.isFinite(heat)) heat = 0.0F;
        heat = net.minecraft.util.Mth.clamp(heat, 0.0F, 1.0F);
        return filled - Math.round(heat * filled);
    }

    /** Legacy extended damage reports fire at a FIRE fraction of at least one half. */
    private static boolean isFireDamage(DamageSource source)
    {
        var profile = com.fiskmods.heroes.common.hero.modifier.DamageGroups.currentDamageProfile();
        if (profile == null && source.getEntity() instanceof net.minecraft.world.entity.player.Player attacker
                && source.getDirectEntity() == attacker
                && (source.is(DamageTypes.PLAYER_ATTACK) || source.is(DamageTypes.MOB_ATTACK)))
        {
            var attackerData = SHDataCapabilities.getPlayer(attacker);
            if (attackerData != null && attackerData.getHeroType() != null)
                profile = attackerData.getHeroType().getMeleeDamageProfile(attacker);
        }
        return profile != null ? profile.getOrDefault("FIRE", 0.0D) >= 0.5D : source.is(DamageTypeTags.IS_FIRE);
    }
}
