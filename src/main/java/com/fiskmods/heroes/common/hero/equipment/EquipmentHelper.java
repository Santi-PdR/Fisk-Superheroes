package com.fiskmods.heroes.common.hero.equipment;

import java.util.List;

import javax.annotation.Nullable;

import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.hero.Hero;
import com.fiskmods.heroes.common.hero.HeroIteration;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Hands out ordinary equipment declared with {@code hero.addEquipment(...)}. Primary weapon
 * candidates are tracked separately by {@link com.fiskmods.heroes.common.hero.WeaponList}; they
 * are not treated as a second equipment inventory.
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
