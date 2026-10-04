package com.fiskmods.heroes.client;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.client.gui.QuiverScreen;
import com.fiskmods.heroes.client.render.TrickArrowRenderer;
import com.fiskmods.heroes.client.render.CactusMinionRenderer;
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
                    (stack, level, entity, seed) -> ItemTrickArrow.EXPLOSIVE.equals(ItemTrickArrow.getType(stack)) ? 1.0F : 0.0F);
        });
    }

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event)
    {
        event.registerEntityRenderer(ModEntities.TRICK_ARROW.get(), TrickArrowRenderer::new);
        event.registerEntityRenderer(ModEntities.CACTUS_MINION.get(), CactusMinionRenderer::new);
        event.registerEntityRenderer(ModEntities.EARTH_CRACK.get(), com.fiskmods.heroes.client.render.EarthCrackRenderer::new);
    }
}
