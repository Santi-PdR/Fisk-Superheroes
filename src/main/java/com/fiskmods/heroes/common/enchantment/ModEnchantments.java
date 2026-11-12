package com.fiskmods.heroes.common.enchantment;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.item.ItemQuiver;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Original quiver enchantments: Holding and Bottomless are mutually exclusive. */
public final class ModEnchantments
{
    public static final DeferredRegister<Enchantment> REGISTRY = DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, FiskHeroes.MODID);
    private static final EnchantmentCategory QUIVER = EnchantmentCategory.create("FISKHEROES_QUIVER", item -> item instanceof ItemQuiver);
    public static final RegistryObject<Enchantment> CAPACITY = REGISTRY.register("capacity", () -> new QuiverEnchantment(false));
    public static final RegistryObject<Enchantment> INFINITY = REGISTRY.register("infinity", () -> new QuiverEnchantment(true));

    private ModEnchantments() {}

    private static final class QuiverEnchantment extends Enchantment
    {
        private final boolean bottomless;
        QuiverEnchantment(boolean bottomless)
        {
            super(bottomless ? Rarity.RARE : Rarity.UNCOMMON, QUIVER, new EquipmentSlot[] {EquipmentSlot.MAINHAND});
            this.bottomless = bottomless;
        }
        @Override public int getMaxLevel() { return bottomless ? 5 : 3; }
        @Override public int getMinCost(int level) { return bottomless ? 7 * level : 15 + 9 * (level - 1); }
        @Override public int getMaxCost(int level) { return 51 + 10 * level; }
        @Override public boolean canEnchant(ItemStack stack) { return stack.getItem() instanceof ItemQuiver; }
        @Override protected boolean checkCompatibility(Enchantment other)
        {
            return super.checkCompatibility(other) && !(other instanceof QuiverEnchantment);
        }
    }
}
