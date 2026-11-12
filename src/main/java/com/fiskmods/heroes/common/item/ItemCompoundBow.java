package com.fiskmods.heroes.common.item;

import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.data.var.Vars;
import com.fiskmods.heroes.common.hero.Hero;
import com.fiskmods.heroes.common.hero.HeroAttribute;
import com.fiskmods.heroes.common.hero.HeroIteration;
import com.fiskmods.heroes.common.hero.HeroTracker;
import com.fiskmods.heroes.common.hero.attribute.SHAttributes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

/** Compound bow that draws ammunition from the selected slot of a carried quiver. */
public class ItemCompoundBow extends BowItem
{
    public ItemCompoundBow(Properties properties)
    {
        // durability() sets the stack size to one. Calling stacksTo() as well makes Forge reject
        // the properties, even when the explicit stack size is also one.
        super(properties.durability(1500));
    }

    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer)
    {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions()
        {
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer()
            {
                return com.fiskmods.heroes.client.render.CompoundBowItemRenderer.getInstance();
            }
        });
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        ItemStack bow = player.getItemInHand(hand);
        if (isBroken(bow)) return InteractionResultHolder.fail(bow);
        boolean hasAmmo = !selectedArrow(player).isEmpty();
        var result = net.minecraftforge.event.ForgeEventFactory.onArrowNock(bow, level, player, hand, hasAmmo);
        if (result != null) return result;
        if (!hasAmmo && !player.getAbilities().instabuild) return InteractionResultHolder.fail(bow);

        player.startUsingItem(hand);
        return InteractionResultHolder.consume(bow);
    }

    @Override
    public void releaseUsing(ItemStack bow, Level level, LivingEntity entity, int timeLeft)
    {
        if (!(entity instanceof Player player) || isBroken(bow)) return;
        ItemStack arrowStack = selectedArrow(player);
        boolean hasInfiniteSupply = player.getAbilities().instabuild
                || EnchantmentHelper.getItemEnchantmentLevel(Enchantments.INFINITY_ARROWS, bow) > 0;
        boolean hasAmmo = !arrowStack.isEmpty() || hasInfiniteSupply;
        int chargeTicks = net.minecraftforge.event.ForgeEventFactory.onArrowLoose(bow, level, player,
                getUseDuration(bow) - timeLeft, hasAmmo);
        if (chargeTicks < 0 || level.isClientSide) return;

        int quiverArrowSlot = ItemQuiver.getSelectedArrowSlot(player);
        arrowStack = selectedArrow(player);
        // A bow drawn with ammunition can still fire an Infinity fallback if the last arrow
        // disappears before release, matching Forge's BowItem behavior.
        if (arrowStack.isEmpty() && hasInfiniteSupply) arrowStack = Items.ARROW.getDefaultInstance();
        if (arrowStack.isEmpty() || !(arrowStack.getItem() instanceof ArrowItem arrowItem)) return;
        boolean quiverAmmo = quiverArrowSlot >= 0;

        float power = getPowerForTime(chargeTicks, player);
        if (power < 0.1F) return;

        String type = arrowStack.getItem() instanceof ItemTrickArrow ? ItemTrickArrow.getType(arrowStack) : "";
        boolean triple = "triple".equals(type) || "triple_explosive".equals(type);
        ItemStack projectileStack = triple ? ItemTrickArrow.createStack("triple".equals(type) ? "normal" : "explosive") : arrowStack.copy();
        AbstractArrow arrow = customArrow(arrowItem.createArrow(level, projectileStack, player));
        boolean horizontal = false;
        SHPlayerData data = SHDataCapabilities.getPlayer(player);
        if (data != null) horizontal = data.getData().get(Vars.HORIZONTAL_BOW);
        float velocity = horizontal ? 2.25F : 3.0F;
        float inaccuracy = horizontal ? 0.5F : 1.0F;
        arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, velocity * power, inaccuracy);
        prepareArrow(arrow, bow, power);
        int bottomlessLevel = quiverAmmo ? EnchantmentHelper.getItemEnchantmentLevel(
                com.fiskmods.heroes.common.enchantment.ModEnchantments.INFINITY.get(), ItemQuiver.findQuiver(player)) : 0;
        boolean infinite = player.getAbilities().instabuild || (!quiverAmmo && arrowItem.isInfinite(arrowStack, bow, player))
                || rollBottomless(bottomlessLevel, level.getRandom());
        if (infinite) arrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;

        if (level.addFreshEntity(arrow))
        {
            if (triple && arrowStack.getItem() instanceof ItemTrickArrow trickArrow)
            {
                // The original triple arrows consume one item and emit three projectiles: the
                // center arrow follows the aim and two arrows fan out on either side.
                for (float offset : new float[] { -8.0F, 8.0F })
                {
                    AbstractArrow extra = customArrow(trickArrow.createArrow(level, projectileStack.copy(), player));
                    extra.shootFromRotation(player, player.getXRot(), player.getYRot() + offset, 0.0F,
                            velocity * power, inaccuracy * 1.5F);
                    prepareArrow(extra, bow, power);
                    if (infinite) extra.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
                    level.addFreshEntity(extra);
                }
            }
            if (!infinite)
            {
                if (quiverAmmo) ItemQuiver.consumeArrow(player, quiverArrowSlot);
                else arrowStack.shrink(1);
            }
            bow.hurtAndBreak(1, player, user -> user.broadcastBreakEvent(player.getUsedItemHand()));
            float pitch = 1.0F / (level.getRandom().nextFloat() * 0.4F + 1.2F) + power * 0.5F;
            player.playSound(SoundEvents.ARROW_SHOOT, 1.0F, pitch);
            player.awardStat(Stats.ITEM_USED.get(this));
            player.gameEvent(GameEvent.PROJECTILE_SHOOT);
        }
    }

    public static boolean isBroken(ItemStack stack)
    {
        return stack.hasTag() && stack.getTag().getBoolean("Broken");
    }

    public static void setBroken(ItemStack stack, boolean broken)
    {
        stack.getOrCreateTag().putBoolean("Broken", broken);
    }

    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.level.Level level,
            java.util.List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag)
    {
        if (isBroken(stack)) tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.bow.broken")
                .withStyle(net.minecraft.ChatFormatting.ITALIC));
    }

    /** Each Bottomless level gives a ten-percent chance not to consume the quiver arrow. */
    public static boolean rollBottomless(int level, net.minecraft.util.RandomSource random)
    {
        return level > 0 && random.nextFloat() < .1F * level;
    }

    /**
     * The original bow uses a 30-tick base drawback and resolves the hero's BOW_DRAWBACK
     * attribute against it. A hero value of 0.5 therefore draws in 15 ticks; using BowItem's
     * fixed 20-tick curve loses those pack-specific timings.
     */
    public static float getDrawDuration(LivingEntity entity)
    {
        double base = 30.0D;
        HeroIteration iteration = HeroTracker.getHero(entity);
        if (iteration == null) return (float) base;

        var modifiers = SHAttributes.collect(entity, iteration.getHero());
        Hero.AttributeMod drawback = modifiers.get(HeroAttribute.BOW_DRAWBACK);
        if (drawback == null) return (float) base;

        double adjusted = switch (drawback.operation())
        {
            case 1, 2 -> base * (1.0D + drawback.amount());
            default -> base + drawback.amount();
        };
        double duration = HeroAttribute.BOW_DRAWBACK.isAdditive() ? adjusted : base * 2.0D - adjusted;
        if (!Double.isFinite(duration)) return (float) base;
        return (float) net.minecraft.util.Mth.clamp(duration, 1.0D, 1200.0D);
    }

    private static float getPowerForTime(int chargeTicks, LivingEntity entity)
    {
        float charge = chargeTicks / getDrawDuration(entity);
        return Math.min((charge * charge + charge * 2.0F) / 3.0F, 1.0F);
    }

    private static void prepareArrow(AbstractArrow arrow, ItemStack bow, float power)
    {
        if (power >= 1.0F) arrow.setCritArrow(true);

        int damage = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.POWER_ARROWS, bow);
        if (damage > 0) arrow.setBaseDamage(arrow.getBaseDamage() + damage * 0.5D + 0.5D);
        int knockback = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PUNCH_ARROWS, bow);
        if (knockback > 0) arrow.setKnockback(knockback);
        if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FLAMING_ARROWS, bow) > 0) arrow.setSecondsOnFire(100);

    }

    /** Display the same ammunition selected for firing, including a creative-mode fallback. */
    public static ItemStack getArrowForDisplay(Player player)
    {
        if (player.level().isClientSide && !player.isLocalPlayer())
            return getTrackedArrowForDisplay(SHDataCapabilities.getPlayer(player));
        ItemStack arrow = selectedArrow(player);
        return arrow.isEmpty() && player.getAbilities().instabuild ? Items.ARROW.getDefaultInstance() : arrow;
    }

    /** Reconstructs the nocked type from server data when a remote inventory is unavailable. */
    public static ItemStack getTrackedArrowForDisplay(@javax.annotation.Nullable SHPlayerData data)
    {
        if (data == null) return ItemStack.EMPTY;
        var id = net.minecraft.resources.ResourceLocation.tryParse(data.getData().get(Vars.CURRENT_ARROW));
        if (id == null || !net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(id)) return ItemStack.EMPTY;
        var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id);
        if (!(item instanceof ArrowItem)) return ItemStack.EMPTY;
        ItemStack arrow = new ItemStack(item);
        boolean trick = item instanceof ItemTrickArrow;
        if (trick) ItemTrickArrow.setType(arrow, data.getData().get(Vars.CURRENT_ARROW_TYPE));
        String payload = data.getData().get(Vars.CURRENT_ARROW_PAYLOAD);
        if ((trick || arrow.is(Items.TIPPED_ARROW)) && !payload.isEmpty() && payload.length() <= 8192)
        {
            try
            {
                var tag = net.minecraft.nbt.TagParser.parseTag(payload);
                String[] keys = trick ? new String[] { "AttachedPotion", "AttachedItem" }
                        : new String[] { "Potion", "CustomPotionEffects", "CustomPotionColor" };
                for (String key : keys)
                    if (tag.contains(key)) arrow.getOrCreateTag().put(key, tag.get(key).copy());
            }
            catch (com.mojang.brigadier.exceptions.CommandSyntaxException ignored) { }
        }
        return arrow;
    }

    /** Prefer the selected quiver slot, then support the standard inventory arrow behavior. */
    private static ItemStack selectedArrow(Player player)
    {
        ItemStack quiverArrow = ItemQuiver.getSelectedArrow(player);
        if (!quiverArrow.isEmpty()) return quiverArrow;

        // As in vanilla, held ammo takes precedence over the general inventory when no
        // selected quiver arrow is available. Return live stacks so release consumes that slot.
        ItemStack offhand = player.getOffhandItem();
        if (ItemQuiver.isArrow(offhand)) return offhand;
        ItemStack mainhand = player.getMainHandItem();
        if (ItemQuiver.isArrow(mainhand)) return mainhand;
        for (ItemStack stack : player.getInventory().items)
        {
            if (ItemQuiver.isArrow(stack)) return stack;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack)
    {
        return UseAnim.BOW;
    }
}
