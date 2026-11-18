package com.fiskmods.heroes.common.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ShieldItem;

/** Captain America's Vibranium shield with its original curved 3D ModelCapsShield renderer. */
public final class ItemCaptainAmericaShield extends ShieldItem
{
    public static final String TAG_ELECTROMAGNETIC = "Electromagnetic";
    private static final java.util.UUID ATTACK_DAMAGE_ID = java.util.UUID.fromString("dc1b37ab-0ff1-44ea-9580-c1b2155c44ef");

    public static boolean isElectromagnetic(ItemStack stack)
    {
        return stack.hasTag() && stack.getTag().getBoolean(TAG_ELECTROMAGNETIC);
    }

    public static ItemStack setElectromagnetic(ItemStack stack, boolean value)
    {
        stack.getOrCreateTag().putBoolean(TAG_ELECTROMAGNETIC, value);
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, @javax.annotation.Nullable net.minecraft.world.level.Level level,
            java.util.List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag)
    {
        super.appendHoverText(stack, level, tooltip, flag);
        if (isElectromagnetic(stack)) tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.shield.electromagnetic")
                .withStyle(net.minecraft.ChatFormatting.GRAY));
    }

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
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(net.minecraft.world.level.Level level,
            net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand)
    {
        var data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(player);
        var hero = data != null ? data.getHeroType() : null;
        boolean allowed = hero != null && hero.hasPermission(player, "USE_SHIELD")
                && data.getData().get(com.fiskmods.heroes.common.data.var.Vars.SHIELD_COOLDOWN) == 0;
        if (Boolean.getBoolean("fiskheroes.ciShieldDiagnostics"))
            com.fiskmods.heroes.FiskHeroes.LOGGER.info("Shield use: player={}, side={}, allowed={}",
                    player.getName().getString(), level.isClientSide ? "client" : "server", allowed);
        if (!allowed) return net.minecraft.world.InteractionResultHolder.fail(player.getItemInHand(hand));
        return super.use(level, player, hand);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, net.minecraft.world.entity.LivingEntity target,
            net.minecraft.world.entity.LivingEntity attacker)
    {
        stack.hurtAndBreak(1, attacker, entity -> entity.broadcastBreakEvent(net.minecraft.world.entity.EquipmentSlot.MAINHAND));
        return true;
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
