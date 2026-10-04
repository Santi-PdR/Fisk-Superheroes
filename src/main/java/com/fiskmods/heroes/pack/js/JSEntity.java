package com.fiskmods.heroes.pack.js;

import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.data.var.DataVar;
import com.fiskmods.heroes.common.hero.HeroTracker;
import com.fiskmods.heroes.common.hero.ItemHeroArmor;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The {@code entity} object passed to pack script functions. It exposes the same surface as the
 * original mod's accessor classes so the shipped hero scripts run unchanged.
 */
public class JSEntity
{
    protected final Entity entity;

    public JSEntity(Entity entity)
    {
        this.entity = entity;
    }

    public Entity unwrap()
    {
        return entity;
    }

    /* --- Data --- */

    public Object getData(String key)
    {
        DataVar<?> var = com.fiskmods.heroes.common.data.DataRegistry.INSTANCE.get(key);

        if (var == null)
        {
            return null;
        }

        SHPlayerData data = SHDataCapabilities.getPlayer(entity);

        if (data == null)
        {
            return var.getDefault();
        }

        Object value = data.getData().get(var);
        return value != null ? value : var.getDefault();
    }

    /**
     * Reads a data variable of an entity, or returns {@code null} when the key does not name one
     * (which is how script-valued properties tell variables and expressions apart).
     */
    public static Object read(Entity entity, String key)
    {
        DataVar<?> var = com.fiskmods.heroes.common.data.DataRegistry.INSTANCE.get(key);

        if (var == null)
        {
            return null;
        }

        SHPlayerData data = SHDataCapabilities.getPlayer(entity);

        if (data == null)
        {
            return var.getDefault();
        }

        Object value = data.getData().get(var);
        return value != null ? value : var.getDefault();
    }

    public void setData(String key, Object value)
    {
        DataVar<?> var = com.fiskmods.heroes.common.data.DataRegistry.INSTANCE.get(key);
        SHPlayerData data = SHDataCapabilities.getPlayer(entity);

        if (var == null || data == null)
        {
            return;
        }

        apply(data, var, value);
    }

    /** The entity's velocity, as the pack scripts use it ({@code entity.motion().length()}). */
    public JSVector motion()
    {
        return new JSVector(entity.getDeltaMovement());
    }

    public float getInterpolatedData(String key)
    {
        DataVar<?> var = com.fiskmods.heroes.common.data.DataRegistry.INSTANCE.get(key);

        if (var != null && var.getType() == com.fiskmods.heroes.common.data.DataType.FLOAT_INTERP)
        {
            SHPlayerData data = SHDataCapabilities.getPlayer(entity);

            if (data != null)
            {
                return data.getData().getInterpolated((DataVar<Float>) var, clientPartialTicks());
            }
        }

        Object value = getData(key);
        return value instanceof Number ? ((Number) value).floatValue() : 0.0F;
    }

    /** The fraction of the way through the current tick, used to interpolate values. */
    private static float clientPartialTicks()
    {
        return com.fiskmods.heroes.common.data.RenderTickInfo.get();
    }

    @SuppressWarnings("unchecked")
    static void apply(SHPlayerData data, DataVar<?> var, Object value)
    {
        try
        {
            if (var.getType() == com.fiskmods.heroes.common.data.DataType.BOOLEAN)
            {
                data.getData().set((DataVar<Boolean>) var, value instanceof Boolean ? (Boolean) value : Boolean.parseBoolean(String.valueOf(value)));
            }
            else if (var.getType() == com.fiskmods.heroes.common.data.DataType.FLOAT || var.getType() == com.fiskmods.heroes.common.data.DataType.FLOAT_INTERP)
            {
                data.getData().set((DataVar<Float>) var, value instanceof Number ? ((Number) value).floatValue() : Float.parseFloat(String.valueOf(value)));
            }
            else if (var.getType() == com.fiskmods.heroes.common.data.DataType.INT)
            {
                data.getData().set((DataVar<Integer>) var, value instanceof Number ? ((Number) value).intValue() : Integer.parseInt(String.valueOf(value)));
            }
            else if (var.getType() == com.fiskmods.heroes.common.data.DataType.BYTE)
            {
                data.getData().set((DataVar<Byte>) var, value instanceof Number ? ((Number) value).byteValue() : Byte.parseByte(String.valueOf(value)));
            }
            else
            {
                data.getData().set((DataVar<String>) var, String.valueOf(value));
            }
        }
        catch (Exception e)
        {
            // Malformed script value: ignore rather than break the tick
        }
    }

    /* --- Entity state --- */

    public String name()
    {
        return entity.getName().getString();
    }

    public boolean isSprinting()
    {
        return entity.isSprinting();
    }

    public boolean isInWater()
    {
        return entity.isInWater();
    }

    public boolean isOnGround()
    {
        return entity.onGround();
    }

    public boolean isSneaking()
    {
        return entity.isShiftKeyDown();
    }

    public boolean isWet()
    {
        return entity.isInWaterRainOrBubble();
    }

    public boolean isPunching()
    {
        return entity instanceof LivingEntity living && living.swinging;
    }

    public boolean isAlive()
    {
        return entity.isAlive();
    }

    /** Original {@code JSPlayer.isUsingItem()} predicate used by flight and web-swing scripts. */
    public boolean isUsingItem()
    {
        return entity instanceof LivingEntity living && living.isUsingItem();
    }

    /** Original player accessor predicate used by shield and wing animations. */
    public boolean isBlocking()
    {
        return entity instanceof Player player && player.isBlocking();
    }

    public float getHealth()
    {
        return entity instanceof LivingEntity living ? living.getHealth() : 0.0F;
    }

    public float getMaxHealth()
    {
        return entity instanceof LivingEntity living ? living.getMaxHealth() : 0.0F;
    }

    public int getTicksExisted()
    {
        return entity.tickCount;
    }

    public boolean hasStatusEffect(String name)
    {
        if (!(entity instanceof LivingEntity living))
        {
            return false;
        }

        net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.tryParse(name);

        if (id != null)
        {
            var effect = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.get(id);

            if (effect != null)
            {
                return living.hasEffect(effect);
            }
        }

        return false;
    }

    public boolean is(String type)
    {
        String name = entity.getType().builtInRegistryHolder().key().location().toString();
        return name.equals(type) || name.endsWith(":" + type) || entity.getClass().getSimpleName().equalsIgnoreCase(type);
    }

    public JSEntity as(String type)
    {
        return this;
    }

    public boolean isBookPlayer()
    {
        return false;
    }

    public void loop(String sound)
    {
        // Looped pack sounds are dispatched by the client sound layer
    }

    public void playSound(String sound, float volume, float pitch)
    {
        net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.tryParse(sound);

        if (id != null)
        {
            var event = net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.get(id);

            if (event != null)
            {
                entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), event, entity.getSoundSource(), volume, pitch);
            }
        }
    }

    /* --- Position / motion (the scripts read these as properties) --- */

    public Vec getMotion()
    {
        return new Vec(entity.getDeltaMovement().x, entity.getDeltaMovement().y, entity.getDeltaMovement().z);
    }

    public Vec getMotionInterpolated()
    {
        return getMotion();
    }

    public Vec getPos()
    {
        return new Vec(entity.getX(), entity.getY(), entity.getZ());
    }

    public double getMotionY()
    {
        return entity.getDeltaMovement().y;
    }

    public float getRotPitch()
    {
        return entity.getXRot();
    }

    public float getRotYaw()
    {
        return entity.getYRot();
    }

    public int getDimension()
    {
        return entity.level().dimension().location().hashCode();
    }

    public JSEntity getWorld()
    {
        return this;
    }

    /* --- Worn suit --- */

    public JSItem getWornHelmet()
    {
        return new JSItem(getArmor(0));
    }

    public JSItem getWornChestplate()
    {
        return new JSItem(getArmor(1));
    }

    public JSItem getWornLeggings()
    {
        return new JSItem(getArmor(2));
    }

    public JSItem getWornBoots()
    {
        return new JSItem(getArmor(3));
    }

    public boolean isWearingFullSuit()
    {
        var iteration = entity instanceof Player player ? HeroTracker.getWornSuit(player) : null;

        if (iteration == null)
        {
            return false;
        }

        for (int i = 0; i < 4; ++i)
        {
            if (iteration.getArmorType(i) != null && getArmor(i).isEmpty())
            {
                return false;
            }
        }

        return true;
    }

    public float getPunchTimerInterpolated()
    {
        return 0.0F;
    }

    private ItemStack getArmor(int slot)
    {
        if (entity instanceof Player player)
        {
            return player.getInventory().armor.get(3 - slot);
        }

        return ItemStack.EMPTY;
    }

    public JSItem getHeldItem()
    {
        return new JSItem(entity instanceof LivingEntity living ? living.getMainHandItem() : ItemStack.EMPTY);
    }

    public JSItem getOffhandItem()
    {
        return new JSItem(entity instanceof LivingEntity living ? living.getOffhandItem() : ItemStack.EMPTY);
    }

    public JSItem getItem(String name)
    {
        return new JSItem(ItemStack.EMPTY);
    }

    /** Simple x/y/z record exposed to scripts as {@code entity.motion.x}. */
    public static class Vec
    {
        public final double x;
        public final double y;
        public final double z;

        Vec(double x, double y, double z)
        {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    /** Item facade: scripts call {@code entity.getHeldItem().name()}. */
    public static class JSItem
    {
        private final ItemStack stack;

        public JSItem(ItemStack stack)
        {
            this.stack = stack;
        }

        public String name()
        {
            if (stack.isEmpty())
            {
                return "minecraft:air";
            }

            return net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();
        }

        public boolean isEmpty()
        {
            return stack.isEmpty();
        }

        public boolean matches(JSItem other)
        {
            return other != null && ItemStack.matches(stack, other.stack);
        }

        public int stackSize()
        {
            return stack.getCount();
        }

        public int maxStackSize()
        {
            return stack.getMaxStackSize();
        }

        public int damage()
        {
            return stack.getDamageValue();
        }

        public int maxDamage()
        {
            return stack.getMaxDamage();
        }

        public String displayName()
        {
            return stack.getHoverName().getString();
        }

        /** True for registered firearm items accepted by pack predicates such as {@code isGun()}. */
        public boolean isGun()
        {
            return com.fiskmods.heroes.common.item.ItemGun.isGun(stack);
        }

        public boolean isLaserGun()
        {
            if (stack.isEmpty()) return false;
            net.minecraft.resources.ResourceLocation id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
            return id != null && id.getNamespace().equals(com.fiskmods.heroes.FiskHeroes.MODID)
                    && java.util.Set.of("chronos_rifle", "rip_hunters_gun", "cold_gun", "heat_gun").contains(id.getPath());
        }

        /** Mirrors the original weapon predicate: any main-hand attack-damage attribute modifier. */
        public boolean isWeapon()
        {
            return !stack.isEmpty() && stack.getAttributeModifiers(net.minecraft.world.entity.EquipmentSlot.MAINHAND)
                    .containsKey(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        }

        /** Rifle classification for the bundled weapon set; the original also accepted FiskTag rifles. */
        public boolean isRifle()
        {
            if (stack.isEmpty()) return false;
            net.minecraft.resources.ResourceLocation id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
            return id != null && id.getNamespace().equals(com.fiskmods.heroes.FiskHeroes.MODID)
                    && id.getPath().equals("chronos_rifle");
        }

        /** Whether the held item occupies both hands for the original suit animation predicates. */
        public boolean doesNeedTwoHands()
        {
            return isRifle() || !stack.isEmpty()
                    && stack.getItem() instanceof com.fiskmods.heroes.common.item.ItemCompoundBow;
        }

        public boolean isRenamed()
        {
            return stack.hasCustomHoverName();
        }

        public boolean isEnchanted()
        {
            return stack.isEnchanted();
        }

        public int getEnchantmentLevel(int numericId)
        {
            net.minecraft.world.item.enchantment.Enchantment enchantment =
                    net.minecraft.core.registries.BuiltInRegistries.ENCHANTMENT.byId(numericId);
            return enchantment == null ? 0 : net.minecraft.world.item.enchantment.EnchantmentHelper
                    .getItemEnchantmentLevel(enchantment, stack);
        }

        public boolean hasEnchantment(int numericId)
        {
            return getEnchantmentLevel(numericId) > 0;
        }

        public int getCount()
        {
            return stack.getCount();
        }

        /** NBT facade used by original pack predicates such as {@code item.nbt().getBoolean(...)}. */
        public net.minecraft.nbt.CompoundTag nbt()
        {
            return stack.getOrCreateTag();
        }

        public String getHero()
        {
            return ItemHeroArmor.getHeroId(stack) != null ? ItemHeroArmor.getHeroId(stack).toString() : null;
        }

        /** Original pack scripts use suitType() to choose mixed-set textures and sound variants. */
        public String suitType()
        {
            com.fiskmods.heroes.common.hero.HeroIteration iteration = ItemHeroArmor.getHero(stack);
            return iteration != null ? iteration.getFullName() : "null";
        }

        public ItemStack unwrap()
        {
            return stack;
        }
    }
}
