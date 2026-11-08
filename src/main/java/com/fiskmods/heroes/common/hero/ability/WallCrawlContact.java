package com.fiskmods.heroes.common.hero.ability;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.data.SHDataCapabilities;

import net.minecraft.world.entity.player.Player;

/** Server-verifiable wall contact, shared by crawling and the vanilla floating check. */
public final class WallCrawlContact
{
    private WallCrawlContact() { }

    public static boolean canCrawl(Player player)
    {
        return !player.onGround() && !player.isPassenger() && !player.getAbilities().flying
                && !player.isFallFlying() && !player.isInWater() && !player.noPhysics;
    }

    public static boolean touchingWall(Player player)
    {
        var box = player.getBoundingBox();
        return player.level().getBlockCollisions(player, box.move(0.05D, 0, 0)).iterator().hasNext()
                || player.level().getBlockCollisions(player, box.move(-0.05D, 0, 0)).iterator().hasNext()
                || player.level().getBlockCollisions(player, box.move(0, 0, 0.05D)).iterator().hasNext()
                || player.level().getBlockCollisions(player, box.move(0, 0, -0.05D)).iterator().hasNext();
    }

    /** Does not trust a client-provided crawl flag: checks the current suit and block shapes. */
    public static boolean hasActiveContact(Player player)
    {
        if (!canCrawl(player) || !touchingWall(player)) return false;
        var data = SHDataCapabilities.getPlayer(player);
        var hero = data != null ? data.getHeroType() : null;
        return hero != null && hero.getPowerContainer().getEntries().stream().anyMatch(entry ->
                entry.getModifier().getId().equals(FiskHeroes.id("wall_crawling"))
                        && entry.isEnabled() && entry.isModifierEnabled(player, data));
    }
}
