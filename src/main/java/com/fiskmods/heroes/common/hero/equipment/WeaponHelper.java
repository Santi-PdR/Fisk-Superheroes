package com.fiskmods.heroes.common.hero.equipment;

import javax.annotation.Nullable;

import com.fiskmods.heroes.common.hero.Hero;
import com.fiskmods.heroes.common.hero.HeroIteration;
import com.fiskmods.heroes.common.hero.ItemHeroArmor;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Resolves a suit's selected weapon set and enforces its candidate predicates. */
public final class WeaponHelper
{
    private WeaponHelper() {}

    @Nullable
    public static ItemStack[] getEquippedWeapons(Player player, Hero hero)
    {
        ItemStack core = getCorePiece(player, hero);
        return core != null ? ItemHeroArmor.getWeapons(core) : null;
    }

    /** Mirrors the original equipment-wheel gate: only the configured weapons may be wielded. */
    public static boolean canEquipWeapon(Player player, Hero hero)
    {
        ItemStack[] selected = getEquippedWeapons(player, hero);
        var candidates = hero.getWeaponStacks();
        if (selected == null || selected.length == 0 || candidates.isEmpty()) return false;

        ItemStack held = player.getMainHandItem();
        boolean emptyHand = held.isEmpty();
        if (candidates.size() == 1)
        {
            boolean selectionExists = selected[0] != null && !selected[0].isEmpty();
            return selectionExists ? emptyHand : !emptyHand && candidates.isValid(0, held);
        }
        return emptyHand || hero.isValidWeapon(held);
    }

    /** Finds the configured core armour slot, matching the hero rather than any other worn set. */
    @Nullable
    private static ItemStack getCorePiece(Player player, Hero hero)
    {
        int slot = hero.getCorePieceOfSet();
        if (slot < 0) return null;
        ItemStack stack = player.getInventory().armor.get(3 - slot);
        HeroIteration iteration = ItemHeroArmor.getHero(stack);
        return iteration != null && iteration.getHero() == hero ? stack : null;
    }
}
