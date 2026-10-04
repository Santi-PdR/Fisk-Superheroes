package com.fiskmods.heroes.client;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.client.keybinds.SHKeyBinds;
import com.fiskmods.heroes.client.render.HeroModelRegistry;
import com.fiskmods.heroes.client.render.HeroSuitLayer;
import com.fiskmods.heroes.common.hero.ItemHeroArmor;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
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
        event.registerReloadListener(com.fiskmods.heroes.client.render.TrailRegistry.INSTANCE);
    }

    /** Mounts the downloaded sound repository as a resource pack. */
    @SubscribeEvent
    public static void addPackFinders(net.minecraftforge.event.AddPackFindersEvent event)
    {
        if (event.getPackType() == net.minecraft.server.packs.PackType.CLIENT_RESOURCES)
        {
            com.fiskmods.heroes.client.sound.SHSoundPack.register(event::addRepositorySource);
        }
    }

    @SubscribeEvent
    public static void addLayers(EntityRenderersEvent.AddLayers event)
    {
        for (var skin : event.getSkins())
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
        ItemHeroArmor.setArmorTextureResolver((stack, entity, armorSlot, type) ->
        {
            var iteration = ItemHeroArmor.getHero(stack);
            if (iteration == null) return null;

            var model = HeroModelRegistry.get(iteration);
            if (model == null) return null;

            var texture = model.getTexture(armorSlot, entity);
            return texture != null ? texture.toString() : null;
        });

        // The original mod downloads its audio on first launch; so does the port.
        com.fiskmods.heroes.client.sound.SHSoundRepository.downloadIfMissing(
                com.fiskmods.heroes.common.sound.SHSounds.getRepository(),
                com.fiskmods.heroes.common.sound.SHSounds.getRepositoryVersion());

        // Packets run in common code, so the client side of the entity lookup is installed here
        com.fiskmods.heroes.common.network.ClientEntityLookup.set(id ->
        {
            net.minecraft.client.multiplayer.ClientLevel level = net.minecraft.client.Minecraft.getInstance().level;
            return level != null ? level.getEntity(id) : null;
        });

        event.enqueueWork(() ->
        {
            ClampedItemPropertyFunction function = (stack, level, entity, seed) ->
            {
                ResourceLocation id = ItemHeroArmor.getHeroId(stack);

                if (id == null)
                {
                    return 0.0F;
                }

                // The item model overrides are generated from Java's floor-mod mapping.
                // Math.abs(hash % n) diverges for negative hashes and selects another hero's
                // armor model (for example Arsenal's index points at a Spider-Man entry).
                return 0.01F + Math.floorMod(id.getPath().hashCode(), 9999) / 10000.0F;
            };

            ItemProperties.register(com.fiskmods.heroes.common.item.ModItems.HELMET.get(), HERO_INDEX, function);
            ItemProperties.register(com.fiskmods.heroes.common.item.ModItems.CHESTPLATE.get(), HERO_INDEX, function);
            ItemProperties.register(com.fiskmods.heroes.common.item.ModItems.LEGGINGS.get(), HERO_INDEX, function);
            ItemProperties.register(com.fiskmods.heroes.common.item.ModItems.BOOTS.get(), HERO_INDEX, function);
        });
    }
}
