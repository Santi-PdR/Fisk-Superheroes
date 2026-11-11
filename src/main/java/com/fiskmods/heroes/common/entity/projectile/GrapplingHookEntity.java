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
    // Network entity ids are transient. Persist the UUID and re-resolve it after reload.
    private java.util.UUID targetUuid;
    private int missingTargetTicks;

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
    }

    /** The second shot flies to a block, with a cable anchored to the previously hooked entity. */
    public GrapplingHookEntity(Player owner, Entity target)
    {
        this(owner);
        entityData.set(PAIR, true);
        setTarget(target);
    }

    private void setTarget(Entity target)
    {
        targetUuid = target.getUUID();
        entityData.set(ATTACHED_ENTITY, target.getId());
        missingTargetTicks = 0;
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
        if (!level().isClientSide && targetUuid != null && level() instanceof net.minecraft.server.level.ServerLevel server)
        {
            Entity target = server.getEntity(targetUuid);
            if (target == null)
            {
                entityData.set(ATTACHED_ENTITY, -1);
                if (++missingTargetTicks > 40) discard();
                // Do not accidentally treat an unresolved target as a block attachment.
                return;
            }
            if (!target.isAlive()) { discard(); return; }
            if (entityData.get(ATTACHED_ENTITY) != target.getId()) entityData.set(ATTACHED_ENTITY, target.getId());
            missingTargetTicks = 0;
        }
        if (isAttached())
        {
            // Keep the entity tick and interpolation history advancing without projectile physics.
            baseTick();
            setNoGravity(true);
            setDeltaMovement(Vec3.ZERO);
            Entity target = getAttachedEntity();
            if (!isPair() && target != null)
                setPos(target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ());
        }
        else
        {
            super.tick();
        }

        if (level().isClientSide || isRemoved()) return;
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
        if (lifeTicks > (isPair() ? 100 : 6000) || !(getOwner() instanceof Player player) || !player.isAlive())
        {
            discard();
            return;
        }

        HeroIteration iteration = HeroTracker.getHero(player);
        if (!isPair() && (!holdsGrapplingGun(player) || Vars.getBoolean(player, Vars.GLIDING)
                || (iteration != null && !iteration.getHero().hasPermission(player, "USE_GRAPPLING_GUN"))))
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
        if (isPair())
        {
            // A pair joins the victim to the second impact point, not to the shooter.
            if (target != null && target != player)
                pull(target, position().subtract(target.position()), 0.35D, target.distanceTo(this));
        }
        else if (target != null && target != player)
        {
            // An entity hit reels only the target in; the shooter is not dragged with it.
            pull(target, player.position().subtract(position()), 0.245D, distanceTo(player));
        }
        else
        {
            pull(player, position().subtract(player.position()), 0.35D, distanceTo(player));
        }
    }

    private static void pull(Entity target, Vec3 toward, double pullFactor, double distance)
    {
        Vec3 impulse = toward.normalize().scale(Math.min(distance / 4.0D, 1.0D) * pullFactor);
        target.setDeltaMovement(target.getDeltaMovement().scale(0.9D).add(impulse));
        target.hasImpulse = true;
        target.hurtMarked = true; // Send server-authoritative impulse to remote players too.
        if (target.getDeltaMovement().y >= 0.0D || impulse.y >= 0.0D) target.fallDistance = 0.0F;
    }

    @Override
    protected boolean canHitEntity(Entity target)
    {
        return !isPair() && target != getOwner() && super.canHitEntity(target);
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
        if (!isPair()) entityData.set(ATTACHED_ENTITY, -1);
        entityData.set(ATTACHED, true);
        setPos(point);
        setDeltaMovement(Vec3.ZERO);
        setNoGravity(true);
    }

    private void attachToEntity(Entity entity)
    {
        setTarget(entity);
        entityData.set(ATTACHED, true);
        setPos(entity.getX(), entity.getY() + entity.getBbHeight() * 0.5D, entity.getZ());
        setDeltaMovement(Vec3.ZERO);
        setNoGravity(true);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag)
    {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("GrappleAttached", isAttached());
        tag.putBoolean("GrapplePair", isPair());
        if (targetUuid != null) tag.putUUID("GrappleTarget", targetUuid);
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
        entityData.set(ATTACHED_ENTITY, -1);
        targetUuid = tag.hasUUID("GrappleTarget") ? tag.getUUID("GrappleTarget") : null;
        // Legacy entity IDs cannot safely identify a target in a new session.
        if (targetUuid == null && tag.contains("GrappleEntity") && tag.getInt("GrappleEntity") >= 0) discard();
        setPos(tag.getDouble("GrappleX"), tag.getDouble("GrappleY"), tag.getDouble("GrappleZ"));
        lifeTicks = tag.getInt("GrappleLife");
    }
}
