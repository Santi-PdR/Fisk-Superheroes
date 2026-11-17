package com.fiskmods.heroes.common.item;

import com.fiskmods.heroes.common.hero.speedster.VelocityNine;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/** Drinkable Velocity 9 bottle; one use takes 32 ticks, like the original. */
public class ItemVelocityNine extends Item
{
    public ItemVelocityNine(Properties properties) { super(properties); }

    @Override public String getDescriptionId() { return "item.velocity_nine.name"; }
    @Override public int getUseDuration(ItemStack stack) { return 32; }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.DRINK; }
    @Override public boolean isFoil(ItemStack stack) { return true; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity)
    {
        if (!(entity instanceof Player player)) return stack;
        if (!level.isClientSide) VelocityNine.dose(player);
        if (!player.getAbilities().instabuild)
        {
            stack.shrink(1);
            if (stack.isEmpty()) return new ItemStack(Items.GLASS_BOTTLE);
            if (!player.getInventory().add(new ItemStack(Items.GLASS_BOTTLE)))
                player.drop(new ItemStack(Items.GLASS_BOTTLE), false);
        }
        return stack;
    }
}
