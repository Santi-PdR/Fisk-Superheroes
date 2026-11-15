package com.fiskmods.heroes.client.render;

import com.fiskmods.heroes.FiskHeroes;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Base pass for custom item meshes, including the vanilla enchantment foil when present. */
final class ItemModelBuffers
{
    private static final java.util.Set<String> DIAGNOSTICS = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private ItemModelBuffers() {}

    static VertexConsumer model(MultiBufferSource buffers, ResourceLocation texture, ItemStack stack, ItemDisplayContext context)
    {
        boolean foil = stack.hasFoil();
        if (Boolean.getBoolean("fiskheroes.ciItemVisualDiagnostics"))
        {
            var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            String key = id + "|" + context + "|" + foil + "|" + texture;
            if (DIAGNOSTICS.add(key))
                FiskHeroes.LOGGER.info("Item model base pass: item={}, context={}, foil={}, texture={}", id, context, foil, texture);
        }
        return ItemRenderer.getFoilBufferDirect(buffers, RenderType.entityCutoutNoCull(texture), true, foil);
    }
}
