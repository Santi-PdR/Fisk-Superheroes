package com.fiskmods.heroes.common.hero.modifier;

import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.data.var.Vars;
import com.fiskmods.heroes.common.hero.power.Modifier;
import com.fiskmods.heroes.common.hero.power.ModifierEntry;
import com.fiskmods.heroes.common.hero.power.PowerProperty;
import com.fiskmods.heroes.common.hero.ability.AbilityHandler;
import com.fiskmods.heroes.common.entity.GravityWaveEntity;
import com.fiskmods.heroes.common.hero.modifier.AbilityData;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Compact implementations of the "stateful but simple" modifiers. Keeping them in one file makes it
 * easy to compare their behaviour against the original mod's equivalents.
 */
/** Full immunity to a damage group (fire immunity, bullet immunity, ...). */
class ModifierImmunity extends Modifier
{
    private final Modifiers.DamageGroup group;

    ModifierImmunity(ResourceLocation id, Modifiers.DamageGroup group)
    {
        super(id);
        this.group = group;
    }

    @Override
    public boolean isImmuneTo(LivingEntity entity, ModifierEntry entry, DamageSource source, float amount)
    {
        if (group == Modifiers.DamageGroup.FIRE)
        {
            SHPlayerData data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(entity);
            if (data != null && data.getData().get(Vars.METAL_SKIN)
                    && data.getData().get(Vars.METAL_HEAT_COOLDOWN) > 0)
            {
                return false;
            }
        }

        float profileFraction = DamageGroups.profileFraction(group);
        if (profileFraction >= 0.0F)
        {
            return profileFraction >= 1.0F;
        }
        return DamageGroups.groupOf(source, entry.get(PowerProperty.DAMAGE_TYPE)) == group;
    }

    @Override
    public float modifyDamage(LivingEntity entity, ModifierEntry entry, DamageSource source, float amount)
    {
        float profileFraction = DamageGroups.profileFraction(group);
        if (profileFraction > 0.0F && profileFraction < 1.0F)
        {
            return amount * (1.0F - profileFraction);
        }
        return amount;
    }
}

/** Scales damage of a group down. */
class ModifierResistance extends Modifier
{
    private final Modifiers.DamageGroup group;

    ModifierResistance(ResourceLocation id, Modifiers.DamageGroup group)
    {
        super(id);
        this.group = group;
    }

    @Override
    public float modifyDamage(LivingEntity entity, ModifierEntry entry, DamageSource source, float amount)
    {
        Modifiers.DamageGroup target = DamageGroups.groupOf(source, entry.get(PowerProperty.DAMAGE_TYPE));
        float profileFraction = DamageGroups.profileFraction(group);
        if (profileFraction >= 0.0F)
        {
            return profileFraction > 0.0F ? amount * (1.0F - entry.getFloat(entity, PowerProperty.FACTOR)) : amount;
        }

        String declared = entry.get(PowerProperty.DAMAGE_TYPE);
        boolean profileMatch = DamageGroups.profileHasType(declared);

        if (profileMatch || target == group || group == null && target != null)
        {
            return amount * entry.getFloat(entity, PowerProperty.FACTOR);
        }

        return amount;
    }
}

/** Scales damage of a group up (kryptonite-style weaknesses). */
class ModifierWeakness extends Modifier
{
    private final Modifiers.DamageGroup group;

    ModifierWeakness(ResourceLocation id, Modifiers.DamageGroup group)
    {
        super(id);
        this.group = group;
    }

    @Override
    public float modifyDamage(LivingEntity entity, ModifierEntry entry, DamageSource source, float amount)
    {
        Modifiers.DamageGroup target = DamageGroups.groupOf(source, entry.get(PowerProperty.DAMAGE_TYPE));
        float profileFraction = DamageGroups.profileFraction(group);
        if (profileFraction >= 0.0F)
        {
            // The original weakness multiplier applies only to the matching share of a
            // mixed DamageProfile; the rest of the hit keeps its normal damage.
            float multiplier = entry.getFloat(entity, PowerProperty.FACTOR);
            return amount * (1.0F + profileFraction * (multiplier - 1.0F));
        }

        if (target == group || group == null && target != null)
        {
            return amount * entry.getFloat(entity, PowerProperty.FACTOR);
        }

        return amount;
    }
}

/** Immunity to projectiles; absolute immunity also blocks explosions and area effects. */
class ModifierProjectileImmunity extends Modifier
{
    ModifierProjectileImmunity(ResourceLocation id)
    {
        super(id);
    }

    @Override
    public boolean isImmuneTo(LivingEntity entity, ModifierEntry entry, DamageSource source, float amount)
    {
        if (source.is(DamageTypeTags.IS_PROJECTILE))
        {
            if (source.getDirectEntity() instanceof com.fiskmods.heroes.common.entity.arrow.TrickArrowEntity arrow
                    && arrow.canPierceDurability(entity))
            {
                return false;
            }
            return true;
        }

        return entry.getBoolean(entity, PowerProperty.IS_ABSOLUTE) && (source.is(DamageTypeTags.IS_EXPLOSION) || source.getDirectEntity() == null);
    }
}

/** Catches arrows out of the air instead of taking damage. */
class ModifierArrowCatching extends Modifier
{
    ModifierArrowCatching(ResourceLocation id)
    {
        super(id);
    }

    @Override
    public boolean isImmuneTo(LivingEntity entity, ModifierEntry entry, DamageSource source, float amount)
    {
        if (!(source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.AbstractArrow arrow)
                || !canCatch(entity, arrow)) return false;
        return catchArrow(entity, arrow);
    }

    @Override
    public void tick(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        if (entity.level().isClientSide || entity.tickCount % 5 != 0)
        {
            return;
        }

        for (net.minecraft.world.entity.Entity projectile : entity.level().getEntities(entity, entity.getBoundingBox().inflate(1.5D)))
        {
            if (projectile instanceof net.minecraft.world.entity.projectile.AbstractArrow arrow && canCatch(entity, arrow))
            {
                if (catchArrow(entity, arrow))
                    entity.level().playSound(null, entity.blockPosition(), net.minecraft.sounds.SoundEvents.ITEM_PICKUP,
                            net.minecraft.sounds.SoundSource.PLAYERS, 0.6F, 1.4F);
            }
        }
    }

    private static boolean canCatch(LivingEntity entity, net.minecraft.world.entity.projectile.AbstractArrow arrow)
    {
        if (arrow.isRemoved() || !entity.getMainHandItem().isEmpty()) return false;
        if (arrow instanceof com.fiskmods.heroes.common.entity.arrow.TrickArrowEntity trick
                && "excessive".equals(trick.getArrowType())) return false;
        Vec3 toArrow = arrow.position().add(0.0D, arrow.getBbHeight() * 0.5D, 0.0D).subtract(entity.getEyePosition());
        return toArrow.lengthSqr() < 1.0E-6D || entity.getLookAngle().dot(toArrow.normalize()) >= 0.8D;
    }

    private static boolean catchArrow(LivingEntity catcher, net.minecraft.world.entity.projectile.AbstractArrow arrow)
    {
        if (arrow instanceof com.fiskmods.heroes.common.entity.arrow.TrickArrowEntity trick)
        {
            return trick.onCaught(catcher);
        }
        if (!catcher.getMainHandItem().isEmpty()) return false;
        catcher.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,
                net.minecraft.world.item.Items.ARROW.getDefaultInstance());
        if (arrow.isOnFire()) arrow.clearFire();
        arrow.setDeltaMovement(Vec3.ZERO);
        arrow.discard();
        return true;
    }
}

/** Immunity to a declared damage type; the pack spells the type out in the property. */
class ModifierDamageImmunity extends Modifier
{
    ModifierDamageImmunity(ResourceLocation id)
    {
        super(id);
    }

    @Override
    public boolean isImmuneTo(LivingEntity entity, ModifierEntry entry, DamageSource source, float amount)
    {
        String declared = entry.get(PowerProperty.DAMAGE_TYPE);
        float profileFraction = DamageGroups.profileFraction(declared);
        if (profileFraction >= 0.0F)
        {
            return profileFraction >= 1.0F;
        }

        if (declared == null || declared.isEmpty())
        {
            return false;
        }

        Modifiers.DamageGroup group = DamageGroups.groupOf(source, declared);
        return group != null && DamageGroups.groupOf(source, declared) == group && matches(source, declared);
    }

    private boolean matches(DamageSource source, String declared)
    {
        return source.getMsgId().toLowerCase(java.util.Locale.ROOT).contains(declared.toLowerCase(java.util.Locale.ROOT).replace("_", ""));
    }

    @Override
    public float modifyDamage(LivingEntity entity, ModifierEntry entry, DamageSource source, float amount)
    {
        float profileFraction = DamageGroups.profileFraction(entry.get(PowerProperty.DAMAGE_TYPE));
        if (profileFraction > 0.0F && profileFraction < 1.0F)
        {
            return amount * (1.0F - profileFraction);
        }
        return amount;
    }
}

/** Damage resistance whose factor comes from the pack. */
class ModifierDamageResistance extends Modifier
{
    ModifierDamageResistance(ResourceLocation id)
    {
        super(id);
    }

    @Override
    public float modifyDamage(LivingEntity entity, ModifierEntry entry, DamageSource source, float amount)
    {
        String declared = entry.get(PowerProperty.DAMAGE_TYPE);
        float profileFraction = DamageGroups.profileFraction(declared);
        if (profileFraction >= 0.0F)
        {
            return amount * (1.0F - profileFraction * (1.0F - entry.getFloat(entity, PowerProperty.FACTOR)));
        }

        if (declared == null || declared.isEmpty() || source.getMsgId().toLowerCase(java.util.Locale.ROOT).contains(declared.toLowerCase(java.util.Locale.ROOT)))
        {
            return amount * entry.getFloat(entity, PowerProperty.FACTOR);
        }

        return amount;
    }
}

/** Removes potion effects as fast as they are applied. */
class ModifierPotionImmunity extends Modifier
{
    ModifierPotionImmunity(ResourceLocation id)
    {
        super(id);
    }

    @Override
    public void tick(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        if (entity.getActiveEffects().isEmpty())
        {
            return;
        }

        for (MobEffectInstance effect : new java.util.ArrayList<>(entity.getActiveEffects()))
        {
            if (effect.getEffect().getCategory() == net.minecraft.world.effect.MobEffectCategory.HARMFUL)
            {
                entity.removeEffect(effect.getEffect());
            }
        }
    }
}

/** Metal skin: damage is converted into heat, and heat builds up over time. */
class ModifierMetalSkin extends Modifier
{
    ModifierMetalSkin(ResourceLocation id)
    {
        super(id);
    }

    @Override
    public float modifyDamage(LivingEntity entity, ModifierEntry entry, DamageSource source, float amount)
    {
        SHPlayerData data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(entity);

        if (data != null && data.getData().get(Vars.METAL_SKIN))
        {
            MetalSkinHeat.add(entity, amount * 0.02F);
            return amount * entry.getFloat(entity, PowerProperty.FACTOR);
        }

        return amount;
    }

    @Override
    public void tick(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        MetalSkinHeat.tick(entity, data);
    }
}

/** Invisibility: the wearer becomes untargetable while the effect is toggled. */
class ModifierInvisibility extends Modifier
{
    ModifierInvisibility(ResourceLocation id)
    {
        super(id);
    }

    @Override
    public void onActivate(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        boolean state = !entry.isToggled(entity);
        entry.setToggled(entity, state);
        data.getData().set(Vars.INVISIBLE, state);
        apply(entity, state);
    }

    @Override
    public void tick(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        if (data.getData().get(Vars.INVISIBLE))
        {
            entity.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, true, false, false));
        }
    }

    static void apply(LivingEntity entity, boolean invisible)
    {
        entity.setInvisible(invisible);
    }
}

/** Hovering: cancels gravity while the ability is active. */
class ModifierHover extends Modifier
{
    ModifierHover(ResourceLocation id)
    {
        super(id);
    }

    @Override
    public void tick(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        if (!data.getData().get(Vars.HOVERING))
        {
            return;
        }

        Vec3 motion = entity.getDeltaMovement();
        entity.setDeltaMovement(motion.x, entity.isShiftKeyDown() ? -0.1D : 0.0D, motion.z);
        entity.fallDistance = 0.0F;
    }
}

/** Gliding: fall slowly while the ability is toggled on. */
class ModifierGliding extends Modifier
{
    ModifierGliding(ResourceLocation id)
    {
        super(id);
    }

    @Override
    public void onActivate(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        boolean state = !entry.isToggled(entity);
        entry.setToggled(entity, state);
        data.getData().set(Vars.GLIDING, state);
    }

    @Override
    public void tick(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        float timer = data.getData().get(Vars.GLIDING_TIMER);

        if (data.getData().get(Vars.GLIDING) && !entity.onGround())
        {
            Vec3 motion = entity.getDeltaMovement();
            double y = Math.max(motion.y, -0.12D);
            entity.setDeltaMovement(motion.x, y, motion.z);
            entity.fallDistance = Math.min(entity.fallDistance, 1.0F);
            data.getData().set(Vars.GLIDING_TIMER, Math.min(10.0F, timer + 0.5F));
        }
        else if (timer > 0)
        {
            data.getData().set(Vars.GLIDING_TIMER, Math.max(0.0F, timer - 1.0F));
        }
    }
}

/** Retaliates against attackers with reflected damage. */
class ModifierThorns extends Modifier
{
    ModifierThorns(ResourceLocation id)
    {
        super(id);
    }

    @Override
    public float modifyDamage(LivingEntity entity, ModifierEntry entry, DamageSource source, float amount)
    {
        if (source.getEntity() instanceof LivingEntity attacker && attacker != entity)
        {
            attacker.hurt(entity.damageSources().thorns(entity), entry.getFloat(entity, PowerProperty.AMOUNT));
        }

        return amount;
    }
}

/** Lets the wearer walk on water by freezing it underneath. */
class ModifierFrostWalking extends Modifier
{
    ModifierFrostWalking(ResourceLocation id)
    {
        super(id);
    }

    @Override
    public void tick(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        if (entity.level().isClientSide || !(entity instanceof Player player) || !player.onGround() && player.getDeltaMovement().y > 0)
        {
            return;
        }

        if (player.isInWater() || player.level().getBlockState(player.blockPosition().below()).getBlock() == net.minecraft.world.level.block.Blocks.WATER)
        {
            net.minecraft.core.BlockPos pos = player.blockPosition().below();

            if (player.level().getBlockState(pos).getBlock() == net.minecraft.world.level.block.Blocks.WATER)
            {
                player.level().setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.FROSTED_ICE.defaultBlockState());
            }
        }
    }
}

/** Manipulates the gravity affecting the wearer. */
class ModifierGravityManipulation extends Modifier
{
    private static final String KEY = "GRAVITY_MANIPULATION";

    ModifierGravityManipulation(ResourceLocation id)
    {
        super(id);
    }

    @Override
    public void tick(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        boolean active = AbilityHandler.isKeyPressed(entity, KEY);
        data.getData().set(Vars.GRAVITY_MANIP, active);

        if (!entity.level().isClientSide && active && entity.tickCount % 8 == 0
                && entry.getFloat(entity, PowerProperty.RADIUS) > 0.0F
                && entity.level() instanceof net.minecraft.server.level.ServerLevel)
        {
            float range = entry.getFloat(entity, PowerProperty.RANGE);
            net.minecraft.world.phys.HitResult hit = entity.pick(range, 0.0F, false);
            if (hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS)
            {
                hit = entity.pick(range, 1.0F, false);
            }

            net.minecraft.world.phys.Vec3 pos = hit.getLocation();
            int side = -1;
            if (hit instanceof net.minecraft.world.phys.BlockHitResult blockHit)
            {
                side = blockHit.getDirection().get3DDataValue();
                net.minecraft.core.Direction direction = blockHit.getDirection();
                pos = pos.add(direction.getStepX() * 0.1D, direction.getStepY() * 0.1D, direction.getStepZ() * 0.1D);
            }
            else if (hit instanceof net.minecraft.world.phys.EntityHitResult entityHit)
            {
                pos = new net.minecraft.world.phys.Vec3(entityHit.getEntity().getX(), entityHit.getEntity().getY(), entityHit.getEntity().getZ());
            }

            float amount = data.getData().get(Vars.GRAVITY_AMOUNT);
            float gravity = amount > 0.0F
                    ? net.minecraft.util.Mth.lerp(amount, 1.0F, entry.getFloat(entity, PowerProperty.MAX_GRAVITY))
                    : net.minecraft.util.Mth.lerp(-amount, 1.0F, entry.getFloat(entity, PowerProperty.MIN_GRAVITY));
            GravityWaveEntity wave = new GravityWaveEntity(entity.level(), entity, pos.x, pos.y, pos.z, side,
                    entry.getFloat(entity, PowerProperty.RADIUS), gravity, entry.getBoolean(entity, PowerProperty.AFFECTS_USER));
            entity.level().addFreshEntity(wave);
            AbilityData.playSound(entity, entry, "WAVE");
        }

        if (!data.getData().get(Vars.NO_GRAVITY))
        {
            return;
        }

        Vec3 motion = entity.getDeltaMovement();

        if (motion.y < 0)
        {
            entity.setDeltaMovement(motion.x, motion.y * 0.5D, motion.z);
        }

        entity.fallDistance = 0.0F;
    }
}

/** The wearer can climb walls they are pressed against. */
class ModifierWallCrawling extends Modifier
{
    ModifierWallCrawling(ResourceLocation id)
    {
        super(id);
    }

    /** Crawl states stored in the {@code wall_crawling} data variable. */
    static final byte CRAWL_NONE = 0;
    static final byte CRAWL_WALL = 1;
    static final byte CRAWL_CEILING = 2;

    @Override
    public void tick(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        if (!(entity instanceof Player player) || player.onGround())
        {
            data.getData().set(Vars.WALL_CRAWLING, CRAWL_NONE);
            return;
        }

        data.getData().set(Vars.WALL_CRAWLING, player.horizontalCollision ? CRAWL_WALL : CRAWL_NONE);

        if (player.horizontalCollision)
        {
            Vec3 motion = player.getDeltaMovement();

            if (player.zza > 0)
            {
                player.setDeltaMovement(motion.x, 0.2D, motion.z);
            }
            else if (player.isShiftKeyDown())
            {
                player.setDeltaMovement(motion.x, -0.2D, motion.z);
            }
            else
            {
                player.setDeltaMovement(motion.x, 0.0D, motion.z);
            }

            player.fallDistance = 0.0F;
        }
    }
}

/** Alerts the wearer of incoming danger. */
class ModifierSpiderSense extends Modifier
{
    ModifierSpiderSense(ResourceLocation id)
    {
        super(id);
    }

    @Override
    public void tick(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        if (entity.level().isClientSide)
        {
            return;
        }

        for (net.minecraft.world.entity.projectile.Projectile projectile : entity.level().getEntitiesOfClass(net.minecraft.world.entity.projectile.Projectile.class, entity.getBoundingBox().inflate(6.0D)))
        {
            Vec3 motion = projectile.getDeltaMovement();

            if (motion.lengthSqr() > 0.01D)
            {
                entity.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 10, 0, true, false, false));
                break;
            }
        }
    }
}

/** Speedsters tear apart what they run into. */
class ModifierSpeedDisintegration extends Modifier
{
    ModifierSpeedDisintegration(ResourceLocation id)
    {
        super(id);
    }

    @Override
    public void tick(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        if (!(entity instanceof Player player) || player.level().isClientSide || !data.getData().get(Vars.SPEEDING))
        {
            return;
        }

        if (player.getDeltaMovement().horizontalDistanceSqr() < 0.6D)
        {
            return;
        }

        net.minecraft.core.BlockPos origin = player.blockPosition();

        for (int i = 0; i < 6; ++i)
        {
            net.minecraft.core.BlockPos pos = origin.offset(player.getRandom().nextInt(3) - 1, player.getRandom().nextInt(2), player.getRandom().nextInt(3) - 1);
            net.minecraft.world.level.block.state.BlockState state = player.level().getBlockState(pos);

            if (!state.isAir() && state.getDestroySpeed(player.level(), pos) >= 0 && state.getDestroySpeed(player.level(), pos) < 1.0F)
            {
                player.level().destroyBlock(pos, false, player);
            }
        }
    }
}

/** Grants the suit's equipment items. */
class ModifierEquipment extends Modifier
{
    ModifierEquipment(ResourceLocation id)
    {
        super(id);
    }

    @Override
    public void onRespawn(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        if (entity instanceof Player player)
        {
            com.fiskmods.heroes.common.hero.equipment.EquipmentHelper.grantEquipment(player, data);
        }
    }
}

/** Keeps only the configured potion effects from expiring while this modifier is active. */
class ModifierPotionRetention extends Modifier
{
    ModifierPotionRetention(ResourceLocation id)
    {
        super(id);
    }

    @Override
    public void tick(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        if (entity.level().isClientSide || entity.tickCount % 2 == 0) return;

        com.google.gson.JsonElement configured = entry.get(entity, PowerProperty.POTION_EFFECTS);
        if (configured == null || !configured.isJsonArray()) return;

        java.util.Set<net.minecraft.resources.ResourceLocation> retained = new java.util.HashSet<>();
        for (com.google.gson.JsonElement element : configured.getAsJsonArray())
        {
            if (element.isJsonPrimitive())
            {
                net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.tryParse(element.getAsString());
                if (id != null) retained.add(id);
            }
        }

        for (MobEffectInstance effect : new java.util.ArrayList<>(entity.getActiveEffects()))
        {
            net.minecraft.resources.ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect());
            if (id != null && retained.contains(id))
            {
                entity.addEffect(new MobEffectInstance(effect.getEffect(), effect.getDuration() + 1, effect.getAmplifier(),
                        effect.isAmbient(), effect.isVisible(), effect.showIcon()));
            }
        }
    }
}

/** Adds pack-defined bonus damage to melee hits and consumes any configured uses. */
class ModifierDamageBonus extends Modifier
{
    private static final java.util.Map<String, com.fiskmods.heroes.pack.ScriptFunction> EXPRESSIONS = new java.util.concurrent.ConcurrentHashMap<>();

    ModifierDamageBonus(ResourceLocation id)
    {
        super(id);
    }

    @Override
    public float modifyOutgoingDamage(LivingEntity entity, ModifierEntry entry, net.minecraft.world.entity.Entity target,
            DamageSource source, float amount)
    {
        if (entity.level().isClientSide || source.getEntity() != entity || source.getDirectEntity() != entity
                || !(source.is(DamageTypes.PLAYER_ATTACK) || source.is(DamageTypes.MOB_ATTACK)))
        {
            return amount;
        }

        SHPlayerData data = com.fiskmods.heroes.common.data.SHDataCapabilities.getPlayer(entity);
        com.google.gson.JsonElement definition = entry.get(entity, PowerProperty.DAMAGE_BONUS);
        if (data == null || definition == null || !definition.isJsonObject()) return amount;

        com.google.gson.JsonObject bonus = definition.getAsJsonObject();
        com.google.gson.JsonElement dataElement = bonus.get("data");
        if (dataElement == null || !dataElement.isJsonPrimitive()) return amount;
        String dataName = dataElement.getAsString();
        com.fiskmods.heroes.common.data.var.DataVar<?> variable = com.fiskmods.heroes.common.data.DataRegistry.INSTANCE.get(dataName);
        float fraction = variable != null ? readFraction(data, variable) : evaluateFraction(entity, dataName);
        if (fraction <= 0.0F) return amount;

        int uses = bonus.has("uses") ? bonus.get("uses").getAsInt() : -1;
        if (uses > 0 && variable != null && (variable.getType() == com.fiskmods.heroes.common.data.DataType.FLOAT
                || variable.getType() == com.fiskmods.heroes.common.data.DataType.FLOAT_INTERP))
        {
            writeFraction(data, variable, Math.max(0.0F, (int) (fraction * uses - 1.0F) / (float) uses));
        }

        return amount + entry.getFloat(entity, PowerProperty.AMOUNT) * fraction;
    }

    private static float readFraction(SHPlayerData data, com.fiskmods.heroes.common.data.var.DataVar<?> variable)
    {
        Object value = data.getData().get(variable);
        return value instanceof Number number ? number.floatValue() : 0.0F;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void writeFraction(SHPlayerData data, com.fiskmods.heroes.common.data.var.DataVar<?> variable, float value)
    {
        data.getData().set((com.fiskmods.heroes.common.data.var.DataVar) variable, value);
    }

    private static float evaluateFraction(LivingEntity entity, String expression)
    {
        com.fiskmods.heroes.pack.ScriptFunction function = EXPRESSIONS.get(expression);
        if (function == null)
        {
            function = com.fiskmods.heroes.pack.js.JSExpressions.compile(expression);
            if (function == null) return 0.0F;
            EXPRESSIONS.put(expression, function);
        }

        Object result = function.call(new com.fiskmods.heroes.pack.js.JSEntity(entity));
        return result instanceof Number number ? number.floatValue() : 0.0F;
    }
}

/** Charges the cryogenic punch while its key is held, matching the original 20-tick ramp. */
class ModifierCryoCharge extends Modifier
{
    static final String KEY = "CHARGE_ICE";

    ModifierCryoCharge(ResourceLocation id)
    {
        super(id);
    }

    @Override
    public void onActivate(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        data.getData().set(Vars.CRYO_CHARGING, true);
    }

    @Override
    public void onToggle(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        if (!entry.getBoolean(entity, PowerProperty.IS_TOGGLE))
        {
            data.getData().set(Vars.CRYO_CHARGING, false);
        }
    }

    @Override
    public void tick(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        float charge = data.getData().get(Vars.CRYO_CHARGE);
        boolean charging = data.getData().get(Vars.CRYO_CHARGING);
        charge = Math.max(0.0F, Math.min(1.0F, charge + (charging ? 0.05F : -0.05F)));
        data.getData().set(Vars.CRYO_CHARGE, charge);

        if (entry.getBoolean(entity, PowerProperty.IS_TOGGLE) && charging && charge >= 1.0F)
        {
            data.getData().set(Vars.CRYO_CHARGING, false);
        }
    }
}
