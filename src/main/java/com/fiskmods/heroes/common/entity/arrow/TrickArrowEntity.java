package com.fiskmods.heroes.common.entity.arrow;

import com.fiskmods.heroes.common.item.ItemTrickArrow;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

/** Server-authoritative special arrow projectile. */
public class TrickArrowEntity extends Arrow
{
    private static final float DEFAULT_EXPLOSION_RADIUS = 2.0F;
    private static final EntityDataAccessor<String> ARROW_TYPE = SynchedEntityData.defineId(TrickArrowEntity.class, EntityDataSerializers.STRING);
    private float explosionRadius = DEFAULT_EXPLOSION_RADIUS;
    private boolean detonated;
    /** Remaining fuse after an explosive pufferfish arrow hits a block or entity. */
    private int pufferfishFuseTicks = -1;
    private ItemStack vialPotion = ItemStack.EMPTY;

    public TrickArrowEntity(EntityType<? extends TrickArrowEntity> type, Level level)
    {
        super(type, level);
    }

    public TrickArrowEntity(EntityType<? extends TrickArrowEntity> type, Level level, LivingEntity shooter, String arrowType)
    {
        this(type, level, shooter, arrowType, DEFAULT_EXPLOSION_RADIUS);
    }

    public TrickArrowEntity(EntityType<? extends TrickArrowEntity> type, Level level, LivingEntity shooter, String arrowType, float explosionRadius)
    {
        this(type, level);
        setOwner(shooter);
        setPos(shooter.getX(), shooter.getEyeY() - 0.1D, shooter.getZ());
        setArrowType(arrowType);
        setExplosionRadius(explosionRadius);
    }

    @Override
    protected void defineSynchedData()
    {
        super.defineSynchedData();
        entityData.define(ARROW_TYPE, ItemTrickArrow.NORMAL);
    }

    public String getArrowType()
    {
        return entityData.get(ARROW_TYPE);
    }

    public void setArrowType(String type)
    {
        entityData.set(ARROW_TYPE, ItemTrickArrow.normalizeType(type));
    }

    public void setVialPotion(ItemStack potion)
    {
        vialPotion = potion != null ? potion.copy() : ItemStack.EMPTY;
    }

    private void setExplosionRadius(float radius)
    {
        explosionRadius = Float.isFinite(radius) ? Math.max(0.0F, radius) : DEFAULT_EXPLOSION_RADIUS;
    }

    @Override
    public void tick()
    {
        super.tick();
        if ("detonator".equals(getArrowType()) && inGround && level().hasNeighborSignal(blockPosition()))
        {
            detonate(4.0F, true, Level.ExplosionInteraction.BLOCK);
        }
        else if ("firework".equals(getArrowType()) && tickCount >= 40)
        {
            detonate(1.5F, false);
        }
        else if ("explosive_pufferfish".equals(getArrowType()) && pufferfishFuseTicks >= 0
                && --pufferfishFuseTicks <= 0)
        {
            detonate(1.5F, false);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result)
    {
        // Detonator arrows are remote mines: the reference arrow passes through entities without
        // dealing damage and only explodes after sticking to a powered block.
        if ("detonator".equals(getArrowType())) return;

        if ("explosive_pufferfish".equals(getArrowType()))
        {
            startPufferfishFuse();
        }

        super.onHitEntity(result);
        if (level().isClientSide || !(result.getEntity() instanceof LivingEntity target)) return;

        switch (getArrowType())
        {
            case "ender_pearl" -> teleportShooter();
            case "carrot" -> target.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1200));
            case "fire_charge", "fireball", "blaze" -> target.setSecondsOnFire(5);
            case "boxing_glove" ->
            {
                Vec3 velocity = getDeltaMovement();
                target.knockback(1.2F, -velocity.x, -velocity.z);
            }
            case "pufferfish" ->
            {
                if (random.nextInt(3) == 0) target.addEffect(new MobEffectInstance(MobEffects.POISON, 80));
            }
            case "slime" ->
            {
                if (random.nextInt(4) != 0) target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60));
            }
            case "phantom" -> target.addEffect(new MobEffectInstance(
                    com.fiskmods.heroes.common.hero.modifier.ModEffects.PHASE_SUPPRESSANT.get(), 100, 0, false, false, true));
            case "tutridium" -> target.addEffect(new MobEffectInstance(
                    com.fiskmods.heroes.common.hero.modifier.ModEffects.TUTRIDIUM.get(), 200, 0, false, false, true));
            case "gross" -> target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 160));
            case "vial" -> applyVial(target);
            default -> { }
        }
        detonateIfExplosive();
        if ("firework".equals(getArrowType()) || "fireball".equals(getArrowType()))
        {
            detonate(1.5F, false);
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result)
    {
        // The original glitch arrow is non-solid: block impacts do not stop or embed it.
        if ("glitch".equals(getArrowType()))
        {
            return;
        }

        if ("explosive_pufferfish".equals(getArrowType()))
        {
            startPufferfishFuse();
        }

        super.onHitBlock(result);
        String type = getArrowType();
        if (level().isClientSide) return;

        if ("vial".equals(type))
        {
            applyVial(null);
        }

        if ("detonator".equals(type)) return;

        if ("ender_pearl".equals(type))
        {
            teleportShooter();
            setArrowType(ItemTrickArrow.NORMAL);
        }
        else if ("fire_charge".equals(type))
        {
            placeBlock(result, Blocks.FIRE.defaultBlockState());
            setArrowType(ItemTrickArrow.NORMAL);
        }
        else if ("torch".equals(type))
        {
            placeBlock(result, Blocks.TORCH.defaultBlockState());
            setArrowType(ItemTrickArrow.NORMAL);
        }
        else if ("grappling_hook".equals(type) || "vine".equals(type))
        {
            // Keep the arrow anchored; the shooter is reeled to its hit point while this key is held.
            LivingEntity owner = getOwner() instanceof LivingEntity living ? living : null;
            if (owner != null)
            {
                Vec3 pull = position().subtract(owner.position()).normalize().scale(1.2D);
                owner.setDeltaMovement(owner.getDeltaMovement().add(pull));
                owner.hurtMarked = true;
            }
        }
        else if ("smoke_bomb".equals(type))
        {
            spawnSmokeCloud();
            discard();
        }
        else if ("firework".equals(type) || "fireball".equals(type))
        {
            detonate(1.5F, false);
        }
        else if ("cactus".equals(type))
        {
            spawnCactusSpikes();
            detonate(2.0F, false);
        }
        else if ("pufferfish".equals(type) || "slime".equals(type))
        {
            bounceAtBlock(result, "pufferfish".equals(type) ? 0.5D : 0.6D);
        }
        else if ("pulse".equals(type))
        {
            var pos = result.getBlockPos();
            var state = level().getBlockState(pos);
            level().updateNeighborsAt(pos, state.getBlock());
            level().updateNeighborsAt(pos.relative(result.getDirection()), state.getBlock());
        }
        detonateIfExplosive();
    }

    @Override
    public void playerTouch(Player player)
    {
        if ("detonator".equals(getArrowType()) && player != getOwner()) return;
        if (ItemTrickArrow.EXPLOSIVE.equals(getArrowType()) && inGround)
        {
            detonateIfExplosive();
            return;
        }
        super.playerTouch(player);
    }

    /** Handles the original arrow-catching modifier's special interaction with trick arrows. */
    public boolean onCaught(LivingEntity catcher)
    {
        String type = getArrowType();
        if ("excessive".equals(type) || !catcher.getMainHandItem().isEmpty()) return false;

        if (ItemTrickArrow.EXPLOSIVE.equals(type) || "triple_explosive".equals(type))
        {
            detonate(explosionRadius, false);
            return true;
        }
        if ("firework".equals(type) || "fireball".equals(type))
        {
            detonate(1.5F, false);
            return true;
        }
        if ("smoke_bomb".equals(type))
        {
            spawnSmokeCloud();
            discard();
            return true;
        }

        catcher.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, getPickupItem().copy());
        discard();
        return true;
    }

    private void detonateIfExplosive()
    {
        String type = getArrowType();
        if (ItemTrickArrow.EXPLOSIVE.equals(type) || "triple_explosive".equals(type))
        {
            detonate("explosive_pufferfish".equals(type) ? 1.5F : explosionRadius, false);
        }
    }

    private void startPufferfishFuse()
    {
        if (pufferfishFuseTicks < 0)
        {
            pufferfishFuseTicks = 30;
        }
    }

    private void detonate(float radius, boolean causesFire)
    {
        detonate(radius, causesFire, Level.ExplosionInteraction.NONE);
    }

    private void detonate(float radius, boolean causesFire, Level.ExplosionInteraction interaction)
    {
        if (level().isClientSide || detonated) return;
        detonated = true;
        level().explode(getOwner(), getX(), getY(), getZ(), radius, causesFire, interaction);
        discard();
    }

    private void teleportShooter()
    {
        if (level().isClientSide || !(getOwner() instanceof Player player)) return;
        double x = getX();
        double y = getY();
        double z = getZ();
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)
        {
            serverPlayer.teleportTo(x, y, z);
        }
        else
        {
            player.teleportTo(x, y, z);
        }
        player.fallDistance = 0.0F;
        player.hurt(level().damageSources().fall(), 5.0F);
    }

    private void placeBlock(BlockHitResult hit, net.minecraft.world.level.block.state.BlockState state)
    {
        if (!level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) return;
        var pos = hit.getBlockPos().relative(hit.getDirection());
        if (level().isEmptyBlock(pos)) level().setBlockAndUpdate(pos, state);
    }

    private void bounceAtBlock(BlockHitResult hit, double factor)
    {
        Vec3 motion = getDeltaMovement();
        if (motion.length() <= 0.25D || !level().getBlockState(hit.getBlockPos()).blocksMotion()) return;
        var adjacent = hit.getBlockPos().relative(hit.getDirection());
        if (!level().isEmptyBlock(adjacent)) return;

        switch (hit.getDirection().getAxis())
        {
            case X -> motion = new Vec3(-motion.x * factor, motion.y * factor, motion.z * factor);
            case Y -> motion = new Vec3(motion.x * factor, -motion.y * factor, motion.z * factor);
            case Z -> motion = new Vec3(motion.x * factor, motion.y * factor, -motion.z * factor);
        }
        setDeltaMovement(motion);
        setPos(getX() + hit.getDirection().getStepX() * 0.05D,
                getY() + hit.getDirection().getStepY() * 0.05D,
                getZ() + hit.getDirection().getStepZ() * 0.05D);
        inGround = false;
        shakeTime = 0;
    }

    private void applyVial(LivingEntity directHit)
    {
        java.util.List<MobEffectInstance> effects = net.minecraft.world.item.alchemy.PotionUtils.getMobEffects(vialPotion);
        if (effects.isEmpty()) return;

        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(4.0D, 2.0D, 4.0D)))
        {
            if (target == getOwner() || target.distanceToSqr(this) > 16.0D) continue;
            double intensity = target == directHit ? 1.0D : 1.0D - Math.sqrt(target.distanceToSqr(this)) / 4.0D;
            for (MobEffectInstance effect : effects)
            {
                var mobEffect = effect.getEffect();
                if (mobEffect.isInstantenous())
                {
                    mobEffect.applyInstantenousEffect(this, getOwner(), target, effect.getAmplifier(), intensity);
                }
                else
                {
                    int duration = (int) (effect.getDuration() * intensity + 0.5D);
                    if (duration > 20) target.addEffect(new MobEffectInstance(mobEffect, duration, effect.getAmplifier()));
                }
            }
        }
    }

    private void spawnSmokeCloud()
    {
        if (!(level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) return;
        var cloud = new net.minecraft.world.entity.AreaEffectCloud(level(), getX(), getY(), getZ());
        cloud.setOwner(getOwner() instanceof LivingEntity owner ? owner : null);
        cloud.setRadius(2.5F);
        cloud.setDuration(100);
        cloud.setRadiusPerTick(-0.025F);
        cloud.setParticle(net.minecraft.core.particles.ParticleTypes.CAMPFIRE_COSY_SMOKE);
        cloud.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80));
        serverLevel.addFreshEntity(cloud);
    }

    private void spawnCactusSpikes()
    {
        if (!(level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) return;
        // The reference arrow bursts into twenty or twenty-one cactus spikes.
        int count = 20 + random.nextInt(2);
        for (int i = 0; i < count; ++i)
        {
            Arrow spike = new Arrow(level(), getOwner() instanceof LivingEntity owner ? owner : null);
            spike.setPos(getX(), getY(), getZ());
            spike.shoot(random.nextDouble() * 2.0D - 1.0D, random.nextDouble() * 2.0D - 0.25D,
                    random.nextDouble() * 2.0D - 1.0D, 1.2F, 12.0F);
            serverLevel.addFreshEntity(spike);
        }
    }

    @Override
    protected ItemStack getPickupItem()
    {
        return switch (getArrowType())
        {
            case "boxing_glove", "carrot", "fire_charge", "torch", "ender_pearl", "vial" -> net.minecraft.world.item.Items.ARROW.getDefaultInstance();
            default -> ItemTrickArrow.createStack(getArrowType());
        };
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag)
    {
        super.addAdditionalSaveData(tag);
        tag.putString("ArrowType", getArrowType());
        tag.putFloat("ExplosionRadius", explosionRadius);
        tag.putBoolean("Detonated", detonated);
        tag.putInt("PufferfishFuseTicks", pufferfishFuseTicks);
        if (!vialPotion.isEmpty()) tag.put("VialPotion", vialPotion.save(new CompoundTag()));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);
        setArrowType(tag.getString("ArrowType"));
        setExplosionRadius(tag.contains("ExplosionRadius") ? tag.getFloat("ExplosionRadius") : DEFAULT_EXPLOSION_RADIUS);
        detonated = tag.getBoolean("Detonated");
        pufferfishFuseTicks = tag.contains("PufferfishFuseTicks") ? tag.getInt("PufferfishFuseTicks") : -1;
        vialPotion = tag.contains("VialPotion", CompoundTag.TAG_COMPOUND)
                ? ItemStack.of(tag.getCompound("VialPotion")) : ItemStack.EMPTY;
    }
}
