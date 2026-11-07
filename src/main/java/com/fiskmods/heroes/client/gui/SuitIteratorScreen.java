package com.fiskmods.heroes.client.gui;

import java.util.ArrayList;
import java.util.List;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.block.SuitIteratorMenu;
import com.fiskmods.heroes.common.hero.HeroIteration;
import com.fiskmods.heroes.common.network.PacketSuitIteration;
import com.fiskmods.heroes.common.network.SHNetwork;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

/**
 * Suit Iterator GUI screen ported from 1.7.10 GuiSuitIterator:
 * displays the scrollable list of suit iterations at (leftPos + 109, topPos + 18, 52x70)
 * and a live rotating 3D preview of the suit at (leftPos + 80, topPos + 85).
 */
public final class SuitIteratorScreen extends AbstractContainerScreen<SuitIteratorMenu>
{
    private static final ResourceLocation TEXTURE = FiskHeroes.id("textures/gui/container/suit_iterator.png");
    private static final int LIST_X = 109;
    private static final int LIST_Y = 18;
    private static final int LIST_W = 52;
    private static final int LIST_H = 70;
    private static final int ROW_H = 12;

    private int scrollOffset;

    public SuitIteratorScreen(SuitIteratorMenu menu, Inventory inventory, Component title)
    {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 188;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    private List<HeroIteration> getIterations()
    {
        HeroIteration current = menu.getCurrentIteration();
        if (current == null) return List.of();
        return new ArrayList<>(current.getHero().getIterations().values());
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

        HeroIteration current = menu.getCurrentIteration();
        if (current != null)
        {
            SuitGuiPreviewRenderer.render(graphics, current, leftPos + 74, topPos + 82, 26.0F, partialTick);
        }

        List<HeroIteration> list = getIterations();
        int visibleRows = LIST_H / ROW_H;
        int maxScroll = Math.max(0, list.size() - visibleRows);
        scrollOffset = Mth.clamp(scrollOffset, 0, maxScroll);

        int lx = leftPos + LIST_X;
        int ly = topPos + LIST_Y;
        graphics.enableScissor(lx, ly, lx + LIST_W, ly + LIST_H);
        for (int i = 0; i < visibleRows && (i + scrollOffset) < list.size(); ++i)
        {
            HeroIteration entry = list.get(i + scrollOffset);
            int ry = ly + i * ROW_H;
            boolean selected = entry.equals(current);
            boolean hovered = mouseX >= lx && mouseX < lx + LIST_W && mouseY >= ry && mouseY < ry + ROW_H;
            if (selected)
            {
                graphics.fill(lx, ry, lx + LIST_W, ry + ROW_H - 1, 0x66FFFFFF);
            }
            else if (hovered)
            {
                graphics.fill(lx, ry, lx + LIST_W, ry + ROW_H - 1, 0x33FFFFFF);
            }
            String label = entry.isDefault() ? "Default" : entry.getLocalizedName();
            String clipped = font.plainSubstrByWidth(label, LIST_W - 4);
            graphics.drawString(font, clipped, lx + 2, ry + 2, selected ? 0xFFFF55 : 0xE0E0E0, true);
        }
        graphics.disableScissor();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if (button == 0)
        {
            int lx = leftPos + LIST_X;
            int ly = topPos + LIST_Y;
            if (mouseX >= lx && mouseX < lx + LIST_W && mouseY >= ly && mouseY < ly + LIST_H)
            {
                List<HeroIteration> list = getIterations();
                int idx = scrollOffset + (int) ((mouseY - ly) / ROW_H);
                if (idx >= 0 && idx < list.size())
                {
                    HeroIteration chosen = list.get(idx);
                    String key = chosen.getKey() != null ? chosen.getKey() : "";
                    // Only the server mutates inventory. Its slot update confirms the selection.
                    SHNetwork.sendToServer(new PacketSuitIteration(key));
                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta)
    {
        List<HeroIteration> list = getIterations();
        int maxScroll = Math.max(0, list.size() - (LIST_H / ROW_H));
        if (maxScroll > 0)
        {
            scrollOffset = Mth.clamp(scrollOffset - (delta > 0 ? 1 : -1), 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
