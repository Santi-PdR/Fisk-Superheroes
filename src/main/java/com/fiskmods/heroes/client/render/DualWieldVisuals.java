package com.fiskmods.heroes.client.render;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.item.ItemDualSword;
import com.fiskmods.heroes.common.item.ModItems;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Render-only second hand for a dual stack; never writes to player inventory. */
public final class DualWieldVisuals
{
    private static final Map<UUID, String> DIAGNOSTICS = new ConcurrentHashMap<>();

    private DualWieldVisuals() {}

    public static ItemStack firstPersonOffhand(ItemStack cachedOffhand, LocalPlayer player)
    {
        ItemStack visual = thirdPersonOffhand(player);
        // Vanilla can still be animating out an old offhand stack; do not obscure it.
        return cachedOffhand.isEmpty() && player.getOffhandItem().isEmpty() ? visual : cachedOffhand;
    }

    public static ItemStack thirdPersonOffhand(LivingEntity entity)
    {
        ItemStack actual = entity.getOffhandItem();
        if (!(entity instanceof Player player)) return actual;
        ItemStack main = player.getMainHandItem();
        if (!supportsDual(main) || !ItemDualSword.isDual(main)) return actual;

        String mode = actual.isEmpty() ? "synthetic" : "occupied";
        String state = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(main.getItem()) + "|" + mode;
        if (!state.equals(DIAGNOSTICS.put(player.getUUID(), state)))
        {
            FiskHeroes.LOGGER.info("Dual visual offhand: player={}, item={}, mode={}",
                    player.getGameProfile().getName(), net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(main.getItem()), mode);
        }
        return actual.isEmpty() ? main : actual;
    }

    private static boolean supportsDual(ItemStack stack)
    {
        return stack.is(ModItems.KATANA.get()) || stack.is(ModItems.CHOKUTO.get())
                || stack.is(ModItems.SCIMITAR.get()) || stack.is(ModItems.TACTICAL_TONFA.get())
                || stack.is(ModItems.DESERT_EAGLE.get()) || stack.is(ModItems.BERETTA_93R.get());
    }
}
