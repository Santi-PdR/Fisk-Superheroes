package com.fiskmods.heroes.common.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
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

    public ItemQuiver(Properties properties)
    {
        super(properties.stacksTo(1));
    }

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
        return !stack.isEmpty() && (stack.is(Items.ARROW) || stack.is(Items.TIPPED_ARROW) || stack.is(Items.SPECTRAL_ARROW));
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
        return createInventory(quiver).getStackInSlot(selected);
    }

    public static void consumeSelectedArrow(Player player)
    {
        ItemStack quiver = findQuiver(player);
        if (quiver.isEmpty()) return;

        SHPlayerData data = SHDataCapabilities.getPlayer(player);
        int selected = data != null ? Byte.toUnsignedInt(data.getData().get(Vars.SELECTED_ARROW)) % 5 : 0;
        createInventory(quiver).extractItem(selected, 1, false);
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
