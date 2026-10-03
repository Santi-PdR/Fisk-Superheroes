package com.fiskmods.heroes.common.item;

import com.fiskmods.heroes.common.entity.arrow.TrickArrowEntity;
import com.fiskmods.heroes.common.entity.ModEntities;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Arrow variants carried in a quiver; the type is stored on each stack. */
public class ItemTrickArrow extends ArrowItem
{
    private static final String TYPE_TAG = "ArrowType";
    public static final String NORMAL = "normal";
    public static final String EXPLOSIVE = "explosive";

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
        stack.getOrCreateTag().putString(TYPE_TAG, EXPLOSIVE.equals(type) ? EXPLOSIVE : NORMAL);
    }

    public static String getType(ItemStack stack)
    {
        return stack.hasTag() && EXPLOSIVE.equals(stack.getTag().getString(TYPE_TAG)) ? EXPLOSIVE : NORMAL;
    }

    @Override
    public Component getName(ItemStack stack)
    {
        return Component.translatable("item.arrow_" + getType(stack) + ".name");
    }

    @Override
    public AbstractArrow createArrow(Level level, ItemStack stack, LivingEntity shooter)
    {
        if (EXPLOSIVE.equals(getType(stack)))
        {
            return new TrickArrowEntity(ModEntities.TRICK_ARROW.get(), level, shooter, getType(stack));
        }
        return super.createArrow(level, stack, shooter);
    }
}
