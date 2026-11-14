package com.fiskmods.heroes.client.gui;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.block.SuitAugmentorMenu;
import com.fiskmods.heroes.common.hero.HeroAttribute;
import com.fiskmods.heroes.common.hero.ItemHeroArmor;
import com.fiskmods.heroes.common.hero.augment.AugmentSet;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Uses the original machine artwork; server-authoritative clicks consume one matching upgrade. */
public final class SuitAugmentorScreen extends AbstractContainerScreen<SuitAugmentorMenu>
{
    private static final ResourceLocation TEXTURE = FiskHeroes.id("textures/gui/container/suit_augmentor.png");
    public SuitAugmentorScreen(SuitAugmentorMenu menu, Inventory inventory, Component title)
    {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 215;
        inventoryLabelY = 121;
    }

    private static int icon(HeroAttribute attribute)
    {
        return switch (attribute)
        {
            case DAMAGE_REDUCTION -> 0; case FALL_RESISTANCE -> 1; case WEAPON_DAMAGE -> 2;
            case PUNCH_DAMAGE -> 3; case BASE_SPEED_LEVELS -> 4; case JUMP_HEIGHT -> 5;
            case BOW_DRAWBACK -> 6; case ARROW_DAMAGE -> 7; case KNOCKBACK -> 8;
            case MAX_HEALTH -> 9; case SPRINT_SPEED -> 10; case BASE_SPEED -> 11;
            case STEP_HEIGHT -> 12; case IMPACT_DAMAGE -> 15; case REACH_DISTANCE -> 16;
        };
    }

    private static Component attributeName(HeroAttribute attribute)
    {
        return Component.translatable(attribute.getDisplayNameKey());
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY)
    {
        graphics.drawString(font, title, (imageWidth - font.width(title)) / 2, 6, 0x404040, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY)
    {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        AugmentSet set = menu.getAugments();
        int packed = ItemHeroArmor.getUpgrades(menu.input.getItem(0));
        for (AugmentSet.Entry entry : set.entries())
        {
            int i = entry.index(), icon = icon(entry.attribute());
            int x = leftPos + 11 + i / 3 * 79, y = topPos + 20 + i % 3 * 14;
            graphics.blit(TEXTURE, x, y, 176 + icon % 6 * 12, 36 + icon / 6 * 12, 12, 12);
            String label = attributeName(entry.attribute()).getString();
            String name = font.width(label) <= 80 ? label : font.plainSubstrByWidth(label, 73) + "…";
            // Slightly smaller text preserves complete attribute names in the original two-column panel.
            graphics.pose().pushPose();
            graphics.pose().translate(x + 14, y + 3, 0);
            graphics.pose().scale(.75F, .75F, 1F);
            graphics.drawString(font, name, 0, 0, 0xFFFFFF, false);
            graphics.pose().popPose();
            int bx = leftPos + 12 + i * 21, by = topPos + 72;
            boolean eligible = menu.canUpgrade(i);
            graphics.blit(TEXTURE, bx, by, eligible ? 212 : 196, 16, 16, 16);
            graphics.blit(TEXTURE, bx + 2, by + 2, 176 + icon % 6 * 12, 36 + icon / 6 * 12, 12, 12);
            int level = entry.min() + set.level(packed, entry);
            graphics.drawString(font, Integer.toString(level), bx + 10, by + 9, 0xFFFF55, true);
        }
    }

    @Override public boolean mouseClicked(double x, double y, int button)
    {
        if (button == 0)
            for (AugmentSet.Entry entry : menu.getAugments().entries())
            {
                int bx = leftPos + 12 + entry.index() * 21, by = topPos + 72;
                if (x >= bx && x < bx + 16 && y >= by && y < by + 16 && menu.canUpgrade(entry.index()))
                {
                    if (minecraft != null && minecraft.gameMode != null)
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, entry.index());
                    return true;
                }
            }
        return super.mouseClicked(x, y, button);
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        AugmentSet set = menu.getAugments();
        int packed = ItemHeroArmor.getUpgrades(menu.input.getItem(0));
        for (AugmentSet.Entry entry : set.entries())
        {
            int x = leftPos + 12 + entry.index() * 21, y = topPos + 72;
            if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16)
            {
                int level = entry.min() + set.level(packed, entry);
                Component status = level >= entry.max() ? Component.translatable("gui.suit_augmentor.max")
                        : menu.canUpgrade(entry.index()) ? Component.translatable("gui.fiskheroes.augment_ready")
                        : Component.translatable("gui.fiskheroes.augment_need",
                                Component.translatable("item.suit_upgrade_" + level + ".name"));
                graphics.renderTooltip(font, java.util.List.of(attributeName(entry.attribute()),
                        Component.translatable("gui.fiskheroes.augment_level", level, entry.max()), status),
                        java.util.Optional.empty(), mouseX, mouseY);
                break;
            }
        }
    }
}
