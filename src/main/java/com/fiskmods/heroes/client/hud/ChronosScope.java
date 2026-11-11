package com.fiskmods.heroes.client.hud;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.data.var.Vars;
import com.fiskmods.heroes.common.item.ModItems;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** First-person Chronos rifle scope; the original art is a 16:9 transparent overlay. */
public final class ChronosScope
{
    private static final ResourceLocation TEXTURE = FiskHeroes.id("textures/misc/scope.png");
    private static boolean reported;

    private ChronosScope() {}

    public static void render(GuiGraphics graphics, float partialTick)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.screen != null
                || !mc.options.getCameraType().isFirstPerson()
                || !mc.player.getMainHandItem().is(ModItems.CHRONOS_RIFLE.get())) return;
        var data = SHDataCapabilities.getPlayer(mc.player);
        if (data == null) return;
        float scope = Mth.clamp(data.getData().getInterpolated(Vars.SCOPE_TIMER, partialTick) * 2.0F - 1.0F, 0.0F, 1.0F);
        if (scope <= 0.0F) return;
        int width = graphics.guiWidth(), height = graphics.guiHeight();
        int drawWidth = Math.max(width, (int) Math.ceil(height * 16.0D / 9.0D));
        int drawHeight = Math.max(height, (int) Math.ceil(width * 9.0D / 16.0D));
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, scope);
        graphics.blit(TEXTURE, (width - drawWidth) / 2, (height - drawHeight) / 2,
                drawWidth, drawHeight, 0.0F, 0.0F, 1920, 1080, 1920, 1080);
        graphics.flush();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
        if (scope >= 0.95F && !reported && Boolean.getBoolean("fiskheroes.ciEnergyGunDiagnostics"))
        {
            reported = true;
            FiskHeroes.LOGGER.info("Chronos scope overlay rendered");
        }
    }
}
