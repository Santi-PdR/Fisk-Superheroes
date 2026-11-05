package com.fiskmods.heroes.common.item;

import java.util.List;
import javax.annotation.Nullable;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.hero.HeroIteration;
import com.fiskmods.heroes.common.hero.HeroTracker;
import com.fiskmods.heroes.common.sound.SHSounds;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * Miniaturized suit item ported from 1.7.10 ItemMiniSuit (used by The Atom's MINIATURIZE_SUIT ability).
 */
public class ItemMiniSuit extends Item
{
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    public ItemMiniSuit(Properties properties)
    {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        ItemStack stack = player.getItemInHand(hand);
        ItemStack[] stored = ItemFlashRing.getContainedArmor(stack);
        if (stored == null)
        {
            var atom = com.fiskmods.heroes.common.hero.Hero.REGISTRY.getHero(FiskHeroes.id("atom"));
            if (atom != null && ModItems.heroArmor != null)
            {
                stored = ItemFlashRing.getContainedArmor(ItemFlashRing.create(this, atom.getDefaultIteration()));
            }
        }
        if (stored == null) return InteractionResultHolder.pass(stack);

        if (!level.isClientSide)
        {
            for (int i = 0; i < 4; ++i)
            {
                ItemStack existing = player.getItemBySlot(ARMOR_SLOTS[i]);
                if (!existing.isEmpty())
                {
                    player.setItemSlot(ARMOR_SLOTS[i], ItemStack.EMPTY);
                    if (!player.getInventory().add(existing))
                    {
                        player.drop(existing, false);
                    }
                }
                if (!stored[i].isEmpty())
                {
                    player.setItemSlot(ARMOR_SLOTS[i], stored[i].copy());
                }
            }
            if (!player.getAbilities().instabuild)
            {
                stack.shrink(1);
            }
            else
            {
                player.setItemInHand(hand, ItemStack.EMPTY);
            }
            HeroTracker.update(player);
            SHSounds.play(player, FiskHeroes.id("atom_grow"), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag)
    {
        HeroIteration iter = ItemFlashRing.getContainedHero(stack);
        if (iter != null)
        {
            tooltip.add(iter.getFormattedName().copy().withStyle(ChatFormatting.GRAY));
            if (iter.getVersionKey() != null)
            {
                tooltip.add(Component.translatable(iter.getVersionKey()).withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }

    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer)
    {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions()
        {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer()
            {
                return com.fiskmods.heroes.client.render.MiniSuitItemRenderer.getInstance();
            }
        });
    }
}
