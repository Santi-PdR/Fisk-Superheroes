package com.fiskmods.heroes.common.hero.modifier;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.data.var.Vars;
import com.fiskmods.heroes.common.item.ModItems;
import com.fiskmods.heroes.common.sound.SHSounds;

import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;

/** The original item shield's 35 degree coverage, shared health and recovery state. */
public final class ItemShieldBlocking
{
    private ItemShieldBlocking() {}

    public static void onAttack(LivingAttackEvent event)
    {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide
                || !player.isUsingItem() || !player.getUseItem().is(ModItems.CAPTAIN_AMERICAS_SHIELD.get())
                || event.getAmount() <= 0.0F) return;
        var data = SHDataCapabilities.getPlayer(player);
        if (data == null || data.getData().get(Vars.SHIELD_COOLDOWN) > 0
                || data.getData().get(Vars.SHIELD_BLOCKING)) return;
        if (!inCoverage(player, event.getSource())) return;

        // The old shield cancels repeat hits during the short hit animation, but
        // neither wears the item nor charges shield damage a second time.
        if (player.hurtTime > 0)
        {
            event.setCanceled(true);
            return;
        }
        var stack = player.getUseItem();
        var hand = player.getUsedItemHand();
        stack.hurtAndBreak(1, player, entity -> entity.broadcastBreakEvent(hand));
        var vars = data.getData();
        float damage = vars.get(Vars.SHIELD_DAMAGE) + event.getAmount();
        vars.set(Vars.SHIELD_DAMAGE, damage);
        vars.set(Vars.TICKS_SINCE_SHIELD_DAMAGED, (short) 0);
        boolean broken = damage >= 80.0F;
        if (broken) vars.set(Vars.SHIELD_COOLDOWN, (short) 100);
        if (Boolean.getBoolean("fiskheroes.ciShieldDiagnostics"))
            FiskHeroes.LOGGER.info("Shield item block: player={}, hit={}, total={}, broken={}",
                    player.getName().getString(), event.getAmount(), damage, broken);

        var source = event.getSource();
        if (source.getEntity() instanceof LivingEntity attacker
                && (source.getDirectEntity() == attacker || source.is(DamageTypeTags.IS_EXPLOSION)))
        {
            float factor = broken ? 0.3F : 0.2F;
            // Legacy knockback applies vanilla's 0.4 impulse, then scales it by
            // the incoming hit (capped at twenty) and the shield's factor.
            player.knockback(0.4F * Math.min(event.getAmount(), 20.0F) * factor,
                    attacker.getX() - player.getX(), attacker.getZ() - player.getZ());
        }
        if (broken || !source.is(DamageTypeTags.IS_EXPLOSION))
        {
            float pitch = (broken ? 0.6F : 0.8F) + 0.1F * player.getRandom().nextFloat()
                    - Math.min(event.getAmount(), 10.0F) / 50.0F;
            SHSounds.play(player, FiskHeroes.id("item.shield.hit"), SoundSource.PLAYERS, 0.6F, pitch);
        }
        if (!broken)
        {
            player.hurtTime = 7;
            event.setCanceled(true);
        }
    }

    /** Disable vanilla's wider shield arc and damage handling for this item only. */
    public static void onVanillaBlock(ShieldBlockEvent event)
    {
        if (event.getEntity().getUseItem().is(ModItems.CAPTAIN_AMERICAS_SHIELD.get())) event.setCanceled(true);
    }

    private static boolean inCoverage(Player player, DamageSource source)
    {
        if (source.is(DamageTypes.FALL) && player.getXRot() > 60.0F || source.is(DamageTypes.FLY_INTO_WALL)) return true;
        var direct = source.getDirectEntity();
        if (direct == null || source.is(DamageTypeTags.BYPASSES_ARMOR)) return false;
        double dx = direct.getX() - player.getX();
        double dy = direct.getY() + direct.getBbHeight() * 0.5D - player.getEyeY();
        double dz = direct.getZ() - player.getZ();
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
        float pitch = -(float) Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        return Math.abs(Mth.wrapDegrees(yaw - player.getYRot())) < 35.0F
                && Math.abs(Mth.wrapDegrees(pitch - player.getXRot())) < 35.0F;
    }
}
