package com.fiskmods.heroes.common.entity.projectile;

import com.fiskmods.heroes.common.data.var.Vars;
import com.fiskmods.heroes.common.entity.ModEntities;
import com.fiskmods.heroes.common.hero.HeroIteration;
import com.fiskmods.heroes.common.hero.HeroTracker;
import com.fiskmods.heroes.common.item.ModItems;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/** Server-authoritative grapple that sticks to a block or entity and pulls toward its anchor. */
public final class GrapplingHookEntity extends ThrowableItemProjectile
{
    private static final EntityDataAccessor<Boolean> ATTACHED = SynchedEntityData.defineId(
            GrapplingHookEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> ATTACHED_ENTITY = SynchedEntityData.defineId(
            GrapplingHookEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> PAIR = SynchedEntityData.defineId(
            GrapplingHookEntity.class, EntityDataSerializers.BOOLEAN);
    private int lifeTicks;

    public GrapplingHookEntity(EntityType<? extends GrapplingHookEntity> type, Level level)
    {
        super(type, level);
    }

    public GrapplingHookEntity(Player owner)
    {
        super(ModEntities.GRAPPLING_HOOK.get(), owner, owner.level());
        setItem(Items.TRIPWIRE_HOOK.getDefaultInstance());
        setPos(owner.getX(), owner.getEyeY() - 0.1D, owner.getZ());
        shootFromRotation(owner, owner.getXRot(), owner.getYRot(), 0.0F, 4.0F, 0.0F);
        setNoGravity(true);
    }

    /** The original second shot turns an entity hit into a short-lived tether pulling that target. */
    public GrapplingHookEntity(Player owner, Entity target)
    {
        super(ModEntities.GRAPPLING_HOOK.get(), owner, owner.level());
        setItem(Items.TRIPWIRE_HOOK.getDefaultInstance());
        entityData.set(PAIR, true);
        entityData.set(ATTACHED, true);
        entityData.set(ATTACHED_ENTITY, target.getId());
        setPos(target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ());
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData()
    {
        super.defineSynchedData();
        entityData.define(ATTACHED, false);
        entityData.define(ATTACHED_ENTITY, -1);
        entityData.define(PAIR, false);
    }

    @Override
    protected Item getDefaultItem()
    {
        return Items.TRIPWIRE_HOOK;
    }

    public boolean isAttached()
    {
        return entityData.get(ATTACHED);
    }

    public boolean isPair()
    {
        return entityData.get(PAIR);
    }

    public Entity getAttachedEntity()
    {
        int id = entityData.get(ATTACHED_ENTITY);
        return id >= 0 ? level().getEntity(id) : null;
    }

    @Override
    protected float getGravity()
    {
        return 0.02F;
    }

    @Override
    public void tick()
    {
        lifeTicks++;
        if (isAttached())
        {
            setNoGravity(true);
            setDeltaMovement(Vec3.ZERO);
            Entity target = getAttachedEntity();
            if (entityData.get(ATTACHED_ENTITY) >= 0)
            {
                if (target == null || !target.isAlive())
                {
                    if (!level().isClientSide) discard();
                    return;
                }
                setPos(target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ());
            }
            else
            {
                setDeltaMovement(Vec3.ZERO);
            }
        }
        else
        {
            super.tick();
        }

        if (level().isClientSide) return;
        if (!isPair() && getOwner() instanceof Player owner)
        {
            for (GrapplingHookEntity other : level().getEntitiesOfClass(GrapplingHookEntity.class,
                    getBoundingBox().inflate(96.0D), hook -> hook != this && !hook.isPair()
                            && hook.isAlive() && hook.getOwner() == owner))
            {
                if (other.tickCount > tickCount) other.discard();
                else
                {
                    discard();
                    return;
                }
            }
        }
        if (lifeTicks > (isPair() ? 100 : 6000) || !(getOwner() instanceof Player player) || !player.isAlive()
                || !holdsGrapplingGun(player) || Vars.getBoolean(player, Vars.GLIDING))
        {
            discard();
            return;
        }

        HeroIteration iteration = HeroTracker.getHero(player);
        if (iteration == null || !iteration.getHero().hasPermission(player, "USE_GRAPPLING_GUN"))
        {
            discard();
            return;
        }

        if (isAttached()) pullTowardAnchor(player);
    }

    private static boolean holdsGrapplingGun(Player player)
    {
        return player.getMainHandItem().is(ModItems.GRAPPLING_GUN.get())
                || player.getOffhandItem().is(ModItems.GRAPPLING_GUN.get());
    }

    private void pullTowardAnchor(Player player)
    {
        Entity target = getAttachedEntity();
        if (isPair() && target != null && target != player)
        {
            pull(target, player.position().subtract(target.position()), 0.35D, distanceTo(player));
            return;
        }

        Vec3 towardHook = position().subtract(player.position());
        double strength = Math.min(towardHook.length() / 4.0D, 1.0D) * 0.35D;
        towardHook = towardHook.normalize().scale(strength);
        player.setDeltaMovement(player.getDeltaMovement().scale(0.9D).add(towardHook));
        player.hasImpulse = true;
        if (player.getDeltaMovement().y >= 0.0D || towardHook.y >= 0.0D) player.fallDistance = 0.0F;

        if (target != null && target != player)
        {
            Vec3 towardPlayer = towardHook.scale(-0.7D);
            target.setDeltaMovement(target.getDeltaMovement().scale(0.9D).add(towardPlayer));
            target.hasImpulse = true;
            if (target.getDeltaMovement().y >= 0.0D || towardPlayer.y >= 0.0D) target.fallDistance = 0.0F;
        }
    }

    private static void pull(Entity target, Vec3 toward, double pullFactor, double distance)
    {
        Vec3 impulse = toward.normalize().scale(Math.min(distance / 4.0D, 1.0D) * pullFactor);
        target.setDeltaMovement(target.getDeltaMovement().scale(0.9D).add(impulse));
        target.hasImpulse = true;
        if (target.getDeltaMovement().y >= 0.0D || impulse.y >= 0.0D) target.fallDistance = 0.0F;
    }

    @Override
    protected void onHitEntity(EntityHitResult hit)
    {
        if (level().isClientSide || hit.getEntity() == getOwner()) return;
        attachToEntity(hit.getEntity());
    }

    @Override
    protected void onHitBlock(BlockHitResult hit)
    {
        if (level().isClientSide) return;
        Vec3 point = hit.getLocation();
        entityData.set(ATTACHED_ENTITY, -1);
        entityData.set(ATTACHED, true);
        setPos(point);
        setDeltaMovement(Vec3.ZERO);
        setNoGravity(true);
    }

    private void attachToEntity(Entity entity)
    {
        entityData.set(ATTACHED_ENTITY, entity.getId());
        entityData.set(ATTACHED, true);
        setDeltaMovement(Vec3.ZERO);
        setNoGravity(true);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag)
    {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("GrappleAttached", isAttached());
        tag.putBoolean("GrapplePair", isPair());
        tag.putInt("GrappleEntity", entityData.get(ATTACHED_ENTITY));
        tag.putDouble("GrappleX", getX());
        tag.putDouble("GrappleY", getY());
        tag.putDouble("GrappleZ", getZ());
        tag.putInt("GrappleLife", lifeTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);
        entityData.set(ATTACHED, tag.getBoolean("GrappleAttached"));
        entityData.set(PAIR, tag.getBoolean("GrapplePair"));
        entityData.set(ATTACHED_ENTITY, tag.getInt("GrappleEntity"));
        setPos(tag.getDouble("GrappleX"), tag.getDouble("GrappleY"), tag.getDouble("GrappleZ"));
        lifeTicks = tag.getInt("GrappleLife");
    }
}
