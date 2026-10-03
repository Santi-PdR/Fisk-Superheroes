package com.fiskmods.heroes.common.entity.arrow;

import com.fiskmods.heroes.common.item.ItemTrickArrow;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

/** Server-authoritative special arrow projectile. */
public class TrickArrowEntity extends Arrow
{
    private static final EntityDataAccessor<String> ARROW_TYPE = SynchedEntityData.defineId(TrickArrowEntity.class, EntityDataSerializers.STRING);

    public TrickArrowEntity(EntityType<? extends TrickArrowEntity> type, Level level)
    {
        super(type, level);
    }

    public TrickArrowEntity(EntityType<? extends TrickArrowEntity> type, Level level, LivingEntity shooter, String arrowType)
    {
        this(type, level);
        setOwner(shooter);
        setPos(shooter.getX(), shooter.getEyeY() - 0.1D, shooter.getZ());
        setArrowType(arrowType);
    }

    @Override
    protected void defineSynchedData()
    {
        super.defineSynchedData();
        entityData.define(ARROW_TYPE, ItemTrickArrow.NORMAL);
    }

    public String getArrowType()
    {
        return entityData.get(ARROW_TYPE);
    }

    public void setArrowType(String type)
    {
        entityData.set(ARROW_TYPE, ItemTrickArrow.EXPLOSIVE.equals(type) ? ItemTrickArrow.EXPLOSIVE : ItemTrickArrow.NORMAL);
    }

    @Override
    protected void onHitEntity(EntityHitResult result)
    {
        super.onHitEntity(result);
        detonateIfExplosive();
    }

    @Override
    protected void onHitBlock(BlockHitResult result)
    {
        super.onHitBlock(result);
        detonateIfExplosive();
    }

    @Override
    public void playerTouch(Player player)
    {
        if (ItemTrickArrow.EXPLOSIVE.equals(getArrowType()) && inGround)
        {
            detonateIfExplosive();
            return;
        }
        super.playerTouch(player);
    }

    private void detonateIfExplosive()
    {
        if (level().isClientSide || !ItemTrickArrow.EXPLOSIVE.equals(getArrowType())) return;
        level().explode(getOwner(), getX(), getY(), getZ(), 2.0F, false, Level.ExplosionInteraction.NONE);
        discard();
    }

    @Override
    protected ItemStack getPickupItem()
    {
        return ItemTrickArrow.createStack(getArrowType());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag)
    {
        super.addAdditionalSaveData(tag);
        tag.putString("ArrowType", getArrowType());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);
        setArrowType(tag.getString("ArrowType"));
    }
}
