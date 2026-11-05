package com.fiskmods.heroes.common.item;

import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.data.var.Vars;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;

/**
 * Tactical Tonfa ported from 1.7.10 ItemTacticalTonfa: 600 durability (900 dual),
 * handle repairable, flips Vars.TONFA_STATE on swing, and uses 3D ModelTonfa renderer.
 */
public final class ItemTacticalTonfa extends SwordItem
{
    public ItemTacticalTonfa(Properties properties)
    {
        super(Tiers.IRON, 3, -2.0F, properties.durability(600));
    }

    @Override
    public int getMaxDamage(ItemStack stack)
    {
        int base = super.getMaxDamage(stack);
        return ItemDualSword.isDual(stack) ? Math.round(base * 1.5F) : base;
    }

    @Override
    public Component getName(ItemStack stack)
    {
        String id = getDescriptionId();
        return Component.translatable(ItemDualSword.isDual(stack) ? id + ".dual" : id);
    }

    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity)
    {
        SHPlayerData data = SHDataCapabilities.getPlayer(entity);
        if (data != null)
        {
            byte current = data.getData().get(Vars.TONFA_STATE);
            data.getData().set(Vars.TONFA_STATE, (byte) (current ^ 1));
        }
        return super.onEntitySwing(stack, entity);
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair)
    {
        return repair.is(ModItems.HANDLE.get());
    }

    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer)
    {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions()
        {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer()
            {
                return com.fiskmods.heroes.client.render.TacticalTonfaItemRenderer.getInstance();
            }
        });
    }
}
