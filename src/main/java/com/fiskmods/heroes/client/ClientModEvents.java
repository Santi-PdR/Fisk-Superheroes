package com.fiskmods.heroes.client;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.client.gui.QuiverScreen;
import com.fiskmods.heroes.common.item.ModMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
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
        event.enqueueWork(() -> MenuScreens.register(ModMenus.QUIVER.get(), QuiverScreen::new));
    }
}
