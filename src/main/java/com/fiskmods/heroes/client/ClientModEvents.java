package com.fiskmods.heroes.client;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.client.gui.QuiverScreen;
import com.fiskmods.heroes.client.render.TrickArrowRenderer;
import com.fiskmods.heroes.client.render.CactusMinionRenderer;
import com.fiskmods.heroes.client.render.EnergyBoltRenderer;
import com.fiskmods.heroes.client.render.IcicleRenderer;
import com.fiskmods.heroes.client.render.FireBlastRenderer;
import com.fiskmods.heroes.common.entity.ModEntities;
import com.fiskmods.heroes.common.item.ItemTrickArrow;
import com.fiskmods.heroes.common.item.ModMenus;
import com.fiskmods.heroes.common.item.ModItems;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = FiskHeroes.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents
{
    private ClientModEvents()
    {
    }

    @SubscribeEvent
    public static void registerScreens(FMLClientSetupEvent event)
    {
        event.enqueueWork(() ->
        {
            MenuScreens.register(ModMenus.QUIVER.get(), QuiverScreen::new);
            ItemProperties.register(ModItems.COMPOUND_BOW.get(), new net.minecraft.resources.ResourceLocation("pulling"),
                    (stack, level, entity, seed) -> entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);
            ItemProperties.register(ModItems.COMPOUND_BOW.get(), new net.minecraft.resources.ResourceLocation("pull"),
                    (stack, level, entity, seed) -> entity != null && entity.isUsingItem() && entity.getUseItem() == stack
                            ? (float) (stack.getUseDuration() - entity.getUseItemRemainingTicks()) / stack.getUseDuration() : 0.0F);
            ItemProperties.register(ModItems.TRICK_ARROW.get(), FiskHeroes.id("arrow_type"),
                    (stack, level, entity, seed) -> ItemTrickArrow.getTypeIndex(stack));
        });
    }

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event)
    {
        event.registerEntityRenderer(ModEntities.TRICK_ARROW.get(), TrickArrowRenderer::new);
        event.registerEntityRenderer(ModEntities.THROWN_SHIELD.get(), net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.EQUIPMENT_PROJECTILE.get(), net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.ENERGY_BOLT.get(), EnergyBoltRenderer::new);
        event.registerEntityRenderer(ModEntities.ICICLE.get(), IcicleRenderer::new);
        event.registerEntityRenderer(ModEntities.FIRE_BLAST.get(), FireBlastRenderer::new);
        event.registerEntityRenderer(ModEntities.SONIC_WAVE.get(), com.fiskmods.heroes.client.render.SonicWaveRenderer::new);
        event.registerEntityRenderer(ModEntities.CACTUS_MINION.get(), CactusMinionRenderer::new);
        event.registerEntityRenderer(ModEntities.EARTH_CRACK.get(), com.fiskmods.heroes.client.render.EarthCrackRenderer::new);
        event.registerEntityRenderer(ModEntities.SPELL_DUPLICATE.get(), com.fiskmods.heroes.client.render.SpellDuplicateRenderer::new);
        event.registerEntityRenderer(ModEntities.ILLUSION_DRONE.get(), com.fiskmods.heroes.client.render.IllusionDroneRenderer::new);
        event.registerEntityRenderer(ModEntities.GRAVITY_WAVE.get(), com.fiskmods.heroes.client.render.GravityWaveRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event)
    {
        event.registerLayerDefinition(com.fiskmods.heroes.client.render.IllusionDroneModel.LAYER,
                com.fiskmods.heroes.client.render.IllusionDroneModel::createBodyLayer);
    }
}
