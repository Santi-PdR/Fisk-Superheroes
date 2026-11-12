package com.fiskmods.heroes.client.hud;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.data.var.Vars;
import com.fiskmods.heroes.common.item.ItemQuiver;
import com.fiskmods.heroes.common.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Five-slot arrow selector from the original quiver HUD. */
public final class QuiverHud
{
    private static final ResourceLocation WIDGETS = FiskHeroes.id("textures/gui/widgets.png");
    private static ItemStack highlighted = ItemStack.EMPTY;
    private static int highlightTicks;
    private static String diagnostic = "";

    private QuiverHud() {}

    /** Tick even when not drawing the HUD, just as vanilla's selected-item name fades. */
    public static void tick(Minecraft mc)
    {
        if (mc.player == null)
        {
            highlighted = ItemStack.EMPTY;
            highlightTicks = 0;
            diagnostic = "";
            return;
        }
        if (mc.isPaused()) return;
        ItemStack quiver = ItemQuiver.findQuiver(mc.player);
        var data = SHDataCapabilities.getPlayer(mc.player);
        if (quiver.isEmpty() || data == null)
        {
            highlighted = ItemStack.EMPTY;
            highlightTicks = 0;
            return;
        }
        int selected = Byte.toUnsignedInt(data.getData().get(Vars.SELECTED_ARROW)) % 5;
        ItemStack stack = ItemQuiver.createInventory(quiver).getStackInSlot(selected);
        if (stack.isEmpty()) highlightTicks = 0;
        else if (!ItemStack.isSameItemSameTags(stack, highlighted)) highlightTicks = 40;
        else if (highlightTicks > 0) --highlightTicks;
        highlighted = stack.copy();
    }

    public static void render(GuiGraphics graphics)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.screen != null
                || !mc.player.getMainHandItem().is(ModItems.COMPOUND_BOW.get())) return;
        ItemStack quiver = ItemQuiver.findQuiver(mc.player);
        var data = SHDataCapabilities.getPlayer(mc.player);
        if (quiver.isEmpty() || data == null) return;
        int selected = Byte.toUnsignedInt(data.getData().get(Vars.SELECTED_ARROW)) % 5;
        int x = graphics.guiWidth() - 22, y = graphics.guiHeight() / 2 - 51;
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        graphics.blit(WIDGETS, x, y, 0, 0, 22, 102);
        graphics.blit(WIDGETS, x - 1, y - 1 + selected * 20, 22, 0, 24, 24);
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
        var inventory = ItemQuiver.createInventory(quiver);
        int present = 0;
        for (int slot = 0; slot < 5; ++slot)
        {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            ++present;
            graphics.renderItem(stack, x + 3, y + 3 + slot * 20);
            graphics.renderItemDecorations(mc.font, stack, x + 3, y + 3 + slot * 20);
        }
        if (highlightTicks > 0 && !highlighted.isEmpty())
        {
            var name = highlighted.getHoverName();
            int alpha = Math.min(255, highlightTicks * 256 / 10);
            int left = x - 5 - mc.font.width(name);
            graphics.drawString(mc.font, name, Math.max(0, left), y + 6 + selected * 20,
                    alpha << 24 | 0xFFFFFF, true);
        }
        if (Boolean.getBoolean("fiskheroes.ciBowDiagnostics"))
        {
            String state = selected + "|" + present;
            if (!state.equals(diagnostic))
            {
                diagnostic = state;
                FiskHeroes.LOGGER.info("Quiver HUD rendered: selected={}, occupied={}", selected, present);
            }
        }
    }
}
