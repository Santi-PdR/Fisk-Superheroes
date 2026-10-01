package com.fiskmods.heroes.common.data;

import javax.annotation.Nullable;

import com.fiskmods.heroes.FiskHeroes;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Capability wiring for the hero player data. The capability is attached to players and persists
 * across death, dimension changes and reconnects.
 */
public class SHDataCapabilities
{
    public static final Capability<SHPlayerData> PLAYER_DATA = CapabilityManager.get(new CapabilityToken<>()
    {
    });

    public static final ResourceLocation ID = new ResourceLocation(FiskHeroes.MODID, "player_data");

    @Nullable
    public static SHPlayerData getPlayer(Entity entity)
    {
        if (entity == null)
        {
            return null;
        }

        return entity.getCapability(PLAYER_DATA).resolve().orElse(null);
    }

    @Nullable
    public static SHPlayerData getPlayer(Player player)
    {
        return getPlayer((Entity) player);
    }

    public static void attach(AttachCapabilitiesEvent<Entity> event)
    {
        if (event.getObject() instanceof Player)
        {
            event.addCapability(ID, new Provider());
        }
    }

    public static void onPlayerClone(PlayerEvent.Clone event)
    {
        SHPlayerData original = getPlayer(event.getOriginal());
        SHPlayerData replacement = getPlayer(event.getEntity());

        if (original != null && replacement != null)
        {
            replacement.copyFrom(original);
        }
    }

    public static class Provider implements ICapabilityProvider, ICapabilitySerializable<CompoundTag>
    {
        private final SHPlayerData data = new SHPlayerData();
        private final LazyOptional<SHPlayerData> optional = LazyOptional.of(() -> data);

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side)
        {
            return capability == PLAYER_DATA ? optional.cast() : LazyOptional.empty();
        }

        @Override
        public CompoundTag serializeNBT()
        {
            CompoundTag tag = new CompoundTag();
            data.writeTo(tag);
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag tag)
        {
            data.readFrom(tag);
        }
    }

    /** Event handlers used by the mod event bus. */
    public static class Events
    {
        @SubscribeEvent
        public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event)
        {
            attach(event);
        }

        @SubscribeEvent
        public static void onPlayerClone(PlayerEvent.Clone event)
        {
            SHDataCapabilities.onPlayerClone(event);
        }
    }
}
