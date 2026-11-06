package com.fiskmods.heroes.common.network;

import com.fiskmods.heroes.common.config.SHConfig;
import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.data.var.Vars;
import com.fiskmods.heroes.common.hero.HeroTracker;
import com.fiskmods.heroes.common.hero.ability.AbilityHandler;
import com.fiskmods.heroes.common.hero.modifier.ModifierSuperSpeed;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/** Client request to change a speedster's selected speed by one level. */
public final class PacketChangeSpeed extends SHPacket
{
    private final int direction;

    public PacketChangeSpeed(int direction)
    {
        this.direction = Integer.signum(direction);
    }

    public PacketChangeSpeed(FriendlyByteBuf buffer)
    {
        this(buffer.readByte());
    }

    @Override
    public void encode(FriendlyByteBuf buffer)
    {
        buffer.writeByte(direction);
    }

    @Override
    public void handle(NetworkEvent.Context context)
    {
        requestChange(context.getSender(), direction);
    }

    /** Validates both the held key and the authoritative suit before changing the speed. */
    public static boolean requestChange(net.minecraft.server.level.ServerPlayer player, int direction)
    {
        if (player == null || direction == 0 || !SHConfig.ALLOW_SUPER_SPEED.get()
                || !AbilityHandler.isKeyPressed(player, "SUPER_SPEED")) return false;
        HeroTracker.update(player);
        var hero = HeroTracker.getHeroType(player);
        var data = SHDataCapabilities.getPlayer(player);
        if (hero == null || data == null || !data.getData().get(Vars.SPEEDING)) return false;
        var entry = AbilityHandler.findModifier(hero, "SUPER_SPEED", player);
        if (entry == null || !(entry.getModifier() instanceof ModifierSuperSpeed)
                || !entry.isEnabled() || !entry.isModifierEnabled(player, data)) return false;
        AbilityHandler.markSpeedScrolled(player);
        ModifierSuperSpeed.changeSpeed(player, entry, data, direction);
        return true;
    }
}
