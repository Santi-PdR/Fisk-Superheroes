package com.fiskmods.heroes.common.item;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

import com.fiskmods.heroes.common.block.FabricatorRecipes;
import com.fiskmods.heroes.common.hero.Hero;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * Suit Data Drive item used by the Suit Fabricator & Cosmic Suit Fabricator.
 * Supports both explicit unlocked hero lists ("Heroes" NBT) and survival hero selection cycling.
 */
public class ItemSuitDrive extends Item
{
    public static final String TAG_HEROES = "Heroes";
    public static final String TAG_INDEX = "Index";

    public ItemSuitDrive(Properties properties)
    {
        super(properties.stacksTo(1));
    }

    public static List<Hero> getAvailableHeroes(ItemStack stack)
    {
        if (!stack.isEmpty() && stack.hasTag() && stack.getTag().contains(TAG_HEROES, Tag.TAG_LIST))
        {
            ListTag list = stack.getTag().getList(TAG_HEROES, Tag.TAG_STRING);
            List<Hero> explicit = new ArrayList<>();
            for (int i = 0; i < list.size(); ++i)
            {
                ResourceLocation id = ResourceLocation.tryParse(list.getString(i));
                if (id != null)
                {
                    Hero h = Hero.REGISTRY.getHero(id);
                    if (h != null && h.getPiecesToSet() > 0) explicit.add(h);
                }
            }
            if (!explicit.isEmpty()) return explicit;
        }
        return FabricatorRecipes.getCraftableHeroes();
    }

    public static int getIndex(ItemStack stack)
    {
        return stack.hasTag() ? stack.getTag().getInt(TAG_INDEX) : 0;
    }

    public static void setIndex(ItemStack stack, int index)
    {
        List<Hero> heroes = getAvailableHeroes(stack);
        if (heroes.isEmpty()) return;
        stack.getOrCreateTag().putInt(TAG_INDEX, Math.floorMod(index, heroes.size()));
    }

    public static void cycleIndex(ItemStack stack, int delta)
    {
        setIndex(stack, getIndex(stack) + delta);
    }

    @Nullable
    public static Hero getSelectedHero(ItemStack stack)
    {
        List<Hero> heroes = getAvailableHeroes(stack);
        if (heroes.isEmpty()) return null;
        return heroes.get(Math.floorMod(getIndex(stack), heroes.size()));
    }

    public static ItemStack createForHero(Hero hero)
    {
        ItemStack stack = new ItemStack(ModItems.SUIT_DRIVE.get());
        ListTag list = new ListTag();
        list.add(StringTag.valueOf(hero.getRegistryName().toString()));
        CompoundTag tag = stack.getOrCreateTag();
        tag.put(TAG_HEROES, list);
        tag.putInt(TAG_INDEX, 0);
        return stack;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide)
        {
            cycleIndex(stack, player.isShiftKeyDown() ? -1 : 1);
            Hero selected = getSelectedHero(stack);
            if (selected != null)
            {
                player.displayClientMessage(Component.translatable("item.superhero_armor.hero",
                        selected.getFormattedName()), true);
                player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.5F, 1.2F);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag)
    {
        Hero selected = getSelectedHero(stack);
        List<Hero> heroes = getAvailableHeroes(stack);
        if (selected != null)
        {
            tooltip.add(Component.translatable("item.superhero_armor.hero", selected.getFormattedName())
                    .withStyle(ChatFormatting.GRAY));
            if (selected.getTier() > 0)
            {
                tooltip.add(Component.translatable("item.superhero_armor.tier",
                        Component.translatable("tier." + selected.getTier())).withStyle(ChatFormatting.DARK_GRAY));
            }
            if (heroes.size() > 1)
            {
                int idx = Math.floorMod(getIndex(stack), heroes.size()) + 1;
                tooltip.add(Component.translatable("gui.suit_fabricator.slot", idx, heroes.size())
                        .withStyle(ChatFormatting.DARK_AQUA));
            }
        }
    }

    @Override
    public boolean isFoil(ItemStack stack)
    {
        return stack.hasTag() && (stack.getTag().contains(TAG_INDEX) || stack.getTag().contains(TAG_HEROES));
    }
}
