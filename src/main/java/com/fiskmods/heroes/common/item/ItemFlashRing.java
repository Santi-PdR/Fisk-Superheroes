package com.fiskmods.heroes.common.item;

import java.util.List;
import javax.annotation.Nullable;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.hero.Hero;
import com.fiskmods.heroes.common.hero.HeroIteration;
import com.fiskmods.heroes.common.hero.HeroTracker;
import com.fiskmods.heroes.common.hero.ItemHeroArmor;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
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
 * Speedster Flash Ring ported from 1.7.10 ItemFlashRing.
 * Stores a full speedster suit in item NBT and deploys/retracts it on right-click.
 */
public class ItemFlashRing extends Item
{
    public static final String TAG_ITEMS = "Items";
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    public ItemFlashRing(Properties properties)
    {
        super(properties);
    }

    public static boolean canContainHero(@Nullable Hero hero)
    {
        if (hero == null || hero.getPiecesToSet() <= 0) return false;
        return hero.getEquipment().stream().anyMatch(entry -> entry.stack().is(ModItems.FLASH_RING.get()));
    }

    @Nullable
    public static HeroIteration getContainedHero(ItemStack stack)
    {
        if (stack.isEmpty() || !stack.hasTag()) return null;
        ItemStack[] armor = getContainedArmor(stack);
        if (armor != null)
        {
            for (ItemStack piece : armor)
            {
                HeroIteration iter = ItemHeroArmor.getHero(piece);
                if (iter != null) return iter;
            }
        }
        return ItemHeroArmor.getHero(stack);
    }

    @Nullable
    public static ItemStack[] getContainedArmor(ItemStack stack)
    {
        if (stack.isEmpty() || !stack.hasTag()) return null;
        CompoundTag tag = stack.getTag();
        if (!tag.contains(TAG_ITEMS, Tag.TAG_LIST)) return null;
        ListTag list = tag.getList(TAG_ITEMS, Tag.TAG_COMPOUND);
        if (list.isEmpty()) return null;

        ItemStack[] armor = new ItemStack[] { ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY };
        boolean any = false;
        for (int i = 0; i < list.size(); ++i)
        {
            CompoundTag entry = list.getCompound(i);
            int slot = entry.contains("Slot", Tag.TAG_ANY_NUMERIC) ? (entry.getByte("Slot") & 0xFF) : i;
            if (slot >= 0 && slot < 4)
            {
                ItemStack piece = ItemStack.of(entry);
                if (!piece.isEmpty())
                {
                    armor[slot] = piece;
                    any = true;
                }
            }
        }
        return any ? armor : null;
    }

    public static ItemStack setContainedArmor(ItemStack stack, ItemStack... armor)
    {
        CompoundTag tag = stack.getOrCreateTag();
        if (armor == null || armor.length == 0)
        {
            tag.remove(TAG_ITEMS);
            tag.remove(ItemHeroArmor.TAG_HERO);
            tag.remove(ItemHeroArmor.TAG_ITERATION);
            if (tag.isEmpty()) stack.setTag(null);
            return stack;
        }

        ListTag list = new ListTag();
        HeroIteration found = null;
        for (int i = 0; i < Math.min(4, armor.length); ++i)
        {
            ItemStack piece = armor[i];
            if (piece == null || piece.isEmpty()) continue;
            CompoundTag saved = piece.save(new CompoundTag());
            saved.putByte("Slot", (byte) i);
            list.add(saved);
            if (found == null) found = ItemHeroArmor.getHero(piece);
        }

        if (list.isEmpty())
        {
            return setContainedArmor(stack);
        }

        tag.put(TAG_ITEMS, list);
        if (found != null)
        {
            ItemHeroArmor.setHero(stack, found);
        }
        return stack;
    }

    public static ItemStack create(Item item, HeroIteration iteration)
    {
        ItemStack ring = new ItemStack(item);
        if (iteration == null || ModItems.heroArmor == null) return ring;
        ItemStack[] pieces = new ItemStack[4];
        for (int i = 0; i < 4; ++i)
        {
            pieces[i] = iteration.hasPieceOfSet(i)
                    ? ItemHeroArmor.create(iteration, ModItems.heroArmor[i])
                    : ItemStack.EMPTY;
        }
        return setContainedArmor(ring, pieces);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        ItemStack stack = player.getItemInHand(hand);
        ItemStack[] stored = getContainedArmor(stack);

        if (stored != null)
        {
            if (!level.isClientSide)
            {
                HeroIteration worn = HeroTracker.getHero(player);
                boolean swapIntoRing = worn != null && canContainHero(worn.getHero());
                ItemStack[] previous = new ItemStack[4];
                for (int i = 0; i < 4; ++i)
                {
                    previous[i] = player.getItemBySlot(ARMOR_SLOTS[i]).copy();
                    player.setItemSlot(ARMOR_SLOTS[i], ItemStack.EMPTY);
                }
                for (int i = 0; i < 4; ++i)
                {
                    if (!stored[i].isEmpty())
                    {
                        player.setItemSlot(ARMOR_SLOTS[i], stored[i].copy());
                    }
                }

                if (swapIntoRing)
                {
                    setContainedArmor(stack, previous);
                }
                else
                {
                    setContainedArmor(stack);
                    for (ItemStack oldPiece : previous)
                    {
                        if (!oldPiece.isEmpty() && !player.getInventory().add(oldPiece))
                        {
                            player.drop(oldPiece, false);
                        }
                    }
                }
                HeroTracker.update(player);
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ARMOR_EQUIP_GOLD, SoundSource.PLAYERS, 1.0F, 1.15F);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        HeroIteration worn = HeroTracker.getHero(player);
        if (worn != null && canContainHero(worn.getHero()))
        {
            if (!level.isClientSide)
            {
                ItemStack[] pieces = new ItemStack[4];
                for (int i = 0; i < 4; ++i)
                {
                    pieces[i] = player.getItemBySlot(ARMOR_SLOTS[i]).copy();
                    player.setItemSlot(ARMOR_SLOTS[i], ItemStack.EMPTY);
                }
                setContainedArmor(stack, pieces);
                HeroTracker.update(player);
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ARMOR_EQUIP_GOLD, SoundSource.PLAYERS, 1.0F, 0.85F);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag)
    {
        HeroIteration iter = getContainedHero(stack);
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
                return com.fiskmods.heroes.client.render.FlashRingItemRenderer.getInstance();
            }
        });
    }
}
