package com.fiskmods.heroes.common.entity.arrow;

import com.fiskmods.heroes.common.item.ItemTrickArrow;
import com.fiskmods.heroes.common.item.ModItems;
import com.google.gson.JsonObject;
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
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.ResourceLocation;

/** Server-authoritative special arrow projectile. */
public class TrickArrowEntity extends Arrow
{
    /** Per-type projectile tuning copied from ArrowTypeManager in the 1.7.10 reference. */
    private static final java.util.Map<String, ArrowPhysics> ARROW_PHYSICS = java.util.Map.ofEntries(
            java.util.Map.entry("explosive", new ArrowPhysics(1.0F, 0.05F, 1.0F)),
            java.util.Map.entry("carrot", new ArrowPhysics(1.5F, 0.05F, 0.2F)),
            java.util.Map.entry("fire_charge", new ArrowPhysics(1.5F, 0.05F, 0.75F)),
            java.util.Map.entry("cactus", new ArrowPhysics(0.75F, 0.05F, 1.0F)),
            java.util.Map.entry("boxing_glove", new ArrowPhysics(1.0F, 0.05F, 0.5F)),
            java.util.Map.entry("vial", new ArrowPhysics(1.0F, 0.075F, 1.0F)),
            java.util.Map.entry("vibranium", new ArrowPhysics(1.5F, 0.075F, 1.0F)),
            java.util.Map.entry("phantom", new ArrowPhysics(1.0F, 0.05F, 0.5F)),
            java.util.Map.entry("pufferfish", new ArrowPhysics(0.75F, 0.05F, 0.6F)),
            java.util.Map.entry("explosive_pufferfish", new ArrowPhysics(0.75F, 0.05F, 0.6F)),
            java.util.Map.entry("smoke_bomb", new ArrowPhysics(1.0F, 0.05F, 1.0F)),
            java.util.Map.entry("slime", new ArrowPhysics(0.75F, 0.05F, 0.3F)),
            java.util.Map.entry("ender_pearl", new ArrowPhysics(0.25F, 0.02F, 0.5F)),
            java.util.Map.entry("tutridium", new ArrowPhysics(1.5F, 0.075F, 0.8F)),
            java.util.Map.entry("excessive", new ArrowPhysics(0.4F, 0.05F, 1.2F)),
            java.util.Map.entry("gross", new ArrowPhysics(1.5F, 0.05F, 0.75F)),
            java.util.Map.entry("detonator", new ArrowPhysics(1.0F, 0.05F, 1.0F)),
            java.util.Map.entry("fireball", new ArrowPhysics(1.0F, 0.05F, 1.0F)));
    private static final float DEFAULT_EXPLOSION_RADIUS = 2.0F;
    private static final float FIREBALL_ARROW_RADIUS = 2.5F;
    private static final float FIREBALL_ARROW_DAMAGE = 6.0F;
    private static final EntityDataAccessor<String> ARROW_TYPE = SynchedEntityData.defineId(TrickArrowEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<ItemStack> VIAL_POTION = SynchedEntityData.defineId(TrickArrowEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> FIREWORK_STACK = SynchedEntityData.defineId(TrickArrowEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Boolean> PUFFERFISH_FUSING = SynchedEntityData.defineId(TrickArrowEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> GRAPPLE_SNAPPED = SynchedEntityData.defineId(TrickArrowEntity.class, EntityDataSerializers.BOOLEAN);
    private float explosionRadius = DEFAULT_EXPLOSION_RADIUS;
    private boolean detonated;
    private boolean grappleSnapped;
    private int vineUseTicks;
    private float grappleFireSpread;
    private static final ResourceLocation GRAPPLE_DISCONNECT_SOUND = new ResourceLocation(
            "fiskheroes", "entity.arrow.grapple.disconnect");
    private static final ResourceLocation VINE_SNAP_SOUND = new ResourceLocation(
            "fiskheroes", "entity.arrow.vine.snap");
    private static final ResourceLocation PUFFERFISH_FLOP_SOUND = new ResourceLocation(
            "fiskheroes", "entity.arrow.pufferfish.flop");
    private static final ResourceLocation PUFFERFISH_PRIMED_SOUND = new ResourceLocation(
            "fiskheroes", "entity.arrow.pufferfish.primed");
    /** Remaining fuse after an explosive pufferfish arrow hits a block or entity. */
    private int pufferfishFuseTicks = -1;
    private ItemStack vialPotion = ItemStack.EMPTY;
    private ItemStack fireworkStack = ItemStack.EMPTY;
    private int fireworkAge;
    private int fireworkLifetime = -1;
    private int fireworkStrength = 120;
    private float fireworkRadius;

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
        entityData.define(VIAL_POTION, ItemStack.EMPTY);
        entityData.define(FIREWORK_STACK, ItemStack.EMPTY);
        entityData.define(PUFFERFISH_FUSING, false);
        entityData.define(GRAPPLE_SNAPPED, false);
    }

    public String getArrowType()
    {
        return entityData.get(ARROW_TYPE);
    }

    private ArrowPhysics getPhysics()
    {
        return ARROW_PHYSICS.getOrDefault(getArrowType(), ArrowPhysics.DEFAULT);
    }

    public float getVelocityFactor()
    {
        return getPhysics().velocity();
    }

    /** Apply the arrow type's launch speed even when it is fired with a vanilla bow. */
    @Override
    public void shoot(double x, double y, double z, float velocity, float inaccuracy)
    {
        super.shoot(x, y, z, velocity * getVelocityFactor(), inaccuracy);
    }

    public float getDamageMultiplier()
    {
        return getPhysics().damage();
    }

    /** Keep type damage modifiers active regardless of which bow launched the arrow. */
    @Override
    public double getBaseDamage()
    {
        return super.getBaseDamage() * getDamageMultiplier();
    }

    @Override
    public void setBaseDamage(double damage)
    {
        super.setBaseDamage(damage / getDamageMultiplier());
    }

    private record ArrowPhysics(float velocity, float gravity, float damage)
    {
        private static final ArrowPhysics DEFAULT = new ArrowPhysics(1.5F, 0.05F, 1.0F);
    }

    public void setArrowType(String type)
    {
        entityData.set(ARROW_TYPE, ItemTrickArrow.normalizeType(type));
    }

    public boolean isGrappleSnapped()
    {
        return entityData.get(GRAPPLE_SNAPPED);
    }

    public boolean isAnchored()
    {
        return inGround;
    }

    /** Whether this arrow bypasses projectile durability defenses for the specified target. */
    public boolean canPierceDurability(LivingEntity target)
    {
        // Both material-tip arrows bypass armor durability in the original mod. Tutridium's
        // poison effect is only useful if the projectile can get through the suit defense too.
        if ("vibranium".equals(getArrowType()) || "tutridium".equals(getArrowType())) return true;
        if (!"blaze".equals(getArrowType())) return false;

        com.fiskmods.heroes.common.data.SHPlayerData targetData =
                com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(target);
        return targetData != null && targetData.getData().get(
                com.fiskmods.heroes.common.data.var.Vars.METAL_SKIN);
    }

    public void setVialPotion(ItemStack potion)
    {
        vialPotion = potion != null ? potion.copy() : ItemStack.EMPTY;
        entityData.set(VIAL_POTION, vialPotion.copy());
    }

    public ItemStack getVialPotion()
    {
        return entityData.get(VIAL_POTION).copy();
    }

    public ItemStack getFireworkStack()
    {
        return entityData.get(FIREWORK_STACK).copy();
    }

    /** Stores the original firework payload and derives the arrow's fuse and flash radius from it. */
    public void setFireworkStack(ItemStack stack)
    {
        fireworkStack = stack != null ? stack.copy() : ItemStack.EMPTY;
        entityData.set(FIREWORK_STACK, fireworkStack.copy());
        fireworkAge = 0;
        fireworkStrength = 120;
        fireworkRadius = 0.0F;

        CompoundTag fireworks = fireworkStack.hasTag() ? fireworkStack.getTag().getCompound("Fireworks") : new CompoundTag();
        int flight = 1 + Byte.toUnsignedInt(fireworks.getByte("Flight"));
        fireworkLifetime = (10 * flight + random.nextInt(6) + random.nextInt(7)) / 2;
        net.minecraft.nbt.ListTag explosions = fireworks.getList("Explosions", CompoundTag.TAG_COMPOUND);
        for (int i = 0; i < explosions.size(); ++i)
        {
            CompoundTag explosion = explosions.getCompound(i);
            if (explosion.getBoolean("Flicker")) fireworkStrength += 20;
            if (explosion.getBoolean("Trail")) fireworkStrength += 30;
            switch (Byte.toUnsignedInt(explosion.getByte("Type")))
            {
                case 1 -> fireworkRadius = Math.max(fireworkRadius, 8.0F);
                case 2 -> fireworkRadius = Math.max(fireworkRadius, 5.0F);
                case 4 ->
                {
                    fireworkStrength += 20;
                    fireworkRadius = Math.max(fireworkRadius, 3.0F);
                }
                default -> fireworkRadius = Math.max(fireworkRadius, 4.0F);
            }
        }
    }

    private void setExplosionRadius(float radius)
    {
        explosionRadius = Float.isFinite(radius) ? Math.max(0.0F, radius) : DEFAULT_EXPLOSION_RADIUS;
    }

    @Override
    public void tick()
    {
        // Glitch arrows ignore block impacts in onHitBlock, but must keep projectile collision
        // enabled so they can still strike entities. Entity.noPhysics disables both kinds of hit
        // detection in 1.20.1, unlike the original arrow's block-only no-clip behavior.
        noPhysics = "explosive_pufferfish".equals(getArrowType()) && entityData.get(PUFFERFISH_FUSING);
        // AbstractArrow hard-codes vanilla gravity (0.05) in this Minecraft version. Compensate
        // immediately before its tick so each variant receives the original ArrowType gravity.
        if (!isNoGravity() && !inGround)
        {
            setDeltaMovement(getDeltaMovement().add(0.0D, 0.05D - getPhysics().gravity(), 0.0D));
        }
        super.tick();
        spawnTypeParticles();
        tickGrapple();
        if ("detonator".equals(getArrowType()) && inGround && level().hasNeighborSignal(blockPosition()))
        {
            detonate(4.0F, true, Level.ExplosionInteraction.BLOCK);
        }
        else if ("firework".equals(getArrowType()) && !level().isClientSide
                && ++fireworkAge > fireworkLifetime)
        {
            // Firework arrows are flashbangs in the original mod; they must never fall back
            // to the explosive-arrow blast just because they travelled without hitting anything.
            spawnFlashbang();
            discard();
        }
        else if ("explosive_pufferfish".equals(getArrowType()) && pufferfishFuseTicks >= 0
                && --pufferfishFuseTicks <= 0)
        {
            detonate(1.5F, false);
        }
    }

    /** Preserve the type-specific trails used by the original fire and pulse arrows. */
    private void spawnTypeParticles()
    {
        if (!level().isClientSide) return;

        switch (getArrowType())
        {
            case "blaze", "fire_charge", "fireball" ->
            {
                for (int i = 0; i < 3; ++i)
                {
                    level().addParticle(net.minecraft.core.particles.ParticleTypes.FLAME,
                            getX() + (random.nextDouble() - 0.5D) * 0.1D,
                            getY() + (random.nextDouble() - 0.5D) * 0.1D,
                            getZ() + (random.nextDouble() - 0.5D) * 0.1D,
                            (random.nextDouble() - 0.5D) * 0.1D,
                            (random.nextDouble() - 0.5D) * 0.1D,
                            (random.nextDouble() - 0.5D) * 0.1D);
                }
            }
            case "pulse" ->
            {
                if (random.nextBoolean())
                {
                    level().addParticle(new net.minecraft.core.particles.DustParticleOptions(
                                    new org.joml.Vector3f(1.0F, 0.0F, 0.0F), 1.0F),
                            getX() + (random.nextDouble() - 0.5D) * 0.05D,
                            getY() + (random.nextDouble() - 0.5D) * 0.05D,
                            getZ() + (random.nextDouble() - 0.5D) * 0.05D,
                            0.0D, 0.0D, 0.0D);
                }
            }
            case "slime" ->
            {
                if (random.nextFloat() < Math.min(1.0F, (float) getDeltaMovement().length()))
                {
                    level().addParticle(new net.minecraft.core.particles.ItemParticleOption(
                                    net.minecraft.core.particles.ParticleTypes.ITEM,
                                    new ItemStack(net.minecraft.world.item.Items.SLIME_BALL)),
                            getX(), getY(), getZ(),
                            (random.nextDouble() - 0.5D) * 0.1D,
                            (random.nextDouble() - 0.5D) * 0.1D,
                            (random.nextDouble() - 0.5D) * 0.1D);
                }
            }
            case "ender_pearl" ->
            {
                for (int i = 0; i < 5; ++i)
                {
                    level().addParticle(net.minecraft.core.particles.ParticleTypes.PORTAL,
                            getX(), getY(), getZ(),
                            (random.nextDouble() - 0.5D) * 0.2D,
                            (random.nextDouble() - 0.5D) * 0.2D,
                            (random.nextDouble() - 0.5D) * 0.2D);
                }
            }
            case "torch" ->
            {
                var particle = random.nextBoolean()
                        ? net.minecraft.core.particles.ParticleTypes.SMOKE
                        : net.minecraft.core.particles.ParticleTypes.FLAME;
                level().addParticle(particle, getX(), getY(), getZ(),
                        (random.nextDouble() - 0.5D) * 0.06D,
                        (random.nextDouble() - 0.5D) * 0.06D,
                        (random.nextDouble() - 0.5D) * 0.06D);
            }
            case "explosive_pufferfish" ->
            {
                if (entityData.get(PUFFERFISH_FUSING))
                {
                    level().addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE,
                            getX(), getY() + 0.05D, getZ(), 0.0D, 0.015D, 0.0D);
                }
            }
            default -> { }
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result)
    {
        if ("grappling_hook".equals(getArrowType()) || "vine".equals(getArrowType()))
        {
            if (!level().isClientSide)
            {
                net.minecraft.world.entity.Entity target = result.getEntity();
                boolean canTarget = !(target instanceof net.minecraft.world.entity.monster.EnderMan);
                if (isOnFire() && canTarget) target.setSecondsOnFire(5);
                if (target instanceof LivingEntity living && canTarget) doPostHurtEffects(living);
                playSound(net.minecraft.sounds.SoundEvents.ARROW_HIT, 1.0F,
                        1.2F / (random.nextFloat() * 0.2F + 0.9F));
                if (canTarget) discard();
            }
            return;
        }

        // Detonator arrows are remote mines and torch arrows only place a torch on blocks; both
        // pass through entities without dealing damage in the reference mod.
        if ("detonator".equals(getArrowType()) || "torch".equals(getArrowType())) return;

        // Smoke bombs detonate at the actual impact point and never deal the vanilla arrow hit.
        if ("smoke_bomb".equals(getArrowType()))
        {
            if (!level().isClientSide)
            {
                spawnSmokeCloud(result.getLocation());
                discard();
            }
            return;
        }

        // Pulse arrows are a remote redstone trigger, not a damaging projectile.
        if ("pulse".equals(getArrowType()))
        {
            // The original pulse arrow has no entity impact handler. Let it pass through
            // entities so its redstone pulse is produced only when it reaches a block.
            return;
        }

        if ("explosive_pufferfish".equals(getArrowType()))
        {
            startPufferfishFuse();
            // Vanilla 1.20 discards a non-piercing arrow as soon as it hits a living target.
            // The original arrow embeds in the target and stays alive until its fuse expires.
            setPierceLevel((byte) Math.max(1, getPierceLevel()));
        }

        if ("sponge".equals(getArrowType()))
        {
            absorbWater(result.getEntity().blockPosition());
        }

        if ("fireball".equals(getArrowType()))
        {
            com.fiskmods.heroes.common.hero.modifier.DamageGroups.withDamageProfile(
                    fireballDamageProfile(), () -> super.onHitEntity(result));
        }
        else if ("blaze".equals(getArrowType()))
        {
            // The original Blaze Arrow's DamageSource is explicitly fire damage. Keep that
            // classification through suit resistances instead of treating it as a plain arrow.
            com.fiskmods.heroes.common.hero.modifier.DamageGroups.withDamageProfile(
                    blazeDamageProfile(), () -> super.onHitEntity(result));
        }
        else
        {
            super.onHitEntity(result);
        }
        if ("explosive_pufferfish".equals(getArrowType()))
        {
            // Keep the projectile at the impact point while its fuse ticks; otherwise the
            // piercing-arrow path would continue through the target and carry the blast away.
            Vec3 impact = result.getLocation();
            setPos(impact.x, impact.y, impact.z);
            setDeltaMovement(Vec3.ZERO);
            setNoGravity(true);
        }
        if (!(result.getEntity() instanceof LivingEntity target))
        {
            if ("fireball".equals(getArrowType()) && !level().isClientSide)
            {
                spawnFireballBurst(result.getLocation(), result.getEntity());
            }
            return;
        }
        if (level().isClientSide) return;

        switch (getArrowType())
        {
            case "ender_pearl" ->
            {
                teleportShooter();
                setArrowType(ItemTrickArrow.NORMAL);
            }
            case "carrot" -> target.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1200));
            case "fire_charge", "blaze" -> target.setSecondsOnFire(5);
            case "fireball" -> target.setSecondsOnFire(3);
            case "boxing_glove" ->
            {
                Vec3 velocity = getDeltaMovement();
                target.knockback(1.2F, -velocity.x, -velocity.z);
            }
            case "pufferfish", "explosive_pufferfish" ->
            {
                if (random.nextInt(3) == 0) target.addEffect(new MobEffectInstance(MobEffects.POISON, 80));
            }
            case "slime" ->
            {
                // The reference slime arrow applies Slowness for 3 seconds with a 3/4 chance.
                if (random.nextInt(4) != 0) target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60));
            }
            case "phantom" -> target.addEffect(new MobEffectInstance(
                    com.fiskmods.heroes.common.hero.modifier.ModEffects.PHASE_SUPPRESSANT.get(), 100, 0, false, false, true));
            case "tutridium" -> target.addEffect(new MobEffectInstance(
                    com.fiskmods.heroes.common.hero.modifier.ModEffects.TUTRIDIUM.get(), 200, 0, false, false, true));
            case "gross" -> target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 180));
            case "vial" -> applyVial(target);
            default -> { }
        }
        if ("blaze".equals(getArrowType()))
        {
            com.fiskmods.heroes.common.hero.modifier.MetalSkinHeat.add(target, 0.1F);
        }
        if ("cactus".equals(getArrowType()))
        {
            // The original cactus arrow bursts on every impact, including living targets.
            spawnCactusSpikes();
            detonate(2.0F, false);
            return;
        }
        detonateIfExplosive();
        if ("firework".equals(getArrowType()))
        {
            spawnFlashbang();
            discard();
        }
        else if ("fireball".equals(getArrowType()))
        {
            spawnFireballBurst(result.getLocation(), target);
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

        if ("sponge".equals(type))
        {
            absorbWater(result.getBlockPos());
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
            // Pulling is applied each server tick while the arrow is anchored and the bow is held,
            // matching the original grapple arrow instead of giving a single weak impact impulse.
        }
        else if ("smoke_bomb".equals(type))
        {
            spawnSmokeCloud(result.getLocation());
            discard();
        }
        else if ("firework".equals(type))
        {
            spawnFlashbang();
            discard();
        }
        else if ("fireball".equals(type))
        {
            spawnFireballBurst(result.getLocation(), null);
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
            // The original pulse arrow leaves an embedded arrow and updates redstone around
            // the impact, instead of disappearing after only toggling buttons and levers.
            if (getOwner() instanceof Player player
                    && (state.getBlock() instanceof net.minecraft.world.level.block.ButtonBlock
                            || state.getBlock() instanceof net.minecraft.world.level.block.LeverBlock))
            {
                state.use(level(), player, net.minecraft.world.InteractionHand.MAIN_HAND, result);
            }
            level().updateNeighborsAt(pos, state.getBlock());
            var adjacent = pos.relative(result.getDirection());
            level().updateNeighborsAt(adjacent, level().getBlockState(adjacent).getBlock());
        }
        detonateIfExplosive();
    }

    @Override
    public void playerTouch(Player player)
    {
        if (("grappling_hook".equals(getArrowType()) || "vine".equals(getArrowType()))
                && !isGrappleSnapped()) return;
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
        if ("firework".equals(type))
        {
            spawnFlashbang();
            discard();
            return true;
        }
        if ("fireball".equals(type))
        {
            spawnFireballBurst(position(), null);
            return true;
        }
        if ("smoke_bomb".equals(type))
        {
            spawnSmokeCloud(position());
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
            entityData.set(PUFFERFISH_FUSING, true);
            if (!level().isClientSide)
            {
                com.fiskmods.heroes.common.sound.SHSounds.play(this, PUFFERFISH_PRIMED_SOUND,
                        net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        }
    }

    private void tickGrapple()
    {
        String type = getArrowType();
        if (level().isClientSide || !inGround || grappleSnapped
                || !("grappling_hook".equals(type) || "vine".equals(type))) return;

        if (!(getOwner() instanceof Player player) || !player.isAlive()
                || !player.getMainHandItem().is(ModItems.COMPOUND_BOW.get()) || player.hurtTime > 0)
        {
            snapGrapple(playerOrNull(), GRAPPLE_DISCONNECT_SOUND);
            return;
        }

        // The original cable slowly burns through while a flaming arrow is attached, then snaps.
        if (isOnFire() || grappleFireSpread > 0.0F)
        {
            if (grappleFireSpread < 1.0F)
            {
                grappleFireSpread = Math.min(grappleFireSpread + 0.025F, 1.0F);
            }
            else
            {
                snapGrapple(player, null);
                return;
            }
        }

        // The original vine cable can snap while it is pulling. Its failure rate rises over
        // time (roughly 1/100 per early tick, then at least 1/10 once it has run for a while).
        if ("vine".equals(type)
                && random.nextInt(Math.max(100 - ++vineUseTicks, 10)) == 0)
        {
            snapGrapple(player, VINE_SNAP_SOUND);
            return;
        }

        Vec3 offset = position().subtract(player.position());
        double distance = offset.length();
        if (distance < 1.0D) return;

        Vec3 pull = offset.normalize().scale(Math.min(distance / 4.0D, 1.0D) * 0.35D);
        player.setDeltaMovement(player.getDeltaMovement().scale(0.9D).add(pull));
        player.hasImpulse = true;
        if (pull.y >= 0.0D || player.getDeltaMovement().y >= 0.0D) player.fallDistance = 0.0F;
    }

    private Player playerOrNull()
    {
        return getOwner() instanceof Player player ? player : null;
    }

    private void snapGrapple(Player player, ResourceLocation sound)
    {
        if (grappleSnapped) return;
        grappleSnapped = true;
        entityData.set(GRAPPLE_SNAPPED, true);
        if (player != null && sound != null)
        {
            com.fiskmods.heroes.common.sound.SHSounds.play(player, sound, SoundSource.PLAYERS, 1.0F, 0.8F);
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

    /** The original fireball arrow deals distance-scaled fire damage without terrain explosion. */
    private void spawnFireballBurst(Vec3 center, net.minecraft.world.entity.Entity directHit)
    {
        if (level().isClientSide || detonated) return;
        detonated = true;

        JsonObject profile = fireballDamageProfile();
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class,
                new net.minecraft.world.phys.AABB(center.x - FIREBALL_ARROW_RADIUS,
                        center.y - FIREBALL_ARROW_RADIUS, center.z - FIREBALL_ARROW_RADIUS,
                        center.x + FIREBALL_ARROW_RADIUS, center.y + FIREBALL_ARROW_RADIUS,
                        center.z + FIREBALL_ARROW_RADIUS),
                entity -> entity != getOwner() && entity != directHit && entity.isAlive()))
        {
            Vec3 targetCenter = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
            double distance = targetCenter.distanceTo(center);
            if (distance > FIREBALL_ARROW_RADIUS) continue;

            float damage = Math.max(1.0F,
                    FIREBALL_ARROW_DAMAGE * (1.0F - (float) distance / FIREBALL_ARROW_RADIUS));
            com.fiskmods.heroes.common.hero.modifier.DamageGroups.applyProfileDamage(target,
                    getOwner() instanceof LivingEntity owner ? owner : null,
                    level().damageSources().magic(), damage, profile);
        }

        if (level() instanceof net.minecraft.server.level.ServerLevel serverLevel)
        {
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION,
                    center.x, center.y, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,
                    center.x, center.y, center.z, 48, 0.5D, 0.35D, 0.5D, 0.08D);
        }
        level().playSound(null, center.x, center.y, center.z,
                net.minecraft.sounds.SoundEvents.FIREWORK_ROCKET_BLAST,
                net.minecraft.sounds.SoundSource.PLAYERS, 1.5F, 0.9F);
        discard();
    }

    private static JsonObject fireballDamageProfile()
    {
        JsonObject profile = new JsonObject();
        profile.addProperty("damage", FIREBALL_ARROW_DAMAGE);
        JsonObject types = new JsonObject();
        types.addProperty("FIRE", 1.0D);
        profile.add("types", types);
        JsonObject properties = new JsonObject();
        properties.addProperty("COOK_ENTITY", true);
        properties.addProperty("HEAT_TRANSFER", 20);
        properties.addProperty("IGNITE", 3);
        profile.add("properties", properties);
        return profile;
    }

    private static JsonObject blazeDamageProfile()
    {
        JsonObject profile = new JsonObject();
        JsonObject types = new JsonObject();
        types.addProperty("FIRE", 1.0D);
        profile.add("types", types);
        return profile;
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

        net.minecraft.world.item.Item particleItem = "slime".equals(getArrowType())
                ? net.minecraft.world.item.Items.SLIME_BALL
                : net.minecraft.world.item.Items.PUFFERFISH;
        net.minecraft.core.particles.ItemParticleOption particles = new net.minecraft.core.particles.ItemParticleOption(
                net.minecraft.core.particles.ParticleTypes.ITEM, particleItem.getDefaultInstance());
        net.minecraft.core.BlockPos pos = hit.getBlockPos();
        if (level() instanceof net.minecraft.server.level.ServerLevel serverLevel)
        {
            Vec3 impact = hit.getLocation();
            serverLevel.sendParticles(particles, impact.x, impact.y, impact.z,
                    20, 0.2D, 0.2D, 0.2D, 0.1D);
            if ("slime".equals(getArrowType()))
            {
                serverLevel.playSound(null, pos, net.minecraft.sounds.SoundEvents.SLIME_BLOCK_HIT,
                        net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            else
            {
                com.fiskmods.heroes.common.sound.SHSounds.play(this, PUFFERFISH_FLOP_SOUND,
                        net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        }
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
        spawnSmokeCloud(position());
    }

    /** Applies the original smoke bomb's temporary stealth to nearby suited heroes. */
    private void spawnSmokeCloud(Vec3 center)
    {
        if (level().isClientSide) return;

        final double radius = 2.5D;
        var bounds = new net.minecraft.world.phys.AABB(center.x - radius, center.y - radius / 2.0D,
                center.z - radius, center.x + radius, center.y + radius / 2.0D, center.z + radius);
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class, bounds))
        {
            if (!com.fiskmods.heroes.common.hero.HeroTracker.hasHero(target)) continue;
            double dx = target.getX() - center.x;
            double dy = (target.getY() + target.getBbHeight() / 2.0D - center.y) * 2.0D;
            double dz = target.getZ() - center.z;
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz) / radius;
            if (distance <= 1.0D)
            {
                int duration = (int) Math.ceil(100.0D + distance * 60.0D);
                target.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, duration));
            }
        }

        if (level() instanceof net.minecraft.server.level.ServerLevel serverLevel)
        {
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    center.x, center.y, center.z, 300, 0.3D, 0.15D, 0.3D, 0.0D);
        }
        level().playSound(null, center.x, center.y, center.z,
                net.minecraft.sounds.SoundEvents.GENERIC_EXTINGUISH_FIRE,
                net.minecraft.sounds.SoundSource.PLAYERS, 2.0F, 0.9F + random.nextFloat() * 0.2F);
    }

    /** Firework arrows flash nearby targets instead of behaving like explosive arrows. */
    private void spawnFlashbang()
    {
        if (level().isClientSide) return;
        if (fireworkRadius > 0.0F)
        {
            var area = getBoundingBox().inflate(fireworkRadius);
            for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class, area,
                    candidate -> candidate != getOwner() && candidate.isAlive()
                            && candidate.distanceToSqr(this) <= fireworkRadius * fireworkRadius))
            {
                double distance = Math.sqrt(target.distanceToSqr(this));
                int duration = (int) Math.ceil(fireworkStrength * Math.max(0.0D, 1.0D - distance / fireworkRadius));
                if (duration > 0) target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, duration, 0, false, true));
            }
        }
        level().playSound(null, getX(), getY(), getZ(), net.minecraft.sounds.SoundEvents.FIREWORK_ROCKET_BLAST,
                net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
        if (level() instanceof net.minecraft.server.level.ServerLevel serverLevel)
        {
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH,
                    getX(), getY(), getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    /** Absorbs nearby source water, matching the utility effect implied by a sponge-tipped arrow. */
    private void absorbWater(net.minecraft.core.BlockPos center)
    {
        if (level().isClientSide || !level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) return;
        int absorbed = 0;
        for (net.minecraft.core.BlockPos pos : net.minecraft.core.BlockPos.betweenClosed(
                center.offset(-3, -2, -3), center.offset(3, 2, 3)))
        {
            if (absorbed >= 64) break;
            if (level().getBlockState(pos).is(Blocks.WATER))
            {
                level().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                ++absorbed;
            }
        }
        if (absorbed > 0 && level() instanceof net.minecraft.server.level.ServerLevel serverLevel)
        {
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.SPLASH,
                    center.getX() + 0.5D, center.getY() + 0.5D, center.getZ() + 0.5D,
                    Math.min(24, absorbed), 0.5D, 0.5D, 0.5D, 0.02D);
        }
    }

    private void spawnCactusSpikes()
    {
        if (!(level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) return;
        // The reference arrow bursts into twenty or twenty-one cactus spikes.
        int count = 20 + random.nextInt(2);
        Vec3 incomingMotion = getDeltaMovement();
        for (int i = 0; i < count; ++i)
        {
            if (!(getOwner() instanceof LivingEntity owner)) break;
            com.fiskmods.heroes.common.entity.projectile.CactusSpikeEntity spike =
                    new com.fiskmods.heroes.common.entity.projectile.CactusSpikeEntity(owner);
            spike.setPos(getX(), getY(), getZ());
            // The reference spike inherits the Cactus Arrow's travel direction and adds a small
            // random spread; firing evenly in random directions made the burst look unrelated.
            spike.setDeltaMovement(incomingMotion.scale(20.0D).add(
                    random.nextDouble() * 0.4D - 0.2D,
                    random.nextDouble() * 0.4D - 0.2D,
                    random.nextDouble() * 0.4D - 0.2D));
            spike.hasImpulse = true;
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
        tag.putBoolean("GrappleSnapped", grappleSnapped);
        tag.putInt("VineUseTicks", vineUseTicks);
        tag.putFloat("GrappleFireSpread", grappleFireSpread);
        tag.putInt("PufferfishFuseTicks", pufferfishFuseTicks);
        tag.putBoolean("PufferfishFusing", entityData.get(PUFFERFISH_FUSING));
        if (!fireworkStack.isEmpty()) tag.put("FireworkStack", fireworkStack.save(new CompoundTag()));
        tag.putInt("FireworkAge", fireworkAge);
        tag.putInt("FireworkLifetime", fireworkLifetime);
        tag.putInt("FireworkStrength", fireworkStrength);
        tag.putFloat("FireworkRadius", fireworkRadius);
        if (!vialPotion.isEmpty()) tag.put("VialPotion", vialPotion.save(new CompoundTag()));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);
        setArrowType(tag.getString("ArrowType"));
        setExplosionRadius(tag.contains("ExplosionRadius") ? tag.getFloat("ExplosionRadius") : DEFAULT_EXPLOSION_RADIUS);
        detonated = tag.getBoolean("Detonated");
        grappleSnapped = tag.getBoolean("GrappleSnapped");
        entityData.set(GRAPPLE_SNAPPED, grappleSnapped);
        vineUseTicks = Math.max(0, tag.getInt("VineUseTicks"));
        grappleFireSpread = net.minecraft.util.Mth.clamp(tag.getFloat("GrappleFireSpread"), 0.0F, 1.0F);
        pufferfishFuseTicks = tag.contains("PufferfishFuseTicks") ? tag.getInt("PufferfishFuseTicks") : -1;
        entityData.set(PUFFERFISH_FUSING, tag.getBoolean("PufferfishFusing") || pufferfishFuseTicks >= 0);
        fireworkStack = tag.contains("FireworkStack", CompoundTag.TAG_COMPOUND)
                ? ItemStack.of(tag.getCompound("FireworkStack")) : ItemStack.EMPTY;
        entityData.set(FIREWORK_STACK, fireworkStack.copy());
        fireworkAge = tag.getInt("FireworkAge");
        fireworkLifetime = tag.contains("FireworkLifetime") ? tag.getInt("FireworkLifetime") : -1;
        fireworkStrength = tag.contains("FireworkStrength") ? tag.getInt("FireworkStrength") : 120;
        fireworkRadius = tag.getFloat("FireworkRadius");
        if (fireworkLifetime < 0 && "firework".equals(getArrowType()))
        {
            if (fireworkStack.isEmpty())
            {
                fireworkStack = ItemTrickArrow.getAttachedItem(ItemTrickArrow.createStack("firework"));
            }
            int savedAge = fireworkAge;
            setFireworkStack(fireworkStack);
            fireworkAge = savedAge;
        }
        vialPotion = tag.contains("VialPotion", CompoundTag.TAG_COMPOUND)
                ? ItemStack.of(tag.getCompound("VialPotion")) : ItemStack.EMPTY;
        entityData.set(VIAL_POTION, vialPotion.copy());
    }
}
