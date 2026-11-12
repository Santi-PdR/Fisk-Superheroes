package com.fiskmods.heroes.common.hero.modifier;

import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.data.var.Vars;
import com.fiskmods.heroes.common.hero.power.Modifier;
import com.fiskmods.heroes.common.hero.power.ModifierEntry;
import net.minecraft.world.entity.LivingEntity;

/** Handles the held horizontal-aim ability used by the archer hero iterations. */
public class ModifierArchery extends Modifier
{
    public static final String KEY_HORIZONTAL = "HORIZONTAL_BOW";
    public static final String KEY_QUIVER = "QUIVER_CYCLE";

    public ModifierArchery(net.minecraft.resources.ResourceLocation id)
    {
        super(id);
    }

    @Override
    public void onActivate(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        data.getData().set(Vars.HORIZONTAL_BOW, true);
    }

    @Override
    public void onToggle(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        if (!isDrawingBow(entity)) data.getData().set(Vars.HORIZONTAL_BOW, false);
    }

    private static boolean isDrawingBow(LivingEntity entity)
    {
        return entity.isUsingItem() && entity.getUseItem().is(com.fiskmods.heroes.common.item.ModItems.COMPOUND_BOW.get());
    }

    @Override
    public void tick(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        boolean held = com.fiskmods.heroes.common.hero.ability.AbilityHandler.isKeyPressed(entity, KEY_HORIZONTAL);
        boolean active = held || (data.getData().get(Vars.HORIZONTAL_BOW) && isDrawingBow(entity));
        data.getData().set(Vars.HORIZONTAL_BOW, active);
        float timer = data.getData().get(Vars.HORIZONTAL_BOW_TIMER);
        data.getData().set(Vars.PREV_HORIZONTAL_BOW_TIMER, timer);
        data.getData().set(Vars.HORIZONTAL_BOW_TIMER, net.minecraft.util.Mth.approach(timer, active ? 1.0F : 0.0F, .2F));
    }

    @Override
    public float modifyDamage(LivingEntity entity, ModifierEntry entry, net.minecraft.world.damagesource.DamageSource source, float amount)
    {
        var data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(entity);
        var held = entity.getMainHandItem();
        if (!entity.level().isClientSide && data != null && data.getData().get(Vars.HORIZONTAL_BOW_TIMER) > 0
                && held.is(com.fiskmods.heroes.common.item.ModItems.COMPOUND_BOW.get())
                && !com.fiskmods.heroes.common.item.ItemCompoundBow.isBroken(held)
                && DamageGroups.profileHasType("SHURIKEN"))
        {
            com.fiskmods.heroes.common.item.ItemCompoundBow.setBroken(held, true);
            entity.playSound(net.minecraft.sounds.SoundEvents.ITEM_BREAK, .8F, .8F + entity.getRandom().nextFloat() * .4F);
        }
        return amount;
    }
}
