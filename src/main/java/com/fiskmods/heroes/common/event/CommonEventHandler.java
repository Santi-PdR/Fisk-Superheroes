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
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * The mod's gameplay event handler: suit tracking, attributes, modifier ticks, damage handling and
 * the client/server data synchronization.
 */
public class CommonEventHandler
{
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof LivingEntity entity))
        {
            return;
        }

        HeroTracker.update(entity);
        ModifierHandler.tick(entity);

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
            DataSyncer.tick(player);
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

        float result = ModifierHandler.modifyDamage(entity, event.getSource(), event.getAmount());

        if (result <= 0)
        {
            event.setCanceled(true);
            return;
        }

        event.setAmount(result);
    }

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event)
    {
        LivingEntity entity = event.getEntity();

        if (!(entity instanceof net.minecraft.world.entity.player.Player player))
        {
            return;
        }

        float resistance = entity.getAttributeValue(SHAttributes.FALL_RESISTANCE.get());
        float immuneTime = resistance * 4.0F;

        if (event.getDistance() <= immuneTime)
        {
            event.setCanceled(true);
            return;
        }

        event.setDamageMultiplier(event.getDamageMultiplier() * Math.max(0.0F, 1.0F - resistance / 10.0F));
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
            EquipmentHelper.grantEquipment(player, SHDataCapabilities.getPlayer(player));
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer player)
        {
            DataSyncer.onPlayerLogout(player);
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
