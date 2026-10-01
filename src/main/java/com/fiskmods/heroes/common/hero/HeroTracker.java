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
        HeroIteration result = null;
        int bestSlot = Integer.MAX_VALUE;

        for (int i = 0; i < 4; ++i)
        {
            ItemStack stack = player.getInventory().armor.get(3 - i);
            HeroIteration iteration = ItemHeroArmor.getHero(stack);

            if (iteration != null && i < bestSlot)
            {
                bestSlot = i;
                result = iteration;
            }
        }

        return result;
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

        HeroIteration worn = getWornSuit(player);
        HeroIteration current = data.getHero();

        if (worn != current)
        {
            onHeroChanged(player, data, current, worn);
        }
    }

    private static void onHeroChanged(Player player, SHPlayerData data, @Nullable HeroIteration previous, @Nullable HeroIteration current)
    {
        data.setHero(current);

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
