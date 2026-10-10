package com.fiskmods.heroes.pack.js;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;

/** Block facade used by hero scripts for world collision and block queries. */
public final class JSBlock
{
    private final Level level;
    private final BlockPos position;

    public JSBlock(Level level, BlockPos position)
    {
        this.level = level;
        this.position = position.immutable();
    }

    public boolean isEmpty()
    {
        return state().isAir();
    }

    public String name()
    {
        return BuiltInRegistries.BLOCK.getKey(state().getBlock()).toString();
    }

    public int metadata()
    {
        return 0;
    }

    public float hardness()
    {
        return state().getDestroySpeed(level, position);
    }

    /** True when the block has a collision shape at this position. */
    public boolean isSolid()
    {
        return !state().getCollisionShape(level, position, CollisionContext.empty()).isEmpty();
    }

    public boolean matches(JSBlock other)
    {
        return other != null && state().getBlock() == other.state().getBlock();
    }

    private BlockState state()
    {
        return level.getBlockState(position);
    }
}
