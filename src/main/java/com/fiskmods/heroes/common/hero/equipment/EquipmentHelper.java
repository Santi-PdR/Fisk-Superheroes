package com.fiskmods.heroes.common.hero.equipment;

import java.util.List;

import javax.annotation.Nullable;

import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.hero.Hero;
import com.fiskmods.heroes.common.hero.HeroIteration;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Hands out the equipment a hero declares with {@code hero.addPrimaryEquipment(...)} /
 * {@code hero.addEquipment(...)}. Equipment is only granted when the player does not already carry
 * it, so re-equipping a suit never duplicates items.
 */
public final class EquipmentHelper
{
    private EquipmentHelper()
    {
    }

    public static void grantEquipment(Player player, @Nullable SHPlayerData data)
    {
        if (data == null)
        {
            return;
        }

        HeroIteration iteration = data.getHero();

        if (iteration == null || player.level().isClientSide)
        {
            return;
        }

        Hero hero = iteration.getHero();

        for (Hero.EquipmentEntry entry : hero.getEquipment())
        {
            if (!entry.primary())
            {
                continue;
            }

            ItemStack stack = entry.stack();

            if (stack.isEmpty() || hasItem(player, stack))
            {
                continue;
            }

            if (!player.getInventory().add(stack.copy()))
            {
                player.drop(stack.copy(), false);
            }
        }
    }

    /** Removes equipment granted by a suit when the suit is taken off. */
    public static void revokeEquipment(Player player, List<ItemStack> equipment)
    {
        for (ItemStack stack : equipment)
        {
            player.getInventory().clearOrCountMatchingItems(s -> ItemStack.isSameItemSameTags(s, stack), stack.getCount(), player.inventoryMenu.getCraftSlots());
        }
    }

    public static boolean hasItem(Player player, ItemStack stack)
    {
        for (int i = 0; i < player.getInventory().getContainerSize(); ++i)
        {
            ItemStack other = player.getInventory().getItem(i);

            if (!other.isEmpty() && ItemStack.isSameItemSameTags(other, stack))
            {
                return true;
            }
        }

        return false;
    }
}
