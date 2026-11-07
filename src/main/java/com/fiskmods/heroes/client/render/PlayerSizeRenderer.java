package com.fiskmods.heroes.client.render;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.hero.ability.PlayerSizeHandler;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Scale skin, armor and held items together; restore the shared pose before post effects. */
@Mod.EventBusSubscriber(modid = FiskHeroes.MODID, value = Dist.CLIENT)
public final class PlayerSizeRenderer
{
    private PlayerSizeRenderer() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void before(RenderPlayerEvent.Pre event)
    {
        event.getPoseStack().pushPose();
        float scale = PlayerSizeHandler.scale(event.getEntity());
        event.getPoseStack().scale(scale, scale, scale);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void after(RenderPlayerEvent.Post event)
    {
        event.getPoseStack().popPose();
    }
}
