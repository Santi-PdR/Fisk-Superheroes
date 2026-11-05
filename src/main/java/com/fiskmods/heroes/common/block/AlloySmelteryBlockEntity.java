package com.fiskmods.heroes.common.block;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Alloy Smeltery BlockEntity ported from 1.7.10 TileEntityAlloySmeltery.
 * Uses Blaze Powder fuel (8 operations per Blaze Powder, 200 ticks per smelt).
 */
public final class AlloySmelteryBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider
{
    public static final int MAX_BURN_TIME = 200;
    public static final int MAX_FUEL = 8;

    private static final int[] SLOTS_UP = { 0, 1 };
    private static final int[] SLOTS_SIDE = { 0, 1, 3 };
    private static final int[] SLOTS_DOWN = { 2 };

    private final NonNullList<ItemStack> items = NonNullList.withSize(4, ItemStack.EMPTY);
    private int burnTime;
    private int fuelAmount;

    public final ContainerData dataAccess = new ContainerData()
    {
        @Override
        public int get(int index)
        {
            return index == 0 ? burnTime : fuelAmount;
        }

        @Override
        public void set(int index, int value)
        {
            if (index == 0) burnTime = value;
            else if (index == 1) fuelAmount = value;
        }

        @Override
        public int getCount()
        {
            return 2;
        }
    };

    public AlloySmelteryBlockEntity(BlockPos pos, BlockState state)
    {
        super(ModBlockEntities.ALLOY_SMELTERY.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AlloySmelteryBlockEntity be)
    {
        boolean dirty = false;
        boolean wasBurning = be.burnTime > 0;

        if (be.fuelAmount <= 0 && be.items.get(3).is(Items.BLAZE_POWDER) && be.canSmelt())
        {
            be.items.get(3).shrink(1);
            be.fuelAmount = MAX_FUEL;
            dirty = true;
        }

        if (be.fuelAmount > 0 && be.canSmelt())
        {
            ++be.burnTime;
            if (be.burnTime >= MAX_BURN_TIME)
            {
                be.burnTime = 0;
                --be.fuelAmount;
                be.smeltItem();
                dirty = true;
            }
        }
        else if (be.burnTime > 0)
        {
            be.burnTime = Math.max(0, be.burnTime - 2);
        }

        boolean isBurning = be.burnTime > 0;
        if (wasBurning != isBurning)
        {
            level.setBlock(pos, state.setValue(AlloySmelteryBlock.LIT, isBurning), 3);
            dirty = true;
        }

        if (dirty)
        {
            be.setChanged();
        }
    }

    private boolean canSmelt()
    {
        AlloyRecipes.Entry recipe = AlloyRecipes.find(items.get(0), items.get(1));
        if (recipe == null) return false;
        ItemStack result = recipe.assemble();
        ItemStack out = items.get(2);
        if (out.isEmpty()) return true;
        if (!ItemStack.isSameItemSameTags(out, result)) return false;
        return out.getCount() + result.getCount() <= out.getMaxStackSize();
    }

    private void smeltItem()
    {
        AlloyRecipes.Entry recipe = AlloyRecipes.find(items.get(0), items.get(1));
        if (recipe == null) return;
        ItemStack result = recipe.assemble();
        ItemStack out = items.get(2);
        if (out.isEmpty())
        {
            items.set(2, result.copy());
        }
        else if (ItemStack.isSameItemSameTags(out, result))
        {
            out.grow(result.getCount());
        }
        recipe.consume(items.get(0), items.get(1));
    }

    @Override
    public Component getDisplayName()
    {
        return Component.translatable("gui.alloy_smeltery");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player)
    {
        return new AlloySmelteryMenu(containerId, inventory, this, dataAccess);
    }

    @Override
    public void load(CompoundTag tag)
    {
        super.load(tag);
        items.clear();
        ContainerHelper.loadAllItems(tag, items);
        burnTime = tag.getShort("BurnTime");
        fuelAmount = tag.getByte("FuelAmount") & 0xFF;
    }

    @Override
    protected void saveAdditional(CompoundTag tag)
    {
        super.saveAdditional(tag);
        tag.putShort("BurnTime", (short) burnTime);
        tag.putByte("FuelAmount", (byte) fuelAmount);
        ContainerHelper.saveAllItems(tag, items);
    }

    @Override
    public int getContainerSize()
    {
        return items.size();
    }

    @Override
    public boolean isEmpty()
    {
        for (ItemStack stack : items)
        {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot)
    {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount)
    {
        ItemStack result = ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) setChanged();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot)
    {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack)
    {
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) stack.setCount(getMaxStackSize());
        setChanged();
    }

    @Override
    public boolean stillValid(Player player)
    {
        return level != null && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public void clearContent()
    {
        items.clear();
    }

    @Override
    public int[] getSlotsForFace(Direction side)
    {
        return side == Direction.DOWN ? SLOTS_DOWN : (side == Direction.UP ? SLOTS_UP : SLOTS_SIDE);
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack)
    {
        if (index == 2) return false;
        if (index == 3) return stack.is(Items.BLAZE_POWDER);
        return true;
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction direction)
    {
        return canPlaceItem(index, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction)
    {
        return index == 2;
    }
}
