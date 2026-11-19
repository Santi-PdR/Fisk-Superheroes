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
    // The original advances the projectile's age to 50 on a slow magnetic impact.
    // Sync only the offset, not a changing timer every tick.
    private static final EntityDataAccessor<Integer> AGE_OFFSET = SynchedEntityData.defineId(
            ThrownShieldEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> MAGNETIC_ACTIVE = SynchedEntityData.defineId(
            ThrownShieldEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<java.util.Optional<java.util.UUID>> OWNER_UUID = SynchedEntityData.defineId(
            ThrownShieldEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    public static final net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType> SHIELD_DAMAGE =
            net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,
                    com.fiskmods.heroes.FiskHeroes.id("thrown_shield"));
    private static final int MAX_FLIGHT_TICKS = 200;
    private static final int MAX_RETURN_TICKS = 200;
    private int returnTicks;
    private boolean canDoGriefing;
    private float spinTime, previousSpinTime;
    private boolean magneticDiagnosticLogged;
    private boolean ownerDiagnosticLogged;

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
        entityData.define(AGE_OFFSET, 0);
        entityData.define(MAGNETIC_ACTIVE, false);
        entityData.define(OWNER_UUID, java.util.Optional.empty());
    }

    @Override
    public void setOwner(Entity owner)
    {
        super.setOwner(owner);
        if (!level().isClientSide && owner != null) entityData.set(OWNER_UUID, java.util.Optional.of(owner.getUUID()));
    }

    @Override
    public Entity getOwner()
    {
        Entity owner = super.getOwner();
        if (owner == null && level().isClientSide)
            return entityData.get(OWNER_UUID).map(id -> level().getPlayerByUUID(id)).orElse(null);
        return owner;
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

    public void setCanDoGriefing(boolean value)
    {
        canDoGriefing = value;
    }

    public boolean canDoGriefing()
    {
        return canDoGriefing;
    }

    private void startReturning()
    {
        if (!level().isClientSide) entityData.set(RETURNING, true);
    }

    /** Interpolated velocity-driven rotation, as in the original projectile. */
    public float getSpin(float partialTick)
    {
        return net.minecraft.util.Mth.lerp(partialTick, previousSpinTime, spinTime) * 32.0F;
    }

    @Override
    protected float getGravity()
    {
        return 0.03F;
    }

    private int flightAge()
    {
        return tickCount + entityData.get(AGE_OFFSET);
    }

    private boolean isMagneticallyAttracted()
    {
        return isReturning() && com.fiskmods.heroes.common.item.ItemCaptainAmericaShield.isElectromagnetic(getItem())
                && flightAge() > 50 && getOwner() instanceof Player;
    }

    private void accelerateMagneticReturn()
    {
        if (!level().isClientSide && flightAge() < 50) entityData.set(AGE_OFFSET, 50 - tickCount);
    }

    @Override
    public void tick()
    {
        previousSpinTime = spinTime;
        if (spinTime == 0.0F) previousSpinTime = spinTime = random.nextFloat() * 100.0F;
        spinTime += (float) getDeltaMovement().length();
        Entity owner = getOwner();
        if (!level().isClientSide && (owner == null || !owner.isAlive()))
        {
            dropShield();
            return;
        }
        // Owner UUIDs loaded from disk bypass setOwner; sync them once resolved.
        if (!level().isClientSide && owner != null && !entityData.get(OWNER_UUID).equals(java.util.Optional.of(owner.getUUID())))
            entityData.set(OWNER_UUID, java.util.Optional.of(owner.getUUID()));
        if (level().isClientSide && owner instanceof Player && !ownerDiagnosticLogged
                && com.fiskmods.heroes.common.item.ItemCaptainAmericaShield.isElectromagnetic(getItem())
                && Boolean.getBoolean("fiskheroes.ciShieldDiagnostics"))
        {
            ownerDiagnosticLogged = true;
            com.fiskmods.heroes.FiskHeroes.LOGGER.info("Shield owner resolved: entity={}, owner={}", getId(), owner.getName().getString());
        }

        if (isReturning() && owner != null)
        {
            if (!level().isClientSide && !(owner instanceof Player))
            {
                dropShield();
                return;
            }
            // Entity tickCount starts at zero for a newly tracking client. Only the
            // server can use it as the projectile's age; clients follow its motion.
            if (!level().isClientSide && isMagneticallyAttracted())
            {
                if (!entityData.get(MAGNETIC_ACTIVE)) entityData.set(MAGNETIC_ACTIVE, true);
                Vec3 offset = new Vec3(owner.getX(), owner.getY() + owner.getBbHeight() * 0.6D, owner.getZ()).subtract(position());
                double distance = offset.length();
                if (distance > 0.001D)
                {
                    // Capping the last step avoids skipping over the owner's pickup volume.
                    double speed = Math.min(0.2D + flightAge() / 15.0D, distance);
                    setDeltaMovement(offset.scale(speed / distance));
                    hasImpulse = true;
                }
            }
            if (!magneticDiagnosticLogged && entityData.get(MAGNETIC_ACTIVE)
                    && Boolean.getBoolean("fiskheroes.ciShieldDiagnostics"))
            {
                magneticDiagnosticLogged = true;
                com.fiskmods.heroes.FiskHeroes.LOGGER.info("Shield magnetic return: side={}, entity={}, owner={}, magnetic={}, age={}",
                        level().isClientSide ? "client" : "server", getId(), owner.getName().getString(),
                        com.fiskmods.heroes.common.item.ItemCaptainAmericaShield.isElectromagnetic(getItem()), flightAge());
            }
            if (!level().isClientSide && distanceTo(owner) <= net.minecraft.util.Mth.clamp(getDeltaMovement().length() * 3.0D, 1.0D, 5.0D))
            {
                returnShield(owner);
                return;
            }
            if (!level().isClientSide && ++returnTicks > MAX_RETURN_TICKS)
            {
                dropShield();
                return;
            }
        }

        // Vanilla 1.20 traces collision shapes, so plants with no collision need a
        // separate, bounded outline trace before the ordinary projectile movement.
        if (!level().isClientSide) breakShortGrassAlongFlight();
        // ThrowableProjectile supplies the same air/water drag and 0.03 gravity as the original.
        super.tick();
        // Retain a bounded fallback for projectiles that never collide or cannot be recovered.
        if (!level().isClientSide && !isRemoved() && tickCount > MAX_FLIGHT_TICKS) startReturning();
    }

    @Override
    protected void onHitEntity(EntityHitResult hit)
    {
        super.onHitEntity(hit);
        if (level().isClientSide) return;

        Entity owner = getOwner();
        Entity target = hit.getEntity();
        if (owner == null || target == owner) return;
        double impactSpeed = getDeltaMovement().length();
        if (target != owner && target instanceof LivingEntity living && owner instanceof LivingEntity attacker)
        {
            var damageType = level().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                    .getHolderOrThrow(SHIELD_DAMAGE);
            float baseDamage = com.fiskmods.heroes.common.hero.ability.MeleeCombat.thrownShieldDamage(attacker, living, getItem());
            float damage = (float) (baseDamage * net.minecraft.util.Mth.clamp(getDeltaMovement().length(), 0.0D, 1.0D));
            boolean damaged = living.hurt(new net.minecraft.world.damagesource.DamageSource(damageType, this, attacker), damage);
            if (damaged)
            {
                Vec3 knockback = getDeltaMovement().normalize().scale(0.6D);
                living.knockback(0.6F, -knockback.x, -knockback.z);
                if (attacker instanceof ServerPlayer serverPlayer) living.setLastHurtByPlayer(serverPlayer);
                ItemStack stack = getItem().copy();
                int fireAspect = net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(
                        net.minecraft.world.item.enchantment.Enchantments.FIRE_ASPECT, stack);
                if (fireAspect > 0) living.setSecondsOnFire(fireAspect * 2);
                stack.hurtAndBreak(1, attacker, ignored -> {});
                setItem(stack);
                if (stack.isEmpty())
                {
                    discard();
                    return;
                }
            }
        }
        Vec3 towardOwner = new Vec3(owner.getX(), owner.getY() + owner.getBbHeight() * 0.5D, owner.getZ())
                .subtract(getX(), getY() + getBbHeight() * 0.5D, getZ()).scale(impactSpeed * 0.075D);
        setDeltaMovement(towardOwner.add(0.0D, 0.175D, 0.0D));
        hasImpulse = true;
        impactSound();
        startReturning();
    }

    @Override
    protected void onHitBlock(BlockHitResult hit)
    {
        if (entityData.get(MAGNETIC_ACTIVE) || (!level().isClientSide && isMagneticallyAttracted())) return;
        super.onHitBlock(hit);
        if (level().isClientSide || getOwner() == null) return;
        breakGlassAtImpact(hit);
        boolean magnetic = com.fiskmods.heroes.common.item.ItemCaptainAmericaShield.isElectromagnetic(getItem());
        double speed = getDeltaMovement().length();
        if (magnetic && speed < 0.75D)
        {
            accelerateMagneticReturn();
        }
        else if (speed > 0.5D)
        {
            var face = hit.getDirection();
            if (!level().getBlockState(hit.getBlockPos()).getCollisionShape(level(), hit.getBlockPos()).isEmpty()
                    && level().isEmptyBlock(hit.getBlockPos().relative(face)))
            {
                Vec3 motion = getDeltaMovement();
                setDeltaMovement(motion.x * (1 - 2 * Math.abs(face.getStepX())) * 0.75D,
                        motion.y * (1 - 2 * Math.abs(face.getStepY())) * 0.75D,
                        motion.z * (1 - 2 * Math.abs(face.getStepZ())) * 0.75D);
                hasImpulse = true;
            }
        }
        else if (!magnetic)
        {
            if (getOwner() instanceof Player && distanceTo(getOwner()) <= 5.0F) returnShield(getOwner());
            else dropShield();
            return;
        }
        impactSound();
        startReturning();
    }

    /** The original breaks a 3x3 plane of glass around the impact face. */
    private void breakGlassAtImpact(BlockHitResult hit)
    {
        if (!griefingAllowed() || !(getOwner() instanceof ServerPlayer player)) return;
        var center = hit.getBlockPos();
        var state = level().getBlockState(center);
        if (!isGlass(state) || state.getDestroySpeed(level(), center) < 0.0F) return;
        var direction = hit.getDirection();
        for (int a = -1; a <= 1; ++a)
        {
            for (int b = -1; b <= 1; ++b)
            {
                var pos = switch (direction.getAxis())
                {
                    case X -> center.offset(0, a, b);
                    case Y -> center.offset(a, 0, b);
                    case Z -> center.offset(a, b, 0);
                };
                if (!mayEdit(player, pos, direction)) continue;
                var candidate = level().getBlockState(pos);
                if (!isGlass(candidate) || candidate.getDestroySpeed(level(), pos) < 0.0F) continue;
                if (com.fiskmods.heroes.common.util.ProtectedBlockChanges.canBreak(player, pos, candidate))
                    level().destroyBlock(pos, true, player);
            }
        }
    }

    private boolean griefingAllowed()
    {
        return canDoGriefing && com.fiskmods.heroes.common.config.SHConfig.ALLOW_GRIEFING.get()
                && com.fiskmods.heroes.common.config.SHConfig.ALLOW_THROWN_SHIELD_GRIEFING.get();
    }

    private boolean mayEdit(ServerPlayer player, net.minecraft.core.BlockPos pos, net.minecraft.core.Direction face)
    {
        return level().hasChunkAt(pos) && level().getWorldBorder().isWithinBounds(pos)
                && level().mayInteract(player, pos) && player.mayUseItemAt(pos, face, getItem());
    }

    /** 1.7 tallgrass corresponds to 1.20 grass and ferns; double plants need separate handling. */
    private void breakShortGrassAlongFlight()
    {
        if (!griefingAllowed() || !(getOwner() instanceof ServerPlayer player)
                || getDeltaMovement().lengthSqr() < 1.0E-7D) return;
        Vec3 start = position();
        var obstacle = net.minecraft.world.entity.projectile.ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        Vec3 end = obstacle.getType() == net.minecraft.world.phys.HitResult.Type.MISS
                ? start.add(getDeltaMovement()) : obstacle.getLocation();
        var hit = net.minecraft.world.level.BlockGetter.traverseBlocks(start, end, this, (projectile, cursor) -> {
            var pos = cursor.immutable();
            if (!level().hasChunkAt(pos)) return null;
            var state = level().getBlockState(pos);
            if (!state.is(net.minecraft.world.level.block.Blocks.GRASS) && !state.is(net.minecraft.world.level.block.Blocks.FERN)) return null;
            return state.getShape(level(), pos).clip(start, end, pos);
        }, projectile -> null);
        if (hit == null || net.minecraftforge.event.ForgeEventFactory.onProjectileImpact(this, hit)) return;
        var pos = hit.getBlockPos();
        if (!mayEdit(player, pos, hit.getDirection())) return;
        var state = level().getBlockState(pos);
        if ((state.is(net.minecraft.world.level.block.Blocks.GRASS) || state.is(net.minecraft.world.level.block.Blocks.FERN))
                && state.getDestroySpeed(level(), pos) >= 0.0F
                && com.fiskmods.heroes.common.util.ProtectedBlockChanges.canBreak(player, pos, state))
            level().destroyBlock(pos, true, player);
    }

    private static boolean isGlass(net.minecraft.world.level.block.state.BlockState state)
    {
        return state.is(net.minecraftforge.common.Tags.Blocks.GLASS) || state.is(net.minecraftforge.common.Tags.Blocks.GLASS_PANES);
    }

    private void impactSound()
    {
        com.fiskmods.heroes.common.sound.SHSounds.play(this, com.fiskmods.heroes.FiskHeroes.id("item.shield.hit"),
                net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F + (random.nextFloat() - 0.5F) * 0.2F);
    }

    private void returnShield(Entity owner)
    {
        if (owner instanceof Player player)
        {
            if (Boolean.getBoolean("fiskheroes.ciShieldDiagnostics"))
                com.fiskmods.heroes.FiskHeroes.LOGGER.info("Shield recovered: entity={}, owner={}, magnetic={}", getId(),
                        player.getName().getString(), com.fiskmods.heroes.common.item.ItemCaptainAmericaShield.isElectromagnetic(getItem()));
            ItemStack stack = getItem().copy();
            if (!player.getInventory().add(stack)) player.drop(stack, false);
        }
        else
        {
            // An owner restored or assigned by another mod may not be a player.
            // Preserve the item rather than silently deleting it.
            dropShield();
            return;
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
        tag.putBoolean("CanDoGriefing", canDoGriefing);
        tag.putInt("ReturnTicks", returnTicks);
        tag.putInt("FlightAge", flightAge());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Shield", CompoundTag.TAG_COMPOUND)) setItem(ItemStack.of(tag.getCompound("Shield")));
        if (tag.contains("Returning")) entityData.set(RETURNING, tag.getBoolean("Returning"));
        canDoGriefing = tag.getBoolean("CanDoGriefing");
        returnTicks = Math.min(MAX_RETURN_TICKS + 1, Math.max(0, tag.getInt("ReturnTicks")));
        if (tag.contains("FlightAge")) entityData.set(AGE_OFFSET, Math.max(0, tag.getInt("FlightAge") - tickCount));
    }
}
