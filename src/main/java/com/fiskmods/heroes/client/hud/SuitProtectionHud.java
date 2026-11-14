package com.fiskmods.heroes.client.hud;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.hero.attribute.SHAttributes;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.ForgeGui;

/** Original 81-pixel protection meter, integrated with Forge's survival HUD stack. */
public final class SuitProtectionHud
{
    private static final ResourceLocation ICONS = FiskHeroes.id("textures/gui/icons.png");
    private static final java.util.Set<String> DIAGNOSTICS = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private SuitProtectionHud() {}

    public static void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height)
    {
        var mc = gui.getMinecraft();
        if (mc.player == null || mc.gameMode == null || mc.options.hideGui || !gui.shouldDrawSurvivalElements()) return;
        var data = SHDataCapabilities.getPlayer(mc.player);
        if (data == null || data.getHeroType() == null) return;
        var attribute = mc.player.getAttribute(SHAttributes.DAMAGE_REDUCTION.get());
        float reduction = attribute != null ? (float) Mth.clamp(attribute.getValue(), 0.0D, 1.0D) : 0.0F;
        if (reduction <= 0.0F) return;

        // Forge reserves an armor row even at zero armor. Reuse that otherwise empty row;
        // with ordinary armor, put the suit meter above it and reserve another row.
        boolean vanillaArmor = mc.player.getArmorValue() > 0;
        int x = width / 2 - 91;
        int y = height - gui.leftHeight + (vanillaArmor ? 0 : 10);
        int filled = Mth.clamp(Math.round(reduction * 79.0F), 0, 79);
        gui.setupOverlayRenderState(true, false);
        graphics.blit(ICONS, x, y, 0, 0, 81, 9);
        if (filled > 0) graphics.blit(ICONS, x + 1, y + 1, 81, 0, filled, 7);
        if (vanillaArmor) gui.leftHeight += 10;

        String key = data.getHeroType().getRegistryName() + ":" + width + "x" + height + ":" + vanillaArmor;
        if (DIAGNOSTICS.add(key))
            FiskHeroes.LOGGER.info("Suit protection HUD: hero={}, fill={}, armor={}, x={}, y={}, screen={}x{}",
                    data.getHeroType().getRegistryName(), filled, vanillaArmor, x, y, width, height);
    }
}
