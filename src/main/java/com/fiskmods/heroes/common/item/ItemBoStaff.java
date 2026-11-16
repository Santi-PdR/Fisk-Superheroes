package com.fiskmods.heroes.common.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;

/**
 * Bo Staff ported from 1.7.10 ItemBoStaff: 800 durability, iron ingot repair,
 * sweeping AOE hit on nearby targets, and 3D telescoping ModelBoStaff renderer.
 */
public final class ItemBoStaff extends SwordItem
{
    public ItemBoStaff(Properties properties)
    {
        super(Tiers.IRON, 3, -2.2F, properties.durability(800));
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker)
    {
        if (!attacker.level().isClientSide)
        {
            double reach = attacker.getBbWidth() * 2.0D;
            var box = attacker.getBoundingBox().inflate(reach, 0.5D, reach);
            for (LivingEntity nearby : attacker.level().getEntitiesOfClass(LivingEntity.class, box,
                    e -> e != attacker && e != target && e.isAlive() && e.isAttackable() && !e.isAlliedTo(attacker)))
            {
                if (attacker instanceof Player player && nearby instanceof Player other && !player.canHarmPlayer(other)) continue;
                if (nearby.skipAttackInteraction(attacker)) continue;
                // The original staff sweep uses the wielder's attack attribute and
                // enchantments, not a fixed four points independent of the weapon.
                var attack = attacker.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
                float damage = attack != null ? (float) attack.getValue() : 0.0F;
                float enchanted = net.minecraft.world.item.enchantment.EnchantmentHelper.getDamageBonus(stack, nearby.getMobType());
                if (attacker.fallDistance > 0.0F && !attacker.onGround() && !attacker.onClimbable()
                        && !attacker.isInWater() && !attacker.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS)
                        && !attacker.isPassenger()) damage *= 1.5F;
                damage += enchanted;
                if (damage <= 0.0F) continue;
                var source = attacker instanceof Player player ? attacker.damageSources().playerAttack(player)
                        : attacker.damageSources().mobAttack(attacker);
                if (nearby.hurt(source, damage))
                {
                    int knockback = net.minecraft.world.item.enchantment.EnchantmentHelper.getKnockbackBonus(attacker)
                            + (attacker.isSprinting() ? 1 : 0);
                    if (knockback > 0)
                        nearby.knockback(knockback * 0.5F, attacker.getX() - nearby.getX(), attacker.getZ() - nearby.getZ());
                    int fire = net.minecraft.world.item.enchantment.EnchantmentHelper.getFireAspect(attacker);
                    if (fire > 0) nearby.setSecondsOnFire(fire * 4);
                    net.minecraft.world.item.enchantment.EnchantmentHelper.doPostHurtEffects(nearby, attacker);
                    net.minecraft.world.item.enchantment.EnchantmentHelper.doPostDamageEffects(attacker, nearby);
                }
            }
        }
        // Damage the item only after the sweep so the last durability point does
        // not erase the enchantments before they are applied to nearby targets.
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair)
    {
        return repair.is(Items.IRON_INGOT) || super.isValidRepairItem(toRepair, repair);
    }

    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer)
    {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions()
        {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer()
            {
                return com.fiskmods.heroes.client.render.BoStaffItemRenderer.getInstance();
            }
        });
    }
}
