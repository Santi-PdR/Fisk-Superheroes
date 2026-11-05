package com.fiskmods.heroes.client.gui;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.block.AlloySmelteryBlockEntity;
import com.fiskmods.heroes.common.block.AlloySmelteryMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Alloy Smeltery GUI screen ported from 1.7.10 GuiAlloySmeltery. */
public final class AlloySmelteryScreen extends AbstractContainerScreen<AlloySmelteryMenu>
{
    private static final ResourceLocation TEXTURE = FiskHeroes.id("textures/gui/container/alloy_smeltery.png");

    public AlloySmelteryScreen(AlloySmelteryMenu menu, Inventory inventory, Component title)
    {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 161;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY)
    {
        graphics.drawString(font, title, (imageWidth - font.width(title)) / 2, 6, 0x404040, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY)
    {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        int burn = menu.getBurnTime();
        if (burn > 0)
        {
            int i = burn * 13 / AlloySmelteryBlockEntity.MAX_BURN_TIME;
            graphics.blit(TEXTURE, leftPos + 85, topPos + 58 - i, 176, 12 - i, 14, i + 1);
        }

        int fuel = menu.getFuelAmount();
        if (fuel > 0)
        {
            int i = fuel * 17 / AlloySmelteryBlockEntity.MAX_FUEL;
            graphics.blit(TEXTURE, leftPos + 83, topPos + 63, 176, 14, i + 1, 6);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
