package com.fiskmods.heroes.common.item;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.data.var.Vars;
import com.fiskmods.heroes.common.hero.HeroTracker;
import com.fiskmods.heroes.common.hero.ability.AbilityHandler;
import com.fiskmods.heroes.common.util.ProtectedBlockChanges;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.entity.projectile.ColdBallEntity;
import com.fiskmods.heroes.common.sound.SHSounds;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Captain Cold's Cold Gun ported from 1.7.10 ItemColdGun:
 * toggles between Cold Beam and Cryo Grenade modes (Sneak + Right-Click), freezes blocks/targets,
 * and uses 3D ModelColdGun + glowing lights layer.
 */
public final class ItemColdGun extends ItemGun
{
    private static final String TAG_GRENADE_MODE = "GrenadeMode";
    private static final String PLAYER_COOLDOWN_TAG = "FiskColdGunCooldownUntil";
    private static final int REQUEST_TIMEOUT = 20;
    private static final int CHARGE_TICKS = 10;
    private static final int MAX_USE_TICKS = 611;
    private static final Map<UUID, BeamRequest> REQUESTS = new HashMap<>();

    private static final class BeamRequest
    {
        final ItemStack stack;
        long expires;
        long lastTick = Long.MIN_VALUE;
        int ticks;
        boolean exhausted;
        BeamRequest(ItemStack stack, long expires) { this.stack = stack; this.expires = expires; }
    }


    public ItemColdGun(int magazineSize, int cooldownTicks, int reloadTicks, double range, float damage, Properties properties)
    {
        super(magazineSize, cooldownTicks, reloadTicks, range, damage, false, properties);
    }

    public static boolean isGrenadeMode(ItemStack stack)
    {
        return stack.hasTag() && stack.getTag().getBoolean(TAG_GRENADE_MODE);
    }

    @Override
    public int getShotCooldownTicks(ItemStack stack)
    {
        return isGrenadeMode(stack) ? 20 : cooldownTicks;
    }

    @Override
    public int getRequestCooldownTicks(ItemStack stack)
    {
        // Retry quickly while aim raises; the actual grenade cadence is server-side.
        return 4;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown())
        {
            if (!level.isClientSide)
            {
                HeroTracker.update(player);
                var hero = HeroTracker.getHeroType(player);
                var data = SHDataCapabilities.getPlayer(player);
                if (hand != InteractionHand.MAIN_HAND || hero == null || data == null
                        || !hasPermission(player, hero) || data.getData().get(Vars.AIMING)
                        || data.getData().get(Vars.RECOIL) > 0.0F) return InteractionResultHolder.fail(stack);
                stopBeam(player);
                boolean next = !isGrenadeMode(stack);
                stack.getOrCreateTag().putBoolean(TAG_GRENADE_MODE, next);
                SHSounds.play(player, FiskHeroes.id("item.coldgun.aim"), SoundSource.PLAYERS, 1.0F, 1.0F);
                Component modeName = Component.translatable(next ? "tooltip.cold_gun.mode.grenade" : "tooltip.cold_gun.mode.beam");
                player.displayClientMessage(Component.translatable("tooltip.mode", modeName), true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return super.use(level, player, hand);
    }

    /** Fire packets are held-input heartbeats, never client-directed raycasts. */
    @Override
    public boolean fireFromAttack(Player player, ItemStack stack)
    {
        if (!(player instanceof ServerPlayer serverPlayer) || player.level().isClientSide
                || !player.isAlive() || player.isSpectator() || stack.isEmpty()
                || stack.getItem() != this || player.getMainHandItem() != stack) return false;
        HeroTracker.update(player);
        var hero = HeroTracker.getHeroType(player);
        var data = SHDataCapabilities.getPlayer(player);
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
        if (!AbilityHandler.isKeyPressed(serverPlayer, "AIM") || !data.getData().get(Vars.AIMING)
                || data.getData().get(Vars.AIMING_TIMER) < 1.0F || data.getData().get(Vars.RECOIL) > 0.0F)
            return false;
        long now = player.level().getGameTime();
        if (player.getPersistentData().getLong(PLAYER_COOLDOWN_TAG) > now) return false;
        if (isGrenadeMode(stack))
        {
            boolean fired = super.fireFromAttack(player, stack);
            if (fired) player.getPersistentData().putLong(PLAYER_COOLDOWN_TAG, now + 20);
            return fired;
        }
        BeamRequest request = REQUESTS.get(player.getUUID());
        if (request != null && request.stack != stack)
        {
            stopBeam(player);
            return false;
        }
        long expires = now + REQUEST_TIMEOUT;
        if (request == null)
        {
            REQUESTS.put(player.getUUID(), new BeamRequest(stack, expires));
            SHSounds.play(player, FiskHeroes.id("item.coldgun.beam.start"), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        else request.expires = expires;
        return true;
    }

    public static void tickBeam(ServerPlayer player)
    {
        BeamRequest request = REQUESTS.get(player.getUUID());
        if (request == null) return;
        long now = player.level().getGameTime();
        var data = SHDataCapabilities.getPlayer(player);
        var hero = HeroTracker.getHeroType(player);
        if (!player.isAlive() || player.isSpectator() || now >= request.expires
                || player.getMainHandItem() != request.stack
                || !(request.stack.getItem() instanceof ItemColdGun gun) || isGrenadeMode(request.stack)
                || hero == null || data == null || !gun.hasPermission(player, hero)
                || !AbilityHandler.isKeyPressed(player, "AIM") || !data.getData().get(Vars.AIMING)
                || data.getData().get(Vars.AIMING_TIMER) < 1.0F || data.getData().get(Vars.RECOIL) > 0.0F)
        {
            stopBeam(player);
            return;
        }
        if (request.lastTick == now || request.exhausted) return;
        request.lastTick = now;
        if (++request.ticks > MAX_USE_TICKS)
        {
            request.exhausted = true;
            player.getPersistentData().putLong(PLAYER_COOLDOWN_TAG, now + 4);
            SHSounds.play(player, FiskHeroes.id("item.coldgun.beam.stop"), SoundSource.PLAYERS, 1.0F, 1.0F);
            return;
        }
        if (request.ticks > CHARGE_TICKS)
        {
            float intensity = Math.min((request.ticks - CHARGE_TICKS) / 5.0F, 1.0F);
            gun.fireRay(player, request.stack, gun.range * intensity);
        }
    }

    public static void stopBeam(Player player)
    {
        BeamRequest request = REQUESTS.remove(player.getUUID());
        if (request != null && !request.exhausted)
        {
            player.getPersistentData().putLong(PLAYER_COOLDOWN_TAG, player.level().getGameTime() + 4);
            SHSounds.play(player, FiskHeroes.id("item.coldgun.beam.stop"), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    public static void clearBeam(Player player) { REQUESTS.remove(player.getUUID()); }

    @Override
    protected void playShotSound(Player player, Level level, ItemStack stack)
    {
        if (isGrenadeMode(stack))
        {
            SHSounds.play(player, FiskHeroes.id("item.coldgun.grenade"), SoundSource.PLAYERS, 2.0F, 1.0F);
        }
        else
        {
            SHSounds.play(player, FiskHeroes.id("item.coldgun.beam.start"), SoundSource.PLAYERS, 2.0F, 1.0F);
        }
    }

    @Override
    protected void fire(ServerPlayer shooter, ItemStack stack)
    {
        if (isGrenadeMode(stack))
        {
            shooter.level().addFreshEntity(new ColdBallEntity(shooter));
            return;
        }
        super.fire(shooter, stack);
    }

    @Override
    protected void onRayBlockHit(ServerPlayer shooter, BlockHitResult block)
    {
        if (block.getType() == HitResult.Type.MISS) return;
        Level level = shooter.level();
        BlockPos pos = block.getBlockPos();
        if (!com.fiskmods.heroes.common.config.SHConfig.ALLOW_GRIEFING.get()
                || !level.getWorldBorder().isWithinBounds(pos) || !level.mayInteract(shooter, pos)
                || !shooter.mayUseItemAt(pos, block.getDirection(), shooter.getMainHandItem())) return;
        BlockState state = level.getBlockState(pos);
        if (state.is(Blocks.WATER) || state.is(Blocks.LAVA))
        {
            if (ProtectedBlockChanges.canBreak(shooter, pos, state))
                ProtectedBlockChanges.placeBlock(shooter, pos, block.getDirection(),
                        (state.is(Blocks.WATER) ? Blocks.ICE : Blocks.OBSIDIAN).defaultBlockState());
            return;
        }
        BlockPos adj = pos.relative(block.getDirection());
        if (!level.getWorldBorder().isWithinBounds(adj) || !level.mayInteract(shooter, adj)
                || !shooter.mayUseItemAt(adj, block.getDirection(), shooter.getMainHandItem())) return;
        BlockState adjacent = level.getBlockState(adj);
        if (adjacent.is(Blocks.FIRE))
        {
            if (ProtectedBlockChanges.canBreak(shooter, adj, adjacent)) level.removeBlock(adj, false);
        }
        else if (level.isEmptyBlock(adj) && Blocks.SNOW.defaultBlockState().canSurvive(level, adj)
                && level.getEntities(null, new net.minecraft.world.phys.AABB(adj)).isEmpty())
        {
            ProtectedBlockChanges.placeBlock(shooter, adj, block.getDirection(), Blocks.SNOW.defaultBlockState());
        }
    }

    @Override
    protected void onRayEntityContact(ServerPlayer shooter, LivingEntity hit, boolean acceptedHit)
    {
        // Vanilla removes two frozen ticks per tick outside powder snow. Add those back
        // during contact, plus one gradual exposure tick, so sustained fire accumulates.
        // The original has a separate body-temperature system; this remains an approximation.
        float exposure = 1.0F;
        var data = SHDataCapabilities.getPlayer(hit);
        if (data != null && data.getHeroType() != null)
        {
            var resistance = data.getHeroType().getPowerContainer().getEntry(FiskHeroes.id("damage_resistance"));
            if (resistance != null && resistance.isEnabled() && resistance.isModifierEnabled(hit, data)
                    && "COLD".equalsIgnoreCase(resistance.get(com.fiskmods.heroes.common.hero.power.PowerProperty.DAMAGE_TYPE)))
                exposure = net.minecraft.util.Mth.clamp(1.0F - resistance.getFloat(hit,
                        com.fiskmods.heroes.common.hero.power.PowerProperty.FACTOR), 0.0F, 1.0F);
        }
        if (exposure <= 0.0F) return;
        final String remainderKey = "FiskColdBeamRemainder";
        float amount = 2.0F + exposure + hit.getPersistentData().getFloat(remainderKey);
        int ticks = (int) amount;
        hit.getPersistentData().putFloat(remainderKey, amount - ticks);
        hit.setTicksFrozen(Math.min(hit.getTicksFrozen() + ticks, hit.getTicksRequiredToFreeze() + 40));
        MobEffectInstance slow = hit.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
        // Avoid broadcasting a redundant effect update on every beam tick, and
        // do not replace a stronger slowdown supplied by another source.
        if (slow == null || slow.getAmplifier() == 0 && slow.getDuration() < 20)
            hit.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0));
    }

    @Override
    protected void onRayEntityHit(ServerPlayer shooter, LivingEntity hit)
    {
        // The gradual contact effect above also applies to invulnerable targets; do not
        // run ItemGun's legacy instant-freeze fallback on accepted damage.
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag)
    {
        super.appendHoverText(stack, level, tooltip, flag);
        Component modeName = Component.translatable(isGrenadeMode(stack)
                ? "tooltip.cold_gun.mode.grenade"
                : "tooltip.cold_gun.mode.beam").withStyle(ChatFormatting.AQUA);
        tooltip.add(Component.translatable("tooltip.mode", modeName).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer)
    {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions()
        {
            @Override
            public boolean applyForgeHandTransform(com.mojang.blaze3d.vertex.PoseStack pose,
                    net.minecraft.client.player.LocalPlayer player, net.minecraft.world.entity.HumanoidArm arm,
                    ItemStack stack, float partialTick, float equip, float swing)
            {
                com.fiskmods.heroes.client.render.ColdGunAimPose.apply(pose, player, arm, partialTick);
                return false;
            }

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer()
            {
                return com.fiskmods.heroes.client.render.ColdGunItemRenderer.getInstance();
            }
        });
    }
}
