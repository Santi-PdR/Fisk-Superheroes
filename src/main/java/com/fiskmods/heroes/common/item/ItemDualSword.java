package com.fiskmods.heroes.common.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import java.util.UUID;

/** Sword with the original optional dual-wield tag and durability rules. */
public final class ItemDualSword extends SwordItem
{
    private static final UUID ORIGINAL_DAMAGE_UUID = UUID.fromString("9a9b0d58-880e-4daf-a92f-45ac1d407e6a");

    public ItemDualSword(Properties properties)
    {
        // The 1.7 sword added 6.5 damage to the player's base attack attribute. Iron tier adds 2,
        // so add the remaining half point to the modern iron sword's 6-point damage modifier.
        super(Tiers.IRON, 4, -2.4F, properties.durability(800));
    }

    public static boolean isDual(ItemStack stack)
    {
        return stack.hasTag() && stack.getTag().getBoolean("Dual");
    }

    public static ItemStack setDual(ItemStack stack)
    {
        stack.getOrCreateTag().putBoolean("Dual", true);
        return stack;
    }

    @Override
    public int getMaxDamage(ItemStack stack)
    {
        int durability = super.getMaxDamage(stack);
        return isDual(stack) ? Math.round(durability * 1.5F) : durability;
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot)
    {
        Multimap<Attribute, AttributeModifier> modifiers = HashMultimap.create(super.getDefaultAttributeModifiers(slot));
        if (slot == EquipmentSlot.MAINHAND)
        {
            modifiers.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(ORIGINAL_DAMAGE_UUID,
                    "Original katana damage", 0.5D, AttributeModifier.Operation.ADDITION));
        }
        return modifiers;
    }

    @Override
    public Component getName(ItemStack stack)
    {
        String id = getDescriptionId();
        return Component.translatable(isDual(stack) ? id + ".dual" : id);
    }
}
