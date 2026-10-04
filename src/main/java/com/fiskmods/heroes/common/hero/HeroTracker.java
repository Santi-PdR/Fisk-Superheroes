package com.fiskmods.heroes.common.hero;

import javax.annotation.Nullable;

import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.data.SHPlayerData;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Resolves the hero a player is currently wearing, from the suit pieces in their armour slots.
 */
public final class HeroTracker
{
    private HeroTracker()
    {
    }

    /**
     * The iteration worn by the given player, or null when no suit piece is equipped. The
     * chestplate is the "core" piece of a set and therefore takes precedence.
     */
    @Nullable
    public static HeroIteration getWornSuit(Player player)
    {
        // PlayerInventory's armor list is ordered feet, legs, chest, head. The chestplate is
        // the suit's core piece, so prefer it when a player has accidentally mixed hero sets.
        for (int armorIndex : new int[] { 2, 3, 1, 0 })
        {
            ItemStack stack = player.getInventory().armor.get(armorIndex);
            HeroIteration iteration = ItemHeroArmor.getHero(stack);
            if (iteration != null) return iteration;
        }
        return null;
    }

    /** The hero currently active for an entity, taken from its player data. */
    @Nullable
    public static HeroIteration getHero(Entity entity)
    {
        SHPlayerData data = SHDataCapabilities.getPlayer(entity);
        return data != null ? data.getHero() : null;
    }

    @Nullable
    public static Hero getHeroType(Entity entity)
    {
        HeroIteration iteration = getHero(entity);
        return iteration != null ? iteration.getHero() : null;
    }

    public static boolean hasHero(Entity entity)
    {
        return getHero(entity) != null;
    }

    /** Recomputes the worn suit, firing the transition logic when it changes. */
    public static void update(LivingEntity entity)
    {
        if (!(entity instanceof Player player))
        {
            return;
        }

        SHPlayerData data = SHDataCapabilities.getPlayer(player);

        if (data == null)
        {
            return;
        }

        // The original player tracker restored a neutral scale before suit rendering. SCALE's
        // data-var default is 0, and the render layer multiplies every suit vertex by this value;
        // without this initialization the whole costume collapses to a point.
        if (data.getData().get(com.fiskmods.heroes.common.data.var.Vars.SCALE) <= 0.0F)
        {
            data.getData().set(com.fiskmods.heroes.common.data.var.Vars.SCALE, 1.0F);
        }

        com.fiskmods.heroes.common.item.ItemQuiver.updatePlayerData(player);

        HeroIteration worn = getWornSuit(player);
        HeroIteration current = data.getHero();

        if (worn != current)
        {
            onHeroChanged(player, data, current, worn);
        }
    }

    private static void onHeroChanged(Player player, SHPlayerData data, @Nullable HeroIteration previous, @Nullable HeroIteration current)
    {
        if (player.level().isClientSide)
        {
            com.fiskmods.heroes.common.hero.ability.AbilityHandler.clearClient(player);
        }
        else if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)
        {
            com.fiskmods.heroes.common.hero.ability.AbilityHandler.clear(serverPlayer);
        }

        data.setHero(current);

        // Pack equipment (for example the compound bow and quiver) belongs to the newly worn
        // hero. Grant it on suit changes as well as login so choosing an archer after entering a
        // world doesn't leave the player with a bow power but no bow or ammunition container.
        if (current != null && !player.level().isClientSide)
        {
            com.fiskmods.heroes.common.hero.equipment.EquipmentHelper.grantEquipment(player, data);
        }

        if (previous != null)
        {
            com.fiskmods.heroes.common.hero.attribute.SHAttributes.clearModifiers(player);
        }

        if (current == null)
        {
            // Leaving a suit resets transient combat/movement state
            data.getData().resetWithoutSuit();
        }
    }
}
