package com.fiskmods.heroes.common.network;

import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.entity.ModEntities;
import com.fiskmods.heroes.common.entity.projectile.ThrownShieldEntity;
import com.fiskmods.heroes.common.hero.Hero;
import com.fiskmods.heroes.common.hero.HeroTracker;
import com.fiskmods.heroes.common.hero.ability.AbilityHandler;
import com.fiskmods.heroes.common.hero.modifier.Modifiers;
import com.fiskmods.heroes.common.hero.power.ModifierEntry;
import com.fiskmods.heroes.common.item.ModItems;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

/** Client request to throw the equipped Captain America shield while SHIELD_THROW is held. */
public final class PacketThrowShield extends SHPacket
{
    public PacketThrowShield() {}

    public PacketThrowShield(FriendlyByteBuf buf) {}

    @Override
    public void encode(FriendlyByteBuf buf) {}

    @Override
    public void handle(NetworkEvent.Context context)
    {
        ServerPlayer player = context.getSender();
        if (player == null || player.getMainHandItem().getItem() != ModItems.CAPTAIN_AMERICAS_SHIELD.get()
                || player.getCooldowns().isOnCooldown(ModItems.CAPTAIN_AMERICAS_SHIELD.get())
                || !AbilityHandler.isKeyPressed(player, "SHIELD_THROW")) return;

        SHPlayerData data = SHDataCapabilities.getPlayer(player);
        Hero hero = HeroTracker.getHeroType(player);
        if (data == null || hero == null) return;

        ModifierEntry entry = AbilityHandler.findModifier(hero, "SHIELD_THROW", player);
        if (entry == null || !entry.isEnabled() || !entry.isModifierEnabled(player, data)) return;

        ItemStack shield = player.getMainHandItem().copy();
        ThrownShieldEntity projectile = new ThrownShieldEntity(ModEntities.THROWN_SHIELD.get(), player.level());
        projectile.setOwner(player);
        projectile.setItem(shield);
        projectile.setPos(player.getX(), player.getEyeY() - 0.15D, player.getZ());
        projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 2.5F, 0.0F);
        projectile.setDeltaMovement(projectile.getDeltaMovement().add(player.getDeltaMovement().scale(0.5D)));
        player.level().addFreshEntity(projectile);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.getCooldowns().addCooldown(ModItems.CAPTAIN_AMERICAS_SHIELD.get(), Math.max(1, entry.getInt(player,
                com.fiskmods.heroes.common.hero.power.PowerProperty.COOLDOWN_TIME)));
    }
}
