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
import net.minecraft.world.phys.BlockHitResult;
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
            case "earth_swallowing" -> castEarthSwallowing(player, spell);
            case "whip" -> castWhip(player, spell);
            case "duplication" -> castDuplication(player, spell);
            case "drones" -> castDrones(player, spell);
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
                || target == caster) return false;

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
        for (Entity entity : caster.level().getEntities(caster, bounds,
                candidate -> candidate != caster && candidate.isAlive()))
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

    /** Ports the original block-ray target collection and delayed Earth Crack attack. */
    private static boolean castEarthSwallowing(ServerPlayer caster, SpellDefinition spell)
    {
        JsonObject properties = spell.properties();
        double range = number(properties, "range", 48.0D);
        double radius = Math.max(0.0D, number(properties, "radius", 6.0D));
        HitResult hit = caster.pick(range, 1.0F, false);
        if (!(hit instanceof BlockHitResult blockHit)) return false;

        net.minecraft.core.BlockPos impact = blockHit.getBlockPos();
        AABB bounds = new AABB(impact).inflate(radius);
        float damage = 14.0F;
        if (properties.has("damageProfile") && properties.get("damageProfile").isJsonObject())
        {
            damage = (float) number(properties.getAsJsonObject("damageProfile"), "damage", damage);
        }

        int spawned = 0;
        for (LivingEntity target : caster.level().getEntitiesOfClass(LivingEntity.class, bounds,
                entity -> entity != caster && entity.isAlive() && !entity.isAlliedTo(caster)
                        && !entity.isInvulnerable() && !hasEarthCrack(caster, entity)))
        {
            var crack = new com.fiskmods.heroes.common.spell.EarthCrackEntity(
                    com.fiskmods.heroes.common.entity.ModEntities.EARTH_CRACK.get(), caster.level(), caster, target, damage);
            caster.level().addFreshEntity(crack);
            spawned++;
        }
        return spawned > 0;
    }

    private static boolean hasEarthCrack(ServerPlayer caster, LivingEntity target)
    {
        return !caster.level().getEntitiesOfClass(EarthCrackEntity.class, target.getBoundingBox().inflate(2.0D),
                crack -> crack.getTargetId() == target.getId()).isEmpty();
    }

    /** Applies the whip's damage and tether pull when its flight path hits a living target. */
    private static boolean castWhip(ServerPlayer caster, SpellDefinition spell)
    {
        HitResult hit = caster.pick(32.0D, 1.0F, false);
        if (!(hit instanceof EntityHitResult entityHit) || !(entityHit.getEntity() instanceof LivingEntity target)
                || target == caster || !target.isAlive()) return false;

        JsonObject properties = spell.properties();
        JsonObject primaryProfile = properties.has("damageProfile") && properties.get("damageProfile").isJsonObject()
                ? properties.getAsJsonObject("damageProfile") : new JsonObject();
        double damage = number(primaryProfile, "damage", 5.0D);
        JsonObject burnProfile = new JsonObject();
        int frequency = 20;
        if (properties.has("whipBurn") && properties.get("whipBurn").isJsonObject())
        {
            JsonObject burn = properties.getAsJsonObject("whipBurn");
            burnProfile = burn.has("damageProfile") && burn.get("damageProfile").isJsonObject()
                    ? burn.getAsJsonObject("damageProfile") : new JsonObject();
            frequency = Math.max(1, (int) number(burn, "frequency", 20.0D));
        }

        SpellWhipEntity tether = new SpellWhipEntity(
                com.fiskmods.heroes.common.entity.ModEntities.SPELL_WHIP.get(), caster.level(), caster,
                target, burnProfile, frequency);
        if (!caster.level().addFreshEntity(tether)) return false;

        com.fiskmods.heroes.common.hero.modifier.DamageGroups.applyProfileDamage(target, caster,
                caster.damageSources().magic(), (float) Math.max(0.0D, damage), primaryProfile);

        Vec3 pull = caster.position().subtract(target.position()).normalize();
        target.setDeltaMovement(target.getDeltaMovement().add(pull.x * 0.8D, Math.max(0.1D, pull.y * 0.4D), pull.z * 0.8D));
        target.hasImpulse = true;
        return true;
    }

    /** Spawns the original number of owner-tracked decoys around the targeted living entity. */
    private static boolean castDuplication(ServerPlayer caster, SpellDefinition spell)
    {
        HitResult hit = caster.pick(32.0D, 1.0F, false);
        if (!(hit instanceof EntityHitResult entityHit) || !(entityHit.getEntity() instanceof LivingEntity target)
                || target == caster || !target.isAlive()) return false;

        for (SpellDuplicateEntity existing : caster.level().getEntitiesOfClass(SpellDuplicateEntity.class,
                caster.getBoundingBox().inflate(128.0D), clone -> clone.getOwner() == caster))
        {
            existing.discard();
        }

        int configured = Math.max(1, (int) Math.ceil(number(spell.properties(), "quantity", 5.0D)));
        int totalPositions = configured + 1;
        int spawned = 0;
        for (int i = 1; i < totalPositions; ++i)
        {
            SpellDuplicateEntity duplicate = new SpellDuplicateEntity(
                    com.fiskmods.heroes.common.entity.ModEntities.SPELL_DUPLICATE.get(), caster.level(), caster,
                    target, 360.0F / totalPositions * i);
            if (caster.level().addFreshEntity(duplicate)) spawned++;
        }
        return spawned > 0;
    }

    /** Summons the configured number of owner-bound illusion drones around a targeted entity. */
    private static boolean castDrones(ServerPlayer caster, SpellDefinition spell)
    {
        JsonObject properties = spell.properties();
        double range = number(properties, "range", 24.0D);
        HitResult hit = caster.pick(range, 1.0F, false);
        if (!(hit instanceof EntityHitResult entityHit) || !(entityHit.getEntity() instanceof LivingEntity target)
                || target == caster || !target.isAlive()) return false;

        int quantity = Math.max(1, (int) Math.ceil(number(properties, "quantity", 2.0D)));
        double centerDistance = Math.max(0.5D, number(properties, "centerDist", 5.0D));
        JsonObject profile = properties.has("damageProfile") && properties.get("damageProfile").isJsonObject()
                ? properties.getAsJsonObject("damageProfile") : new JsonObject();
        Vec3 direction = caster.position().subtract(target.position()).normalize();
        if (direction.horizontalDistanceSqr() < 1.0E-6D) direction = new Vec3(1.0D, 0.0D, 0.0D);

        int spawned = 0;
        int positions = quantity + 1;
        for (int i = 1; i < positions; i++)
        {
            double angle = Math.PI * 2.0D * i / positions;
            double x = direction.x * Math.cos(angle) - direction.z * Math.sin(angle);
            double z = direction.z * Math.cos(angle) + direction.x * Math.sin(angle);
            double radius = centerDistance;
            IllusionDroneEntity drone = new IllusionDroneEntity(
                    com.fiskmods.heroes.common.entity.ModEntities.ILLUSION_DRONE.get(), caster.level(), caster,
                    target, (float) Math.toDegrees(Math.atan2(z, x)), (float) radius, profile);
            drone.setPos(target.getX() + x * radius, target.getY() + 1.0D, target.getZ() + z * radius);
            while (radius > 0.5D && (!caster.level().isEmptyBlock(drone.blockPosition()) || !drone.hasLineOfSight(target)))
            {
                radius -= 0.5D;
                drone.setPos(target.getX() + x * radius, target.getY() + 1.0D, target.getZ() + z * radius);
            }
            drone.setOrbitRadius((float) radius);
            if (caster.level().addFreshEntity(drone)) spawned++;
        }
        return spawned > 0;
    }

    private static double number(JsonObject object, String key, double fallback)
    {
        return object.has(key) && object.get(key).isJsonPrimitive() && object.get(key).getAsJsonPrimitive().isNumber()
                ? object.get(key).getAsDouble() : fallback;
    }
}
