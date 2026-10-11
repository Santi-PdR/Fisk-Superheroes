package com.fiskmods.heroes.client.hud;

import java.util.List;

import com.fiskmods.heroes.client.keybinds.SHKeyBinds;
import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.hero.Hero;
import com.fiskmods.heroes.common.hero.HeroIteration;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/**
 * On-screen suit information: the hero currently worn and the abilities bound to the ability keys,
 * mirroring the ability HUD of the original mod.
 */
public class SuitHud
{
    private static final int COLOR_TEXT = 0xFFFFFF;
    private static final int COLOR_KEY = 0xFFAA00;
    private static final int COLOR_BACKGROUND = 0x55000000;

    public static void render(GuiGraphics graphics, float partialTick)
    {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        if (player == null || mc.options.hideGui || mc.screen != null)
        {
            return;
        }

        SHPlayerData data = SHDataCapabilities.getPlayer(player);

        if (data == null)
        {
            return;
        }

        HeroIteration iteration = data.getHero();

        if (iteration == null)
        {
            return;
        }

        Hero hero = iteration.getHero();
        int x = 4;
        int y = mc.getWindow().getGuiScaledHeight() - 60;

        Component name = iteration.getFormattedName().copy().withStyle(ChatFormatting.AQUA);
        graphics.drawString(mc.font, name, x, y, COLOR_TEXT, true);
        y += 12;

        List<Hero.KeyBind> keyBinds = hero.getKeyBinds();

        for (Hero.KeyBind keyBind : keyBinds)
        {
            boolean enabled = hero.isKeyBindEnabled(player, keyBind.name());
            int color = enabled ? COLOR_TEXT : 0x777777;
            String key = keyBind.index() == -1
                    ? mc.options.keyAttack.getTranslatedKeyMessage().getString()
                    : SHKeyBinds.describe(keyBind.index() - 1);
            Component label = Component.translatable(keyBind.keyName());
            int rowRight = Math.min(mc.getWindow().getGuiScaledWidth(),
                    x + Math.max(90, 18 + mc.font.width(label) + 2));

            graphics.fill(x - 1, y - 1, rowRight, y + 9, COLOR_BACKGROUND);
            graphics.drawString(mc.font, key, x, y, COLOR_KEY, true);
            graphics.drawString(mc.font, label, x + 18, y, color, true);
            y += 11;
        }
    }

    /** Returns the keybind row under the pointer, or {@link Integer#MIN_VALUE} when outside it. */
    public static int findKeyBindAt(double mouseX, double mouseY)
    {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options.hideGui || mc.screen != null) return Integer.MIN_VALUE;

        SHPlayerData data = SHDataCapabilities.getPlayer(player);
        HeroIteration iteration = data != null ? data.getHero() : null;
        if (iteration == null || mouseX < 3.0D) return Integer.MIN_VALUE;

        int y = mc.getWindow().getGuiScaledHeight() - 48;
        for (Hero.KeyBind keyBind : iteration.getHero().getKeyBinds())
        {
            Component label = Component.translatable(keyBind.keyName());
            double rowRight = Math.min(mc.getWindow().getGuiScaledWidth(),
                    4 + Math.max(90, 18 + mc.font.width(label) + 2));
            if (mouseY >= y - 1.0D && mouseY < y + 9.0D && mouseX < rowRight) return keyBind.index();
            y += 11;
        }
        return Integer.MIN_VALUE;
    }

    private SuitHud()
    {
    }
}
