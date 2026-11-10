package com.fiskmods.heroes.common.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** One server-accepted Heat Gun shot, with its block/entity-clipped visual segment. */
public final class PacketHeatGunFlame extends SHPacket
{
    private final int shooterId;
    private final Vec3 start;
    private final Vec3 end;

    public PacketHeatGunFlame(int shooterId, Vec3 start, Vec3 end)
    {
        this.shooterId = shooterId;
        this.start = start;
        this.end = end;
    }

    public PacketHeatGunFlame(FriendlyByteBuf buf)
    {
        shooterId = buf.readVarInt();
        start = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        end = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    public int shooterId() { return shooterId; }
    public Vec3 start() { return start; }
    public Vec3 end() { return end; }

    @Override
    public void encode(FriendlyByteBuf buf)
    {
        buf.writeVarInt(shooterId);
        buf.writeDouble(start.x); buf.writeDouble(start.y); buf.writeDouble(start.z);
        buf.writeDouble(end.x); buf.writeDouble(end.y); buf.writeDouble(end.z);
    }

    @Override
    public void handle(NetworkEvent.Context context)
    {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.fiskmods.heroes.client.render.HeatGunFlameEffects.spawn(shooterId, start, end));
    }
}
