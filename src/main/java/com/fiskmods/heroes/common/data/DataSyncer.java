package com.fiskmods.heroes.common.data;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.fiskmods.heroes.common.data.var.DataContainer;
import com.fiskmods.heroes.common.hero.HeroTracker;
import com.fiskmods.heroes.common.network.PacketSyncData;
import com.fiskmods.heroes.common.network.PacketSyncSuit;
import com.fiskmods.heroes.common.network.PacketSyncToggles;
import com.fiskmods.heroes.common.network.SHNetwork;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Pushes dirty data variables and suit changes to the clients tracking each player. Only values
 * that actually changed since the previous synchronization are sent.
 */
public final class DataSyncer
{
    private static final Map<UUID, Integer> LAST_HERO = new HashMap<>();
    private static final Map<UUID, java.util.Set<ResourceLocation>> LAST_TOGGLES = new HashMap<>();

    private DataSyncer()
    {
    }

    public static void tick(ServerPlayer player)
    {
        SHPlayerData data = SHDataCapabilities.getPlayer(player);

        if (data == null)
        {
            return;
        }

        DataContainer container = data.getData();

        if (!container.getDirty().isEmpty())
        {
            CompoundTag tag = container.writeDirty();
            container.clearDirty();

            if (!tag.isEmpty())
            {
                SHNetwork.sendToTracking(new PacketSyncData(player.getId(), tag), player);
            }
        }

        int heroHash = data.getHero() != null ? data.getHero().hashCode() : 0;
        Integer prev = LAST_HERO.get(player.getUUID());

        if (prev == null || prev != heroHash)
        {
            LAST_HERO.put(player.getUUID(), heroHash);
            SHNetwork.sendToTracking(new PacketSyncSuit(player.getId(), data.getHero()), player);
        }

        java.util.Set<ResourceLocation> toggles = data.getEnabledToggles();
        java.util.Set<ResourceLocation> previousToggles = LAST_TOGGLES.put(player.getUUID(), toggles);
        if (!toggles.equals(previousToggles))
        {
            sendTogglesToTracking(player, toggles);
        }
    }

    public static void sendFullSync(ServerPlayer player)
    {
        SHPlayerData data = SHDataCapabilities.getPlayer(player);

        if (data != null)
        {
            CompoundTag tag = new CompoundTag();
            data.getData().writeTo(tag);
            SHNetwork.sendToPlayer(new PacketSyncData(player.getId(), tag), player);
            SHNetwork.sendToPlayer(new PacketSyncSuit(player.getId(), data.getHero()), player);
            SHNetwork.sendToPlayer(new PacketSyncToggles(player.getId(), data.getEnabledToggles()), player);
        }
    }

    public static void sendTogglesToTracking(ServerPlayer subject)
    {
        SHPlayerData data = SHDataCapabilities.getPlayer(subject);
        if (data != null)
        {
            sendTogglesToTracking(subject, data.getEnabledToggles());
        }
    }

    public static void sendTogglesTo(ServerPlayer recipient, ServerPlayer subject)
    {
        SHPlayerData data = SHDataCapabilities.getPlayer(subject);
        if (data != null)
        {
            SHNetwork.sendToPlayer(new PacketSyncToggles(subject.getId(), data.getEnabledToggles()), recipient);
        }
    }

    private static void sendTogglesToTracking(ServerPlayer subject, java.util.Set<ResourceLocation> toggles)
    {
        SHNetwork.sendToTracking(new PacketSyncToggles(subject.getId(), toggles), subject);
    }

    public static void onPlayerLogout(ServerPlayer player)
    {
        LAST_HERO.remove(player.getUUID());
        LAST_TOGGLES.remove(player.getUUID());
    }

    public static void onPlayerLogin(ServerPlayer player)
    {
        HeroTracker.update(player);
        sendFullSync(player);
    }
}
