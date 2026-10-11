package com.fiskmods.heroes.common.hero.modifier;

import com.fiskmods.heroes.common.block.ModBlocks;
import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.hero.power.Modifier;
import com.fiskmods.heroes.common.hero.power.ModifierEntry;
import com.fiskmods.heroes.common.hero.power.PowerProperty;
import com.fiskmods.heroes.common.item.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;

/** Applies the original temporary Eternium status when poison Eternium is carried or nearby. */
public final class ModifierEterniumWeakness extends Modifier
{
    public ModifierEterniumWeakness(ResourceLocation id)
    {
        super(id);
    }

    @Override
    public void tick(LivingEntity entity, ModifierEntry entry, SHPlayerData data)
    {
        if (entity.tickCount % 10 != 0)
        {
            return;
        }

        float radius = entry.getFloat(entity, PowerProperty.RADIUS);
        int duration = entry.getInt(entity, PowerProperty.DURATION);
        if (duration <= 0 || !exposedToPoisonEternium(entity, radius))
        {
            return;
        }

        var effect = ModEffects.ETERNIUM.get();
        if (!entity.hasEffect(effect))
        {
            AbilityData.playSound(entity, entry, "INFLICT");
        }
        // The hidden vanilla effect is the synchronized, expiring carrier for the original
        // custom StatusEffect. Hero scripts query it by the original id fiskheroes:eternium.
        entity.addEffect(new MobEffectInstance(effect, duration, 0, true, false, false));
    }

    private static boolean exposedToPoisonEternium(LivingEntity entity, float radius)
    {
        if (entity instanceof Player player)
        {
            for (ItemStack stack : player.getInventory().items)
            {
                if (!stack.isEmpty() && isPoisonEterniumItem(stack))
                {
                    return true;
                }
            }
        }

        if (radius <= 0.0F)
        {
            return false;
        }

        AABB area = entity.getBoundingBox().inflate(radius);
        BlockPos min = BlockPos.containing(area.minX, area.minY, area.minZ);
        BlockPos max = BlockPos.containing(area.maxX, area.maxY, area.maxZ);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = min.getX(); x <= max.getX(); ++x)
        {
            for (int y = min.getY(); y <= max.getY(); ++y)
            {
                for (int z = min.getZ(); z <= max.getZ(); ++z)
                {
                    pos.set(x, y, z);
                    // Do not make a proximity check load a neighbouring chunk.
                    if (entity.level().hasChunkAt(pos) && isPoisonEterniumBlock(entity.level().getBlockState(pos).getBlock()))
                    {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean isPoisonEterniumItem(ItemStack stack)
    {
        return stack.is(ModItems.ETERNIUM_SHARD.get())
                || stack.is(ModBlocks.ETERNIUM_ORE.get().asItem())
                || stack.is(ModBlocks.ETERNIUM_BLOCK.get().asItem())
                || stack.is(ModBlocks.SUPERCHARGED_ETERNIUM.get().asItem());
    }

    private static boolean isPoisonEterniumBlock(Block block)
    {
        return block == ModBlocks.ETERNIUM_ORE.get() || block == ModBlocks.ETERNIUM_BLOCK.get()
                || block == ModBlocks.SUPERCHARGED_ETERNIUM.get();
    }
}
