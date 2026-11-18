package com.fiskmods.heroes.common.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ShieldItem;

/** Captain America's Vibranium shield with its original curved 3D ModelCapsShield renderer. */
public final class ItemCaptainAmericaShield extends ShieldItem
{
    private static final java.util.UUID ATTACK_DAMAGE_ID = java.util.UUID.fromString("dc1b37ab-0ff1-44ea-9580-c1b2155c44ef");

    public ItemCaptainAmericaShield(Properties properties)
    {
        super(properties.durability(1561).rarity(Rarity.RARE));
    }

    @Override
    public com.google.common.collect.Multimap<net.minecraft.world.entity.ai.attributes.Attribute, net.minecraft.world.entity.ai.attributes.AttributeModifier>
            getDefaultAttributeModifiers(net.minecraft.world.entity.EquipmentSlot slot)
    {
        var modifiers = com.google.common.collect.HashMultimap.create(super.getDefaultAttributeModifiers(slot));
        if (slot == net.minecraft.world.entity.EquipmentSlot.MAINHAND)
            modifiers.put(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(ATTACK_DAMAGE_ID, "Shield weapon damage",
                            6.0D, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION));
        return modifiers;
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair)
    {
        return repair.is(ModItems.VIBRANIUM_INGOT.get()) || repair.is(ModItems.VIBRANIUM_DISC.get());
    }

    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer)
    {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions()
        {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer()
            {
                return com.fiskmods.heroes.client.render.CaptainAmericaShieldItemRenderer.getInstance();
            }
        });
    }
}
