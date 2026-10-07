package com.fiskmods.heroes.common.hero;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
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
    public static final String TAG_WEAPONS = "Equipment";

    private final int slot;
    private static volatile ArmorTextureResolver armorTextureResolver;

    public ItemHeroArmor(ArmorMaterial material, ArmorItem.Type type, int slot, Properties properties)
    {
        super(material, type, properties);
        this.slot = slot;
    }

    /**
     * Suit textures use the player-skin UV layout and are drawn by HeroSuitLayer. The vanilla
     * armor layer also sees this ArmorItem, but maps the same texture onto armor-model UVs, which
     * creates a second, visibly scrambled pass over the correctly rendered suit.
     */
    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer)
    {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions()
        {
            @Override
            public net.minecraft.client.model.HumanoidModel<?> getHumanoidArmorModel(
                    net.minecraft.world.entity.LivingEntity entity,
                    ItemStack stack,
                    EquipmentSlot equipmentSlot,
                    net.minecraft.client.model.HumanoidModel<?> original)
            {
                // Forge resets armor-model visibility before asking for this model, so hiding the
                // supplied instance here is local to this render pass and does not affect other armor.
                original.setAllVisible(false);
                return original;
            }
        });
    }

    /** Armour slot index: 0 = helmet, 1 = chestplate, 2 = leggings, 3 = boots. */
    public int getSlot()
    {
        return slot;
    }

    /**
     * Installs the client-side texture resolver without adding client references to this common
     * item class, so dedicated servers can load suit items safely.
     */
    public static void setArmorTextureResolver(ArmorTextureResolver resolver)
    {
        armorTextureResolver = resolver;
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot equipmentSlot, String type)
    {
        ArmorTextureResolver resolver = armorTextureResolver;
        return resolver != null ? resolver.get(stack, entity, slot, type) : null;
    }

    @FunctionalInterface
    public interface ArmorTextureResolver
    {
        @Nullable
        String get(ItemStack stack, Entity entity, int armorSlot, String type);
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

    /** Reads the weapon choices stored on a suit core, or the hero's included defaults. */
    @Nullable
    public static ItemStack[] getWeapons(ItemStack stack)
    {
        HeroIteration iteration = getHero(stack);
        if (iteration == null) return null;

        WeaponList weapons = iteration.getHero().getWeaponStacks();
        if (weapons.isEmpty()) return null;
        ItemStack[] result = new ItemStack[weapons.size()];
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TAG_WEAPONS, Tag.TAG_COMPOUND))
        {
            ItemStack choice = ItemStack.of(tag.getCompound(TAG_WEAPONS));
            result[0] = weapons.isValid(0, choice) ? choice : ItemStack.EMPTY;
            return result;
        }
        if (tag != null && tag.contains(TAG_WEAPONS, Tag.TAG_LIST))
        {
            ListTag list = tag.getList(TAG_WEAPONS, Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); ++i)
            {
                CompoundTag choice = list.getCompound(i);
                int index = choice.getByte("Index") & 0xFF;
                if (index < result.length && choice.contains("Item", Tag.TAG_COMPOUND))
                {
                    ItemStack selected = ItemStack.of(choice.getCompound("Item"));
                    result[index] = weapons.isValid(index, selected) ? selected : ItemStack.EMPTY;
                }
            }
            return result;
        }
        for (WeaponList.Entry entry : weapons.entries().toList())
        {
            if (entry.included()) result[entry.index()] = entry.value().copy();
        }
        return result;
    }

    /** Stores selected weapons by their stable candidate index on the suit core. */
    public static ItemStack setWeapons(ItemStack stack, @Nullable ItemStack[] choices)
    {
        ListTag list = new ListTag();
        if (choices != null)
        {
            for (int i = 0; i < choices.length; ++i)
            {
                ItemStack choice = choices[i];
                if (choice == null || choice.isEmpty()) continue;
                CompoundTag encoded = new CompoundTag();
                encoded.putByte("Index", (byte) i);
                encoded.put("Item", choice.save(new CompoundTag()));
                list.add(encoded);
            }
        }
        stack.getOrCreateTag().put(TAG_WEAPONS, list);
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
