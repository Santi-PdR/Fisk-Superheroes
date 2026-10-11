package com.fiskmods.heroes.common.network;

import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.hero.Hero;
import com.fiskmods.heroes.common.hero.HeroIteration;
import com.fiskmods.heroes.common.hero.ItemHeroArmor;
import com.fiskmods.heroes.common.hero.WeaponList;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

/** Selects, equips or unequips one declared weapon through the server-authoritative wheel. */
public class PacketEquipment extends SHPacket
{
    private final int index;

    public PacketEquipment(int index) { this.index = index; }
    public PacketEquipment(FriendlyByteBuf buffer) { index = buffer.readVarInt(); }

    @Override
    public void encode(FriendlyByteBuf buffer) { buffer.writeVarInt(index); }

    @Override
    public void handle(NetworkEvent.Context context)
    {
        ServerPlayer player = context.getSender();
        if (player == null || index < 0 || index > 255) return;

        SHPlayerData data = SHDataCapabilities.getPlayer(player);
        HeroIteration iteration = data != null ? data.getHero() : null;
        if (iteration == null) return;

        Hero hero = iteration.getHero();
        WeaponList candidates = hero.getWeaponStacks();
        if (index >= candidates.size()) return;

        ItemStack core = findCore(player, hero);
        ItemStack[] selected = core != null ? ItemHeroArmor.getWeapons(core) : null;
        if (selected == null || index >= selected.length) return;

        ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack selectedWeapon = selected[index];
        boolean hasSelection = selectedWeapon != null && !selectedWeapon.isEmpty();

        if (hasSelection && held.isEmpty())
        {
            selected[index] = ItemStack.EMPTY;
            ItemHeroArmor.setWeapons(core, selected);
            player.setItemInHand(InteractionHand.MAIN_HAND, selectedWeapon.copy());
        }
        else if (!hasSelection && !held.isEmpty() && candidates.isValid(index, held))
        {
            selected[index] = held.copy();
            ItemHeroArmor.setWeapons(core, selected);
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }
        else if (hasSelection && !held.isEmpty())
        {
            int heldIndex = candidates.indexOf(held);
            if (heldIndex >= 0 && (selected[heldIndex] == null || selected[heldIndex].isEmpty() || heldIndex == index))
            {
                selected[heldIndex] = held.copy();
                if (heldIndex != index) selected[index] = ItemStack.EMPTY;
                ItemHeroArmor.setWeapons(core, selected);
                player.setItemInHand(InteractionHand.MAIN_HAND, selectedWeapon.copy());
            }
        }
        player.getInventory().setChanged();
    }

    private static ItemStack findCore(ServerPlayer player, Hero hero)
    {
        int slot = hero.getCorePieceOfSet();
        if (slot < 0) return null;
        ItemStack core = player.getInventory().armor.get(3 - slot);
        HeroIteration iteration = ItemHeroArmor.getHero(core);
        return iteration != null && iteration.getHero() == hero ? core : null;
    }
}
