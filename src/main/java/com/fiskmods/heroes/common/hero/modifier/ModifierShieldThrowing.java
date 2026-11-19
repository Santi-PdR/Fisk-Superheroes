package com.fiskmods.heroes.common.hero.modifier;

import com.fiskmods.heroes.common.entity.ModEntities;
import com.fiskmods.heroes.common.entity.projectile.ThrownShieldEntity;
import com.fiskmods.heroes.common.hero.power.Modifier;
import com.fiskmods.heroes.common.hero.power.ModifierEntry;
import com.fiskmods.heroes.common.item.ModItems;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Server-authoritative Captain America shield throw ability. */
public final class ModifierShieldThrowing extends Modifier
{
    public ModifierShieldThrowing(ResourceLocation id)
    {
        super(id);
    }

    /** Spawn first so a cancelled spawn never consumes the held shield or its cooldown. */
    public boolean throwShield(Player player, ModifierEntry entry)
    {
        if (player.level().isClientSide || !player.getMainHandItem().is(ModItems.CAPTAIN_AMERICAS_SHIELD.get())) return false;

        ItemStack shield = player.getMainHandItem().copy();
        ThrownShieldEntity projectile = new ThrownShieldEntity(ModEntities.THROWN_SHIELD.get(), player.level());
        projectile.setOwner(player);
        projectile.setItem(shield);
        projectile.setCanDoGriefing(entry.getBoolean(player, com.fiskmods.heroes.common.hero.power.PowerProperty.CAN_DO_GRIEFING));
        projectile.setPos(player.getX(), player.getEyeY() - 0.15D, player.getZ());
        projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 2.5F, 0.0F);
        projectile.setDeltaMovement(projectile.getDeltaMovement().add(player.getDeltaMovement().scale(0.5D)));

        if (!player.level().addFreshEntity(projectile)) return false;
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        AbilityData.playSound(player, entry, "THROW");
        return true;
    }
}
