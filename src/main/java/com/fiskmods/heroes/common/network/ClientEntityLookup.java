package com.fiskmods.heroes.common.network;

import java.util.function.IntFunction;

import javax.annotation.Nullable;

import net.minecraft.world.entity.Entity;

/**
 * Looks up an entity by its network id in the client level.
 * <p>
 * Server-to-client packets need it, but their classes are loaded on a dedicated server as well, so
 * the client call is kept out of them: the client installs the implementation during its setup and
 * the packets simply ask here.
 */
public final class ClientEntityLookup
{
    private static IntFunction<Entity> lookup = id -> null;

    private ClientEntityLookup()
    {
    }

    public static void set(IntFunction<Entity> implementation)
    {
        lookup = implementation;
    }

    @Nullable
    public static Entity get(int entityId)
    {
        return lookup.apply(entityId);
    }
}
