package com.fiskmods.heroes.common.hero.equipment;

import java.util.List;

import javax.annotation.Nullable;

import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.hero.Hero;
import com.fiskmods.heroes.common.hero.HeroIteration;
import com.fiskmods.heroes.common.hero.power.ModifierEntry;
import com.fiskmods.heroes.common.hero.power.PowerProperty;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

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

    /** Finds the enabled equipment power which owns the hero's utility belt. */
    @Nullable
    public static ModifierEntry getUtilityBelt(Hero hero, Player player, SHPlayerData data)
    {
        for (ModifierEntry entry : hero.getPowerContainer().getEntries())
        {
            if (!"equipment".equals(entry.getModifier().getId().getPath())
                    || !entry.isEnabled() || !entry.isModifierEnabled(player, data)
                    || equipmentOptions(entry).size() == 0)
            {
                continue;
            }

            return entry;
        }

        return null;
    }

    /** The insertion-ordered option map is the selection order used by the original belt. */
    public static JsonObject equipmentOptions(ModifierEntry entry)
    {
        JsonElement configuration = entry.get(PowerProperty.EQUIPMENT);
        if (configuration == null || !configuration.isJsonObject()) return new JsonObject();

        JsonElement options = configuration.getAsJsonObject().get("equipment");
        return options != null && options.isJsonObject() ? options.getAsJsonObject() : new JsonObject();
    }

    /** Advances the active pack-defined gadget, after validating the selection server-side. */
    public static boolean cycleUtilityBelt(net.minecraft.server.level.ServerPlayer player, int direction)
    {
        if (direction == 0) return false;

        SHPlayerData data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(player);
        HeroIteration iteration = data != null ? data.getHero() : null;
        if (iteration == null) return false;

        Hero hero = iteration.getHero();
        if (!hero.isKeyPressed(player, "UTILITY_BELT")) return false;

        ModifierEntry entry = getUtilityBelt(hero, player, data);
        if (entry == null) return false;

        int count = equipmentOptions(entry).size();
        int current = data.getData().get(com.fiskmods.heroes.common.data.var.Vars.UTILITY_BELT_TYPE);
        int selected = Math.floorMod(current + Integer.signum(direction), count);
        data.getData().set(com.fiskmods.heroes.common.data.var.Vars.PREV_UTILITY_BELT_TYPE, (byte) current);
        data.getData().set(com.fiskmods.heroes.common.data.var.Vars.UTILITY_BELT_TYPE, (byte) selected);
        com.fiskmods.heroes.common.hero.modifier.AbilityData.playSound(player, entry, "SWITCH");
        return true;
    }
}
