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
        boolean result = super.hurtEnemy(stack, target, attacker);
        double reach = attacker.getBbWidth() * 2.0D;
        var box = attacker.getBoundingBox().inflate(reach, 0.5D, reach);
        for (LivingEntity nearby : attacker.level().getEntitiesOfClass(LivingEntity.class, box,
                e -> e != attacker && e != target && e.isAlive() && !e.isAlliedTo(attacker)))
        {
            nearby.hurt(attacker instanceof Player player
                    ? attacker.damageSources().playerAttack(player)
                    : attacker.damageSources().mobAttack(attacker), 4.0F);
        }
        return result;
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
