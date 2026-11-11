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
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
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
        if ("firework".equals(normalizeType(type)))
        {
            ItemStack firework = new ItemStack(Items.FIREWORK_ROCKET);
            CompoundTag fireworks = new CompoundTag();
            fireworks.putByte("Flight", (byte) 3);
            net.minecraft.nbt.ListTag explosions = new net.minecraft.nbt.ListTag();
            CompoundTag explosion = new CompoundTag();
            explosion.putByte("Type", (byte) 0);
            net.minecraft.nbt.IntArrayTag colors = new net.minecraft.nbt.IntArrayTag(new int[] { 15790320, 11743532 });
            explosion.put("Colors", colors);
            explosions.add(explosion);
            fireworks.put("Explosions", explosions);
            firework.getOrCreateTag().put("Fireworks", fireworks);
            setAttachedItem(stack, firework);
        }
        if ("vial".equals(normalizeType(type)))
        {
            setAttachedPotion(stack, PotionUtils.setPotion(Items.SPLASH_POTION.getDefaultInstance(), Potions.POISON));
        }
        return stack;
    }

    public static void setAttachedPotion(ItemStack arrow, ItemStack potion)
    {
        if (potion != null && !potion.isEmpty())
        {
            arrow.getOrCreateTag().put("AttachedPotion", potion.save(new CompoundTag()));
        }
    }

    public static ItemStack getAttachedPotion(ItemStack arrow)
    {
        if (!arrow.hasTag() || !arrow.getTag().contains("AttachedPotion", CompoundTag.TAG_COMPOUND))
        {
            return ItemStack.EMPTY;
        }
        return ItemStack.of(arrow.getTag().getCompound("AttachedPotion"));
    }

    public static void setAttachedItem(ItemStack arrow, ItemStack item)
    {
        if (item != null && !item.isEmpty())
        {
            arrow.getOrCreateTag().put("AttachedItem", item.save(new CompoundTag()));
        }
    }

    public static ItemStack getAttachedItem(ItemStack arrow)
    {
        if (!arrow.hasTag() || !arrow.getTag().contains("AttachedItem", CompoundTag.TAG_COMPOUND))
        {
            return ItemStack.EMPTY;
        }
        return ItemStack.of(arrow.getTag().getCompound("AttachedItem"));
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
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, net.minecraft.world.entity.player.Player player,
            net.minecraft.world.InteractionHand hand)
    {
        ItemStack stack = player.getItemInHand(hand);
        if ("carrot".equals(getType(stack)) && player.canEat(false))
        {
            player.startUsingItem(hand);
            return net.minecraft.world.InteractionResultHolder.consume(stack);
        }
        return net.minecraft.world.InteractionResultHolder.pass(stack);
    }

    @Override public net.minecraft.world.item.UseAnim getUseAnimation(ItemStack stack)
    { return "carrot".equals(getType(stack)) ? net.minecraft.world.item.UseAnim.EAT : net.minecraft.world.item.UseAnim.NONE; }
    @Override public int getUseDuration(ItemStack stack) { return "carrot".equals(getType(stack)) ? 32 : 0; }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity living)
    {
        if (!(living instanceof net.minecraft.world.entity.player.Player player) || !"carrot".equals(getType(stack))) return stack;
        if (!level.isClientSide)
        {
            player.getFoodData().eat(4, 0.6F);
            player.playSound(net.minecraft.sounds.SoundEvents.PLAYER_BURP, 0.5F, 1.0F);
            if (!player.getAbilities().instabuild)
            {
                stack.shrink(1);
                ItemStack recovered = createStack(NORMAL);
                if (stack.isEmpty()) return recovered;
                if (!player.getInventory().add(recovered)) player.drop(recovered, false);
            }
        }
        return stack;
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
        TrickArrowEntity arrow = new TrickArrowEntity(ModEntities.TRICK_ARROW.get(), level, shooter, type, radius);
        if ("firework".equals(type))
        {
            ItemStack firework = getAttachedItem(stack);
            if (firework.isEmpty()) firework = getAttachedItem(createStack("firework"));
            arrow.setFireworkStack(firework);
        }
        if ("vial".equals(type))
        {
            ItemStack potion = getAttachedPotion(stack);
            if (potion.isEmpty()) potion = PotionUtils.setPotion(Items.SPLASH_POTION.getDefaultInstance(), Potions.POISON);
            arrow.setVialPotion(potion);
        }
        return arrow;
    }
}
