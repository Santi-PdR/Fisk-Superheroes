package com.fiskmods.heroes.common.hero.ability;

import java.util.UUID;

import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.hero.Hero;
import com.fiskmods.heroes.common.hero.attribute.SHAttributes;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Applies the original suit melee attributes before vanilla calculates cooldown and critical hits. */
public final class MeleeCombat
{
    private static final UUID ATTACK_BONUS = UUID.fromString("4ecc64a9-232d-49a5-9ea0-0dc98d6dcc13");

    private MeleeCombat() {}

    /** Called from Forge's AttackEntityEvent, at the start of Player.attack. */
    public static void prepareAttack(Player player)
    {
        if (player.level().isClientSide) return;
        clearAttackBonus(player);
        var data = SHDataCapabilities.getPlayer(player);
        Hero hero = data != null ? data.getHeroType() : null;
        if (hero == null) return;

        // A charge or held item may have changed since the last tick. Resolve the current
        // profile before calculating the attack, not after the charged punch is consumed.
        SHAttributes.applyModifiers(player);
        AttributeInstance attack = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack == null) return;
        ItemStack held = player.getMainHandItem();
        boolean weapon = held.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE)
                .stream().anyMatch(mod -> mod.getAmount() > 0.0D);
        AttributeInstance suit = player.getAttribute(weapon ? SHAttributes.WEAPON_DAMAGE.get() : SHAttributes.PUNCH_DAMAGE.get());
        if (suit == null) return;
        double base = attack.getValue();
        double difference = apply(suit, base) - base;
        if (weapon)
        {
            boolean matching = hero.getEquipment().stream().anyMatch(e -> e.stack().getItem() == held.getItem());
            // Original gives the full positive bonus to the hero's own equipment, but only
            // half to other weapons; negative bonuses have the opposite rule.
            if ((difference > 0.0D && !matching) || (difference < 0.0D && matching)) difference *= 0.5D;
        }
        // Vanilla adds operation 0 before multiplying. Divide by the existing multipliers so
        // our resulting delta matches the original wrapper and doesn't double-scale bonuses.
        double factor = multiplier(attack);
        if (Math.abs(difference) > 1.0E-7D && factor > 1.0E-8D && Double.isFinite(difference))
            attack.addTransientModifier(new AttributeModifier(ATTACK_BONUS, "fiskheroes:melee_attack", difference / factor,
                    AttributeModifier.Operation.ADDITION));
    }

    /** Remove even after cancelled/non-living attacks, suit changes, and at the next player tick. */
    public static void clearAttackBonus(LivingEntity entity)
    {
        AttributeInstance attack = entity.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null) attack.removeModifier(ATTACK_BONUS);
    }

    /** Snapshot before outgoing modifiers consume a charged punch. */
    public static double extraKnockback(LivingEntity attacker, DamageSource source, boolean hooked)
    {
        if (!(attacker instanceof Player player) || player.level().isClientSide || !isDirectMelee(attacker, source)) return 0.0D;
        var data = SHDataCapabilities.getPlayer(player);
        if (data == null || data.getHeroType() == null) return 0.0D;
        SHAttributes.applyModifiers(player);
        AttributeInstance knockback = player.getAttribute(SHAttributes.KNOCKBACK.get());
        if (knockback == null) return 0.0D;
        double vanilla = net.minecraft.world.item.enchantment.EnchantmentHelper.getKnockbackBonus(player);
        if (player.isSprinting() && player.getAttackStrengthScale(0.5F) > 0.9F) vanilla++;
        return Math.max(0.0D, apply(knockback, vanilla + (hooked ? 4.0D : 0.0D)) - vanilla);
    }

    public static boolean isDirectMelee(LivingEntity attacker, DamageSource source)
    {
        return attacker != null && source.getEntity() == attacker && source.getDirectEntity() == attacker
                && (source.is(DamageTypes.PLAYER_ATTACK) || source.is(DamageTypes.MOB_ATTACK));
    }

    /** Original AttributeWrapper applies both kinds of multiplier before additions. */
    private static double apply(AttributeInstance attribute, double value)
    {
        for (AttributeModifier.Operation operation : new AttributeModifier.Operation[] {
                AttributeModifier.Operation.MULTIPLY_BASE, AttributeModifier.Operation.MULTIPLY_TOTAL })
            for (AttributeModifier modifier : attribute.getModifiers(operation)) value *= 1.0D + modifier.getAmount();
        for (AttributeModifier modifier : attribute.getModifiers(AttributeModifier.Operation.ADDITION)) value += modifier.getAmount();
        return value;
    }

    private static double multiplier(AttributeInstance attribute)
    {
        double base = 1.0D;
        for (AttributeModifier modifier : attribute.getModifiers(AttributeModifier.Operation.MULTIPLY_BASE)) base += modifier.getAmount();
        for (AttributeModifier modifier : attribute.getModifiers(AttributeModifier.Operation.MULTIPLY_TOTAL)) base *= 1.0D + modifier.getAmount();
        return base;
    }
}
