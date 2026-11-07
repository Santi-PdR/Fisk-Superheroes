package com.fiskmods.heroes.common.block;

import javax.annotation.Nullable;

import com.fiskmods.heroes.common.hero.Hero;
import com.fiskmods.heroes.common.hero.HeroIteration;
import com.fiskmods.heroes.common.hero.ItemHeroArmor;
import com.fiskmods.heroes.common.item.ModMenus;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Suit Iterator container menu ported from 1.7.10 ContainerSuitIterator.
 * 4 armor slots at (15, 18 + i * 18), player inv at y = 106, hotbar at y = 164 (yOff = 22).
 */
public final class SuitIteratorMenu extends AbstractContainerMenu
{
    public final SimpleContainer armorInput = new SimpleContainer(4);
    private final BlockPos pos;

    public SuitIteratorMenu(int id, Inventory playerInv, FriendlyByteBuf buf)
    {
        this(id, playerInv, buf.readBlockPos());
    }

    public SuitIteratorMenu(int id, Inventory playerInv, BlockPos pos)
    {
        super(ModMenus.SUIT_ITERATOR.get(), id);
        this.pos = pos;

        for (int i = 0; i < 4; ++i)
        {
            final int armorSlot = i;
            addSlot(new Slot(armorInput, i, 15, 18 + i * 18)
            {
                @Override
                public boolean mayPlace(ItemStack stack)
                {
                    return stack.getItem() instanceof ItemHeroArmor armor
                            && armor.getSlot() == armorSlot
                            && ItemHeroArmor.getHero(stack) != null;
                }

                @Override
                public int getMaxStackSize()
                {
                    return 1;
                }
            });
        }

        for (int row = 0; row < 3; ++row)
        {
            for (int col = 0; col < 9; ++col)
            {
                addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 106 + row * 18));
            }
        }
        for (int col = 0; col < 9; ++col)
        {
            addSlot(new Slot(playerInv, col, 8 + col * 18, 164));
        }
    }

    @Nullable
    public HeroIteration getCurrentIteration()
    {
        Hero matchedHero = null;
        HeroIteration firstIter = null;
        for (int i = 0; i < 4; ++i)
        {
            ItemStack stack = armorInput.getItem(i);
            if (stack.isEmpty()) continue;
            HeroIteration iter = ItemHeroArmor.getHero(stack);
            if (iter == null) return null;
            if (matchedHero == null)
            {
                matchedHero = iter.getHero();
                firstIter = iter;
            }
            else if (!matchedHero.equals(iter.getHero()))
            {
                return null;
            }
        }
        return firstIter;
    }

    public void applyIteration(String iterationKey)
    {
        HeroIteration current = getCurrentIteration();
        if (current == null) return;
        Hero hero = current.getHero();
        HeroIteration target = iterationKey == null || iterationKey.isEmpty()
                ? hero.getDefaultIteration() : hero.findIteration(iterationKey).orElse(null);
        if (target == null) return;

        // Reject the whole change if any inserted piece does not exist in the target
        // iteration; partially changing the set would leave it in an inconsistent state.
        for (int i = 0; i < 4; ++i)
        {
            if (!armorInput.getItem(i).isEmpty() && !target.hasPieceOfSet(i)) return;
        }
        for (int i = 0; i < 4; ++i)
        {
            ItemStack stack = armorInput.getItem(i);
            if (!stack.isEmpty()) ItemHeroArmor.setHero(stack, target);
        }
        armorInput.setChanged();
        broadcastChanges();
    }

    @Override
    public void removed(Player player)
    {
        super.removed(player);
        if (!player.level().isClientSide)
        {
            clearContainer(player, armorInput);
        }
    }

    @Override
    public boolean stillValid(Player player)
    {
        return player.level().getBlockState(pos).is(ModBlocks.SUIT_ITERATOR.get())
                && player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index)
    {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < 4)
        {
            if (!moveItemStackTo(stack, 4, 40, true)) return ItemStack.EMPTY;
        }
        else if (stack.getItem() instanceof ItemHeroArmor armor && ItemHeroArmor.getHero(stack) != null)
        {
            int target = armor.getSlot();
            if (target < 0 || target >= 4 || !moveItemStackTo(stack, target, target + 1, false))
            {
                return ItemStack.EMPTY;
            }
        }
        else if (index < 31)
        {
            if (!moveItemStackTo(stack, 31, 40, false)) return ItemStack.EMPTY;
        }
        else if (!moveItemStackTo(stack, 4, 31, false))
        {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }
}
