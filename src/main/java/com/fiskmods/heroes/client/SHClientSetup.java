package com.fiskmods.heroes.client;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.client.keybinds.SHKeyBinds;
import com.fiskmods.heroes.client.render.HeroModelRegistry;
import com.fiskmods.heroes.client.render.HeroSuitLayer;
import com.fiskmods.heroes.common.hero.ItemHeroArmor;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Client-only setup: key mappings, suit model loading, the suit render layer and the item model
 * property used to pick the icon of a suit piece.
 */
@Mod.EventBusSubscriber(modid = FiskHeroes.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class SHClientSetup
{
    /** Item model property: a stable per-hero value used by the generated suit piece models. */
    public static final ResourceLocation HERO_INDEX = new ResourceLocation(FiskHeroes.MODID, "hero_index");

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event)
    {
        SHKeyBinds.register(event);
    }

    @SubscribeEvent
    public static void registerReloadListeners(RegisterClientReloadListenersEvent event)
    {
        event.registerReloadListener(HeroModelRegistry.INSTANCE);
    }

    @SubscribeEvent
    public static void addLayers(EntityRenderersEvent.AddLayers event)
    {
        for (PlayerSkin.Model skin : event.getSkins())
        {
            LivingEntityRenderer<?, ?> renderer = event.getSkin(skin);

            if (renderer instanceof PlayerRenderer playerRenderer)
            {
                playerRenderer.addLayer(new HeroSuitLayer(playerRenderer));
            }
        }
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event)
    {
        event.enqueueWork(() ->
        {
            ClampedItemPropertyFunction function = (stack, level, entity, seed) ->
            {
                ResourceLocation id = ItemHeroArmor.getHeroId(stack);

                if (id == null)
                {
                    return 0.0F;
                }

                return 0.01F + Math.abs(id.getPath().hashCode() % 9999) / 10000.0F;
            };

            ItemProperties.register(com.fiskmods.heroes.common.item.ModItems.HELMET.get(), HERO_INDEX, function);
            ItemProperties.register(com.fiskmods.heroes.common.item.ModItems.CHESTPLATE.get(), HERO_INDEX, function);
            ItemProperties.register(com.fiskmods.heroes.common.item.ModItems.LEGGINGS.get(), HERO_INDEX, function);
            ItemProperties.register(com.fiskmods.heroes.common.item.ModItems.BOOTS.get(), HERO_INDEX, function);
        });
    }
}
