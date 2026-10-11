package com.fiskmods.heroes.common.network;

import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.data.var.Vars;
import com.fiskmods.heroes.common.hero.Hero;
import com.fiskmods.heroes.common.hero.modifier.ModifierShapeShifting;

import com.mojang.authlib.GameProfile;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** Client request to begin or reset a hero's name-based disguise. */
public final class PacketSetDisguise extends SHPacket
{
    private static final Map<UUID, Long> REQUESTS = new ConcurrentHashMap<>();
    private final String name;

    public PacketSetDisguise(String name)
    {
        this.name = name == null ? "" : name;
    }

    public PacketSetDisguise(FriendlyByteBuf buf)
    {
        this.name = buf.readUtf(16);
    }

    @Override
    public void encode(FriendlyByteBuf buf)
    {
        buf.writeUtf(name, 16);
    }

    @Override
    public void handle(NetworkEvent.Context context)
    {
        ServerPlayer player = context.getSender();
        if (player == null || name.length() > 16 || !name.matches("[A-Za-z0-9_]*")) return;

        SHPlayerData data = SHDataCapabilities.getPlayer(player);
        Hero hero = data != null ? data.getHeroType() : null;
        if (hero == null || !canShapeShift(player, data, hero)) return;

        MinecraftServer server = player.getServer();
        if (server == null) return;
        long request = REQUESTS.merge(player.getUUID(), 1L, Long::sum);
        if (name.isEmpty())
        {
            apply(player, "", null);
            REQUESTS.remove(player.getUUID());
            return;
        }

        server.getProfileCache().getAsync(name, result -> server.execute(() ->
        {
            if (REQUESTS.getOrDefault(player.getUUID(), -1L) == request)
            {
                if (result.isPresent() && player.isAlive() && !player.isRemoved())
                {
                    apply(player, name, result.get());
                }
                REQUESTS.remove(player.getUUID(), request);
            }
        }));
    }

    private static boolean canShapeShift(ServerPlayer player, SHPlayerData data, Hero hero)
    {
        return data != null && hero.hasKeyBind("SHAPE_SHIFT") && hero.hasKeyBind("SHAPE_SHIFT_RESET")
                && hero.isKeyBindEnabled(player, "SHAPE_SHIFT")
                && hero.getPowerContainer().getEntries().stream()
                        .anyMatch(entry -> entry.getModifier() instanceof ModifierShapeShifting
                                && entry.isEnabled() && entry.isModifierEnabled(player, data));
    }

    private static void apply(ServerPlayer player, String name, GameProfile profile)
    {
        SHPlayerData data = SHDataCapabilities.getPlayer(player);
        Hero hero = data != null ? data.getHeroType() : null;
        if (hero == null || !canShapeShift(player, data, hero)) return;

        String current = data.getData().get(Vars.DISGUISE);
        data.getData().set(Vars.SHAPE_SHIFTING_FROM, current == null ? "" : current);
        data.getData().set(Vars.SHAPE_SHIFTING_TO, name);
        data.getData().set(Vars.SHAPE_SHIFT_TIMER, 1.0F);
        // The original transition starts at 1 and counts down through its midpoint.
        data.getData().set(Vars.SHAPE_SHIFTING, false);
        data.getData().set(Vars.SHAPE_SHIFTING_TO_UUID,
                profile != null && profile.getId() != null ? profile.getId().toString() : "");
    }
}
