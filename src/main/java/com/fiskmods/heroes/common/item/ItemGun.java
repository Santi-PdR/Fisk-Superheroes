package com.fiskmods.heroes.common.item;

import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.data.var.Vars;
import com.fiskmods.heroes.common.hero.Hero;
import com.fiskmods.heroes.common.hero.HeroIteration;
import com.fiskmods.heroes.common.hero.HeroTracker;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Basic magazine firearm used by the bundled firearm heroes. */
public class ItemGun extends Item
{
    private static final String AMMO_TAG = "Ammo";
    private static final String NEXT_SHOT_TAG = "NextShotTick";
    public static final String RELOAD_END_TAG = "fiskheroes_gun_reload_end";
    private static final String RELOAD_DURATION_TAG = "fiskheroes_gun_reload_duration";
    private final int magazineSize;
    private final int cooldownTicks;
    private final int reloadTicks;
    private final double range;
    private final float damage;

    public ItemGun(int magazineSize, int cooldownTicks, int reloadTicks, double range, float damage, Properties properties)
    {
        super(properties.stacksTo(1));
        this.magazineSize = magazineSize;
        this.cooldownTicks = cooldownTicks;
        this.reloadTicks = reloadTicks;
        this.range = range;
        this.damage = damage;
    }

    public boolean isGun()
    {
        return true;
    }

    public int getAmmo(ItemStack stack)
    {
        return stack.hasTag() && stack.getTag().contains(AMMO_TAG)
                ? stack.getTag().getInt(AMMO_TAG) : magazineSize;
    }

    public int getMagazineSize()
    {
        return magazineSize;
    }

    public static boolean isGun(ItemStack stack)
    {
        return !stack.isEmpty() && stack.getItem() instanceof ItemGun;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.consume(stack);

        HeroIteration iteration = HeroTracker.getHero(player);
        Hero hero = iteration != null ? iteration.getHero() : null;
        SHPlayerData data = SHDataCapabilities.getPlayer(player);
        if (hero == null || data == null || !data.getData().get(Vars.AIMING)
                || !hero.hasPermission(player, "USE_GUN") || data.getData().get(Vars.RELOAD_TIMER) > 0.0F)
        {
            return InteractionResultHolder.fail(stack);
        }

        long now = level.getGameTime();
        if (stack.getOrCreateTag().getLong(NEXT_SHOT_TAG) > now)
        {
            return InteractionResultHolder.consume(stack);
        }
        stack.getOrCreateTag().putLong(NEXT_SHOT_TAG, now + cooldownTicks);

        int ammo = getAmmo(stack);
        if (ammo <= 0)
        {
            player.playSound(SoundEvents.DISPENSER_FAIL, 0.8F, 0.8F + level.random.nextFloat() * 0.3F);
            return InteractionResultHolder.consume(stack);
        }

        stack.getOrCreateTag().putInt(AMMO_TAG, ammo - 1);
        if (player instanceof ServerPlayer serverPlayer) fire(serverPlayer);
        player.playSound(SoundEvents.CROSSBOW_SHOOT, 1.0F, 0.9F + level.random.nextFloat() * 0.2F);
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.consume(stack);
    }

    private void fire(ServerPlayer shooter)
    {
        Level level = shooter.level();
        Vec3 start = shooter.getEyePosition();
        Vec3 direction = shooter.getViewVector(1.0F);
        Vec3 end = start.add(direction.scale(range));
        BlockHitResult block = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, shooter));
        if (block.getType() != HitResult.Type.MISS) end = block.getLocation();

        AABB search = shooter.getBoundingBox().expandTowards(direction.scale(range)).inflate(1.0D);
        LivingEntity hit = null;
        double nearest = Double.MAX_VALUE;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, search,
                entity -> entity != shooter && entity.isAlive() && !entity.isAlliedTo(shooter)))
        {
            var intersection = target.getBoundingBox().inflate(0.25D).clip(start, end);
            if (intersection.isPresent())
            {
                double distance = start.distanceToSqr(intersection.get());
                if (distance < nearest)
                {
                    nearest = distance;
                    hit = target;
                }
            }
        }

        if (hit != null) hit.hurt(level.damageSources().playerAttack(shooter), damage);
    }

    /** Reload starts from the hero keybind and restores the magazine with a visible reload timer. */
    public void reload(Player player, Hero hero)
    {
        ItemStack stack = player.getMainHandItem();
        if (stack.getItem() != this || !hero.hasPermission(player, "USE_GUN")
                || getAmmo(stack) >= magazineSize || player.getPersistentData().getLong(RELOAD_END_TAG) > player.level().getGameTime())
        {
            return;
        }

        stack.getOrCreateTag().putInt(AMMO_TAG, magazineSize);
        player.getPersistentData().putLong(RELOAD_END_TAG, player.level().getGameTime() + reloadTicks);
        player.getPersistentData().putInt(RELOAD_DURATION_TAG, reloadTicks);
        SHPlayerData data = SHDataCapabilities.getPlayer(player);
        if (data != null) data.getData().set(Vars.RELOAD_TIMER, 1.0F);
        player.playSound(SoundEvents.ARMOR_EQUIP_IRON, 0.7F, 1.25F);
    }

    public static void tickReload(Player player, SHPlayerData data)
    {
        long end = player.getPersistentData().getLong(RELOAD_END_TAG);
        if (end <= 0L) return;
        long remaining = end - player.level().getGameTime();
        if (remaining <= 0L)
        {
            player.getPersistentData().remove(RELOAD_END_TAG);
            player.getPersistentData().remove(RELOAD_DURATION_TAG);
            data.getData().set(Vars.RELOAD_TIMER, 0.0F);
        }
        else
        {
            int duration = Math.max(1, player.getPersistentData().getInt(RELOAD_DURATION_TAG));
            data.getData().set(Vars.RELOAD_TIMER, Math.min(1.0F, remaining / (float) duration));
        }
    }
}
