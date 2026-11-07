package com.fiskmods.heroes.common.hero.ability;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.data.var.Vars;
import com.fiskmods.heroes.common.hero.attribute.SHAttributes;
import com.fiskmods.heroes.common.hero.modifier.AbilityData;
import com.fiskmods.heroes.common.hero.power.ModifierEntry;
import com.fiskmods.heroes.common.hero.power.PowerProperty;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Server-side collision effects from the original gliding flight. */
public final class GlidingCollision
{
    private static final ResourceKey<DamageType> ENTITY_IMPACT = ResourceKey.create(Registries.DAMAGE_TYPE, FiskHeroes.id("flight_collision"));
    private GlidingCollision() {}

    public static void apply(Player player, SHPlayerData data, ModifierEntry entry, double previousHorizontalSpeed)
    {
        if (player.level().isClientSide || player.noPhysics) return;
        JsonElement raw = entry.get(player, PowerProperty.COLLISION);
        JsonObject config = raw != null && raw.isJsonObject() ? raw.getAsJsonObject() : null;
        Vec3 motion = player.getDeltaMovement();
        double speed = motion.length();
        float wallDamage = (float) ((previousHorizontalSpeed - speed) * 10.0D - 3.0D);
        boolean stop = false;
        if (player.horizontalCollision && wallDamage > 0.0F)
        {
            if (flag(config, "blocks", "takeDamage")) player.hurt(player.damageSources().flyIntoWall(), wallDamage);
            if (flag(config, "blocks", "stopFlying"))
            {
                stop(player, data, entry);
                stop = true;
            }
            AbilityData.playSound(player, entry, "IMPACT_BLOCK");
        }

        // As in 1.7.10, entity impacts are only considered if entity damage is enabled and
        // no stopping block impact already occurred. Vanilla invulnerability/PvP checks apply.
        float damage = (float) (speed * 5.0D - 2.0D);
        if (stop || !flag(config, "entities", "dealDamage") || damage <= 0.0F) return;
        var attribute = player.getAttribute(SHAttributes.IMPACT_DAMAGE.get());
        if (attribute != null)
        {
            double modified = damage;
            for (var mod : attribute.getModifiers())
                if (mod.getOperation() != AttributeModifier.Operation.ADDITION) modified *= 1.0D + mod.getAmount();
            modified += attribute.getBaseValue() - 1.0D;
            for (var mod : attribute.getModifiers())
                if (mod.getOperation() == AttributeModifier.Operation.ADDITION) modified += mod.getAmount();
            damage = (float) Math.max(0.0D, modified);
        }
        if (damage <= 0.0F) return;
        var source = new DamageSource(player.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(ENTITY_IMPACT), player, player);
        float knockback = Math.max(0.0F, entry.getFloat(player, PowerProperty.KNOCKBACK));
        boolean hit = false;
        for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(player.getBbWidth(), 0.0D, player.getBbWidth()),
                candidate -> candidate != player && candidate.isAlive() && !candidate.isSpectator()))
        {
            if (!target.hurt(source, damage)) continue;
            if (knockback > 0.0F)
            {
                target.push(motion.x * knockback, motion.y * knockback + 0.1D, motion.z * knockback);
            }
            hit = true;
        }
        if (hit)
        {
            player.fallDistance = 0.0F;
            if (flag(config, "entities", "stopFlying")) stop(player, data, entry);
            AbilityData.playSound(player, entry, "IMPACT_ENTITY");
        }
    }

    /** Original collision objects default each omitted or malformed flag to true. */
    public static boolean flag(JsonObject config, String group, String name)
    {
        JsonObject branch = config != null && config.has(group) && config.get(group).isJsonObject()
                ? config.getAsJsonObject(group) : null;
        JsonElement value = branch != null ? branch.get(name) : null;
        return value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean() || value.getAsBoolean();
    }

    private static void stop(Player player, SHPlayerData data, ModifierEntry entry)
    {
        data.getData().set(Vars.GLIDING, false);
        data.getData().set(Vars.GLIDE_FLYING, false);
        data.getData().set(Vars.JETPACKING, false);
        entry.setToggled(player, false);
    }
}
