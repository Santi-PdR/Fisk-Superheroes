package com.fiskmods.heroes.client.render;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.data.var.Vars;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/** The original V9 wrapper fades lightning and flicker colors towards its blue trail. */
final class VelocityTrailColor
{
    private VelocityTrailColor() { }

    /** Vanilla does not reliably expose another player's hidden effect to observers. */
    static boolean isActive(Player player)
    {
        var data = SHDataCapabilities.getPlayer(player);
        return player.hasEffect(com.fiskmods.heroes.common.hero.modifier.ModEffects.VELOCITY_NINE.get())
                || data != null && data.getData().get(Vars.VEL9_CONVERT) > 0.0F;
    }

    static int blend(int base, Player player, float partialTick)
    {
        var data = SHDataCapabilities.getPlayer(player);
        float amount = data != null ? data.getData().getInterpolated(Vars.VEL9_CONVERT, partialTick)
                : player.hasEffect(com.fiskmods.heroes.common.hero.modifier.ModEffects.VELOCITY_NINE.get()) ? 1.0F : 0.0F;
        amount = Mth.clamp(amount, 0.0F, 1.0F);
        TrailDefinition velocity = TrailRegistry.get(FiskHeroes.id("velocity_nine"));
        if (amount <= 0.0F || velocity == null || velocity.lightning() == null) return base;
        var lightning = velocity.lightning();
        String source = velocity.resolveConstant(lightning.has("color") ? lightning.get("color").getAsString() : null);
        int target;
        try { target = source != null ? Long.decode(source).intValue() : 0xFFFFFF; }
        catch (NumberFormatException e) { return base; }
        int red = (int) Mth.lerp(amount, (base >> 16) & 255, (target >> 16) & 255);
        int green = (int) Mth.lerp(amount, (base >> 8) & 255, (target >> 8) & 255);
        int blue = (int) Mth.lerp(amount, base & 255, target & 255);
        return red << 16 | green << 8 | blue;
    }
}
