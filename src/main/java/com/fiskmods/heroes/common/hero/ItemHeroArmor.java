package com.fiskmods.heroes.common.hero;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * The suit pieces worn by heroes. Every piece stores the hero (and iteration) it belongs to in its
 * item NBT, mirroring the {@code HeroType} tag of the original mod.
 */
public class ItemHeroArmor extends ArmorItem
{
    public static final String TAG_HERO = "HeroType";
    public static final String TAG_ITERATION = "Iteration";
    public static final String TAG_UNLOCKED = "NeedsUnlock";

    private final int slot;

    public ItemHeroArmor(ArmorMaterial material, ArmorItem.Type type, int slot, Properties properties)
    {
        super(material, type, properties);
        this.slot = slot;
    }

    /** Armour slot index: 0 = helmet, 1 = chestplate, 2 = leggings, 3 = boots. */
    public int getSlot()
    {
        return slot;
    }

    @Nullable
    public static ResourceLocation getHeroId(ItemStack stack)
    {
        if (stack.hasTag() && stack.getTag().contains(TAG_HERO))
        {
            return ResourceLocation.tryParse(stack.getTag().getString(TAG_HERO));
        }

        return null;
    }

    @Nullable
    public static HeroIteration getHero(ItemStack stack)
    {
        ResourceLocation id = getHeroId(stack);

        if (id == null)
        {
            return null;
        }

        Hero hero = Hero.REGISTRY.getHero(id);

        if (hero == null)
        {
            return null;
        }

        String iteration = stack.hasTag() ? stack.getTag().getString(TAG_ITERATION) : "";
        return hero.getIteration(iteration);
    }

    public static void setHero(ItemStack stack, @Nullable HeroIteration hero)
    {
        if (hero == null)
        {
            if (stack.hasTag())
            {
                stack.getTag().remove(TAG_HERO);
                stack.getTag().remove(TAG_ITERATION);
            }

            return;
        }

        stack.getOrCreateTag().putString(TAG_HERO, hero.getHero().getRegistryName().toString());

        if (hero.getKey() != null)
        {
            stack.getOrCreateTag().putString(TAG_ITERATION, hero.getKey());
        }
        else
        {
            stack.getOrCreateTag().remove(TAG_ITERATION);
        }
    }

    /** The armour piece description ("cowl", "chestpiece", ...) this stack uses. */
    @Nullable
    public static String getArmorType(ItemStack stack)
    {
        HeroIteration hero = getHero(stack);
        return hero != null ? hero.getArmorType(((ItemHeroArmor) stack.getItem()).getSlot()) : null;
    }

    public static boolean isSuitPiece(ItemStack stack)
    {
        return !stack.isEmpty() && stack.getItem() instanceof ItemHeroArmor && getHero(stack) != null;
    }

    public static ItemStack create(HeroIteration hero, ItemHeroArmor item)
    {
        ItemStack stack = new ItemStack(item);
        setHero(stack, hero);
        return stack;
    }

    public static ItemStack create(HeroIteration hero, ItemHeroArmor item, int damage)
    {
        ItemStack stack = create(hero, item);
        stack.setDamageValue(damage);
        return stack;
    }

    @Override
    public Component getName(ItemStack stack)
    {
        HeroIteration hero = getHero(stack);

        if (hero != null)
        {
            String type = hero.getArmorType(slot);
            Component piece = type != null ? Component.translatable(type) : getDescription();
            return Component.translatable("item.superhero_armor.name", hero.getFormattedName(), piece);
        }

        return super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag)
    {
        HeroIteration hero = getHero(stack);

        if (hero != null)
        {
            tooltip.add(Component.translatable("item.superhero_armor.hero", hero.getFormattedName()).withStyle(ChatFormatting.GRAY));

            if (hero.getHero().getTier() > 0)
            {
                tooltip.add(Component.translatable("item.superhero_armor.tier", Component.translatable("tier." + hero.getHero().getTier())).withStyle(ChatFormatting.DARK_GRAY));
            }

            if (hero.getVersionKey() != null)
            {
                tooltip.add(Component.translatable(hero.getVersionKey()).withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repair)
    {
        return false;
    }

    @Override
    public boolean isFoil(ItemStack stack)
    {
        return getHero(stack) != null && getHero(stack).getVanity() != null;
    }

    @Override
    public boolean canBeDepleted()
    {
        return false;
    }
}
