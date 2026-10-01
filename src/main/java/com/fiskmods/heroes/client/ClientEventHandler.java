package com.fiskmods.heroes.client;

import com.fiskmods.heroes.client.hud.SuitHud;
import com.fiskmods.heroes.client.keybinds.SHKeyBinds;
import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.data.var.Vars;
import com.fiskmods.heroes.common.network.PacketAbility;
import com.fiskmods.heroes.common.network.SHNetwork;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-side input handling. Ability keys are reported to the server, which validates them against
 * the worn hero; the client never applies an ability on its own.
 */
@Mod.EventBusSubscriber(modid = com.fiskmods.heroes.FiskHeroes.MODID, value = Dist.CLIENT)
public class ClientEventHandler
{
    /** Ability indices are 1-based to match the pack scripts; 0 is the mask toggle. */
    private static final int[] ABILITY_INDICES = { 1, 2, 3, 4, 5 };

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event)
    {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        if (player == null || mc.screen != null)
        {
            return;
        }

        for (int i = 0; i < SHKeyBinds.ABILITY_COUNT; ++i)
        {
            while (SHKeyBinds.ABILITIES[i].consumeClick())
            {
                SHNetwork.sendToServer(new PacketAbility(ABILITY_INDICES[i], true));
            }
        }

        while (SHKeyBinds.MASK.consumeClick())
        {
            SHNetwork.sendToServer(new PacketAbility(0, true));
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END)
        {
            return;
        }

        LocalPlayer player = Minecraft.getInstance().player;

        if (player == null)
        {
            return;
        }

        SHPlayerData data = SHDataCapabilities.getPlayer(player);

        if (data == null)
        {
            return;
        }

        // Interpolate the values the HUD animates
        interpolate(data, Vars.MASK_OPEN_TIMER, data.getData().get(Vars.MASK_OPEN) ? 1.0F : 0.0F);
        interpolate(data, Vars.SUIT_OPEN_TIMER, data.getData().get(Vars.SUIT_OPEN) ? 1.0F : 0.0F);
        interpolate(data, Vars.BOOSTER_TIMER, data.getData().get(Vars.FLYING) ? 1.0F : 0.0F);
    }

    private static void interpolate(SHPlayerData data, com.fiskmods.heroes.common.data.var.DataVar<Float> var, float target)
    {
        float value = data.getData().get(var);
        data.getData().set(var, value + (target - value) * 0.15F);
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event)
    {
        SuitHud.render(event.getGuiGraphics(), event.getPartialTick());
    }
}
