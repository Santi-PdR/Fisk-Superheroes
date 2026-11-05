package com.fiskmods.heroes.common.block;

import com.fiskmods.heroes.common.item.ModMenus;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Alloy Smeltery container menu ported from 1.7.10 ContainerAlloySmeltery.
 * Exact slot layout: input0 (67,19), input1 (85,19), output (116,34), fuel (39,39), yOff = -5.
 */
public final class AlloySmelteryMenu extends AbstractContainerMenu
{
    private final Container container;
    private final ContainerData data;

    public AlloySmelteryMenu(int id, Inventory playerInv, FriendlyByteBuf buf)
    {
        this(id, playerInv, new SimpleContainer(4), new SimpleContainerData(2));
        buf.readBlockPos();
    }

    public AlloySmelteryMenu(int id, Inventory playerInv, Container container, ContainerData data)
    {
        super(ModMenus.ALLOY_SMELTERY.get(), id);
        this.container = container;
        this.data = data;

        addSlot(new Slot(container, 0, 67, 19));
        addSlot(new Slot(container, 1, 85, 19));
        addSlot(new Slot(container, 2, 116, 34)
        {
            @Override
            public boolean mayPlace(ItemStack stack)
            {
                return false;
            }
        });
        addSlot(new Slot(container, 3, 39, 39)
        {
            @Override
            public boolean mayPlace(ItemStack stack)
            {
                return stack.is(Items.BLAZE_POWDER);
            }
        });

        for (int row = 0; row < 3; ++row)
        {
            for (int col = 0; col < 9; ++col)
            {
                addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 79 + row * 18));
            }
        }
        for (int col = 0; col < 9; ++col)
        {
            addSlot(new Slot(playerInv, col, 8 + col * 18, 137));
        }

        addDataSlots(data);
    }

    public int getBurnTime()
    {
        return data.get(0);
    }

    public int getFuelAmount()
    {
        return data.get(1);
    }

    @Override
    public boolean stillValid(Player player)
    {
        return container.stillValid(player);
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
        else if (stack.is(Items.BLAZE_POWDER))
        {
            if (!moveItemStackTo(stack, 3, 4, false)) return ItemStack.EMPTY;
        }
        else if (AlloyRecipes.isValidIngredient(stack))
        {
            if (!moveItemStackTo(stack, 0, 2, false)) return ItemStack.EMPTY;
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
