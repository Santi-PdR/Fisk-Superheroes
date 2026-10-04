package com.fiskmods.heroes.client.gui;

import java.util.List;

import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.hero.Hero;
import com.fiskmods.heroes.common.hero.HeroIteration;
import com.fiskmods.heroes.common.hero.WeaponList;
import com.fiskmods.heroes.common.hero.equipment.WeaponHelper;
import com.fiskmods.heroes.common.network.PacketEquipment;
import com.fiskmods.heroes.common.network.SHNetwork;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Hold-to-open radial selector for the weapon candidates declared by the worn hero. */
public final class EquipmentWheelScreen extends Screen
{
    private final List<WeaponList.Entry> entries;
    private final ItemStack[] selected;
    private final int wheelColor;
    private int hovered = -1;

    public EquipmentWheelScreen(Hero hero, ItemStack[] selected, int wheelColor)
    {
        super(Component.translatable("gui.fiskheroes.equipment"));
        this.entries = hero.getWeaponStacks().entries().toList();
        this.selected = selected;
        this.wheelColor = wheelColor & 0xFFFFFF;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        graphics.fill(0, 0, width, height, 0x66000000);
        int cx = width / 2;
        int cy = height / 2;
        int radius = Math.min(width, height) / 4;
        hovered = getHovered(mouseX, mouseY, cx, cy, radius);

        for (int i = 0; i < entries.size(); ++i)
        {
            double angle = -Math.PI / 2 + Math.PI * 2 * i / entries.size();
            int x = cx + (int) (Math.cos(angle) * radius) - 9;
            int y = cy + (int) (Math.sin(angle) * radius) - 9;
            boolean available = i < selected.length && selected[i] != null && !selected[i].isEmpty();
            int bg = i == hovered ? 0xCC000000 | wheelColor
                    : available ? 0xAA000000 | darken(wheelColor, 0.48F) : 0xAA303030;
            graphics.fill(x - 4, y - 4, x + 22, y + 22, bg);
            ItemStack icon = available ? selected[i] : entries.get(i).value();
            if (icon != null && !icon.isEmpty()) graphics.renderItem(icon, x, y);
        }

        if (hovered >= 0)
        {
            WeaponList.Entry entry = entries.get(hovered);
            ItemStack icon = hovered < selected.length && selected[hovered] != null && !selected[hovered].isEmpty()
                    ? selected[hovered] : entry.value();
            if (icon != null && !icon.isEmpty()) graphics.drawCenteredString(font, icon.getHoverName(), cx, cy + radius + 24, 0xFFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if (button == 0 && hovered >= 0)
        {
            SHNetwork.sendToServer(new PacketEquipment(entries.get(hovered).index()));
            Minecraft.getInstance().setScreen(null);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private int getHovered(double mouseX, double mouseY, int cx, int cy, int radius)
    {
        if (entries.isEmpty()) return -1;
        double dx = mouseX - cx;
        double dy = mouseY - cy;
        if (dx * dx + dy * dy < radius * radius * 0.12) return -1;
        double angle = Math.atan2(dy, dx) + Math.PI / 2;
        if (angle < 0) angle += Math.PI * 2;
        return (int) Math.floor((angle / (Math.PI * 2)) * entries.size() + 0.5) % entries.size();
    }

    public static EquipmentWheelScreen createForCurrentPlayer()
    {
        var player = Minecraft.getInstance().player;
        if (player == null) return null;
        SHPlayerData data = SHDataCapabilities.getPlayer(player);
        HeroIteration iteration = data != null ? data.getHero() : null;
        if (iteration == null) return null;
        Hero hero = iteration.getHero();
        if (!WeaponHelper.canEquipWeapon(player, hero)) return null;
        ItemStack[] selected = WeaponHelper.getEquippedWeapons(player, hero);
        return selected != null ? new EquipmentWheelScreen(hero, selected, getWheelColor(iteration)) : null;
    }

    private static int getWheelColor(HeroIteration iteration)
    {
        int fallback = 0x2860A8;
        com.fiskmods.heroes.client.render.HeroModelData model =
                com.fiskmods.heroes.client.render.HeroModelRegistry.get(iteration);
        if (model == null) return fallback;
        com.google.gson.JsonObject wheel = model.getCustom().get("fiskheroes:equipment_wheel");
        if (wheel == null || !wheel.has("color")) return fallback;
        String raw = wheel.get("color").getAsString();
        try
        {
            return Integer.decode(raw) & 0xFFFFFF;
        }
        catch (NumberFormatException ignored)
        {
            return fallback;
        }
    }

    private static int darken(int color, float factor)
    {
        int red = Math.round(((color >> 16) & 0xFF) * factor);
        int green = Math.round(((color >> 8) & 0xFF) * factor);
        int blue = Math.round((color & 0xFF) * factor);
        return red << 16 | green << 8 | blue;
    }
}
