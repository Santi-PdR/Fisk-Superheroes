package com.fiskmods.heroes.common.network;

import com.fiskmods.heroes.common.spell.SpellCastHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** Client request to cast an indexed spell; the server revalidates the active hero and cooldown. */
public final class PacketCastSpell extends SHPacket
{
    private final int spellIndex;

    public PacketCastSpell(int spellIndex)
    {
        this.spellIndex = spellIndex;
    }

    public PacketCastSpell(FriendlyByteBuf buf)
    {
        spellIndex = buf.readVarInt();
    }

    @Override
    public void encode(FriendlyByteBuf buf)
    {
        buf.writeVarInt(spellIndex);
    }

    @Override
    public void handle(NetworkEvent.Context context)
    {
        ServerPlayer player = context.getSender();
        if (player != null && spellIndex >= 0 && spellIndex < 32)
        {
            context.enqueueWork(() -> player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable(SpellCastHandler.cast(player, spellIndex)
                            ? "message.fiskheroes.spell_cast" : "message.fiskheroes.spell_failed"), true));
        }
        context.setPacketHandled(true);
    }
}
