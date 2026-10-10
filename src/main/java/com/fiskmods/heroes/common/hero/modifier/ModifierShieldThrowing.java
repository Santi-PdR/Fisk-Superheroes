package com.fiskmods.heroes.common.hero.modifier;

import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.entity.ModEntities;
import com.fiskmods.heroes.common.entity.projectile.ThrownShieldEntity;
import com.fiskmods.heroes.common.hero.power.Modifier;
import com.fiskmods.heroes.common.hero.power.ModifierEntry;
import com.fiskmods.heroes.common.item.ModItems;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Server-authoritative Captain America shield throw ability. */
public final class ModifierShieldThrowing extends Modifier
{
    public ModifierShieldThrowing(ResourceLocation id)
    {
        super(id);
    }

    @Override
    public void onActivate(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        if (!(entity instanceof Player player) || entity.level().isClientSide
                || player.getMainHandItem().isEmpty()
                || !player.getMainHandItem().is(ModItems.CAPTAIN_AMERICAS_SHIELD.get())) return;

        ItemStack shield = player.getMainHandItem().copy();
        ThrownShieldEntity projectile = new ThrownShieldEntity(ModEntities.THROWN_SHIELD.get(), player.level());
        projectile.setOwner(player);
        projectile.setItem(shield);
        projectile.setPos(player.getX(), player.getEyeY() - 0.15D, player.getZ());
        projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 2.5F, 0.0F);
        projectile.setDeltaMovement(projectile.getDeltaMovement().add(player.getDeltaMovement().scale(0.5D)));

        if (player.level().addFreshEntity(projectile))
        {
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            player.playSound(SoundEvents.TRIDENT_THROW, 1.0F, 0.9F);
        }
    }
}
