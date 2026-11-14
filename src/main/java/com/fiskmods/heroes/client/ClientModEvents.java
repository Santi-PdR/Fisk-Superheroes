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
            MenuScreens.register(ModMenus.ALLOY_SMELTERY.get(), com.fiskmods.heroes.client.gui.AlloySmelteryScreen::new);
            MenuScreens.register(ModMenus.SUIT_ITERATOR.get(), com.fiskmods.heroes.client.gui.SuitIteratorScreen::new);
            MenuScreens.register(ModMenus.SUIT_AUGMENTOR.get(), com.fiskmods.heroes.client.gui.SuitAugmentorScreen::new);
            MenuScreens.register(ModMenus.SUIT_FABRICATOR.get(), com.fiskmods.heroes.client.gui.SuitFabricatorScreen::new);
            ItemProperties.register(ModItems.COMPOUND_BOW.get(), new net.minecraft.resources.ResourceLocation("pulling"),
                    (stack, level, entity, seed) -> entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);
            ItemProperties.register(ModItems.COMPOUND_BOW.get(), new net.minecraft.resources.ResourceLocation("pull"),
                    (stack, level, entity, seed) -> entity != null && entity.isUsingItem() && entity.getUseItem() == stack
                            ? net.minecraft.util.Mth.clamp((stack.getUseDuration() - entity.getUseItemRemainingTicks())
                                    / com.fiskmods.heroes.common.item.ItemCompoundBow.getDrawDuration(entity), 0.0F, 1.0F)
                            : 0.0F);
            ItemProperties.register(ModItems.COMPOUND_BOW.get(), FiskHeroes.id("broken"),
                    (stack, level, entity, seed) -> com.fiskmods.heroes.common.item.ItemCompoundBow.isBroken(stack) ? 1.0F : 0.0F);
            ItemProperties.register(ModItems.TRICK_ARROW.get(), FiskHeroes.id("arrow_type"),
                    (stack, level, entity, seed) -> ItemTrickArrow.getTypeIndex(stack));
        });
    }

    @SubscribeEvent
    public static void registerBowInventoryModels(net.minecraftforge.client.event.ModelEvent.RegisterAdditional event)
    {
        event.register(FiskHeroes.id("item/compound_bow_inventory"));
        event.register(FiskHeroes.id("item/compound_bow_inventory_broken"));
    }

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event)
    {
        event.registerEntityRenderer(ModEntities.TRICK_ARROW.get(), TrickArrowRenderer::new);
        event.registerEntityRenderer(ModEntities.THROWN_SHIELD.get(), net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.EQUIPMENT_PROJECTILE.get(), com.fiskmods.heroes.client.render.EquipmentProjectileRenderer::new);
        event.registerEntityRenderer(ModEntities.COLD_BALL.get(), com.fiskmods.heroes.client.render.ColdBallRenderer::new);
        event.registerEntityRenderer(ModEntities.ENERGY_BOLT.get(), EnergyBoltRenderer::new);
        event.registerEntityRenderer(ModEntities.ICICLE.get(), IcicleRenderer::new);
        event.registerEntityRenderer(ModEntities.FIRE_BLAST.get(), FireBlastRenderer::new);
        event.registerEntityRenderer(ModEntities.SONIC_WAVE.get(), com.fiskmods.heroes.client.render.SonicWaveRenderer::new);
        event.registerEntityRenderer(ModEntities.CACTUS_SPIKE.get(), com.fiskmods.heroes.client.render.CactusSpikeRenderer::new);
        event.registerEntityRenderer(ModEntities.GRAPPLING_HOOK.get(), com.fiskmods.heroes.client.render.GrapplingHookRenderer::new);
        event.registerEntityRenderer(ModEntities.CACTUS_MINION.get(), CactusMinionRenderer::new);
        event.registerEntityRenderer(ModEntities.EARTH_CRACK.get(), com.fiskmods.heroes.client.render.EarthCrackRenderer::new);
        event.registerEntityRenderer(ModEntities.SPELL_WHIP.get(), com.fiskmods.heroes.client.render.SpellWhipRenderer::new);
        event.registerEntityRenderer(ModEntities.SPELL_DUPLICATE.get(), com.fiskmods.heroes.client.render.SpellDuplicateRenderer::new);
        event.registerEntityRenderer(ModEntities.ILLUSION_DRONE.get(), com.fiskmods.heroes.client.render.IllusionDroneRenderer::new);
        event.registerEntityRenderer(ModEntities.GRAVITY_WAVE.get(), com.fiskmods.heroes.client.render.GravityWaveRenderer::new);
        event.registerBlockEntityRenderer(com.fiskmods.heroes.common.block.ModBlockEntities.SUIT_FABRICATOR.get(),
                com.fiskmods.heroes.client.render.SuitFabricatorBlockEntityRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event)
    {
        event.registerLayerDefinition(com.fiskmods.heroes.client.render.IllusionDroneModel.LAYER,
                com.fiskmods.heroes.client.render.IllusionDroneModel::createBodyLayer);
    }
}
