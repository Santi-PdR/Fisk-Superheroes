package com.fiskmods.heroes.common.network;

import javax.annotation.Nullable;

import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.hero.Hero;
import com.fiskmods.heroes.common.hero.HeroIteration;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * Server -> client: the suit currently worn by an entity. The client needs it for suit rendering
 * and the ability HUD.
 */
public class PacketSyncSuit extends SHPacket
{
    private final int entityId;
    @Nullable
    private final ResourceLocation hero;
    @Nullable
    private final String iteration;

    public PacketSyncSuit(int entityId, @Nullable HeroIteration value)
    {
        this.entityId = entityId;
        this.hero = value != null ? value.getHero().getRegistryName() : null;
        this.iteration = value != null && value.getKey() != null ? value.getKey() : "";
    }

    public PacketSyncSuit(FriendlyByteBuf buf)
    {
        entityId = buf.readVarInt();
        hero = buf.readBoolean() ? buf.readResourceLocation() : null;
        iteration = buf.readUtf();
    }

    @Override
    public void encode(FriendlyByteBuf buf)
    {
        buf.writeVarInt(entityId);
        buf.writeBoolean(hero != null);

        if (hero != null)
        {
            buf.writeResourceLocation(hero);
        }

        buf.writeUtf(iteration != null ? iteration : "");
    }

    @Override
    public void handle(NetworkEvent.Context context)
    {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleClient());
    }

    private void handleClient()
    {
        Entity entity = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getEntity(entityId) : null;

        if (entity == null)
        {
            return;
        }

        SHPlayerData data = SHDataCapabilities.getPlayer(entity);

        if (data == null)
        {
            return;
        }

        if (hero == null)
        {
            data.setHero(null);
            return;
        }

        Hero heroType = Hero.REGISTRY.getHero(hero);

        if (heroType != null)
        {
            data.setHero(heroType.getIteration(iteration));
        }
    }
}
