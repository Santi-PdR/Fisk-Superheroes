package com.fiskmods.heroes.common.entity;

import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsTargetGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/** Faithful cactus minion behavior from the original mod: autonomous combat, water healing and
 * returning to a cactus after 600 idle ticks. */
public class CactusMinionEntity extends AbstractGolem
{
    private static final EntityDataAccessor<Integer> SIZE = SynchedEntityData.defineId(CactusMinionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DONATOR_SUMMONED = SynchedEntityData.defineId(CactusMinionEntity.class, EntityDataSerializers.BOOLEAN);
    private int idleTicks;

    public CactusMinionEntity(EntityType<? extends CactusMinionEntity> type, Level level)
    {
        super(type, level);
        getNavigation().setCanFloat(true);
        setCactusSize(1 + random.nextInt(3));
    }

    public static AttributeSupplier.Builder createAttributes()
    {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 5.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.35D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void registerGoals()
    {
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, true));
        goalSelector.addGoal(2, new MoveTowardsTargetGoal(this, 0.9D, 32.0F));
        goalSelector.addGoal(6, new RandomStrollGoal(this, 0.6D));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(2, new HurtByTargetGoal(this));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 0, true, false, this::canAttackTarget));
    }

    @Override
    protected void defineSynchedData()
    {
        super.defineSynchedData();
        entityData.define(SIZE, 1);
        entityData.define(DONATOR_SUMMONED, false);
    }

    private boolean canAttackTarget(LivingEntity target)
    {
        if (target == this || target instanceof CactusMinionEntity || target.isAlliedTo(this) || target.isInvulnerable()) return false;
        if (target instanceof Player player && player.getAbilities().invulnerable) return false;
        if (target instanceof Animal || target instanceof AbstractGolem) return false;
        if (target instanceof Player player)
        {
            var hero = com.fiskmods.heroes.common.hero.HeroTracker.getHero(player);
            var data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(player);
            if (hero != null && data != null && hero.getHero().getPowerContainer().getEntries().stream()
                    .anyMatch(entry -> entry.getModifier().getId().getPath().equals("cactus_recruitment")
                            && entry.isEnabled() && entry.isModifierEnabled(player, data)))
            {
                return false;
            }
        }
        return target instanceof Enemy || !(target instanceof Animal || target instanceof AbstractGolem);
    }

    @Override
    public MobType getMobType()
    {
        return MobType.UNDEFINED;
    }

    @Override
    public boolean canBreatheUnderwater()
    {
        return true;
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target)
    {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living && canAttackTarget(living) && random.nextInt(20) == 0)
        {
            setTarget(living);
        }
        return hit;
    }

    @Override
    public void tick()
    {
        super.tick();
        refreshDimensions();

        if (level() instanceof ServerLevel serverLevel)
        {
            // The original minion damages every valid living entity touching its cactus body.
            List<LivingEntity> touching = serverLevel.getEntitiesOfClass(LivingEntity.class,
                    getBoundingBox().inflate(0.3D), this::canAttackTarget);
            float thornDamage = Math.min(getCactusSize(), 3) * 2.0F;
            for (LivingEntity living : touching)
            {
                living.hurt(serverLevel.damageSources().cactus(), thornDamage);
            }

            int healRate = com.fiskmods.heroes.common.config.SHConfig.CACTUS_HEAL_RATE.get();
            if ((isInWater() || level().isRainingAt(blockPosition().above())) && getHealth() < getMaxHealth() && tickCount % healRate == 0)
            {
                heal(1.0F);
            }

            if (getTarget() == null)
            {
                if (++idleTicks > com.fiskmods.heroes.common.config.SHConfig.CACTUS_LIFESPAN.get()) returnToCactus(serverLevel);
            }
            else
            {
                idleTicks = 0;
            }
        }
    }

    private void returnToCactus(ServerLevel level)
    {
        net.minecraft.core.BlockPos base = blockPosition();
        boolean canGrow = Blocks.CACTUS.defaultBlockState().canSurvive(level, base);
        for (int y = 0; y < getCactusSize() && canGrow; y++)
        {
            canGrow = level.getBlockState(base.above(y)).isAir();
        }

        if (canGrow)
        {
            for (int y = 0; y < getCactusSize(); y++) level.setBlockAndUpdate(base.above(y), Blocks.CACTUS.defaultBlockState());
            discard();
        }
        else
        {
            hurt(level.damageSources().generic(), getHealth());
        }
    }

    @Override
    public void die(DamageSource source)
    {
        if (!level().isClientSide && getCactusSize() > 1)
        {
            int count = Math.min(getCactusSize(), 3);
            for (int i = 0; i < count; i++)
            {
                CactusMinionEntity child = ModEntities.CACTUS_MINION.get().create(level());
                if (child == null) continue;
                child.setCactusSize(1);
                child.setDonatorSummoned(isDonatorSummoned());
                child.moveTo(getX() + (i % 2 - 0.5D), getY() + 0.5D, getZ() + (i / 2 - 0.5D), random.nextFloat() * 360.0F, 0.0F);
                level().addFreshEntity(child);
            }
        }
        super.die(source);
    }

    @Override
    public net.minecraft.world.entity.EntityDimensions getDimensions(net.minecraft.world.entity.Pose pose)
    {
        return net.minecraft.world.entity.EntityDimensions.scalable(1.0F, getCactusSize());
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit)
    {
        int size = getCactusSize();
        int drops = size == 1 ? 1 : Math.max(0, size - 3);
        if (drops > 0) spawnAtLocation(new net.minecraft.world.item.ItemStack(Blocks.CACTUS, drops));
    }

    public int getCactusSize()
    {
        return entityData.get(SIZE);
    }

    public void setCactusSize(int size)
    {
        int validSize = Math.max(1, size);
        entityData.set(SIZE, validSize);
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(Math.min(validSize, 3) * 5.0D);
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(Math.min(validSize, 3) * 2.0D);
        setHealth(getMaxHealth());
        refreshDimensions();
    }

    public boolean isDonatorSummoned()
    {
        return entityData.get(DONATOR_SUMMONED);
    }

    public void setDonatorSummoned(boolean value)
    {
        entityData.set(DONATOR_SUMMONED, value);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag)
    {
        super.addAdditionalSaveData(tag);
        tag.putInt("CactusSize", getCactusSize());
        tag.putBoolean("DonatorSummoned", isDonatorSummoned());
        tag.putInt("IdleTicks", idleTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);
        setCactusSize(tag.getInt("CactusSize"));
        setDonatorSummoned(tag.getBoolean("DonatorSummoned"));
        idleTicks = tag.getInt("IdleTicks");
    }
}
