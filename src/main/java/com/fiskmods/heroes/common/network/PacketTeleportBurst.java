package com.fiskmods.heroes.common.network;

import com.fiskmods.heroes.common.hero.HeroIteration;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** One of the two teleport endpoints; independent of whether the caster is tracked locally. */
public final class PacketTeleportBurst extends SHPacket
{
    private final int entityId;
    private final ResourceLocation hero;
    private final String iteration;
    private final Vec3 center;
    private final float width, height, scale;
    private final boolean destination;

    public PacketTeleportBurst(int entityId, HeroIteration hero, Vec3 center, float width, float height, float scale, boolean destination)
    {
        this(entityId, hero.getHero().getRegistryName(), hero.getKey() != null ? hero.getKey() : "",
                center, width, height, scale, destination);
    }

    private PacketTeleportBurst(int entityId, ResourceLocation hero, String iteration, Vec3 center,
            float width, float height, float scale, boolean destination)
    {
        this.entityId = entityId; this.hero = hero; this.iteration = iteration; this.center = center;
        this.width = width; this.height = height; this.scale = scale; this.destination = destination;
    }

    public PacketTeleportBurst(FriendlyByteBuf buffer)
    {
        this(buffer.readVarInt(), buffer.readResourceLocation(), buffer.readUtf(),
                new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()),
                buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readBoolean());
    }

    public int entityId() { return entityId; }
    public ResourceLocation hero() { return hero; }
    public String iteration() { return iteration; }
    public Vec3 center() { return center; }
    public float width() { return width; }
    public float height() { return height; }
    public float scale() { return scale; }
    public boolean destination() { return destination; }

    @Override public void encode(FriendlyByteBuf buffer)
    {
        buffer.writeVarInt(entityId); buffer.writeResourceLocation(hero); buffer.writeUtf(iteration);
        buffer.writeDouble(center.x); buffer.writeDouble(center.y); buffer.writeDouble(center.z);
        buffer.writeFloat(width); buffer.writeFloat(height); buffer.writeFloat(scale); buffer.writeBoolean(destination);
    }

    @Override public void handle(NetworkEvent.Context context)
    {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.fiskmods.heroes.client.render.TeleportBurstEffects.spawn(this));
    }
}
