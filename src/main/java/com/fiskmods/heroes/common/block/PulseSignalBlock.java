package com.fiskmods.heroes.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Temporary invisible redstone source backing the original ten-tick pulse-arrow signal.
 * It is only placed into an empty block next to the arrow's impact, never over a player's block.
 */
public final class PulseSignalBlock extends Block
{
    private static final int DURATION_TICKS = 10;

    public PulseSignalBlock(BlockBehaviour.Properties properties)
    {
        super(properties);
    }

    public static boolean trigger(Level level, BlockPos target, Direction face)
    {
        Block block = ModBlocks.PULSE_SIGNAL.get();
        BlockPos source = target.relative(face);
        if (!level.isEmptyBlock(source)) return false;

        BlockState state = block.defaultBlockState();
        if (!level.setBlock(source, state, 3)) return false;
        level.scheduleTick(source, block, DURATION_TICKS);
        level.updateNeighborsAt(target, block);
        return true;
    }

    @Override
    public boolean isSignalSource(BlockState state)
    {
        return true;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction)
    {
        return 15;
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction)
    {
        return 15;
    }

    @Override
    public RenderShape getRenderShape(BlockState state)
    {
        return RenderShape.INVISIBLE;
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random)
    {
        if (!state.is(this)) return;
        level.removeBlock(pos, false);
        level.updateNeighborsAt(pos, this);
    }
}
