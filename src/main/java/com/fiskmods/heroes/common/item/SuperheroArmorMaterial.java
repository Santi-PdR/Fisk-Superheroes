package com.fiskmods.heroes.common.item;

import java.util.Map;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * The armour material shared by every suit piece.
 * <p>
 * 1.20.1 turns {@code ArmorMaterial} into an interface, so unlike the 1.7.10 enum this is a plain
 * implementation. Suit pieces are not durability items (the original mod's suits never wore out
 * either; {@code suits.suitPiecesTakeDamage} only controls whether the {@code ArmorAttribute}s are
 * degraded), so the durability value is zero and the item is treated as unbreakable.
 */
public enum SuperheroArmorMaterial implements ArmorMaterial
{
    INSTANCE;

    private static final Map<ArmorItem.Type, Integer> DEFENSE = Map.of(
            ArmorItem.Type.HELMET, 2,
            ArmorItem.Type.CHESTPLATE, 4,
            ArmorItem.Type.LEGGINGS, 3,
            ArmorItem.Type.BOOTS, 1);

    private static final Map<ArmorItem.Type, Integer> DURABILITY = Map.of(
            ArmorItem.Type.HELMET, 0,
            ArmorItem.Type.CHESTPLATE, 0,
            ArmorItem.Type.LEGGINGS, 0,
            ArmorItem.Type.BOOTS, 0);

    @Override
    public int getDurabilityForType(ArmorItem.Type type)
    {
        return DURABILITY.getOrDefault(type, 0);
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type)
    {
        return DEFENSE.getOrDefault(type, 0);
    }

    @Override
    public int getEnchantmentValue()
    {
        return 15;
    }

    @Override
    public SoundEvent getEquipSound()
    {
        return SoundEvents.ARMOR_EQUIP_LEATHER;
    }

    @Override
    public Ingredient getRepairIngredient()
    {
        return Ingredient.EMPTY;
    }

    @Override
    public String getName()
    {
        return "fiskheroes:superhero";
    }

    @Override
    public float getToughness()
    {
        return 0.0F;
    }

    @Override
    public float getKnockbackResistance()
    {
        return 0.0F;
    }
}
