package com.fiskmods.heroes.common.block;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

import com.fiskmods.heroes.common.hero.Hero;
import com.fiskmods.heroes.common.hero.HeroIteration;
import com.fiskmods.heroes.common.item.ItemSuitDrive;
import com.fiskmods.heroes.common.item.ModItems;
import com.fiskmods.heroes.common.item.ModMenus;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Suit Fabricator container menu ported from 1.7.10 ContainerSuitFabricator.
 * Exact slot layout: 0..9 inputs at (53 + col*18, 18 + row*18), 10 Suit Drive at (13,66),
 * 11 Suit Core at (147,18), player inv at y = 100, hotbar at y = 158 (yOff = 16).
 */
public final class SuitFabricatorMenu extends AbstractContainerMenu
{
    public final Container container;
    private final ContainerData data;
    private final BlockPos pos;
    private final boolean cosmic;

    public SuitFabricatorMenu(int id, Inventory playerInv, FriendlyByteBuf buf)
    {
        this(id, playerInv, new SimpleContainer(16), new SimpleContainerData(3),
                buf.readBlockPos(), buf.readBoolean());
    }

    public SuitFabricatorMenu(int id, Inventory playerInv, Container container,
            ContainerData data, BlockPos pos, boolean cosmic)
    {
        super(ModMenus.SUIT_FABRICATOR.get(), id);
        this.container = container;
        this.data = data;
        this.pos = pos;
        this.cosmic = cosmic;

        for (int i = 0; i < 10; ++i)
        {
            addSlot(new Slot(container, i, 53 + (i % 5) * 18, 18 + (i / 5) * 18));
        }
        addSlot(new Slot(container, 10, 13, 66)
        {
            @Override
            public boolean mayPlace(ItemStack stack)
            {
                return stack.is(ModItems.SUIT_DRIVE.get());
            }

            @Override
            public int getMaxStackSize()
            {
                return 1;
            }
        });
        addSlot(new Slot(container, 11, 147, 18)
        {
            @Override
            public boolean mayPlace(ItemStack stack)
            {
                return stack.is(ModItems.SUIT_CORE.get());
            }

            @Override
            public int getMaxStackSize()
            {
                return 1;
            }
        });

        for (int row = 0; row < 3; ++row)
        {
            for (int col = 0; col < 9; ++col)
            {
                addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 100 + row * 18));
            }
        }
        for (int col = 0; col < 9; ++col)
        {
            addSlot(new Slot(playerInv, col, 8 + col * 18, 158));
        }

        addDataSlots(data);
    }

    public BlockPos getPos()
    {
        return pos;
    }

    public boolean isCosmic()
    {
        return cosmic;
    }

    public int getPrintTime()
    {
        return data.get(0);
    }

    public boolean isPrinting()
    {
        return data.get(1) != 0;
    }

    public boolean hasFinishedOutput()
    {
        return data.get(2) != 0;
    }

    @Nullable
    public HeroIteration getSelectedIteration()
    {
        ItemStack drive = container.getItem(10);
        if (drive.isEmpty() || !drive.is(ModItems.SUIT_DRIVE.get())) return null;
        Hero hero = ItemSuitDrive.getSelectedHero(drive);
        return hero != null ? hero.getDefaultIteration() : null;
    }

    public boolean canPrintClient()
    {
        if (isPrinting() || hasFinishedOutput()) return false;
        HeroIteration iter = getSelectedIteration();
        if (iter == null) return false;
        FabricatorRecipes.Recipe recipe = FabricatorRecipes.get(iter.getHero());
        if (recipe == null) return false;
        if (recipe.cosmic() && !cosmic) return false;
        if (container.getItem(11).isEmpty() || !container.getItem(11).is(ModItems.SUIT_CORE.get())) return false;

        Map<Item, Integer> needed = new HashMap<>(recipe.items());
        for (int i = 0; i < 10 && !needed.isEmpty(); ++i)
        {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) continue;
            Integer req = needed.get(stack.getItem());
            if (req != null)
            {
                int rem = req - stack.getCount();
                if (rem <= 0) needed.remove(stack.getItem());
                else needed.put(stack.getItem(), rem);
            }
        }
        return needed.isEmpty();
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
        if (index < 12)
        {
            if (!moveItemStackTo(stack, 12, 48, true)) return ItemStack.EMPTY;
        }
        else if (stack.is(ModItems.SUIT_DRIVE.get()))
        {
            if (!moveItemStackTo(stack, 10, 11, false)) return ItemStack.EMPTY;
        }
        else if (stack.is(ModItems.SUIT_CORE.get()))
        {
            if (!moveItemStackTo(stack, 11, 12, false)) return ItemStack.EMPTY;
        }
        else if (!moveItemStackTo(stack, 0, 10, false))
        {
            if (index < 39)
            {
                if (!moveItemStackTo(stack, 39, 48, false)) return ItemStack.EMPTY;
            }
            else if (!moveItemStackTo(stack, 12, 39, false))
            {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }
}
