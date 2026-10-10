package com.fiskmods.heroes.common.network;

import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.data.var.Vars;
import com.fiskmods.heroes.common.hero.HeroTracker;
import com.fiskmods.heroes.common.hero.ability.AbilityHandler;
import com.fiskmods.heroes.common.hero.modifier.AbilityData;
import com.fiskmods.heroes.common.hero.power.ModifierEntry;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** Client request to adjust the active gravity manipulation strength. */
public final class PacketGravityAmount extends SHPacket
{
    private final int direction;

    public PacketGravityAmount(int direction)
    {
        this.direction = Integer.signum(direction);
    }

    public PacketGravityAmount(FriendlyByteBuf buffer)
    {
        direction = Integer.signum(buffer.readByte());
    }

    @Override
    public void encode(FriendlyByteBuf buffer)
    {
        buffer.writeByte(direction);
    }

    @Override
    public void handle(NetworkEvent.Context context)
    {
        ServerPlayer player = context.getSender();
        if (player == null || direction == 0 || !AbilityHandler.isKeyPressed(player, "GRAVITY_MANIPULATION")) return;
        var iteration = HeroTracker.getHero(player);
        var data = SHDataCapabilities.getPlayer(player);
        if (iteration == null || data == null) return;
        ModifierEntry entry = AbilityHandler.findModifier(iteration.getHero(), "GRAVITY_MANIPULATION", player);
        if (entry == null || !entry.isEnabled() || !entry.isModifierEnabled(player, data)) return;

        float amount = net.minecraft.util.Mth.clamp(data.getData().get(Vars.GRAVITY_AMOUNT) + direction / 3.0F, -1.0F, 1.0F);
        data.getData().set(Vars.GRAVITY_AMOUNT, amount);
        AbilityData.playSound(player, entry, "SWITCH");
    }
}
