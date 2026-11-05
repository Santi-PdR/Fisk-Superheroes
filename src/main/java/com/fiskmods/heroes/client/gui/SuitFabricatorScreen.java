package com.fiskmods.heroes.client.gui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.block.FabricatorRecipes;
import com.fiskmods.heroes.common.block.SuitFabricatorBlockEntity;
import com.fiskmods.heroes.common.block.SuitFabricatorMenu;
import com.fiskmods.heroes.common.hero.Hero;
import com.fiskmods.heroes.common.hero.HeroIteration;
import com.fiskmods.heroes.common.hero.ItemHeroArmor;
import com.fiskmods.heroes.common.item.ItemSuitDrive;
import com.fiskmods.heroes.common.item.ModItems;
import com.fiskmods.heroes.common.network.PacketFabricatorAction;
import com.fiskmods.heroes.common.network.SHNetwork;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Suit Fabricator & Cosmic Suit Fabricator GUI screen ported from 1.7.10 GuiSuitFabricator:
 * - Print / Claim button at (leftPos + 92, topPos + 73, 72x15)
 * - Hero Drive slot cycler at (leftPos + 30, topPos + 65, 18x18) (left-click next, right-click prev)
 * - Progress bar at (leftPos + 53, topPos + 60, max 110x6)
 * - Ghost overlays + red tint & hover tooltips for missing recipe items & Suit Core
 * - Live rotating 3D suit preview at (leftPos + 30, topPos + 58)
 */
public final class SuitFabricatorScreen extends AbstractContainerScreen<SuitFabricatorMenu>
{
    private static final ResourceLocation TEXTURE = FiskHeroes.id("textures/gui/container/suit_fabricator.png");
    private static final int BTN_X = 92;
    private static final int BTN_Y = 73;
    private static final int BTN_W = 72;
    private static final int BTN_H = 15;

    private ItemStack hoveredMissingStack = ItemStack.EMPTY;

    public SuitFabricatorScreen(SuitFabricatorMenu menu, Inventory inventory, Component title)
    {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 182;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    private boolean isButtonActive()
    {
        return menu.hasFinishedOutput() || (!menu.isPrinting() && menu.canPrintClient());
    }

    private boolean isOverButton(double mouseX, double mouseY)
    {
        int bx = leftPos + BTN_X;
        int by = topPos + BTN_Y;
        return mouseX >= bx && mouseX < bx + BTN_W && mouseY >= by && mouseY < by + BTN_H;
    }

    private boolean isOverHeroCycle(double mouseX, double mouseY)
    {
        int hx = leftPos + 30;
        int hy = topPos + 65;
        return mouseX >= hx && mouseX < hx + 18 && mouseY >= hy && mouseY < hy + 18;
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
        hoveredMissingStack = ItemStack.EMPTY;
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        if (menu.isPrinting() || menu.hasFinishedOutput())
        {
            int barWidth = menu.isPrinting()
                    ? Math.min(110, 110 * menu.getPrintTime() / SuitFabricatorBlockEntity.MAX_PRINT_TIME)
                    : 110;
            if (barWidth > 0)
            {
                graphics.blit(TEXTURE, leftPos + 53, topPos + 60, 0, 182 + (menu.isCosmic() ? 6 : 0), barWidth, 6);
            }
        }

        HeroIteration iter = menu.getSelectedIteration();
        FabricatorRecipes.Recipe recipe = iter != null ? FabricatorRecipes.get(iter.getHero()) : null;
        if (iter != null)
        {
            if (recipe != null && recipe.cosmic() && !menu.isCosmic())
            {
                graphics.blit(TEXTURE, leftPos + 17, topPos + 28, 176, 45, 26, 26);
            }
            else
            {
                SuitGuiPreviewRenderer.render(graphics, iter, leftPos + 30, topPos + 58, 18.0F, partialTick);
            }

            if (ModItems.heroArmor != null)
            {
                int coreSlot = Math.max(0, iter.getHero().getCorePieceOfSet());
                ItemStack icon = ItemHeroArmor.create(iter, ModItems.heroArmor[coreSlot]);
                graphics.renderItem(icon, leftPos + 31, topPos + 66);
            }

            if (recipe != null && !menu.isPrinting() && !menu.hasFinishedOutput())
            {
                renderMissingIngredients(graphics, recipe, mouseX, mouseY);
            }
        }

        // Render Print / Claim button
        int bx = leftPos + BTN_X;
        int by = topPos + BTN_Y;
        boolean active = isButtonActive();
        boolean hovered = isOverButton(mouseX, mouseY);
        int uvState = !active ? 0 : (hovered ? 2 : 1);
        graphics.blit(TEXTURE, bx, by, 176, uvState * BTN_H, BTN_W, BTN_H);
        Component btnLabel = Component.translatable(menu.hasFinishedOutput()
                ? "gui.suit_fabricator.printed"
                : "gui.suit_fabricator.print");
        int textColor = !active ? 0x919191 : (hovered ? 0xFFFFA0 : 0x686868);
        graphics.drawString(font, btnLabel, bx + (BTN_W - font.width(btnLabel)) / 2, by + 4, textColor, false);
    }

    private void renderMissingIngredients(GuiGraphics graphics, FabricatorRecipes.Recipe recipe, int mouseX, int mouseY)
    {
        Map<Item, Integer> missing = new HashMap<>(recipe.items());
        for (int i = 0; i < 10 && !missing.isEmpty(); ++i)
        {
            ItemStack inSlot = menu.container.getItem(i);
            if (inSlot.isEmpty()) continue;
            Integer count = missing.get(inSlot.getItem());
            if (count != null)
            {
                int rem = count - inSlot.getCount();
                if (rem <= 0) missing.remove(inSlot.getItem());
                else missing.put(inSlot.getItem(), rem);
            }
        }

        List<ItemStack> stacks = missing.entrySet().stream()
                .map(e -> new ItemStack(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingInt((ItemStack s) -> s.getCount()).reversed())
                .toList();

        int stackIdx = 0;
        int remainingInCurrent = stacks.isEmpty() ? 0 : stacks.get(0).getCount();
        for (int slotIdx = 0; slotIdx < 10 && stackIdx < stacks.size(); ++slotIdx)
        {
            Slot slot = menu.slots.get(slotIdx);
            if (slot.hasItem()) continue;

            int chunk = Math.min(64, remainingInCurrent);
            ItemStack ghost = new ItemStack(stacks.get(stackIdx).getItem(), chunk);
            drawGhostMissing(graphics, ghost, leftPos + slot.x, topPos + slot.y, mouseX, mouseY);

            remainingInCurrent -= chunk;
            if (remainingInCurrent <= 0)
            {
                ++stackIdx;
                if (stackIdx < stacks.size()) remainingInCurrent = stacks.get(stackIdx).getCount();
            }
        }

        Slot coreSlot = menu.slots.get(11);
        if (!coreSlot.hasItem())
        {
            drawGhostMissing(graphics, new ItemStack(ModItems.SUIT_CORE.get(), 1),
                    leftPos + coreSlot.x, topPos + coreSlot.y, mouseX, mouseY);
        }
    }

    private void drawGhostMissing(GuiGraphics graphics, ItemStack stack, int x, int y, int mouseX, int mouseY)
    {
        graphics.renderFakeItem(stack, x, y);
        graphics.renderItemDecorations(font, stack, x, y);
        graphics.fill(x, y, x + 16, y + 16, 0x55FF0000);
        if (mouseX >= x - 1 && mouseX < x + 17 && mouseY >= y - 1 && mouseY < y + 17)
        {
            hoveredMissingStack = stack;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if (button == 0 && isOverButton(mouseX, mouseY) && isButtonActive())
        {
            SHNetwork.sendToServer(new PacketFabricatorAction(PacketFabricatorAction.ACTION_PRINT_OR_CLAIM));
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return true;
        }
        if ((button == 0 || button == 1) && !menu.isPrinting() && menu.getSelectedIteration() != null
                && isOverHeroCycle(mouseX, mouseY))
        {
            SHNetwork.sendToServer(new PacketFabricatorAction(button == 0
                    ? PacketFabricatorAction.ACTION_NEXT_HERO
                    : PacketFabricatorAction.ACTION_PREV_HERO));
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);

        if (!hoveredMissingStack.isEmpty())
        {
            List<Component> lines = List.of(
                    hoveredMissingStack.getHoverName(),
                    Component.translatable("gui.suit_fabricator.missing").withStyle(ChatFormatting.RED));
            graphics.renderTooltip(font, lines, Optional.empty(), mouseX, mouseY);
            return;
        }

        ItemStack drive = menu.container.getItem(10);
        HeroIteration iter = menu.getSelectedIteration();
        FabricatorRecipes.Recipe recipe = iter != null ? FabricatorRecipes.get(iter.getHero()) : null;

        if (mouseX >= leftPos + 12 && mouseX < leftPos + 48 && mouseY >= topPos + 17 && mouseY < topPos + 65)
        {
            if (drive.isEmpty())
            {
                graphics.renderTooltip(font, Component.translatable("gui.suit_fabricator.no_drive")
                        .withStyle(ChatFormatting.RED), mouseX, mouseY);
                return;
            }
            if (recipe != null && recipe.cosmic() && !menu.isCosmic())
            {
                graphics.renderTooltip(font, Component.translatable("gui.suit_fabricator.not_cosmic")
                        .withStyle(ChatFormatting.RED), mouseX, mouseY);
                return;
            }
        }

        if ((menu.isPrinting() || menu.hasFinishedOutput())
                && mouseX >= leftPos + 52 && mouseX < leftPos + 164 && mouseY >= topPos + 59 && mouseY < topPos + 67)
        {
            int pct = menu.isPrinting()
                    ? Mth.floor(Math.min(100.0F, menu.getPrintTime() * 100.0F / SuitFabricatorBlockEntity.MAX_PRINT_TIME))
                    : 100;
            List<Component> lines = List.of(
                    Component.translatable(menu.isPrinting() ? "gui.suit_fabricator.printing" : "gui.suit_fabricator.printed"),
                    Component.literal(pct + "%").withStyle(ChatFormatting.GRAY));
            graphics.renderTooltip(font, lines, Optional.empty(), mouseX, mouseY);
            return;
        }

        if (!menu.isPrinting() && iter != null && recipe != null)
        {
            if (isOverButton(mouseX, mouseY) && !menu.hasFinishedOutput())
            {
                List<Component> reqLines = new ArrayList<>();
                recipe.items().forEach((item, count) -> reqLines.add(
                        Component.literal(count + "x ").withStyle(ChatFormatting.YELLOW)
                                .append(new ItemStack(item).getHoverName().copy().withStyle(ChatFormatting.WHITE))));
                graphics.renderTooltip(font, reqLines, Optional.empty(), mouseX, mouseY);
            }
            else if (isOverHeroCycle(mouseX, mouseY))
            {
                List<Hero> available = ItemSuitDrive.getAvailableHeroes(drive);
                int idx = available.isEmpty() ? 1 : (Math.floorMod(ItemSuitDrive.getIndex(drive), available.size()) + 1);
                List<Component> lines = List.of(
                        iter.getFormattedName(),
                        Component.translatable("gui.suit_fabricator.slot", idx, Math.max(1, available.size()))
                                .withStyle(ChatFormatting.GRAY));
                graphics.renderTooltip(font, lines, Optional.empty(), mouseX, mouseY);
            }
        }
    }
}
