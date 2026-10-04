package com.fiskmods.heroes.common.item;

import com.fiskmods.heroes.common.hero.ability.AbilityHandler;
import com.fiskmods.heroes.common.hero.Hero;
import com.fiskmods.heroes.common.hero.HeroTracker;
import com.fiskmods.heroes.common.hero.power.ModifierEntry;
import com.fiskmods.heroes.common.hero.power.PowerProperty;
import com.fiskmods.heroes.common.network.PacketThrowShield;
import com.fiskmods.heroes.common.network.SHNetwork;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** Captain America's shield uses the vanilla shield block action and its original durability. */
public final class ItemCaptainAmericaShield extends ShieldItem
{
    public ItemCaptainAmericaShield(Properties properties)
    {
        super(properties.durability(1561).rarity(Rarity.RARE));
    }

    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity)
    {
        if (entity instanceof net.minecraft.world.entity.player.Player player
                && player.level().isClientSide
                && player.getMainHandItem() == stack
                && !player.getCooldowns().isOnCooldown(this)
                && AbilityHandler.isKeyPressed(player, "SHIELD_THROW"))
        {
            Hero hero = HeroTracker.getHeroType(player);
            ModifierEntry entry = hero != null ? AbilityHandler.findModifier(hero, "SHIELD_THROW") : null;
            int cooldown = entry != null ? entry.getInt(PowerProperty.COOLDOWN_TIME) : 10;
            SHNetwork.sendToServer(new PacketThrowShield());
            player.getCooldowns().addCooldown(this, Math.max(1, cooldown));
        }
        return false;
    }
}
