package com.fiskmods.heroes.common.item;

import java.util.List;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.data.var.Vars;
import com.fiskmods.heroes.common.hero.Hero;
import com.fiskmods.heroes.common.hero.HeroIteration;
import com.fiskmods.heroes.common.hero.HeroTracker;
import com.fiskmods.heroes.common.sound.SHSounds;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Base firearm/launcher item used by Fisk's Superheroes weapons. */
public class ItemGun extends Item
{
    protected static final String AMMO_TAG = "Ammo";
    protected static final String NEXT_SHOT_TAG = "NextShotTick";
    private static final String NEXT_NOTICE_TAG = "FiskHeroesGunNoticeTick";
    public static final String RELOAD_END_TAG = "fiskheroes_gun_reload_end";
    private static final String RELOAD_DURATION_TAG = "fiskheroes_gun_reload_duration";
    protected final int magazineSize;
    protected final int cooldownTicks;
    protected final int reloadTicks;
    protected final double range;
    protected final float damage;
    protected final boolean usesAmmo;

    public ItemGun(int magazineSize, int cooldownTicks, int reloadTicks, double range, float damage, Properties properties)
    {
        this(magazineSize, cooldownTicks, reloadTicks, range, damage, true, properties);
    }

    public ItemGun(int magazineSize, int cooldownTicks, int reloadTicks, double range, float damage,
            boolean usesAmmo, Properties properties)
    {
        super(properties.stacksTo(1));
        this.magazineSize = magazineSize;
        this.cooldownTicks = cooldownTicks;
        this.reloadTicks = reloadTicks;
        this.range = range;
        this.damage = damage;
        this.usesAmmo = usesAmmo;
    }

    public boolean isGun()
    {
        return true;
    }

    /** Shots, cooldowns, modes and ammo update NBT without switching the held item. */
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged)
    {
        return slotChanged || oldStack.isEmpty() || newStack.isEmpty() || !ItemStack.isSameItem(oldStack, newStack);
    }

    public int getAmmo(ItemStack stack)
    {
        if (!usesAmmo) return Integer.MAX_VALUE;
        return stack.hasTag() && stack.getTag().contains(AMMO_TAG)
                ? net.minecraft.util.Mth.clamp(stack.getTag().getInt(AMMO_TAG), 0, getMagazineSize(stack)) : getMagazineSize(stack);
    }

    public int getMagazineSize()
    {
        return magazineSize;
    }

    public int getMagazineSize(ItemStack stack)
    {
        return magazineSize * (isDual(stack) ? 2 : 1);
    }

    /** The original firearms used the durability bar to show rounds remaining. */
    @Override
    public boolean isBarVisible(ItemStack stack)
    {
        return usesAmmo;
    }

    @Override
    public int getBarWidth(ItemStack stack)
    {
        return usesAmmo ? Math.round(13.0F * ammoFraction(stack)) : super.getBarWidth(stack);
    }

    @Override
    public int getBarColor(ItemStack stack)
    {
        return usesAmmo ? net.minecraft.util.Mth.hsvToRgb(ammoFraction(stack) / 3.0F, 1.0F, 1.0F)
                : super.getBarColor(stack);
    }

    private float ammoFraction(ItemStack stack)
    {
        return getAmmo(stack) / (float) Math.max(1, getMagazineSize(stack));
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag)
    {
        super.appendHoverText(stack, level, tooltip, flag);
        if (usesAmmo)
        {
            tooltip.add(Component.translatable("tooltip.gun.ammo", getAmmo(stack), getMagazineSize(stack)));
        }
    }

    /** Client-side cadence for repeated primary-attack input; the server revalidates every shot. */
    public int getShotCooldownTicks(ItemStack stack)
    {
        return isDual(stack) ? Math.round(cooldownTicks * 1.2F) : cooldownTicks;
    }

    /** The server can apply per-wearer rules without trusting client-side prediction. */
    protected int getServerShotCooldownTicks(Player player, ItemStack stack)
    {
        return getShotCooldownTicks(stack);
    }

    /** Client heartbeat cadence; it may be faster than the authoritative shot cooldown. */
    public int getRequestCooldownTicks(ItemStack stack)
    {
        return getShotCooldownTicks(stack);
    }

    public static boolean isDual(ItemStack stack)
    {
        return ItemDualSword.isDual(stack);
    }

    public static boolean isGun(ItemStack stack)
    {
        return !stack.isEmpty() && stack.getItem() instanceof ItemGun;
    }

    @Override
    public Component getName(ItemStack stack)
    {
        if (!isDual(stack)) return super.getName(stack);
        String id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(this).getPath();
        return Component.translatable("item.fiskheroes." + id + ".dual");
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.sidedSuccess(stack, true);

        fireFromAttack(player, stack);
        return InteractionResultHolder.consume(stack);
    }

    /** Server-authoritative shot requested by primary attack input or item use. */
    public boolean fireFromAttack(Player player, ItemStack stack)
    {
        Level level = player.level();
        if (level.isClientSide || stack.isEmpty() || stack.getItem() != this
                || player.getMainHandItem() != stack)
        {
            return false;
        }

        HeroTracker.update(player);
        HeroIteration iteration = HeroTracker.getHero(player);
        Hero hero = iteration != null ? iteration.getHero() : null;
        SHPlayerData data = SHDataCapabilities.getPlayer(player);
        if (hero == null || data == null)
        {
            showUseNotice(player, "message.fiskheroes.gun.requires_suit");
            return false;
        }
        if (!hasPermission(player, hero))
        {
            showUseNotice(player, "message.fiskheroes.gun.not_permitted");
            return false;
        }
        // Energy firearms originally fire only while their AIM binding is active. Validate
        // this on the server, including direct item-use and forged fire packets.
        if ((this instanceof ItemChronosRifle || this instanceof ItemRipHuntersGun)
                && !data.getData().get(Vars.AIMING)) return false;
        if (data.getData().get(Vars.RELOAD_TIMER) > 0.0F)
        {
            return false;
        }

        long now = level.getGameTime();
        int shotCooldown = getServerShotCooldownTicks(player, stack);
        if (stack.getOrCreateTag().getLong(NEXT_SHOT_TAG) > now)
        {
            return false;
        }
        stack.getOrCreateTag().putLong(NEXT_SHOT_TAG, now + shotCooldown);

        return triggerSingleShot(player, stack);
    }

    protected boolean triggerSingleShot(Player player, ItemStack stack)
    {
        Level level = player.level();
        int ammo = getAmmo(stack);
        if (ammo <= 0)
        {
            SHSounds.play(player, FiskHeroes.id("item.gun.click"), SoundSource.PLAYERS, 0.8F,
                    0.85F + level.random.nextFloat() * 0.25F);
            showUseNotice(player, "message.fiskheroes.gun.empty");
            return false;
        }

        if (usesAmmo) stack.getOrCreateTag().putInt(AMMO_TAG, ammo - 1);
        if (player instanceof ServerPlayer serverPlayer)
        {
            fire(serverPlayer, stack);
            if (stack.is(ModItems.DESERT_EAGLE.get()) || stack.is(ModItems.BERETTA_93R.get()))
            {
                boolean offhand = com.fiskmods.heroes.common.util.GunVisualMath.useOffhand(
                        isDual(stack), player.getOffhandItem().isEmpty(), getMagazineSize(stack), ammo);
                com.fiskmods.heroes.common.network.SHNetwork.sendToTracking(
                        new com.fiskmods.heroes.common.network.PacketGunShotEffect(player.getId(), offhand), player);
            }
        }
        playShotSound(player, level, stack);
        player.awardStat(Stats.ITEM_USED.get(this));
        return true;
    }

    protected void playShotSound(Player player, Level level, ItemStack stack)
    {
        String id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(this).getPath();
        float pitch = 1.0F / (level.random.nextFloat() * 0.3F + 0.7F);
        switch (id)
        {
            case "desert_eagle" -> SHSounds.play(player, FiskHeroes.id("item.gun.deagle.shoot"), SoundSource.PLAYERS, 4.0F, pitch);
            case "beretta_93r" -> SHSounds.play(player, FiskHeroes.id("item.gun.beretta.shoot"), SoundSource.PLAYERS, 3.5F, pitch);
            case "chronos_rifle" -> SHSounds.play(player, FiskHeroes.id("item.chronosrifle.shoot"), SoundSource.PLAYERS, 3.5F, 1.0F);
            case "rip_hunters_gun" -> SHSounds.play(player, FiskHeroes.id("item.ripsgun.shoot"), SoundSource.PLAYERS, 3.0F, 1.0F);
            case "cold_gun" -> SHSounds.play(player, FiskHeroes.id("item.coldgun.beam.start"), SoundSource.PLAYERS, 2.0F, 1.0F);
            case "heat_gun" -> player.playSound(net.minecraft.sounds.SoundEvents.FIRECHARGE_USE, 1.0F, 0.9F + level.random.nextFloat() * 0.2F);
            default -> player.playSound(net.minecraft.sounds.SoundEvents.CROSSBOW_SHOOT, 1.0F, 0.9F + level.random.nextFloat() * 0.2F);
        }
    }

    protected static void showUseNotice(Player player, String translationKey)
    {
        long now = player.level().getGameTime();
        if (player.getPersistentData().getLong(NEXT_NOTICE_TAG) > now) return;
        player.getPersistentData().putLong(NEXT_NOTICE_TAG, now + 20L);
        player.displayClientMessage(Component.translatable(translationKey), true);
    }

    protected double getRayRange(ServerPlayer shooter)
    {
        return range;
    }

    protected float getRayDamage(ServerPlayer shooter)
    {
        return damage;
    }

    protected void fire(ServerPlayer shooter, ItemStack stack)
    {
        fireRay(shooter, stack, getRayRange(shooter));
    }

    protected void fireRay(ServerPlayer shooter, ItemStack stack, double rayRange)
    {
        Level level = shooter.level();
        String id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(this).getPath();

        if (id.equals("chronos_rifle") || id.equals("rip_hunters_gun"))
        {
            com.google.gson.JsonObject profile = new com.google.gson.JsonObject();
            profile.addProperty("damage", damage);
            com.google.gson.JsonObject types = new com.google.gson.JsonObject();
            types.addProperty("ENERGY", 1.0D);
            profile.add("types", types);

            SHPlayerData data = SHDataCapabilities.getPlayer(shooter);
            boolean scoped = data != null && data.getData().get(Vars.SCOPE_TIMER) > 0.0F;
            var bolt = new com.fiskmods.heroes.common.entity.projectile.EnergyBoltEntity(
                    shooter, profile, damage * (scoped ? 1.6F : 1.0F), id.equals("chronos_rifle"), 4.0F, 0.0F);
            level.addFreshEntity(bolt);
            if (Boolean.getBoolean("fiskheroes.ciEnergyGunDiagnostics"))
                FiskHeroes.LOGGER.info("Energy gun shot: item={}, shooter={}, scoped={}, damage={}, explosive={}",
                        id, shooter.getName().getString(), scoped, damage * (scoped ? 1.6F : 1.0F), id.equals("chronos_rifle"));
            return;
        }

        Vec3 start = shooter.getEyePosition();
        Vec3 direction = shooter.getViewVector(1.0F);
        Vec3 end = start.add(direction.scale(rayRange));
        BlockHitResult block = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                id.equals("cold_gun") ? ClipContext.Fluid.ANY : ClipContext.Fluid.NONE, shooter));
        if (block.getType() != HitResult.Type.MISS) end = block.getLocation();

        AABB search = shooter.getBoundingBox().expandTowards(direction.scale(rayRange)).inflate(1.0D);
        Entity hit = null;
        double nearest = Double.MAX_VALUE;
        boolean heatGun = id.equals("heat_gun");
        boolean beamGun = heatGun || id.equals("cold_gun");
        for (Entity target : level.getEntities(shooter, search, entity -> entity.isAlive()
                && (beamGun ? entity.isPickable() && (entity != shooter.getVehicle() || entity.canRiderInteract())
                        : entity instanceof LivingEntity && !entity.isAlliedTo(shooter))))
        {
            // The original heat-gun ray uses the target's collision border rather than a
            // blanket 0.25-block enlargement, and can stop on pickable non-living entities.
            AABB bounds = target.getBoundingBox().inflate(beamGun ? target.getPickRadius() : 0.25D);
            var intersection = bounds.clip(start, end);
            if (intersection.isPresent() || beamGun && bounds.contains(start))
            {
                double distance = beamGun && bounds.contains(start) ? 0.0D : start.distanceToSqr(intersection.get());
                if (distance < nearest)
                {
                    nearest = distance;
                    hit = target;
                }
            }
        }

        Vec3 tracerEnd = hit != null ? start.add(direction.scale(Math.sqrt(nearest))) : end;
        if (id.equals("heat_gun") && level.getGameTime() % 3L == 0L)
        {
            Vec3 right = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
            right = right.lengthSqr() < 1.0E-6D ? new Vec3(1.0D, 0.0D, 0.0D) : right.normalize();
            // Keep nearby particles in front of the camera without placing the muzzle beyond the impact.
            double forward = Math.min(0.9D, start.distanceTo(tracerEnd) * 0.7D);
            double side = shooter.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT ? 1.0D : -1.0D;
            Vec3 muzzle = start.add(right.scale(0.28D * side)).add(0.0D, -0.16D, 0.0D).add(direction.scale(forward));
            com.fiskmods.heroes.common.network.SHNetwork.sendToNearby(
                    new com.fiskmods.heroes.common.network.PacketHeatGunFlame(shooter.getId(), muzzle, tracerEnd),
                    level, start.x, start.y, start.z, 64.0D);
        }
        else if (!id.equals("heat_gun") && (!id.equals("cold_gun") || level.getGameTime() % 3L == 0L) && level instanceof net.minecraft.server.level.ServerLevel serverLevel)
        {
            net.minecraft.core.particles.ParticleOptions tracer = switch (id)
            {
                case "cold_gun" -> net.minecraft.core.particles.ParticleTypes.SNOWFLAKE;
                default -> new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(1.0F, 0.72F, 0.2F), 0.7F);
            };
            double distance = start.distanceTo(tracerEnd);
            int points = Math.min(18, Math.max(1, (int) Math.ceil(distance / 3.5D)));
            for (int i = 1; i <= points; ++i)
            {
                Vec3 point = start.lerp(tracerEnd, i / (double) (points + 1));
                serverLevel.sendParticles(tracer, point.x, point.y, point.z, 1, 0.02D, 0.02D, 0.02D, 0.0D);
            }
        }

        if (id.equals("cold_gun"))
        {
            SHPlayerData data = SHDataCapabilities.getPlayer(shooter);
            if (data != null)
            {
                double length = hit != null ? Math.sqrt(nearest) : start.distanceTo(end);
                data.getData().set(Vars.HEAT_VISION_LENGTH, Math.min(rayRange, length));
                data.getData().set(Vars.ENERGY_PROJECTION_TIMER,
                        net.minecraft.util.Mth.clamp((float) (rayRange / getRayRange(shooter)), 0.0F, 1.0F));
            }
        }

        // A target in front absorbs the shot; do not modify the block behind it.
        if (hit == null) onRayBlockHit(shooter, block);

        boolean acceptedHit = false;
        if (hit != null)
        {
            var damageType = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                    .getHolderOrThrow(id.equals("heat_gun") ? net.minecraft.world.damagesource.DamageTypes.FIREBALL
                            : id.equals("cold_gun") ? net.minecraft.resources.ResourceKey.create(
                                    net.minecraft.core.registries.Registries.DAMAGE_TYPE, FiskHeroes.id("cold_beam"))
                            : net.minecraft.world.damagesource.DamageTypes.ARROW);
            var source = new net.minecraft.world.damagesource.DamageSource(damageType, shooter, shooter);
            Entity target = hit;
            String dmgProfile = id.equals("cold_gun") ? "COLD" : (id.equals("heat_gun") ? "FIRE" : "BULLET");
            final boolean[] applied = { false };
            com.fiskmods.heroes.common.hero.modifier.DamageGroups.withDamageProfile(
                    java.util.Map.of(dmgProfile, 1.0D), () -> applied[0] = target.hurt(source, getRayDamage(shooter)));
            acceptedHit = applied[0];
        }

        if (hit instanceof LivingEntity living)
        {
            onRayEntityContact(shooter, living, acceptedHit);
            if (acceptedHit) onRayEntityHit(shooter, living);
        }
        else if (acceptedHit && heatGun)
        {
            hit.setSecondsOnFire(5);
        }
    }

    /** Contact effects that must run even when the damage itself was rejected. */
    protected void onRayEntityContact(ServerPlayer shooter, LivingEntity hit, boolean acceptedHit)
    {
    }

    protected void onRayBlockHit(ServerPlayer shooter, BlockHitResult block)
    {
    }

    protected void onRayEntityHit(ServerPlayer shooter, LivingEntity hit)
    {
        String id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(this).getPath();
        if (id.equals("cold_gun"))
        {
            hit.setTicksFrozen(Math.min(hit.getTicksFrozen() + 120, hit.getTicksRequiredToFreeze()));
        }
        else if (id.equals("heat_gun"))
        {
            hit.setSecondsOnFire(6);
        }
        else if (id.equals("chronos_rifle"))
        {
            hit.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
        }
    }

    /** Reload starts from the hero keybind and restores the magazine with a visible reload timer. */
    public void reload(Player player, Hero hero)
    {
        ItemStack stack = player.getMainHandItem();
        int magazine = getMagazineSize(stack);
        if (!usesAmmo || stack.getItem() != this || !hasPermission(player, hero)
                || getAmmo(stack) >= magazine || player.getPersistentData().getLong(RELOAD_END_TAG) > player.level().getGameTime())
        {
            return;
        }

        stack.getOrCreateTag().putInt(AMMO_TAG, magazine);
        player.getPersistentData().putLong(RELOAD_END_TAG, player.level().getGameTime() + reloadTicks);
        player.getPersistentData().putInt(RELOAD_DURATION_TAG, reloadTicks);
        SHPlayerData data = SHDataCapabilities.getPlayer(player);
        if (data != null) data.getData().set(Vars.RELOAD_TIMER, 1.0F);
        SHSounds.play(player, FiskHeroes.id(isDual(stack) ? "item.gun.reload.dual" : "item.gun.reload"),
                SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    protected boolean hasPermission(Player player, Hero hero)
    {
        String path = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(this).getPath();
        String permission = switch (path)
        {
            case "chronos_rifle" -> "USE_CHRONOS_RIFLE";
            case "rip_hunters_gun" -> "USE_RIPS_GUN";
            case "cold_gun" -> "USE_COLD_GUN";
            case "heat_gun" -> "USE_HEAT_GUN";
            default -> "USE_GUN";
        };
        return hero.hasPermission(player, permission);
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
