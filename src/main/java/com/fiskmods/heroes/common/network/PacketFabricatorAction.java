package com.fiskmods.heroes.common.network;

import com.fiskmods.heroes.common.block.SuitFabricatorBlockEntity;
import com.fiskmods.heroes.common.block.SuitFabricatorMenu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/**
 * Client -> server action packet for SuitFabricatorMenu:
 * action 0 = start print (or claim completed suit),
 * action 1 = next hero on Suit Drive,
 * action 2 = previous hero on Suit Drive.
 */
public final class PacketFabricatorAction extends SHPacket
{
    public static final byte ACTION_PRINT_OR_CLAIM = 0;
    public static final byte ACTION_NEXT_HERO = 1;
    public static final byte ACTION_PREV_HERO = 2;

    private final byte action;

    public PacketFabricatorAction(byte action)
    {
        this.action = action;
    }

    public PacketFabricatorAction(FriendlyByteBuf buf)
    {
        this.action = buf.readByte();
    }

    @Override
    public void encode(FriendlyByteBuf buf)
    {
        buf.writeByte(action);
    }

    @Override
    public void handle(NetworkEvent.Context context)
    {
        ServerPlayer sender = context.getSender();
        if (sender == null || !(sender.containerMenu instanceof SuitFabricatorMenu menu)) return;
        if (!(sender.level().getBlockEntity(menu.getPos()) instanceof SuitFabricatorBlockEntity fabricator)) return;
        if (!fabricator.stillValid(sender)) return;

        switch (action)
        {
            case ACTION_PRINT_OR_CLAIM ->
            {
                if (fabricator.hasFinishedOutput())
                {
                    fabricator.claimPrintedSuit(sender);
                }
                else
                {
                    fabricator.startPrint();
                }
            }
            case ACTION_NEXT_HERO -> fabricator.cycleDriveIndex(1);
            case ACTION_PREV_HERO -> fabricator.cycleDriveIndex(-1);
        }
        menu.broadcastChanges();
    }
}
