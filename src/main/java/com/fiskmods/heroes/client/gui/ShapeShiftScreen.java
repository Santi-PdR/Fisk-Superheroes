package com.fiskmods.heroes.client.gui;

import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.data.var.Vars;
import com.fiskmods.heroes.common.network.PacketSetDisguise;
import com.fiskmods.heroes.common.network.SHNetwork;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Name input for the Martian Manhunter's original username-based disguise ability. */
public final class ShapeShiftScreen extends Screen
{
    private EditBox name;

    public ShapeShiftScreen()
    {
        super(Component.translatable("gui.disguise"));
    }

    @Override
    protected void init()
    {
        int center = width / 2;
        name = new EditBox(font, center - 100, height / 2 - 18, 200, 20, Component.translatable("gui.disguise"));
        name.setMaxLength(16);
        SHPlayerData data = Minecraft.getInstance().player != null
                ? SHDataCapabilities.getPlayer(Minecraft.getInstance().player) : null;
        String current = data != null ? data.getData().get(Vars.DISGUISE) : "";
        name.setValue(current == null ? "" : current);
        addRenderableWidget(name);
        addRenderableWidget(Button.builder(Component.translatable("gui.disguise.done"), button -> submit())
                .bounds(center - 100, height / 2 + 14, 96, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.disguise.cancel"), button -> onClose())
                .bounds(center + 4, height / 2 + 14, 96, 20).build());
        setInitialFocus(name);
    }

    private void submit()
    {
        String selected = name.getValue().trim();
        if (selected.matches("[A-Za-z0-9_]{0,16}"))
        {
            SHNetwork.sendToServer(new PacketSetDisguise(selected));
            onClose();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 48, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.translatable("gui.disguise.prompt"), width / 2, height / 2 - 32, 0xA0A0A0);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers)
    {
        if (keyCode == 257 || keyCode == 335)
        {
            submit();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}
