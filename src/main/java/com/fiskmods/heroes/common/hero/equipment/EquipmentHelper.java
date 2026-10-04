package com.fiskmods.heroes.common.hero.equipment;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.hero.Hero;
import com.fiskmods.heroes.common.hero.HeroIteration;
import com.fiskmods.heroes.common.hero.power.ModifierEntry;
import com.fiskmods.heroes.common.hero.power.PowerProperty;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;

/**
 * Hands out ordinary equipment declared with {@code hero.addEquipment(...)}. Primary weapon
 * candidates are tracked separately by {@link com.fiskmods.heroes.common.hero.WeaponList}; they
 * are not treated as a second equipment inventory.
 */
public final class EquipmentHelper
{
    private EquipmentHelper()
    {
    }

    public static void grantEquipment(Player player, @Nullable SHPlayerData data)
    {
        if (data == null)
        {
            return;
        }

        HeroIteration iteration = data.getHero();

        if (iteration == null || player.level().isClientSide)
        {
            return;
        }

        Hero hero = iteration.getHero();

        for (Hero.EquipmentEntry entry : hero.getEquipment())
        {
            ItemStack stack = entry.stack();

            if (stack.isEmpty() || hasItem(player, stack))
            {
                continue;
            }

            if (!player.getInventory().add(stack.copy()))
            {
                player.drop(stack.copy(), false);
            }
        }
    }

    /** Removes equipment granted by a suit when the suit is taken off. */
    public static void revokeEquipment(Player player, List<ItemStack> equipment)
    {
        for (ItemStack stack : equipment)
        {
            player.getInventory().clearOrCountMatchingItems(s -> ItemStack.isSameItemSameTags(s, stack), stack.getCount(), player.inventoryMenu.getCraftSlots());
        }
    }

    public static boolean hasItem(Player player, ItemStack stack)
    {
        for (int i = 0; i < player.getInventory().getContainerSize(); ++i)
        {
            ItemStack other = player.getInventory().getItem(i);

            if (!other.isEmpty() && ItemStack.isSameItemSameTags(other, stack))
            {
                return true;
            }
        }

        return false;
    }

    /** Finds the enabled equipment power which owns the hero's utility belt. */
    @Nullable
    public static ModifierEntry getUtilityBelt(Hero hero, Player player, SHPlayerData data)
    {
        for (ModifierEntry entry : hero.getPowerContainer().getEntries())
        {
            if (!"equipment".equals(entry.getModifier().getId().getPath())
                    || !entry.isEnabled() || !entry.isModifierEnabled(player, data)
                    || equipmentOptions(entry).size() == 0)
            {
                continue;
            }

            return entry;
        }

        return null;
    }

    /** The insertion-ordered option map is the selection order used by the original belt. */
    public static JsonObject equipmentOptions(ModifierEntry entry)
    {
        JsonElement configuration = entry.get(PowerProperty.EQUIPMENT);
        if (configuration == null || !configuration.isJsonObject()) return new JsonObject();

        JsonElement options = configuration.getAsJsonObject().get("equipment");
        return options != null && options.isJsonObject() ? options.getAsJsonObject() : new JsonObject();
    }

    /** Advances the active pack-defined gadget, after validating the selection server-side. */
    public static boolean cycleUtilityBelt(net.minecraft.server.level.ServerPlayer player, int direction)
    {
        if (direction == 0) return false;

        SHPlayerData data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(player);
        HeroIteration iteration = data != null ? data.getHero() : null;
        if (iteration == null) return false;

        Hero hero = iteration.getHero();
        if (!hero.isKeyPressed(player, "UTILITY_BELT")) return false;

        ModifierEntry entry = getUtilityBelt(hero, player, data);
        if (entry == null) return false;

        int count = equipmentOptions(entry).size();
        int current = data.getData().get(com.fiskmods.heroes.common.data.var.Vars.UTILITY_BELT_TYPE);
        int selected = Math.floorMod(current + Integer.signum(direction), count);
        data.getData().set(com.fiskmods.heroes.common.data.var.Vars.PREV_UTILITY_BELT_TYPE, (byte) current);
        data.getData().set(com.fiskmods.heroes.common.data.var.Vars.UTILITY_BELT_TYPE, (byte) selected);
        com.fiskmods.heroes.common.hero.modifier.AbilityData.playSound(player, entry, "SWITCH");
        String id = new java.util.ArrayList<>(equipmentOptions(entry).keySet()).get(selected);
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                "equipment." + id.replace(':', '.') + ".name"), true);
        return true;
    }

    /** Uses the currently selected pack-defined gadget on the server. */
    public static boolean useUtilityBelt(net.minecraft.server.level.ServerPlayer player)
    {
        SHPlayerData data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(player);
        HeroIteration iteration = data != null ? data.getHero() : null;
        if (iteration == null) return false;

        Hero hero = iteration.getHero();
        ModifierEntry entry = getUtilityBelt(hero, player, data);
        if (entry == null) return false;

        JsonObject options = equipmentOptions(entry);
        if (options.size() == 0) return false;
        int current = data.getData().get(com.fiskmods.heroes.common.data.var.Vars.UTILITY_BELT_TYPE);
        if (current < 0) return false;
        int selected = Math.floorMod(current, options.size());
        Map.Entry<String, JsonElement> equipment = new java.util.ArrayList<>(options.entrySet()).get(selected);
        if (!equipment.getValue().isJsonObject()) return false;

        String id = equipment.getKey();
        String path = net.minecraft.resources.ResourceLocation.tryParse(id) != null
                ? net.minecraft.resources.ResourceLocation.parse(id).getPath() : id;
        ItemStack display = com.fiskmods.heroes.common.item.ModItems.equipmentGadget(path);
        if (display.isEmpty()) return false;

        JsonObject configuration = equipment.getValue().getAsJsonObject();
        int cooldown = Math.max(0, number(configuration, "cooldown", 0));
        int maxUses = Math.max(1, number(configuration, "uses", 1));
        CompoundTag state = equipmentState(player, id, maxUses, cooldown);
        long now = player.level().getGameTime();
        int uses = state.getInt("uses");
        if (uses <= 0) return false;

        int quantity = Math.max(1, Math.min(16, number(configuration, "quantity", 1)));
        int spawned = 0;
        for (int i = 0; i < quantity; ++i)
        {
            float yawOffset = quantity == 1 ? 0.0F : (i - (quantity - 1) / 2.0F) * 5.0F;
            var projectile = new com.fiskmods.heroes.common.entity.projectile.EquipmentProjectileEntity(
                    player, id, configuration, display, yawOffset);
            if (player.level().addFreshEntity(projectile)) spawned++;
        }

        if (spawned == 0) return false;
        if (cooldown == 0)
        {
            state.putInt("uses", maxUses);
            state.putLong("cooldownUntil", 0L);
        }
        else
        {
            uses = Math.max(0, uses - 1);
            state.putInt("uses", uses);
            if (state.getLong("cooldownUntil") == 0L)
            {
                state.putLong("cooldownUntil", now + cooldown);
            }
        }

        CompoundTag root = player.getPersistentData().getCompound("FiskHeroesEquipment");
        root.put(id, state);
        player.getPersistentData().put("FiskHeroesEquipment", root);
        player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
        player.gameEvent(net.minecraft.world.level.gameevent.GameEvent.PROJECTILE_SHOOT);
        player.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(display.getItem()));
        com.fiskmods.heroes.common.hero.modifier.AbilityData.playSound(player, entry, "USE");
        playEquipmentSound(player, configuration, "USE");
        return true;
    }

    private static CompoundTag equipmentState(net.minecraft.server.level.ServerPlayer player, String id, int maxUses, int cooldown)
    {
        CompoundTag root = player.getPersistentData().getCompound("FiskHeroesEquipment");
        CompoundTag state = root.contains(id, CompoundTag.TAG_COMPOUND) ? root.getCompound(id).copy() : new CompoundTag();
        long now = player.level().getGameTime();
        int uses = state.contains("uses") ? Math.min(maxUses, state.getInt("uses")) : maxUses;
        long until = state.getLong("cooldownUntil");

        while (until > 0L && until <= now && uses < maxUses)
        {
            ++uses;
            until = uses < maxUses && cooldown > 0 ? until + cooldown : 0L;
        }

        state.putInt("uses", uses);
        state.putLong("cooldownUntil", until);
        return state;
    }

    private static int number(JsonObject object, String key, int fallback)
    {
        try
        {
            return object.has(key) ? object.get(key).getAsInt() : fallback;
        }
        catch (RuntimeException ignored)
        {
            return fallback;
        }
    }

    private static void playEquipmentSound(Player player, JsonObject configuration, String trigger)
    {
        JsonElement events = configuration.get("soundEvents");
        if (events == null || !events.isJsonObject()) return;
        JsonElement sound = events.getAsJsonObject().get(trigger);
        if (sound == null) return;
        if (sound.isJsonArray())
        {
            var sounds = sound.getAsJsonArray();
            if (sounds.isEmpty()) return;
            sound = sounds.get(player.getRandom().nextInt(sounds.size()));
        }
        net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.tryParse(sound.getAsString());
        if (id != null)
        {
            com.fiskmods.heroes.common.sound.SHSounds.play(player, id,
                    net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }
}
