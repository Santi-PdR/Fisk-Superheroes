package com.fiskmods.heroes.common.entity.projectile;

import com.fiskmods.heroes.common.entity.ModEntities;
import com.fiskmods.heroes.common.item.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/** A Captain America shield that returns to its thrower after hitting a target or block. */
public final class ThrownShieldEntity extends ThrowableItemProjectile
{
    private static final EntityDataAccessor<ItemStack> SHIELD_STACK = SynchedEntityData.defineId(
            ThrownShieldEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Boolean> RETURNING = SynchedEntityData.defineId(
            ThrownShieldEntity.class, EntityDataSerializers.BOOLEAN);
    private static final float DAMAGE = 7.0F;
    private static final int MAX_FLIGHT_TICKS = 200;

    public ThrownShieldEntity(EntityType<? extends ThrownShieldEntity> type, Level level)
    {
        super(type, level);
    }

    public ThrownShieldEntity(Level level, Player owner, ItemStack shield)
    {
        super(ModEntities.THROWN_SHIELD.get(), owner, level);
        setItem(shield.copy());
        setPos(owner.getX(), owner.getEyeY() - 0.15D, owner.getZ());
        setDeltaMovement(getDeltaMovement().add(owner.getDeltaMovement().scale(0.5D)));
    }

    @Override
    protected void defineSynchedData()
    {
        super.defineSynchedData();
        entityData.define(SHIELD_STACK, new ItemStack(ModItems.CAPTAIN_AMERICAS_SHIELD.get()));
        entityData.define(RETURNING, false);
    }

    @Override
    protected Item getDefaultItem()
    {
        return ModItems.CAPTAIN_AMERICAS_SHIELD.get();
    }

    @Override
    public ItemStack getItem()
    {
        return entityData.get(SHIELD_STACK);
    }

    @Override
    public void setItem(ItemStack stack)
    {
        entityData.set(SHIELD_STACK, stack.copy());
    }

    public boolean isReturning()
    {
        return entityData.get(RETURNING);
    }

    private void startReturning()
    {
        if (!level().isClientSide) entityData.set(RETURNING, true);
    }

    @Override
    protected float getGravity()
    {
        return 0.03F;
    }

    @Override
    public void tick()
    {
        Entity owner = getOwner();
        if (!level().isClientSide && (owner == null || !owner.isAlive()))
        {
            dropShield();
            return;
        }

        if (isReturning() && owner != null)
        {
            Vec3 offset = new Vec3(owner.getX(), owner.getEyeY() - 0.2D, owner.getZ()).subtract(position());
            double distance = offset.length();
            if (!level().isClientSide && distance <= 1.25D)
            {
                returnShield(owner);
                return;
            }
            if (distance > 0.001D)
            {
                setDeltaMovement(offset.normalize().scale(Math.min(1.0D + tickCount * 0.025D, 2.5D)));
            }
            setNoGravity(true);
            if (!level().isClientSide && tickCount > MAX_FLIGHT_TICKS) dropShield();
            else super.tick();
            return;
        }

        setNoGravity(false);
        super.tick();
        if (!level().isClientSide && tickCount > MAX_FLIGHT_TICKS) startReturning();
    }

    @Override
    protected void onHitEntity(EntityHitResult hit)
    {
        super.onHitEntity(hit);
        if (level().isClientSide || isReturning()) return;

        Entity owner = getOwner();
        Entity target = hit.getEntity();
        if (target != owner && target instanceof LivingEntity living && owner instanceof LivingEntity attacker)
        {
            boolean damaged = living.hurt(attacker instanceof Player player
                    ? damageSources().playerAttack(player) : damageSources().mobAttack(attacker), DAMAGE);
            if (damaged)
            {
                Vec3 knockback = getDeltaMovement().normalize().scale(0.6D);
                living.knockback(0.6F, -knockback.x, -knockback.z);
                if (attacker instanceof ServerPlayer serverPlayer) living.setLastHurtByPlayer(serverPlayer);
            }
        }
        startReturning();
    }

    @Override
    protected void onHitBlock(BlockHitResult hit)
    {
        super.onHitBlock(hit);
        startReturning();
    }

    private void returnShield(Entity owner)
    {
        if (owner instanceof Player player)
        {
            ItemStack stack = getItem().copy();
            if (!player.getInventory().add(stack)) player.drop(stack, false);
        }
        discard();
    }

    private void dropShield()
    {
        if (!level().isClientSide && !getItem().isEmpty())
        {
            spawnAtLocation(getItem().copy());
            setItem(ItemStack.EMPTY);
        }
        discard();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag)
    {
        super.addAdditionalSaveData(tag);
        tag.put("Shield", getItem().save(new CompoundTag()));
        tag.putBoolean("Returning", isReturning());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Shield", CompoundTag.TAG_COMPOUND)) setItem(ItemStack.of(tag.getCompound("Shield")));
        if (tag.contains("Returning")) entityData.set(RETURNING, tag.getBoolean("Returning"));
    }
}
