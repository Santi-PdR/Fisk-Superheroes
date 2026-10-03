package com.fiskmods.heroes.common.item;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraft.world.InteractionHand;

/** Five arrow slots followed by the player's inventory. */
public class QuiverMenu extends AbstractContainerMenu
{
    private final Inventory playerInventory;
    private final InteractionHand hand;
    private final ItemStackHandler arrows;

    public QuiverMenu(int id, Inventory inventory, net.minecraft.network.FriendlyByteBuf buffer)
    {
        this(id, inventory, buffer.readEnum(InteractionHand.class));
    }

    public QuiverMenu(int id, Inventory inventory, InteractionHand hand)
    {
        super(ModMenus.QUIVER.get(), id);
        this.playerInventory = inventory;
        this.hand = hand;
        ItemStack quiver = inventory.player.getItemInHand(hand);
        this.arrows = ItemQuiver.createInventory(quiver);

        for (int slot = 0; slot < 5; slot++)
        {
            addSlot(new SlotItemHandler(arrows, slot, 44 + slot * 18, 19));
        }

        for (int row = 0; row < 3; row++)
        {
            for (int column = 0; column < 9; column++)
            {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++)
        {
            addSlot(new Slot(inventory, column, 8 + column * 18, 142));
        }
    }

    @Override
    public boolean stillValid(Player player)
    {
        ItemStack stack = player.getItemInHand(hand);
        return !stack.isEmpty() && stack.getItem() instanceof ItemQuiver;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index)
    {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        boolean moved;
        if (index < 5)
        {
            moved = moveItemStackTo(stack, 5, slots.size(), true);
        }
        else
        {
            moved = ItemQuiver.isArrow(stack) && moveItemStackTo(stack, 0, 5, false);
            if (!moved)
            {
                int start = index < 32 ? 32 : 5;
                int end = index < 32 ? slots.size() : 32;
                moved = moveItemStackTo(stack, start, end, false);
            }
        }

        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return original;
    }
}
