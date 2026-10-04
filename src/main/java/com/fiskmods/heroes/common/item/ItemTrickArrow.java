package com.fiskmods.heroes.common.item;

import com.fiskmods.heroes.common.entity.arrow.TrickArrowEntity;
import com.fiskmods.heroes.common.entity.ModEntities;
import com.fiskmods.heroes.common.hero.Hero;
import com.fiskmods.heroes.common.hero.HeroTracker;
import com.fiskmods.heroes.common.hero.modifier.Modifiers;
import com.fiskmods.heroes.common.hero.power.ModifierEntry;
import com.fiskmods.heroes.common.hero.power.PowerProperty;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import java.util.List;

/** Arrow variants carried in a quiver; the type is stored on each stack. */
public class ItemTrickArrow extends ArrowItem
{
    private static final String TYPE_TAG = "ArrowType";
    public static final String NORMAL = "normal";
    public static final String EXPLOSIVE = "explosive";
    /** Arrow subtypes shipped by the original hero pack, in stable item-model predicate order. */
    public static final List<String> TYPES = List.of("normal", "blaze", "boxing_glove", "cactus", "carrot",
            "detonator", "ender_pearl", "excessive", "explosive", "explosive_pufferfish", "fire_charge",
            "fireball", "firework", "glitch", "grappling_hook", "gross", "phantom", "pufferfish", "pulse",
            "slime", "smoke_bomb", "sponge", "torch", "triple", "triple_explosive", "tutridium", "vial",
            "vibranium", "vine");

    public ItemTrickArrow(Properties properties)
    {
        super(properties.stacksTo(64));
    }

    public static ItemStack createStack(String type)
    {
        ItemStack stack = new ItemStack(ModItems.TRICK_ARROW.get());
        setType(stack, type);
        return stack;
    }

    public static void setType(ItemStack stack, String type)
    {
        stack.getOrCreateTag().putString(TYPE_TAG, normalizeType(type));
    }

    public static String getType(ItemStack stack)
    {
        return stack.hasTag() ? normalizeType(stack.getTag().getString(TYPE_TAG)) : NORMAL;
    }

    public static String normalizeType(String type)
    {
        return TYPES.contains(type) ? type : NORMAL;
    }

    public static int getTypeIndex(ItemStack stack)
    {
        return TYPES.indexOf(getType(stack));
    }

    @Override
    public Component getName(ItemStack stack)
    {
        return Component.translatable("item.arrow_" + getType(stack) + ".name");
    }

    @Override
    public AbstractArrow createArrow(Level level, ItemStack stack, LivingEntity shooter)
    {
        String type = getType(stack);
        float radius = 2.0F;
        if (EXPLOSIVE.equals(type) || "explosive_pufferfish".equals(type) || "triple_explosive".equals(type))
        {
            Hero hero = HeroTracker.getHeroType(shooter);
            if (hero != null)
            {
                ModifierEntry archery = hero.getPowerContainer().getEntry(Modifiers.ARCHERY.getId());
                if (archery != null) radius = archery.getFloat(shooter, PowerProperty.RADIUS);
            }
        }
        return new TrickArrowEntity(ModEntities.TRICK_ARROW.get(), level, shooter, type, radius);
    }
}
