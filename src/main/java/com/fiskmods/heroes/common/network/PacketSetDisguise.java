package com.fiskmods.heroes.common.network;

import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.data.var.Vars;
import com.fiskmods.heroes.common.hero.Hero;
import com.fiskmods.heroes.common.hero.modifier.ModifierShapeShifting;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraftforge.network.NetworkEvent;

/** Client request to begin or reset a hero's name-based disguise. */
public final class PacketSetDisguise extends SHPacket
{
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
        if (hero == null || !hero.hasKeyBind("SHAPE_SHIFT") || !hero.hasKeyBind("SHAPE_SHIFT_RESET")
                || !hero.isKeyBindEnabled(player, "SHAPE_SHIFT")) return;

        boolean hasPower = hero.getPowerContainer().getEntries().stream()
                .anyMatch(entry -> entry.getModifier() instanceof ModifierShapeShifting
                        && entry.isEnabled() && entry.isModifierEnabled(player, data));
        if (!hasPower) return;

        String current = data.getData().get(Vars.DISGUISE);
        data.getData().set(Vars.SHAPE_SHIFTING_FROM, current == null ? "" : current);
        data.getData().set(Vars.SHAPE_SHIFTING_TO, name);
        data.getData().set(Vars.SHAPE_SHIFT_TIMER, 1.0F);
        // The original transition starts at 1 and counts down through its midpoint.
        data.getData().set(Vars.SHAPE_SHIFTING, false);
    }
}
