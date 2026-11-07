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

    /** The visible iteration, preferring a valid chestpiece over other worn suit pieces. */
    @Nullable
    public static HeroIteration getWornSuit(Player player)
    {
        // Inventory armor is ordered feet, legs, chest, head; ItemHeroArmor slots are reversed.
        for (int armorIndex : new int[] { 2, 3, 1, 0 })
        {
            ItemStack stack = player.getInventory().armor.get(armorIndex);
            if (!(stack.getItem() instanceof ItemHeroArmor armor) || armor.getSlot() != 3 - armorIndex) continue;
            HeroIteration iteration = ItemHeroArmor.getHero(stack);
            if (iteration != null && iteration.hasPieceOfSet(armor.getSlot())) return iteration;
        }
        return null;
    }

    /** Every defined piece must be the correct armor item and the same suit iteration. */
    public static boolean hasCompleteSuit(Player player, @Nullable HeroIteration iteration)
    {
        if (iteration == null) return false;
        boolean hasPiece = false;
        for (int slot = 0; slot < 4; ++slot)
        {
            if (!iteration.hasPieceOfSet(slot)) continue;
            hasPiece = true;
            ItemStack stack = player.getInventory().armor.get(3 - slot);
            if (!(stack.getItem() instanceof ItemHeroArmor armor) || armor.getSlot() != slot
                    || ItemHeroArmor.getHero(stack) != iteration) return false;
        }
        return hasPiece;
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
        update(entity, com.fiskmods.heroes.common.config.SHConfig.REQUIRE_FULL_SUIT.get());
    }

    /** Resolve with the given server policy; kept separate so tests need not mutate the live config file. */
    public static void update(LivingEntity entity, boolean requireFullSuit)
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

        // Once a suit packet has arrived, the server is authoritative. A local COMMON config may
        // differ from the server's requireFullSuit setting and must not overwrite its decision.
        if (player.level().isClientSide && data.hasAuthoritativeHero()) return;
        HeroIteration worn = getWornSuit(player);
        if (requireFullSuit && !hasCompleteSuit(player, worn))
            worn = null;
        HeroIteration current = data.getHero();

        if (worn != current)
        {
            onHeroChanged(player, data, current, worn);
        }
    }

    /** Apply an authoritative suit transition received from the server. */
    public static void applySynchronizedHero(Player player, @Nullable HeroIteration iteration)
    {
        SHPlayerData data = SHDataCapabilities.getPlayer(player);
        if (data == null) return;
        data.markAuthoritativeHero();
        if (data.getHero() != iteration) onHeroChanged(player, data, data.getHero(), iteration);
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

        if (previous != null && !player.level().isClientSide)
        {
            for (var entry : previous.getHero().getPowerContainer().getEntries())
                com.fiskmods.heroes.common.hero.modifier.AbilityData.stopSoundLoops(player, entry, "ENABLE");
        }
        data.getSoundStates().clear();
        data.setHero(current);

        // Suit-dependent states must not leak into a hero without the relevant power. In
        // particular, a speed toggle left behind used to survive taking the suit off.
        if (!hasModifier(current, "super_speed"))
            com.fiskmods.heroes.common.hero.modifier.ModifierSuperSpeed.disable(player);
        if (!hasModifier(current, "slow_motion"))
        {
            data.getData().set(com.fiskmods.heroes.common.data.var.Vars.SLOW_MOTION, false);
            data.setToggleEnabled(com.fiskmods.heroes.FiskHeroes.id("slow_motion"), false);
        }

        if (!hasModifier(current, "intangibility"))
        {
            data.getData().set(com.fiskmods.heroes.common.data.var.Vars.INTANGIBLE, false);
            data.getData().set(com.fiskmods.heroes.common.data.var.Vars.INTANGIBILITY_TIMER, 0.0F);
            data.setToggleEnabled(com.fiskmods.heroes.FiskHeroes.id("intangibility"), false);
            com.fiskmods.heroes.common.hero.modifier.IntangibilityPhysics.release(player, data);
        }

        if (!hasModifier(current, "shield"))
        {
            data.getData().set(com.fiskmods.heroes.common.data.var.Vars.SHIELD, false);
            data.getData().set(com.fiskmods.heroes.common.data.var.Vars.SHIELD_BLOCKING, false);
            data.getData().set(com.fiskmods.heroes.common.data.var.Vars.SHIELD_TIMER, 0.0F);
            data.getData().set(com.fiskmods.heroes.common.data.var.Vars.SHIELD_BLOCKING_TIMER, 0.0F);
            data.setToggleEnabled(com.fiskmods.heroes.FiskHeroes.id("shield"), false);
        }

        if (!hasModifier(current, "size_manipulation"))
        {
            data.getData().set(com.fiskmods.heroes.common.data.var.Vars.SIZE_STATE, (byte) 0);
            data.getData().set(com.fiskmods.heroes.common.data.var.Vars.SCALE, 1.0F);
            data.getData().set(com.fiskmods.heroes.common.data.var.Vars.SHRINK_TIMER, 0.0F);
            data.getData().set(com.fiskmods.heroes.common.data.var.Vars.GIANT_MODE, false);
            data.getData().set(com.fiskmods.heroes.common.data.var.Vars.GIANT_MODE_TIMER, 0.0F);
        }

        if (!hasModifier(current, "web_swinging"))
        {
            data.getData().set(com.fiskmods.heroes.common.data.var.Vars.WEB_SWINGING, false);
            data.getData().set(com.fiskmods.heroes.common.data.var.Vars.TICKS_SINCE_SWINGING, Short.MAX_VALUE);
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)
                com.fiskmods.heroes.common.hero.ability.WebSwingHandler.release(serverPlayer);
        }

        if (!hasModifier(current, "invisibility"))
        {
            data.getData().set(com.fiskmods.heroes.common.data.var.Vars.INVISIBLE, false);
            data.getData().set(com.fiskmods.heroes.common.data.var.Vars.INVISIBILITY_TIMER, 0.0F);
            data.setToggleEnabled(com.fiskmods.heroes.FiskHeroes.id("invisibility"), false);
            com.fiskmods.heroes.common.hero.modifier.InvisibilityState.release(player, data);
        }

        if (previous != null && !player.level().isClientSide)
        {
            com.fiskmods.heroes.common.hero.attribute.SHAttributes.clearModifiers(player);
        }

        if (current == null)
        {
            // Leaving a suit resets transient combat/movement state
            data.getData().resetWithoutSuit();
        }
    }
    private static boolean hasModifier(@Nullable HeroIteration iteration, String path)
    {
        return iteration != null && iteration.getHero().getPowerContainer().getEntries().stream()
                .anyMatch(entry -> entry.getModifier().getId().getPath().equals(path));
    }

}
