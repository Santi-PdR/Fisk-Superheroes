package com.fiskmods.heroes.common.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Forge claim-aware block placement and removal checks for player-driven effects.
 * Callers must independently check the griefing rule, world border and player edit permissions.
 */
public final class ProtectedBlockChanges
{
    private ProtectedBlockChanges() {}

    /** Follow Forge's placement transaction so a canceled claim does not notify neighbors first. */
    public static void placeBlock(ServerPlayer shooter, BlockPos pos, net.minecraft.core.Direction face, BlockState replacement)
    {
        Level level = shooter.level();
        // Never steal another placement's captured snapshots in a re-entrant mod callback.
        if (level.captureBlockSnapshots || level.restoringBlockSnapshots) return;
        int first = level.capturedBlockSnapshots.size();
        boolean placed;
        level.captureBlockSnapshots = true;
        try
        {
            placed = level.setBlock(pos, replacement, 3);
        }
        finally
        {
            level.captureBlockSnapshots = false;
        }
        var snapshots = new java.util.ArrayList<>(level.capturedBlockSnapshots.subList(first, level.capturedBlockSnapshots.size()));
        level.capturedBlockSnapshots.subList(first, level.capturedBlockSnapshots.size()).clear();
        if (!placed || snapshots.isEmpty()) return;
        boolean canceled = snapshots.size() == 1
                ? net.minecraftforge.event.ForgeEventFactory.onBlockPlace(shooter, snapshots.get(0), face)
                : net.minecraftforge.event.ForgeEventFactory.onMultiBlockPlace(shooter, snapshots, face);
        if (canceled)
        {
            level.restoringBlockSnapshots = true;
            try
            {
                for (int i = snapshots.size() - 1; i >= 0; --i) snapshots.get(i).restore(true, false);
            }
            finally
            {
                level.restoringBlockSnapshots = false;
            }
            return;
        }
        for (var snapshot : snapshots)
        {
            BlockPos changed = snapshot.getPos();
            BlockState oldState = snapshot.getReplacedBlock();
            BlockState newState = level.getBlockState(changed);
            newState.onPlace(level, changed, oldState, false);
            level.markAndNotifyBlock(changed, level.getChunkAt(changed), oldState, newState, snapshot.getFlag(), 512);
        }
    }

    public static boolean canBreak(ServerPlayer shooter, BlockPos pos, BlockState state)
    {
        var event = new net.minecraftforge.event.level.BlockEvent.BreakEvent(shooter.level(), pos, state, shooter);
        return !net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event)
                && shooter.level().getBlockState(pos).equals(state);
    }

}
