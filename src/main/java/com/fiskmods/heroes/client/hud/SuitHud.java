package com.fiskmods.heroes.client.hud;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import com.fiskmods.heroes.client.keybinds.SHKeyBinds;
import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.hero.Hero;
import com.fiskmods.heroes.common.hero.HeroIteration;
import com.fiskmods.heroes.common.util.SuitHudLayout;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/** Suit name and clickable ability bindings, positioned above the vanilla bottom HUD. */
public final class SuitHud
{
    private static final int COLOR_TEXT = 0xFFFFFF;
    private static final int COLOR_KEY = 0xFFAA00;
    private static final int COLOR_BACKGROUND = 0x55000000;
    private static final java.util.Set<String> DIAGNOSTICS = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private record Row(int index, String key, String label, boolean enabled) {}
    private record View(HeroIteration hero, List<Row> rows, SuitHudLayout.Layout layout) {}

    private SuitHud() {}

    private static View view(Minecraft mc)
    {
        LocalPlayer player = mc.player;
        if (player == null || mc.options.hideGui || mc.screen != null) return null;
        var data = SHDataCapabilities.getPlayer(player);
        HeroIteration iteration = data != null ? data.getHero() : null;
        if (iteration == null) return null;

        // Some packs bind two internal actions (e.g. AIM and ENERGY_PROJECTION) to the
        // same index and label. They are one control, while differently named actions that
        // share an index remain separate rows.
        LinkedHashMap<String, Row> unique = new LinkedHashMap<>();
        for (Hero.KeyBind binding : iteration.getHero().getKeyBinds())
        {
            var mapping = SHKeyBinds.get(binding.index());
            String key = binding.index() == -1 ? mc.options.keyAttack.getTranslatedKeyMessage().getString()
                    : mapping != null ? mapping.getTranslatedKeyMessage().getString() : "?";
            String label = Component.translatable(binding.keyName()).getString();
            boolean enabled = iteration.getHero().isKeyBindEnabled(player, binding.name());
            String id = binding.index() + "\u0000" + binding.keyName();
            Row previous = unique.get(id);
            unique.put(id, new Row(binding.index(), key, label, enabled || previous != null && previous.enabled()));
        }
        List<Row> rows = List.copyOf(unique.values());
        List<Integer> widths = new ArrayList<>(rows.size());
        for (Row row : rows) widths.add(Math.max(90, mc.font.width(row.key()) + mc.font.width(row.label()) + 10));
        int bottomReserve = mc.gui instanceof net.minecraftforge.client.gui.overlay.ForgeGui gui
                && mc.gameMode != null && !mc.options.hideGui && gui.shouldDrawSurvivalElements()
                ? gui.leftHeight + 10 : 60;
        return new View(iteration, rows, SuitHudLayout.calculate(mc.getWindow().getGuiScaledWidth(),
                mc.getWindow().getGuiScaledHeight(), widths, bottomReserve));
    }

    public static void render(GuiGraphics graphics, float partialTick)
    {
        Minecraft mc = Minecraft.getInstance();
        View view = view(mc);
        if (view == null) return;
        var layout = view.layout();
        Component name = view.hero().getFormattedName().copy().withStyle(ChatFormatting.AQUA);
        // Title and long translated bindings are clipped to the same bounds used for clicks.
        graphics.enableScissor(layout.titleX(), layout.titleY(), layout.titleX() + layout.titleWidth(), layout.titleY() + 10);
        graphics.drawString(mc.font, name, layout.titleX(), layout.titleY(), COLOR_TEXT, true);
        graphics.disableScissor();
        for (int i = 0; i < view.rows().size(); ++i)
        {
            Row row = view.rows().get(i);
            var rect = layout.rows().get(i);
            graphics.fill(rect.x(), rect.y(), rect.x() + rect.width(), rect.y() + rect.height(), COLOR_BACKGROUND);
            int content = Math.max(0, rect.width() - 4);
            int keyWidth = Math.min(mc.font.width(row.key()), Math.max(0, content / 2));
            String key = mc.font.plainSubstrByWidth(row.key(), keyWidth);
            int labelX = rect.x() + 2 + keyWidth + 4;
            String label = mc.font.plainSubstrByWidth(row.label(), Math.max(0, rect.x() + rect.width() - labelX - 2));
            graphics.drawString(mc.font, key, rect.x() + 2, rect.y() + 1, COLOR_KEY, true);
            graphics.drawString(mc.font, label, labelX, rect.y() + 1, row.enabled() ? COLOR_TEXT : 0x777777, true);
        }
        String id = view.hero().getRegistryName() + ":" + mc.getWindow().getGuiScaledWidth() + "x" + mc.getWindow().getGuiScaledHeight();
        if (DIAGNOSTICS.add(id))
            com.fiskmods.heroes.FiskHeroes.LOGGER.info("Ability HUD layout: hero={}, screen={}x{}, titleY={}, rows={}",
                    view.hero().getRegistryName(), mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(),
                    layout.titleY(), layout.rows());
    }

    /** Returns the index of the row drawn at the GUI-scaled pointer coordinates. */
    public static int findKeyBindAt(double mouseX, double mouseY)
    {
        View view = view(Minecraft.getInstance());
        if (view == null) return Integer.MIN_VALUE;
        for (int i = 0; i < view.rows().size(); ++i)
            if (view.layout().rows().get(i).contains(mouseX, mouseY)) return view.rows().get(i).index();
        return Integer.MIN_VALUE;
    }
}
