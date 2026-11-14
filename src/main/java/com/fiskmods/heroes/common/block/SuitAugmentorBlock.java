package com.fiskmods.heroes.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

/** The augmentor owns no inventory: the menu returns its two temporary inputs on close. */
public final class SuitAugmentorBlock extends Block
{
    public SuitAugmentorBlock(Properties properties) { super(properties); }
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit)
    {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer)
            NetworkHooks.openScreen(serverPlayer, new SimpleMenuProvider(
                    (id, inv, p) -> new SuitAugmentorMenu(id, inv, pos),
                    Component.translatable("gui.suit_augmentor")), pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
