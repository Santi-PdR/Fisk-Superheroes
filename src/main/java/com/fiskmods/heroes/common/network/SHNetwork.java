package com.fiskmods.heroes.common.network;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.fiskmods.heroes.FiskHeroes;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * The mod's networking channel. Packets are registered explicitly (unlike the reflective
 * auto-discovery of the 1.7.10 version) so both sides always agree on ids.
 */
public class SHNetwork
{
    private static final String PROTOCOL = "2";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(FiskHeroes.MODID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private static int nextId = 0;

    public static <T> void register(Class<T> type, Function<FriendlyByteBuf, T> decoder, BiConsumer<T, Supplier<NetworkEvent.Context>> handler, NetworkDirection direction)
    {
        CHANNEL.registerMessage(nextId++, type, (msg, buf) -> msgEncoder(msg, buf), decoder,
                (msg, ctx) ->
                {
                    NetworkEvent.Context context = ctx.get();
                    context.setPacketHandled(true);
                    context.enqueueWork(() -> handler.accept(msg, ctx));
                },
                Optional.of(direction));
    }

    @SuppressWarnings("unchecked")
    private static <T> void msgEncoder(T msg, FriendlyByteBuf buf)
    {
        if (msg instanceof SHPacket packet)
        {
            packet.encode(buf);
        }
    }

    public static <T extends SHPacket> void registerPacket(Class<T> type, Function<FriendlyByteBuf, T> decoder, NetworkDirection direction)
    {
        register(type, decoder, (msg, ctx) -> msg.handle(ctx.get()), direction);
    }

    /** Client -> server. */
    public static void sendToServer(SHPacket packet)
    {
        CHANNEL.sendToServer(packet);
    }

    /** Server -> one player. */
    public static void sendToPlayer(SHPacket packet, ServerPlayer player)
    {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    /** Server -> everyone tracking the player. */
    public static void sendToTracking(SHPacket packet, net.minecraft.world.entity.Entity entity)
    {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), packet);
    }

    /** Server -> everyone. */
    public static void sendToAll(SHPacket packet)
    {
        CHANNEL.send(PacketDistributor.ALL.noArg(), packet);
    }
}
