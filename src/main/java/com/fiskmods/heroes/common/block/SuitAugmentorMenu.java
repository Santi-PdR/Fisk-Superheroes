package com.fiskmods.heroes.common.block;

import com.fiskmods.heroes.common.hero.HeroIteration;
import com.fiskmods.heroes.common.hero.ItemHeroArmor;
import com.fiskmods.heroes.common.hero.augment.AugmentSet;
import com.fiskmods.heroes.common.item.ItemSuitUpgrade;
import com.fiskmods.heroes.common.item.ModMenus;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** All upgrade decisions, including material and distance checks, are made on the server. */
public final class SuitAugmentorMenu extends AbstractContainerMenu
{
    public final SimpleContainer input = new SimpleContainer(2);
    private final BlockPos pos;

    public SuitAugmentorMenu(int id, Inventory inv, FriendlyByteBuf buf) { this(id, inv, buf.readBlockPos()); }
    public SuitAugmentorMenu(int id, Inventory inv, BlockPos pos)
    {
        super(ModMenus.SUIT_AUGMENTOR.get(), id);
        this.pos = pos;
        addSlot(new Slot(input, 0, 148, 72)
        {
            @Override public boolean mayPlace(ItemStack stack) { return ItemHeroArmor.isCore(stack); }
            @Override public int getMaxStackSize() { return 1; }
        });
        addSlot(new Slot(input, 1, 148, 98)
        {
            @Override public boolean mayPlace(ItemStack stack) { return stack.getItem() instanceof ItemSuitUpgrade; }
        });
        for (int row = 0; row < 3; ++row)
            for (int col = 0; col < 9; ++col)
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 133 + row * 18));
        for (int col = 0; col < 9; ++col) addSlot(new Slot(inv, col, 8 + col * 18, 191));
    }

    public AugmentSet getAugments()
    {
        HeroIteration hero = ItemHeroArmor.getHero(input.getItem(0));
        return hero != null && ItemHeroArmor.isCore(input.getItem(0)) ? hero.getHero().getAugmentSet() : AugmentSet.EMPTY;
    }

    public boolean canUpgrade(int index)
    {
        ItemStack core = input.getItem(0), material = input.getItem(1);
        AugmentSet set = getAugments();
        AugmentSet.Entry entry = set.get(index);
        if (entry == null || !(material.getItem() instanceof ItemSuitUpgrade upgrade)) return false;
        int added = set.level(ItemHeroArmor.getUpgrades(core), entry);
        return added < entry.capacity() && upgrade.level() == entry.min() + added;
    }

    @Override public boolean clickMenuButton(Player player, int index)
    {
        if (player.level().isClientSide || !stillValid(player) || !canUpgrade(index)) return false;
        ItemStack core = input.getItem(0);
        AugmentSet.Entry entry = getAugments().get(index);
        int packed = ItemHeroArmor.getUpgrades(core);
        int shift = index * 4;
        int next = getAugments().level(packed, entry) + 1;
        ItemHeroArmor.setUpgrades(core, (packed & ~(15 << shift)) | (next << shift));
        input.removeItem(1, 1);
        input.setChanged();
        broadcastChanges();
        return true;
    }

    @Override public void removed(Player player)
    {
        super.removed(player);
        if (!player.level().isClientSide) clearContainer(player, input);
    }

    @Override public boolean stillValid(Player player)
    {
        return player.level().getBlockState(pos).is(ModBlocks.SUIT_AUGMENTOR.get())
                && player.distanceToSqr(pos.getX() + .5D, pos.getY() + .5D, pos.getZ() + .5D) <= 64.0D;
    }

    @Override public ItemStack quickMoveStack(Player player, int index)
    {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), copy = stack.copy();
        if (index < 2)
        {
            if (!moveItemStackTo(stack, 2, 38, true)) return ItemStack.EMPTY;
        }
        else if (ItemHeroArmor.isCore(stack))
        {
            if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
        }
        else if (stack.getItem() instanceof ItemSuitUpgrade)
        {
            if (!moveItemStackTo(stack, 1, 2, false)) return ItemStack.EMPTY;
        }
        else if (index < 29)
        {
            if (!moveItemStackTo(stack, 29, 38, false)) return ItemStack.EMPTY;
        }
        else if (!moveItemStackTo(stack, 2, 29, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        return copy;
    }
}
