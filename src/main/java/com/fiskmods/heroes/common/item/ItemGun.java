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
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.List;

/** Basic magazine firearm used by the bundled firearm heroes. */
public class ItemGun extends Item
{
    private static final String AMMO_TAG = "Ammo";
    private static final String NEXT_SHOT_TAG = "NextShotTick";
    private static final String NEXT_NOTICE_TAG = "FiskHeroesGunNoticeTick";
    public static final String RELOAD_END_TAG = "fiskheroes_gun_reload_end";
    private static final String RELOAD_DURATION_TAG = "fiskheroes_gun_reload_duration";
    private final int magazineSize;
    private final int cooldownTicks;
    private final int reloadTicks;
    private final double range;
    private final float damage;
    private final boolean usesAmmo;

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

    public int getAmmo(ItemStack stack)
    {
        if (!usesAmmo) return Integer.MAX_VALUE;
        return stack.hasTag() && stack.getTag().contains(AMMO_TAG)
                ? Math.min(stack.getTag().getInt(AMMO_TAG), getMagazineSize(stack)) : getMagazineSize(stack);
    }

    public int getMagazineSize()
    {
        return magazineSize;
    }

    public int getMagazineSize(ItemStack stack)
    {
        return magazineSize * (isDual(stack) ? 2 : 1);
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

    public static boolean isDual(ItemStack stack)
    {
        return stack.hasTag() && stack.getTag().getBoolean("Dual");
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

        HeroIteration iteration = HeroTracker.getHero(player);
        Hero hero = iteration != null ? iteration.getHero() : null;
        SHPlayerData data = SHDataCapabilities.getPlayer(player);
        // AIM is a presentation/input state, not a prerequisite for firing. Several pack heroes
        // bind AIM to the primary attack key, which leaves ordinary right-click use otherwise
        // silently doing nothing unless the player holds two different mouse buttons at once.
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
        if (data.getData().get(Vars.RELOAD_TIMER) > 0.0F)
        {
            return false;
        }

        long now = level.getGameTime();
        int shotCooldown = getShotCooldownTicks(stack);
        if (stack.getOrCreateTag().getLong(NEXT_SHOT_TAG) > now)
        {
            return false;
        }
        stack.getOrCreateTag().putLong(NEXT_SHOT_TAG, now + shotCooldown);

        int ammo = getAmmo(stack);
        if (ammo <= 0)
        {
            player.playSound(SoundEvents.DISPENSER_FAIL, 0.8F, 0.8F + level.random.nextFloat() * 0.3F);
            showUseNotice(player, "message.fiskheroes.gun.empty");
            return false;
        }

        // Dual models represent two pistols, but the original alternates the hand/shot; it does
        // not discharge both rounds at once. Keep the larger combined magazine and cadence while
        // consuming one round per attack.
        if (usesAmmo) stack.getOrCreateTag().putInt(AMMO_TAG, ammo - 1);
        if (player instanceof ServerPlayer serverPlayer)
        {
            fire(serverPlayer);
        }
        player.playSound(SoundEvents.CROSSBOW_SHOOT, 1.0F, 0.9F + level.random.nextFloat() * 0.2F);
        player.awardStat(Stats.ITEM_USED.get(this));
        return true;
    }

    /** Explain the common silent failure cases without spamming while primary attack is held. */
    private static void showUseNotice(Player player, String translationKey)
    {
        long now = player.level().getGameTime();
        if (player.getPersistentData().getLong(NEXT_NOTICE_TAG) > now) return;
        player.getPersistentData().putLong(NEXT_NOTICE_TAG, now + 20L);
        player.displayClientMessage(Component.translatable(translationKey), true);
    }

    private void fire(ServerPlayer shooter)
    {
        Level level = shooter.level();
        String id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(this).getPath();

        // These two weapons are projectile launchers in the original pack, not hitscan guns.
        // Keep their energy bolt collision, impact and explosion behavior on the registered
        // projectile path; aiming the Chronos Rifle grants the original scoped damage bonus.
        if (id.equals("chronos_rifle") || id.equals("rip_hunters_gun"))
        {
            com.google.gson.JsonObject profile = new com.google.gson.JsonObject();
            profile.addProperty("damage", damage);
            com.google.gson.JsonObject types = new com.google.gson.JsonObject();
            types.addProperty("BULLET", 1.0D);
            profile.add("types", types);

            SHPlayerData data = SHDataCapabilities.getPlayer(shooter);
            boolean scoped = id.equals("chronos_rifle") && data != null
                    && data.getData().get(Vars.AIMING);
            var bolt = new com.fiskmods.heroes.common.entity.projectile.EnergyBoltEntity(
                    shooter, profile, damage * (scoped ? 1.6F : 1.0F), id.equals("chronos_rifle"), 4.0F, 0.0F);
            level.addFreshEntity(bolt);
            return;
        }

        Vec3 start = shooter.getEyePosition();
        Vec3 direction = shooter.getViewVector(1.0F);
        Vec3 end = start.add(direction.scale(range));
        BlockHitResult block = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, shooter));
        if (block.getType() != HitResult.Type.MISS) end = block.getLocation();

        // The original guns render a moving bullet ray/trail. The 1.20 port resolves damage on
        // the server, so show a short, server-synchronized tracer along that same clipped ray;
        // otherwise a valid miss looks exactly like a broken weapon to the player.
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel)
        {
            net.minecraft.core.particles.ParticleOptions tracer = net.minecraftforge.registries.ForgeRegistries.ITEMS
                    .getKey(this).getPath().equals("cold_gun")
                    ? net.minecraft.core.particles.ParticleTypes.SNOWFLAKE
                    : new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(1.0F, 0.72F, 0.2F), 0.7F);
            double distance = start.distanceTo(end);
            int points = Math.min(16, Math.max(1, (int) Math.ceil(distance / 4.0D)));
            for (int i = 1; i <= points; ++i)
            {
                Vec3 point = start.lerp(end, i / (double) (points + 1));
                serverLevel.sendParticles(tracer, point.x, point.y, point.z, 1, 0.015D, 0.015D, 0.015D, 0.0D);
            }
        }

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

        if (id.equals("cold_gun"))
        {
            SHPlayerData data = SHDataCapabilities.getPlayer(shooter);
            if (data != null)
            {
                double length = hit != null ? Math.sqrt(nearest) : start.distanceTo(end);
                data.getData().set(Vars.HEAT_VISION_LENGTH, Math.min(range, length));
                data.getData().set(Vars.ENERGY_PROJECTION_TIMER, 1.0F);
            }
        }

        boolean acceptedHit = false;
        if (hit != null)
        {
            // Hitscan bullets still need projectile damage semantics: hero immunities and
            // resistances inspect both the DamageType tags and the active original profile.
            var damageType = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                    .getHolderOrThrow(net.minecraft.world.damagesource.DamageTypes.ARROW);
            var source = new net.minecraft.world.damagesource.DamageSource(damageType, shooter, shooter);
            LivingEntity target = hit;
            final boolean[] applied = { false };
            com.fiskmods.heroes.common.hero.modifier.DamageGroups.withDamageProfile(
                    java.util.Map.of("BULLET", 1.0D), () -> applied[0] = target.hurt(source, damage));
            acceptedHit = applied[0];
        }

        if (acceptedHit)
        {
            if (id.equals("cold_gun"))
            {
                hit.setTicksFrozen(Math.min(hit.getTicksFrozen() + 100, hit.getTicksRequiredToFreeze()));
            }
            else if (id.equals("heat_gun"))
            {
                hit.setSecondsOnFire(5);
            }
            else if (id.equals("chronos_rifle"))
            {
                hit.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            }
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
        player.playSound(SoundEvents.ARMOR_EQUIP_IRON, 0.7F, 1.25F);
    }

    private boolean hasPermission(Player player, Hero hero)
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
