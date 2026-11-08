package com.fiskmods.heroes.client.render;

import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;

/** Scoped texture for an item rendered on a specific player's suit, not the local player. */
final class SuitItemRenderContext
{
    private static final ThreadLocal<ResourceLocation> TEXTURE = new ThreadLocal<>();

    private SuitItemRenderContext() {}

    static void withTexture(@Nullable ResourceLocation texture, Runnable render)
    {
        ResourceLocation previous = TEXTURE.get();
        if (texture == null) TEXTURE.remove(); else TEXTURE.set(texture);
        try
        {
            render.run();
        }
        finally
        {
            if (previous == null) TEXTURE.remove(); else TEXTURE.set(previous);
        }
    }

    static ResourceLocation textureOr(ResourceLocation fallback)
    {
        ResourceLocation texture = TEXTURE.get();
        return texture != null ? texture : fallback;
    }
}
