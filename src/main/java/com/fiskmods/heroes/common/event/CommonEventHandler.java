package com.fiskmods.heroes.common.event;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.command.CommandHero;
import com.fiskmods.heroes.common.command.CommandSuit;
import com.fiskmods.heroes.common.data.DataSyncer;
import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.hero.HeroTracker;
import com.fiskmods.heroes.common.hero.ability.ModifierHandler;
import com.fiskmods.heroes.common.hero.attribute.SHAttributes;
import com.fiskmods.heroes.common.hero.equipment.EquipmentHelper;
import com.fiskmods.heroes.common.hero.power.ModifierEntry;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * The mod's gameplay event handler: suit tracking, attributes, modifier ticks, damage handling and
 * the client/server data synchronization.
 */
public class CommonEventHandler
{
    private static final String CACTUS_COOLDOWN_UNTIL = "FiskHeroesCactusSummonUntil";

    /** Install the current suit bonus before vanilla computes attack cooldown and critical damage. */
    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public static void onAttackEntity(net.minecraftforge.event.entity.player.AttackEntityEvent event)
    {
        if (com.fiskmods.heroes.common.item.ItemGun.isGun(event.getEntity().getMainHandItem()))
        {
            event.setCanceled(true);
            return;
        }
        com.fiskmods.heroes.common.hero.ability.MeleeCombat.prepareAttack(event.getEntity());
    }

    /** The server also rejects vanilla mining while a firearm occupies the main hand. */
    @SubscribeEvent
    public static void onGunMine(PlayerInteractEvent.LeftClickBlock event)
    {
        if (event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.ABORT
                && com.fiskmods.heroes.common.item.ItemGun.isGun(event.getEntity().getMainHandItem()))
            event.setCanceled(true);
    }

    /** Uses the selected belt gadget when the player's hand is empty. */
    @SubscribeEvent
    public static void onEquipmentUse(PlayerInteractEvent.RightClickItem event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND
                || !event.getItemStack().isEmpty()) return;
        if (EquipmentHelper.useUtilityBelt(player))
        {
            event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    /** Recruits the cactus column being pointed at while the hero's AIM ability is held. */
    @SubscribeEvent
    public static void onCactusRecruitment(PlayerInteractEvent.RightClickBlock event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.isShiftKeyDown() || event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND
                || !player.getMainHandItem().isEmpty()) return;

        var iteration = HeroTracker.getHero(player);
        if (iteration == null || !com.fiskmods.heroes.common.hero.ability.AbilityHandler.isKeyPressed(player, "AIM")) return;

        var hero = iteration.getHero();
        var data = SHDataCapabilities.getPlayer(player);
        if (data == null) return;

        com.fiskmods.heroes.common.hero.power.ModifierEntry entry = hero.getPowerContainer().getEntries().stream()
                .filter(candidate -> candidate.getModifier().getId().equals(com.fiskmods.heroes.FiskHeroes.id("cactus_recruitment")))
                .filter(candidate -> candidate.isEnabled() && candidate.isModifierEnabled(player, data))
                .findFirst().orElse(null);
        if (entry == null) return;

        float reach = Math.max(1.0F, entry.getFloat(player, com.fiskmods.heroes.common.hero.power.PowerProperty.RANGE));
        var hit = player.pick(reach, 0.0F, false);
        if (!(hit instanceof net.minecraft.world.phys.BlockHitResult blockHit)
                || !player.level().getBlockState(blockHit.getBlockPos()).is(net.minecraft.world.level.block.Blocks.CACTUS)) return;

        long now = player.level().getGameTime();
        if (player.getPersistentData().getLong(CACTUS_COOLDOWN_UNTIL) > now) return;

        var level = (net.minecraft.server.level.ServerLevel) player.level();
        if (!recruitCactusColumn(level, player, blockHit.getBlockPos())) return;

        com.fiskmods.heroes.common.hero.modifier.AbilityData.playSound(player, entry, "RECRUIT");
        int cooldown = Math.max(1, Math.round(entry.getInt(player, com.fiskmods.heroes.common.hero.power.PowerProperty.COOLDOWN_TIME)
                * (float) com.fiskmods.heroes.common.config.SHConfig.cooldown(1.0D)));
        player.getPersistentData().putLong(CACTUS_COOLDOWN_UNTIL, now + cooldown);
        event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    /** Preflight every block and entity spawn before consuming a cactus column. */
    public static boolean recruitCactusColumn(net.minecraft.server.level.ServerLevel level, ServerPlayer player,
            net.minecraft.core.BlockPos target)
    {
        if (!level.getBlockState(target).is(net.minecraft.world.level.block.Blocks.CACTUS)) return false;
        int bottom = target.getY(), top = bottom;
        while (bottom > level.getMinBuildHeight()
                && level.getBlockState(target.atY(bottom - 1)).is(net.minecraft.world.level.block.Blocks.CACTUS)) --bottom;
        while (top + 1 < level.getMaxBuildHeight()
                && level.getBlockState(target.atY(top + 1)).is(net.minecraft.world.level.block.Blocks.CACTUS)) ++top;

        var states = new java.util.ArrayList<net.minecraft.world.level.block.state.BlockState>();
        for (int y = bottom; y <= top; ++y)
        {
            var pos = target.atY(y);
            if (!level.getWorldBorder().isWithinBounds(pos) || !level.mayInteract(player, pos)) return false;
            var event = new net.minecraftforge.event.level.BlockEvent.BreakEvent(level, pos, level.getBlockState(pos), player);
            if (net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event)) return false;
            states.add(event.getState());
        }
        for (int y = bottom; y <= top; ++y)
            if (!level.getBlockState(target.atY(y)).equals(states.get(y - bottom))) return false;

        var minion = com.fiskmods.heroes.common.entity.ModEntities.CACTUS_MINION.get().create(level);
        if (minion == null) return false;
        minion.setCactusSize(top - bottom + 1);
        minion.moveTo(target.getX() + 0.5D, bottom, target.getZ() + 0.5D, player.getYRot(), 0.0F);
        if (!level.addFreshEntity(minion)) return false;
        for (int y = bottom; y <= top; ++y)
        {
            if (!level.getBlockState(target.atY(y)).equals(states.get(y - bottom)))
            {
                minion.discard();
                return false;
            }
        }
        for (int y = top; y >= bottom; --y)
        {
            var pos = target.atY(y);
            if (!level.getBlockState(pos).equals(states.get(y - bottom)) || !level.removeBlock(pos, false))
            {
                // Roll back already-consumed blocks if another event changed the world during spawn.
                for (int restore = y + 1; restore <= top; ++restore)
                    level.setBlock(target.atY(restore), states.get(restore - bottom), 2);
                minion.discard();
                return false;
            }
        }
        return true;
    }

    /** LivingTick fires inside Player.tick, after vanilla overwrites noPhysics with spectator mode. */
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event)
    {
        if (event.getEntity() instanceof net.minecraft.world.entity.player.Player player)
            com.fiskmods.heroes.common.hero.modifier.IntangibilityPhysics.update(player, SHDataCapabilities.getPlayer(player));
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        if (event.phase == TickEvent.Phase.START)
        {
            ModifierHandler.tickPotionEffects(event.player);
            return;
        }

        if (event.phase != TickEvent.Phase.END)
        {
            return;
        }

        LivingEntity entity = event.player;
        if (!entity.level().isClientSide)
            com.fiskmods.heroes.common.hero.ability.MeleeCombat.clearAttackBonus(entity);

        // Snapshot the values so interpolated variables can be rendered smoothly this tick
        if (entity instanceof net.minecraft.world.entity.player.Player)
        {
            com.fiskmods.heroes.common.data.SHPlayerData data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(entity);

            if (data != null)
            {
                data.getData().updatePrevious();
                if (!entity.level().isClientSide)
                {
                    short elapsed = data.getData().get(com.fiskmods.heroes.common.data.var.Vars.TIME_SINCE_DAMAGED);
                    if (elapsed < Short.MAX_VALUE - 1)
                        data.getData().set(com.fiskmods.heroes.common.data.var.Vars.TIME_SINCE_DAMAGED, (short) (elapsed + 1));
                }
                if (entity instanceof net.minecraft.world.entity.player.Player player)
                {
                    com.fiskmods.heroes.common.item.ItemGun.tickReload(player, data);
                }
            }
        }

        HeroTracker.update(entity);
        if (entity instanceof net.minecraft.world.entity.player.Player)
        {
            var data = SHDataCapabilities.getPlayer(entity);
            // Remote clients have no local key snapshot for this player. Their aim state
            // and timers are server-synchronized and must not be overwritten each tick.
            if (data != null && (!entity.level().isClientSide || event.player.isLocalPlayer()))
            {
                com.fiskmods.heroes.common.hero.ability.AbilityHandler.tickAiming(entity, data);
            }
        }
        // Landing/crouching clears manual flight first; suit scripts may deliberately restore it
        // (e.g. Vision's flight while intangible) during their tick handler.
        com.fiskmods.heroes.common.hero.ability.ControlledFlightInputHandler.tick(event.player);
        ModifierHandler.tickTracked(entity);
        com.fiskmods.heroes.common.hero.ability.PlayerSizeHandler.refreshIfChanged(event.player);

        if (entity instanceof ServerPlayer serverPlayer)
        {
            com.fiskmods.heroes.common.hero.ability.AbilityHandler.tickHeldAbilities(serverPlayer);
            com.fiskmods.heroes.common.hero.ability.WebSwingHandler.tick(serverPlayer);
            EquipmentHelper.tickRapidWebs(serverPlayer);
        }

        if (!entity.level().isClientSide)
        {
            SHAttributes.applyModifiers(entity);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END)
        {
            return;
        }

        var server = ServerLifecycleHooks.getCurrentServer();

        if (server == null)
        {
            return;
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers())
        {
            com.fiskmods.heroes.common.hero.ability.FlightKickGuard.tick(player);
            DataSyncer.tick(player);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event)
    {
        if (event.phase == TickEvent.Phase.END && event.level instanceof net.minecraft.server.level.ServerLevel level)
        {
            com.fiskmods.heroes.common.entity.GravityEffectHandler.tick(level);
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(net.minecraftforge.event.level.LevelEvent.Unload event)
    {
        if (event.getLevel() instanceof net.minecraft.server.level.ServerLevel level)
        {
            com.fiskmods.heroes.common.entity.GravityEffectHandler.clear(level);
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event)
    {
        LivingEntity entity = event.getEntity();

        if (entity.level().isClientSide)
        {
            return;
        }

        net.minecraft.world.entity.Entity attackerEntity = event.getSource().getEntity();
        LivingEntity attacker = attackerEntity instanceof LivingEntity living ? living : null;
        boolean hooked = com.fiskmods.heroes.common.hero.ability.MeleeCombat.isDirectMelee(attacker, event.getSource())
                && com.fiskmods.heroes.common.hero.HeroTracker.getHeroType(attacker) != null
                && com.fiskmods.heroes.common.entity.projectile.GrapplingHookEntity.isHooked(attacker, entity);
        double knockback = com.fiskmods.heroes.common.hero.ability.MeleeCombat.extraKnockback(attacker, event.getSource(), hooked);
        float outgoing;
        try
        {
            outgoing = ModifierHandler.modifyOutgoingDamage(attacker, entity, event.getSource(), event.getAmount());
        }
        finally
        {
            if (attacker != null && com.fiskmods.heroes.common.hero.ability.MeleeCombat.isDirectMelee(attacker, event.getSource()))
                com.fiskmods.heroes.common.hero.ability.MeleeCombat.clearAttackBonus(attacker);
        }
        if (hooked) outgoing *= 1.25F;
        float result = ModifierHandler.modifyDamage(entity, event.getSource(), outgoing);
        result *= com.fiskmods.heroes.common.hero.SuitProtection.damageMultiplier(entity, event.getSource());

        if (result <= 0)
        {
            event.setCanceled(true);
            return;
        }

        if (knockback > 0.0D)
        {
            double yaw = Math.toRadians(attacker.getYRot());
            entity.push(-Math.sin(yaw) * knockback * 0.5D, 0.0D, Math.cos(yaw) * knockback * 0.5D);
            entity.hurtMarked = true;
        }
        event.setAmount(result);
    }

    /** Reset only after damage actually reaches the entity, not when an immunity cancels a hit. */
    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public static void onLivingDamage(net.minecraftforge.event.entity.living.LivingDamageEvent event)
    {
        if (event.getAmount() <= 0.0F || event.getEntity().level().isClientSide) return;
        var data = SHDataCapabilities.getPlayer(event.getEntity());
        if (data != null) data.getData().set(com.fiskmods.heroes.common.data.var.Vars.TIME_SINCE_DAMAGED, (short) 0);
    }

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event)
    {
        LivingEntity entity = event.getEntity();

        if (!(entity instanceof net.minecraft.world.entity.player.Player player))
        {
            return;
        }

        var data = SHDataCapabilities.getPlayer(player);
        if (data != null && data.getHeroType() != null)
        {
            var flight = data.getHeroType().getPowerContainer().getEntry(com.fiskmods.heroes.FiskHeroes.id("flight"));
            if (flight != null && flight.isEnabled() && flight.isModifierEnabled(entity, data))
            {
                event.setCanceled(true);
                return;
            }
        }

        // Original ArmorAttribute.FALL_RESISTANCE is inverse: compute the attribute against the
        // fall distance, then subtract its contribution from that distance. Multipliers operate
        // before flat values (1.7's AttributeWrapper), rather than using a percent damage cap.
        double distance = event.getDistance();
        var attribute = entity.getAttribute(SHAttributes.FALL_RESISTANCE.get());
        if (attribute != null)
        {
            double modified = distance;
            for (var modifier : attribute.getModifiers())
                if (modifier.getOperation() != net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION)
                    modified *= 1.0D + modifier.getAmount();
            modified += attribute.getBaseValue();
            for (var modifier : attribute.getModifiers())
                if (modifier.getOperation() == net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION)
                    modified += modifier.getAmount();
            distance = 2.0D * distance - modified;
        }
        if (data != null)
        {
            float scale = data.getData().get(com.fiskmods.heroes.common.data.var.Vars.SCALE);
            if (scale > 1.0F) distance /= scale;
        }
        event.setDistance((float) distance);
    }

    @SubscribeEvent
    public static void onLivingJump(LivingEvent.LivingJumpEvent event)
    {
        ModifierHandler.onJump(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer player)
        {
            DataSyncer.onPlayerLogin(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer player)
        {
            DataSyncer.onPlayerLogout(player);
            EquipmentHelper.clearRapidWebs(player);
            com.fiskmods.heroes.common.hero.ability.FlightKickGuard.clear(player);
            com.fiskmods.heroes.common.data.PlayerInputTracker.clear(player);
            com.fiskmods.heroes.common.network.PacketInput.clearServerState(player);
            com.fiskmods.heroes.common.hero.ability.AbilityHandler.clear(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerStartTracking(PlayerEvent.StartTracking event)
    {
        if (event.getEntity() instanceof ServerPlayer tracker
                && event.getTarget() instanceof ServerPlayer subject)
        {
            DataSyncer.sendFullSyncTo(tracker, subject);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer player)
        {
            HeroTracker.update(player);

            com.fiskmods.heroes.common.data.SHPlayerData data = SHDataCapabilities.getPlayer(player);

            if (data != null)
            {
                DataSyncer.sendFullSync(player);

                if (data.getHeroType() != null)
                {
                    for (ModifierEntry entry : data.getHeroType().getPowerContainer().getEntries())
                    {
                        entry.getModifier().onRespawn(player, entry, data);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer player)
        {
            HeroTracker.update(player);
            DataSyncer.sendFullSync(player);
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event)
    {
        CommandSuit.register(event.getDispatcher());
        CommandHero.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onServerStarted(net.minecraftforge.event.server.ServerStartedEvent event)
    {
        FiskHeroes.LOGGER.info("Fisk's Superheroes ready: {} heroes registered", com.fiskmods.heroes.common.hero.Hero.REGISTRY.size());
    }
}
