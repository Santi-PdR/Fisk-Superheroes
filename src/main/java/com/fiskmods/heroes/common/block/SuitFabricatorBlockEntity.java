package com.fiskmods.heroes.common.block;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.hero.Hero;
import com.fiskmods.heroes.common.hero.HeroIteration;
import com.fiskmods.heroes.common.hero.ItemHeroArmor;
import com.fiskmods.heroes.common.item.ItemSuitDrive;
import com.fiskmods.heroes.common.item.ModItems;
import com.fiskmods.heroes.common.sound.SHSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Suit Fabricator BlockEntity ported from 1.7.10 TileEntitySuitFabricator.
 * Slots 0..9: material inputs, Slot 10: Suit Drive, Slot 11: Suit Core, Slots 12..15: printed suit output.
 */
public final class SuitFabricatorBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider
{
    public static final int MAX_PRINT_TIME = 1200;

    private static final int[] SLOTS_INPUT = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 11 };
    private static final int[] SLOTS_OUTPUT = { 12, 13, 14, 15 };

    private final NonNullList<ItemStack> items = NonNullList.withSize(16, ItemStack.EMPTY);
    private int printTime;
    private boolean printing;
    private String printingHeroId = "";

    public final ContainerData dataAccess = new ContainerData()
    {
        @Override
        public int get(int index)
        {
            return switch (index)
            {
                case 0 -> printTime;
                case 1 -> printing ? 1 : 0;
                case 2 -> hasFinishedOutput() ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value)
        {
            if (index == 0) printTime = value;
            else if (index == 1) printing = value != 0;
        }

        @Override
        public int getCount()
        {
            return 3;
        }
    };

    public SuitFabricatorBlockEntity(BlockPos pos, BlockState state)
    {
        super(ModBlockEntities.SUIT_FABRICATOR.get(), pos, state);
    }

    public boolean isCosmic()
    {
        return getBlockState().getBlock() instanceof SuitFabricatorBlock block && block.isCosmic();
    }

    public boolean isPrinting()
    {
        return printing;
    }

    public int getPrintTime()
    {
        return printTime;
    }

    public boolean hasFinishedOutput()
    {
        for (int i = 12; i < 16; ++i)
        {
            if (!items.get(i).isEmpty()) return true;
        }
        return false;
    }

    @Nullable
    public HeroIteration getTargetIteration()
    {
        if (printing && !printingHeroId.isEmpty())
        {
            ResourceLocation id = ResourceLocation.tryParse(printingHeroId);
            if (id != null)
            {
                Hero h = Hero.REGISTRY.getHero(id);
                if (h != null) return h.getDefaultIteration();
            }
        }
        for (int i = 12; i < 16; ++i)
        {
            ItemStack out = items.get(i);
            if (!out.isEmpty())
            {
                HeroIteration outIter = ItemHeroArmor.getHero(out);
                if (outIter != null) return outIter;
            }
        }
        ItemStack drive = items.get(10);
        if (drive.isEmpty() || !drive.is(ModItems.SUIT_DRIVE.get())) return null;
        Hero hero = ItemSuitDrive.getSelectedHero(drive);
        return hero != null ? hero.getDefaultIteration() : null;
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, SuitFabricatorBlockEntity be)
    {
        if (be.printing && be.printTime < MAX_PRINT_TIME)
        {
            ++be.printTime;
        }
    }

    public boolean canPrint()
    {
        if (printing || hasFinishedOutput()) return false;
        HeroIteration iter = getTargetIteration();
        if (iter == null) return false;
        FabricatorRecipes.Recipe recipe = FabricatorRecipes.get(iter.getHero());
        if (recipe == null) return false;
        if (recipe.cosmic() && !isCosmic()) return false;
        if (items.get(11).isEmpty() || !items.get(11).is(ModItems.SUIT_CORE.get())) return false;

        Map<Item, Integer> needed = new HashMap<>(recipe.items());
        for (int i = 0; i < 10 && !needed.isEmpty(); ++i)
        {
            ItemStack stack = items.get(i);
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

    public boolean startPrint()
    {
        if (!canPrint()) return false;
        HeroIteration iter = getTargetIteration();
        if (iter == null) return false;
        FabricatorRecipes.Recipe recipe = FabricatorRecipes.get(iter.getHero());
        if (recipe == null) return false;

        Map<Item, Integer> needed = new HashMap<>(recipe.items());
        for (int i = 0; i < 10 && !needed.isEmpty(); ++i)
        {
            ItemStack stack = items.get(i);
            if (stack.isEmpty()) continue;
            Integer req = needed.get(stack.getItem());
            if (req != null)
            {
                int take = Math.min(req, stack.getCount());
                stack.shrink(take);
                int rem = req - take;
                if (rem <= 0) needed.remove(stack.getItem());
                else needed.put(stack.getItem(), rem);
            }
        }
        items.get(11).shrink(1);

        printing = true;
        printTime = 0;
        printingHeroId = iter.getHero().getRegistryName().toString();
        if (level != null)
        {
            SHSounds.play(level, worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D,
                    FiskHeroes.id("block.suitfabricator.print"), SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        syncAndSave();
        return true;
    }

    public void cycleDriveIndex(int delta)
    {
        if (printing) return;
        ItemStack drive = items.get(10);
        if (!drive.isEmpty() && drive.is(ModItems.SUIT_DRIVE.get()))
        {
            ItemSuitDrive.cycleIndex(drive, delta);
            syncAndSave();
        }
    }

    public boolean claimPrintedSuit(Player player)
    {
        if (!hasFinishedOutput()) return false;
        for (int i = 12; i < 16; ++i)
        {
            ItemStack piece = items.get(i);
            if (piece.isEmpty()) continue;
            items.set(i, ItemStack.EMPTY);
            if (!player.getInventory().add(piece))
            {
                player.drop(piece, false);
            }
        }
        syncAndSave();
        return true;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SuitFabricatorBlockEntity be)
    {
        if (!be.printing) return;
        ++be.printTime;
        if (be.printTime >= MAX_PRINT_TIME)
        {
            be.printing = false;
            be.printTime = 0;
            ResourceLocation id = ResourceLocation.tryParse(be.printingHeroId);
            be.printingHeroId = "";
            if (id != null && ModItems.heroArmor != null)
            {
                Hero hero = Hero.REGISTRY.getHero(id);
                if (hero != null)
                {
                    HeroIteration iter = hero.getDefaultIteration();
                    for (int i = 0; i < 4; ++i)
                    {
                        if (iter.hasPieceOfSet(i))
                        {
                            be.items.set(12 + i, ItemHeroArmor.create(iter, ModItems.heroArmor[i]));
                        }
                    }
                }
            }
            be.syncAndSave();
        }
        else if (be.printTime % 20 == 0)
        {
            be.setChanged();
            level.updateNeighbourForOutputSignal(pos, state.getBlock());
        }
    }

    private void syncAndSave()
    {
        setChanged();
        if (level != null)
        {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
            level.updateNeighbourForOutputSignal(worldPosition, state.getBlock());
        }
    }

    @Override
    public Component getDisplayName()
    {
        return Component.translatable(isCosmic() ? "gui.cosmic_suit_fabricator" : "gui.suit_fabricator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player)
    {
        return new SuitFabricatorMenu(containerId, inventory, this, dataAccess, worldPosition, isCosmic());
    }

    @Override
    public void load(CompoundTag tag)
    {
        super.load(tag);
        items.clear();
        ContainerHelper.loadAllItems(tag, items);
        printTime = tag.getInt("PrintTime");
        printing = tag.getBoolean("Printing");
        printingHeroId = tag.getString("PrintingHero");
    }

    @Override
    protected void saveAdditional(CompoundTag tag)
    {
        super.saveAdditional(tag);
        tag.putInt("PrintTime", printTime);
        tag.putBoolean("Printing", printing);
        tag.putString("PrintingHero", printingHeroId);
        ContainerHelper.saveAllItems(tag, items);
    }

    @Override
    public CompoundTag getUpdateTag()
    {
        return saveWithoutMetadata();
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket()
    {
        return ClientboundBlockEntityDataPacket.create(this);
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
        if (!result.isEmpty()) syncAndSave();
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
        syncAndSave();
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
        return side == Direction.DOWN ? SLOTS_OUTPUT : SLOTS_INPUT;
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack)
    {
        if (index >= 12) return false;
        if (index == 10) return stack.is(ModItems.SUIT_DRIVE.get());
        if (index == 11) return stack.is(ModItems.SUIT_CORE.get());
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
        return index >= 12;
    }
}
