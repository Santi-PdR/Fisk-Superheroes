package com.fiskmods.heroes.common.network;

import com.fiskmods.heroes.common.hero.ability.AbilityHandler;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/**
 * Client -> server: the player pressed (or released) one of the suit ability keys. The server is
 * authoritative: it validates the key against the worn hero and runs the ability there.
 */
public class PacketAbility extends SHPacket
{
    private final int index;
    private final boolean pressed;

    public PacketAbility(int index, boolean pressed)
    {
        this.index = index;
        this.pressed = pressed;
    }

    public PacketAbility(FriendlyByteBuf buf)
    {
        index = buf.readVarInt();
        pressed = buf.readBoolean();
    }

    @Override
    public void encode(FriendlyByteBuf buf)
    {
        buf.writeVarInt(index);
        buf.writeBoolean(pressed);
    }

    @Override
    public void handle(NetworkEvent.Context context)
    {
        ServerPlayer player = context.getSender();

        // -1 is the primary attack input used by hero-pack keybinds for AIM/SHOOT.
        if (player != null && index >= -1 && index < 16)
        {
            if (pressed)
            {
                com.fiskmods.heroes.FiskHeroes.LOGGER.info(
                        "Ability packet received: player={}, index={}", player.getGameProfile().getName(), index);
            }
            AbilityHandler.onAbilityKey(player, index, pressed);
        }
    }
}
