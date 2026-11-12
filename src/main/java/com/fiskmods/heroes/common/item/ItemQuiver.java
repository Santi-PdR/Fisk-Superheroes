package com.fiskmods.heroes.common.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.data.var.Vars;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.NetworkHooks;

/** Five-slot arrow container used by the compound bow. */
public class ItemQuiver extends Item
{
    private static final String INVENTORY_TAG = "QuiverInventory";
    private static final String AUTO_TAG = "Automatic";

    public static boolean isAutomatic(ItemStack stack)
    {
        return stack.is(ModItems.QUIVER.get()) && stack.hasTag() && stack.getTag().getBoolean(AUTO_TAG);
    }

    public static ItemStack makeAutomatic(ItemStack stack)
    {
        ItemStack result = stack.copy();
        result.setCount(1);
        result.getOrCreateTag().putBoolean(AUTO_TAG, true);
        return result;
    }

    @Override
    public Component getName(ItemStack stack)
    {
        return isAutomatic(stack) ? Component.translatable("item.quiver.auto.name") : super.getName(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slot, boolean selected)
    {
        if (level.isClientSide || !(entity instanceof Player player) || !isAutomatic(stack) || selected
                || player.tickCount % 8 != 0
                || (player.containerMenu instanceof QuiverMenu menu && menu.isOpenedQuiver(stack))) return;
        ItemStackHandler arrows = createInventory(stack);
        for (ItemStack candidate : player.getInventory().items)
        {
            // Original auto-quiver transfers one trick arrow every eight ticks.
            if (candidate.isEmpty() || !(candidate.getItem() instanceof ItemTrickArrow)) continue;
            ItemStack one = candidate.copy();
            one.setCount(1);
            for (int i = 0; i < arrows.getSlots(); ++i)
            {
                if (arrows.insertItem(i, one, false).isEmpty())
                {
                    candidate.shrink(1);
                    player.getInventory().setChanged();
                    return;
                }
            }
        }
    }

    public ItemQuiver(Properties properties)
    {
        super(properties.stacksTo(1));
    }

    @Override public int getEnchantmentValue() { return 10; }
    @Override public boolean isEnchantable(ItemStack stack) { return stack.getCount() == 1; }

    public static ItemStackHandler createInventory(ItemStack stack)
    {
        ItemStackHandler handler = new ItemStackHandler(5)
        {
            @Override
            protected void onContentsChanged(int slot)
            {
                if (!stack.isEmpty()) stack.getOrCreateTag().put(INVENTORY_TAG, serializeNBT());
            }

            @Override
            public int getSlotLimit(int slot)
            {
                int level = net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(
                        com.fiskmods.heroes.common.enchantment.ModEnchantments.CAPACITY.get(), stack);
                return 16 * (1 + Math.max(0, Math.min(3, level)));
            }

            @Override
            public boolean isItemValid(int slot, ItemStack item)
            {
                return isArrow(item);
            }
        };

        if (stack.hasTag() && stack.getTag().contains(INVENTORY_TAG))
        {
            handler.deserializeNBT(stack.getTag().getCompound(INVENTORY_TAG));
        }
        return handler;
    }

    public static boolean isArrow(ItemStack stack)
    {
        return !stack.isEmpty() && stack.getItem() instanceof net.minecraft.world.item.ArrowItem;
    }

    public static ItemStack findQuiver(Player player)
    {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++)
        {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty() && stack.getItem() instanceof ItemQuiver) return stack;
        }
        return ItemStack.EMPTY;
    }

    public static int findQuiverSlot(Player player)
    {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++)
        {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty() && stack.getItem() instanceof ItemQuiver) return slot;
        }
        return -1;
    }

    public static void updatePlayerData(Player player)
    {
        SHPlayerData data = SHDataCapabilities.getPlayer(player);
        if (data == null) return;

        int quiverSlot = findQuiverSlot(player);
        ItemStack quiver = quiverSlot >= 0 ? player.getInventory().getItem(quiverSlot) : ItemStack.EMPTY;
        data.getData().set(Vars.EQUIPPED_QUIVER, quiver.isEmpty() ? null : "fiskheroes:quiver");
        data.getData().set(Vars.EQUIPPED_QUIVER_SLOT, (byte) quiverSlot);

        ItemStack arrow = quiver.isEmpty() ? ItemStack.EMPTY : getSelectedArrow(player);
        String arrowId = arrow.isEmpty() ? "" : net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(arrow.getItem()).toString();
        data.getData().set(Vars.CURRENT_ARROW, arrowId);
    }

    public static ItemStack getSelectedArrow(Player player)
    {
        ItemStack quiver = findQuiver(player);
        if (quiver.isEmpty()) return ItemStack.EMPTY;

        SHPlayerData data = SHDataCapabilities.getPlayer(player);
        int selected = data != null ? Byte.toUnsignedInt(data.getData().get(Vars.SELECTED_ARROW)) % 5 : 0;
        ItemStackHandler inventory = createInventory(quiver);
        ItemStack arrow = inventory.getStackInSlot(selected);
        if (!arrow.isEmpty()) return arrow;

        // Keep the bow usable when the selected slot is empty but another quiver slot has ammo.
        // The selected slot remains preferred so cycling still chooses the player's intended arrow.
        for (int slot = 0; slot < inventory.getSlots(); ++slot)
        {
            arrow = inventory.getStackInSlot(slot);
            if (!arrow.isEmpty()) return arrow;
        }
        return ItemStack.EMPTY;
    }

    public static void consumeSelectedArrow(Player player)
    {
        consumeArrow(player, getSelectedArrowSlot(player));
    }

    /** Returns the selected quiver slot, or the first populated slot as a usable fallback. */
    public static int getSelectedArrowSlot(Player player)
    {
        ItemStack quiver = findQuiver(player);
        if (quiver.isEmpty()) return -1;

        SHPlayerData data = SHDataCapabilities.getPlayer(player);
        int selected = data != null ? Byte.toUnsignedInt(data.getData().get(Vars.SELECTED_ARROW)) % 5 : 0;
        ItemStackHandler inventory = createInventory(quiver);
        if (!inventory.getStackInSlot(selected).isEmpty()) return selected;
        for (int slot = 0; slot < inventory.getSlots(); ++slot)
        {
            if (!inventory.getStackInSlot(slot).isEmpty()) return slot;
        }
        return -1;
    }

    public static void consumeArrow(Player player, int slot)
    {
        ItemStack quiver = findQuiver(player);
        if (quiver.isEmpty() || slot < 0 || slot >= 5) return;
        createInventory(quiver).extractItem(slot, 1, false);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer)
        {
            NetworkHooks.openScreen(serverPlayer, new net.minecraft.world.MenuProvider()
            {
                @Override
                public Component getDisplayName()
                {
                    return Component.translatable("gui.quiver");
                }

                @Override
                public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id,
                        net.minecraft.world.entity.player.Inventory inventory, Player menuPlayer)
                {
                    return new QuiverMenu(id, inventory, hand);
                }
            }, buffer -> buffer.writeEnum(hand));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
