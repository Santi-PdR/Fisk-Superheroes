package com.fiskmods.heroes.common.spell;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.hero.Hero;
import com.fiskmods.heroes.common.hero.HeroIteration;
import com.fiskmods.heroes.common.hero.modifier.AbilityData;
import com.fiskmods.heroes.common.hero.power.ModifierEntry;
import com.fiskmods.heroes.common.hero.power.PowerProperty;
import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Validates and executes the server-authoritative portion of a selected spell. */
public final class SpellCastHandler
{
    private SpellCastHandler()
    {
    }

    public static boolean cast(ServerPlayer player, int spellIndex)
    {
        SHPlayerData data = SHDataCapabilities.getPlayer(player);
        HeroIteration iteration = data != null ? data.getHero() : null;
        Hero hero = iteration != null ? iteration.getHero() : null;
        if (data == null || hero == null) return false;

        ModifierEntry entry = hero.getPowerContainer().getEntries().stream()
                .filter(candidate -> candidate.getModifier() instanceof ModifierSpellcasting)
                .filter(ModifierEntry::isEnabled)
                .filter(candidate -> candidate.isModifierEnabled(player, data))
                .findFirst().orElse(null);
        if (entry == null) return false;

        SpellDefinition spell = entry.get(PowerProperty.SPELLS).get(spellIndex);
        if (spell == null) return false;

        long now = player.level().getGameTime();
        if (data.getSpellCooldownUntil(spell.id()) > now) return false;

        boolean cast = switch (spell.id().getPath())
        {
            case "blindness" -> castBlindness(player, spell);
            case "atmospheric" -> castAtmospheric(player, spell);
            default ->
            {
                FiskHeroes.LOGGER.warn("Spell {} is registered but has no 1.20.1 cast implementation yet", spell.id());
                yield false;
            }
        };

        if (cast)
        {
            data.setSpellCooldownUntil(spell.id(), now + spell.cooldown());
            AbilityData.playSound(player, entry, "CAST");
        }

        return cast;
    }

    private static boolean castBlindness(ServerPlayer caster, SpellDefinition spell)
    {
        JsonObject properties = spell.properties();
        double range = number(properties, "range", 48.0D);
        int duration = Math.max(1, (int) Math.ceil(number(properties, "duration", 400.0D)));
        HitResult hit = caster.pick(range, 0.0F, false);
        if (!(hit instanceof EntityHitResult entityHit) || !(entityHit.getEntity() instanceof LivingEntity target)
                || target == caster || caster.isAlliedTo(target)) return false;

        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, duration));
        return true;
    }

    /** Port of the original directional atmosphere blast's cone and distance-scaled impulse. */
    private static boolean castAtmospheric(ServerPlayer caster, SpellDefinition spell)
    {
        JsonObject properties = spell.properties();
        double push = number(properties, "pushPower", 0.0D);
        if (push <= 0.0D) return false;

        Vec3 look = caster.getLookAngle().normalize();
        double range = 34.0D;
        AABB bounds = caster.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0D);
        double baseDamage = 0.0D;
        if (properties.has("damageProfile") && properties.get("damageProfile").isJsonObject())
        {
            baseDamage = number(properties.getAsJsonObject("damageProfile"), "damage", 0.0D);
        }

        int affected = 0;
        for (Entity entity : caster.level().getEntities(caster, bounds, candidate -> candidate.isAlive()))
        {
            Vec3 targetEye = entity.getEyePosition();
            Vec3 originEye = caster.getEyePosition();
            Vec3 offset = targetEye.subtract(originEye);
            double distance = offset.length();
            if (distance < 1.0E-4D || !caster.hasLineOfSight(entity)) continue;

            double dot = look.dot(offset.scale(1.0D / distance));
            if (dot <= 1.0D - 0.9D / distance) continue;

            double falloff = Math.pow(entity.distanceTo(caster), 0.25D) / 2.0D;
            if (falloff <= 0.0D || falloff >= 1.2D * push) continue;

            if (entity instanceof LivingEntity living && baseDamage > 0.0D)
            {
                living.hurt(caster.damageSources().magic(), (float) (baseDamage / falloff));
            }

            Vec3 impulse = look.scale(push / falloff);
            entity.setDeltaMovement(entity.getDeltaMovement().add(impulse));
            entity.hasImpulse = true;
            affected++;
        }

        return affected > 0;
    }

    private static double number(JsonObject object, String key, double fallback)
    {
        return object.has(key) && object.get(key).isJsonPrimitive() && object.get(key).getAsJsonPrimitive().isNumber()
                ? object.get(key).getAsDouble() : fallback;
    }
}
