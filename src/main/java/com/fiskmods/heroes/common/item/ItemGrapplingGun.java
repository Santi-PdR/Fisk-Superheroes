package com.fiskmods.heroes.common.item;

import com.fiskmods.heroes.common.data.var.Vars;
import com.fiskmods.heroes.common.entity.ModEntities;
import com.fiskmods.heroes.common.entity.projectile.GrapplingHookEntity;
import com.fiskmods.heroes.common.hero.HeroIteration;
import com.fiskmods.heroes.common.hero.HeroTracker;

import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Batman's grappling gun: fire one tether and recall it with another use. */
public final class ItemGrapplingGun extends Item
{
    public ItemGrapplingGun(Properties properties)
    {
        // durability() already forces a stack size of one.
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        ItemStack stack = player.getItemInHand(hand);
        HeroIteration iteration = HeroTracker.getHero(player);
        if (iteration == null || !iteration.getHero().hasPermission(player, "USE_GRAPPLING_GUN")
                || Vars.getBoolean(player, Vars.GLIDING))
        {
            return InteractionResultHolder.fail(stack);
        }

        if (level.isClientSide) return InteractionResultHolder.sidedSuccess(stack, true);

        var existing = level.getEntitiesOfClass(GrapplingHookEntity.class, player.getBoundingBox().inflate(96.0D),
                hook -> hook.getOwner() == player && hook.isAlive() && !hook.isPair());
        GrapplingHookEntity hookedTarget = existing.stream()
                .filter(GrapplingHookEntity::isAttached)
                .filter(hook -> hook.getAttachedEntity() != null)
                .findFirst().orElse(null);
        if (hookedTarget != null)
        {
            GrapplingHookEntity pair = new GrapplingHookEntity(player, hookedTarget.getAttachedEntity());
            if (!level.addFreshEntity(pair)) return InteractionResultHolder.fail(stack);
            hookedTarget.discard();
        }
        else
        {
            GrapplingHookEntity hook = new GrapplingHookEntity(player);
            if (!level.addFreshEntity(hook)) return InteractionResultHolder.fail(stack);
        }

        stack.hurtAndBreak(1, player, user -> user.broadcastBreakEvent(hand));
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(stack, false);
    }
}
