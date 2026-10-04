package com.fiskmods.heroes.common.item;

import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.data.var.Vars;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

/** Compound bow that draws ammunition from the selected slot of a carried quiver. */
public class ItemCompoundBow extends BowItem
{
    public ItemCompoundBow(Properties properties)
    {
        // durability() sets the stack size to one. Calling stacksTo() as well makes Forge reject
        // the properties, even when the explicit stack size is also one.
        super(properties.durability(1500));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        ItemStack bow = player.getItemInHand(hand);
        if (selectedArrow(player).isEmpty())
        {
            return InteractionResultHolder.fail(bow);
        }

        player.startUsingItem(hand);
        return InteractionResultHolder.consume(bow);
    }

    @Override
    public void releaseUsing(ItemStack bow, Level level, LivingEntity entity, int timeLeft)
    {
        if (!(entity instanceof Player player) || level.isClientSide) return;

        ItemStack arrowStack = selectedArrow(player);
        if (arrowStack.isEmpty() || !(arrowStack.getItem() instanceof ArrowItem arrowItem)) return;
        boolean quiverAmmo = !ItemQuiver.getSelectedArrow(player).isEmpty();

        int chargeTicks = getUseDuration(bow) - timeLeft;
        float power = BowItem.getPowerForTime(chargeTicks);
        if (power < 0.1F) return;

        AbstractArrow arrow = arrowItem.createArrow(level, arrowStack.copy(), player);
        boolean horizontal = false;
        SHPlayerData data = SHDataCapabilities.getPlayer(player);
        if (data != null) horizontal = data.getData().get(Vars.HORIZONTAL_BOW);
        float velocity = horizontal ? 2.25F : 3.0F;
        float inaccuracy = horizontal ? 0.5F : 1.0F;
        arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, velocity * power, inaccuracy);
        if (power >= 1.0F) arrow.setCritArrow(true);

        int damage = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.POWER_ARROWS, bow);
        if (damage > 0) arrow.setBaseDamage(arrow.getBaseDamage() + damage * 0.5D + 0.5D);
        int knockback = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PUNCH_ARROWS, bow);
        if (knockback > 0) arrow.setKnockback(knockback);
        if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FLAMING_ARROWS, bow) > 0) arrow.setSecondsOnFire(100);

        if (level.addFreshEntity(arrow))
        {
            if (quiverAmmo)
            {
                ItemQuiver.consumeSelectedArrow(player);
            }
            else if (!player.getAbilities().instabuild
                    && !(arrowStack.is(net.minecraft.world.item.Items.ARROW)
                            && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.INFINITY_ARROWS, bow) > 0))
            {
                arrowStack.shrink(1);
            }
            bow.hurtAndBreak(1, player, user -> user.broadcastBreakEvent(player.getUsedItemHand()));
            float pitch = 1.0F / (level.getRandom().nextFloat() * 0.4F + 1.2F) + power * 0.5F;
            player.playSound(SoundEvents.ARROW_SHOOT, 1.0F, pitch);
            player.awardStat(Stats.ITEM_USED.get(this));
            player.gameEvent(GameEvent.PROJECTILE_SHOOT);
        }
    }

    /** Prefer the selected quiver slot, then support the standard inventory arrow behavior. */
    private static ItemStack selectedArrow(Player player)
    {
        ItemStack quiverArrow = ItemQuiver.getSelectedArrow(player);
        if (!quiverArrow.isEmpty()) return quiverArrow;

        for (ItemStack stack : player.getInventory().items)
        {
            if (ItemQuiver.isArrow(stack)) return stack;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack)
    {
        return UseAnim.BOW;
    }
}
