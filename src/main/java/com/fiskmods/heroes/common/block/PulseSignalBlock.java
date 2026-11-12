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
    // The server owns these maps. Weak level keys prevent an unloaded world from being retained.
    private static final java.util.Map<Level, java.util.Map<BlockPos, Long>> EXPIRATIONS = new java.util.WeakHashMap<>();

    public PulseSignalBlock(BlockBehaviour.Properties properties)
    {
        super(properties);
    }

    public static boolean trigger(Level level, BlockPos target, Direction face)
    {
        if (level.isClientSide) return false;
        Block block = ModBlocks.PULSE_SIGNAL.get();
        BlockPos source = target.relative(face);
        boolean existing = level.getBlockState(source).is(block);
        if (!existing && !level.isEmptyBlock(source)) return false;

        if (!existing && !level.setBlock(source, block.defaultBlockState(), 3)) return false;
        // Repeated hits refresh the ten-tick window, as the original map-data power did.
        EXPIRATIONS.computeIfAbsent(level, ignored -> new java.util.HashMap<>())
                .put(source.immutable(), level.getGameTime() + DURATION_TICKS);
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
        var expirations = EXPIRATIONS.get(level);
        Long expiration = expirations != null ? expirations.get(pos) : null;
        if (expiration != null && expiration > level.getGameTime())
        {
            level.scheduleTick(pos, this, (int) (expiration - level.getGameTime()));
            return;
        }
        level.removeBlock(pos, false);
        level.updateNeighborsAt(pos, this);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving)
    {
        if (!level.isClientSide && !replacement.is(this))
        {
            var expirations = EXPIRATIONS.get(level);
            if (expirations != null)
            {
                expirations.remove(pos);
                if (expirations.isEmpty()) EXPIRATIONS.remove(level);
            }
        }
        super.onRemove(state, level, pos, replacement, moving);
    }
}
