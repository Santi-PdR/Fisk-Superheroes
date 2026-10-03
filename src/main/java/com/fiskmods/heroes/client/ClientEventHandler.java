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
    private static final boolean[] abilityKeysDown = new boolean[SHKeyBinds.ABILITY_COUNT];
    private static boolean weaponKeyHeld;
    private static boolean maskKeyDown;

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event)
    {
        Minecraft mc = Minecraft.getInstance();
        if (SHKeyBinds.WEAPON.matches(event.getKey(), event.getScanCode()))
        {
            if (event.getAction() == org.lwjgl.glfw.GLFW.GLFW_PRESS
                    && (mc.screen == null || mc.screen instanceof com.fiskmods.heroes.client.gui.EquipmentWheelScreen)) weaponKeyHeld = true;
            else if (event.getAction() == org.lwjgl.glfw.GLFW.GLFW_RELEASE) weaponKeyHeld = false;
        }

        LocalPlayer player = mc.player;

        if (player == null || mc.screen != null)
        {
            return;
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
            java.util.Arrays.fill(abilityKeysDown, false);
            maskKeyDown = false;
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        for (int i = 0; i < SHKeyBinds.ABILITY_COUNT; ++i)
        {
            boolean down = mc.screen == null && SHKeyBinds.ABILITIES[i].isDown();
            if (down != abilityKeysDown[i])
            {
                abilityKeysDown[i] = down;
                SHNetwork.sendToServer(new PacketAbility(ABILITY_INDICES[i], down));
            }
        }

        boolean maskDown = mc.screen == null && SHKeyBinds.MASK.isDown();
        if (maskDown != maskKeyDown)
        {
            maskKeyDown = maskDown;
            SHNetwork.sendToServer(new PacketAbility(0, maskDown));
        }

        if (mc.screen instanceof com.fiskmods.heroes.client.gui.EquipmentWheelScreen)
        {
            if (!weaponKeyHeld) mc.setScreen(null);
        }
        else if (mc.screen == null && weaponKeyHeld)
        {
            com.fiskmods.heroes.client.gui.EquipmentWheelScreen wheel =
                    com.fiskmods.heroes.client.gui.EquipmentWheelScreen.createForCurrentPlayer();
            if (wheel != null) mc.setScreen(wheel);
        }

        com.fiskmods.heroes.client.sound.SHSoundPlayer.tick();

        boolean jump = Minecraft.getInstance().options.keyJump.isDown();
        boolean sneak = Minecraft.getInstance().options.keyShift.isDown();
        com.fiskmods.heroes.common.data.PlayerInputTracker.set(player, jump, sneak);

        if (!com.fiskmods.heroes.common.network.PacketInput.lastSent
                || jump != com.fiskmods.heroes.common.network.PacketInput.lastJump
                || sneak != com.fiskmods.heroes.common.network.PacketInput.lastSneak)
        {
            com.fiskmods.heroes.common.network.PacketInput.lastSent = true;
            com.fiskmods.heroes.common.network.PacketInput.lastJump = jump;
            com.fiskmods.heroes.common.network.PacketInput.lastSneak = sneak;
            SHNetwork.sendToServer(new com.fiskmods.heroes.common.network.PacketInput(jump, sneak));
        }

        SHPlayerData data = SHDataCapabilities.getPlayer(player);

        if (data == null)
        {
            return;
        }

        // Interpolate the values the HUD animates
        interpolate(data, Vars.MASK_OPEN_TIMER2, data.getData().get(Vars.MASK_OPEN) ? 1.0F : 0.0F);
        interpolate(data, Vars.BOOSTER_TIMER, data.getData().get(Vars.FLYING) ? 1.0F : 0.0F);
    }

    @SubscribeEvent
    public static void onMouseInput(InputEvent.MouseButton event)
    {
        if (!SHKeyBinds.WEAPON.matchesMouse(event.getButton())) return;
        Minecraft mc = Minecraft.getInstance();
        if (event.getAction() == org.lwjgl.glfw.GLFW.GLFW_PRESS
                && (mc.screen == null || mc.screen instanceof com.fiskmods.heroes.client.gui.EquipmentWheelScreen)) weaponKeyHeld = true;
        else if (event.getAction() == org.lwjgl.glfw.GLFW.GLFW_RELEASE) weaponKeyHeld = false;
    }

    private static void interpolate(SHPlayerData data, com.fiskmods.heroes.common.data.var.DataVar<Float> var, float target)
    {
        float value = data.getData().get(var);
        data.getData().set(var, value + (target - value) * 0.15F);
    }

    /** The current frame's partial tick, kept by the render events. */
    public static float partialTicks()
    {
        return net.minecraft.client.Minecraft.getInstance().getFrameTime();
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event)
    {
        com.fiskmods.heroes.common.data.RenderTickInfo.set(com.fiskmods.heroes.client.ClientEventHandler.partialTicks());

        SuitHud.render(event.getGuiGraphics(), event.getPartialTick());
    }
}
