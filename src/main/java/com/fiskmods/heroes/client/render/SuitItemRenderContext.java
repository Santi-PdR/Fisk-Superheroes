package com.fiskmods.heroes.client.render;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.client.texture.TextureResolver;
import com.fiskmods.heroes.common.hero.HeroIteration;
import com.fiskmods.heroes.common.hero.HeroTracker;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Scoped texture for an item rendered for a specific wearer, not necessarily the local player. */
public final class SuitItemRenderContext
{
    private static final ThreadLocal<ResourceLocation> TEXTURE = new ThreadLocal<>();
    private static final ThreadLocal<LivingEntity> WEARER = new ThreadLocal<>();
    private static final Set<String> HELD_DIAGNOSTICS = ConcurrentHashMap.newKeySet();

    private SuitItemRenderContext() {}

    @Nullable
    static ResourceLocation resolveLivery(@Nullable HeroModelData model, Entity wearer, ResourceLocation itemId)
    {
        if (model == null || !itemId.getNamespace().equals(FiskHeroes.MODID)) return null;
        var liveries = model.getCustom().get("fiskheroes:livery");
        if (liveries == null || !liveries.has(itemId.getPath())) return null;
        var alias = liveries.get(itemId.getPath());
        if (!alias.isJsonPrimitive() || !alias.getAsJsonPrimitive().isString()) return null;
        return TextureResolver.resolve(model.getResources().get(alias.getAsString()), wearer);
    }

    /** Called by the hand-render coremod with the same owner and arguments vanilla supplied. */
    public static void renderHeld(ItemRenderer renderer, LivingEntity wearer, ItemStack stack,
            ItemDisplayContext context, boolean leftHand, PoseStack pose, MultiBufferSource buffers,
            @Nullable Level level, int light, int overlay, int seed)
    {
        HeroIteration hero = wearer instanceof Player player ? HeroTracker.getWornSuit(player) : null;
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        ResourceLocation livery = resolveLivery(HeroModelRegistry.get(hero), wearer, itemId);
        if (itemId.getNamespace().equals(FiskHeroes.MODID)
                && (itemId.getPath().equals("katana") || itemId.getPath().equals("desert_eagle")
                        || itemId.getPath().equals("beretta_93r") || itemId.getPath().equals("cold_gun")))
        {
            String heroName = hero != null ? hero.getRegistryName().toString() : "none";
            String key = wearer.getUUID() + "|" + heroName + "|" + itemId + "|" + context + "|" + livery;
            if (HELD_DIAGNOSTICS.add(key))
            {
                FiskHeroes.LOGGER.info("Held weapon livery: wearer={}, hero={}, item={}, context={}, texture={}",
                        wearer.getName().getString(), heroName, itemId, context, livery != null ? livery : "default");
            }
        }
        LivingEntity previous = WEARER.get();
        WEARER.set(wearer);
        try
        {
            withTexture(livery, () -> renderer.renderStatic(wearer, stack, context, leftHand, pose, buffers,
                    level, light, overlay, seed));
        }
        finally
        {
            if (previous == null) WEARER.remove(); else WEARER.set(previous);
        }
    }

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

    @Nullable
    static LivingEntity wearer()
    {
        return WEARER.get();
    }

    static ResourceLocation textureOr(ResourceLocation fallback)
    {
        ResourceLocation texture = TEXTURE.get();
        return texture != null ? texture : fallback;
    }
}
