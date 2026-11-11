package com.fiskmods.heroes.client.render;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.client.texture.TextureResolver;
import com.fiskmods.heroes.common.hero.HeroIteration;
import com.fiskmods.heroes.common.hero.HeroTracker;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Scoped texture for an item rendered for a specific wearer, not necessarily the local player. */
public final class SuitItemRenderContext
{
    private static final ThreadLocal<ResourceLocation> TEXTURE = new ThreadLocal<>();
    private static final ThreadLocal<LivingEntity> WEARER = new ThreadLocal<>();
    private record Frame(@Nullable ResourceLocation texture, @Nullable LivingEntity wearer) {}
    private static final ThreadLocal<java.util.ArrayDeque<Frame>> FRAMES = ThreadLocal.withInitial(java.util.ArrayDeque::new);
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

    /** Entered inside ItemRenderer so other mods can still inject at the vanilla call site. */
    public static void push(@Nullable LivingEntity wearer, ItemStack stack, ItemDisplayContext context)
    {
        // Stowed item rendering supplies its own texture with withTexture; inventory and
        // other non-hand renders must retain that surrounding context unchanged.
        if (context != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                && context != ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                && context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
                && context != ItemDisplayContext.THIRD_PERSON_LEFT_HAND)
        {
            FRAMES.get().push(new Frame(TEXTURE.get(), WEARER.get()));
            return;
        }
        HeroIteration hero = wearer instanceof Player player ? HeroTracker.getWornSuit(player) : null;
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        ResourceLocation livery = wearer != null ? resolveLivery(HeroModelRegistry.get(hero), wearer, itemId) : null;
        if (wearer != null && itemId.getNamespace().equals(FiskHeroes.MODID)
                && (itemId.getPath().equals("katana") || itemId.getPath().equals("desert_eagle")
                        || itemId.getPath().equals("beretta_93r") || itemId.getPath().equals("cold_gun")
                        || itemId.getPath().equals("heat_gun")))
        {
            String heroName = hero != null ? hero.getRegistryName().toString() : "none";
            String key = wearer.getUUID() + "|" + heroName + "|" + itemId + "|" + context + "|" + livery;
            if (HELD_DIAGNOSTICS.add(key))
            {
                FiskHeroes.LOGGER.info("Held weapon livery: wearer={}, hero={}, item={}, context={}, texture={}",
                        wearer.getName().getString(), heroName, itemId, context, livery != null ? livery : "default");
            }
        }
        FRAMES.get().push(new Frame(TEXTURE.get(), WEARER.get()));
        if (wearer == null) WEARER.remove(); else WEARER.set(wearer);
        if (livery == null) TEXTURE.remove(); else TEXTURE.set(livery);
    }

    /** Restore the enclosing render even when nested rendering or an exception occurs. */
    public static void pop()
    {
        var frames = FRAMES.get();
        Frame previous = frames.pop();
        if (previous.wearer() == null) WEARER.remove(); else WEARER.set(previous.wearer());
        if (previous.texture() == null) TEXTURE.remove(); else TEXTURE.set(previous.texture());
        if (frames.isEmpty()) FRAMES.remove();
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
