package com.fiskmods.heroes.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

import com.fiskmods.heroes.common.hero.HeroIteration;
import com.fiskmods.heroes.common.hero.ItemHeroArmor;

import java.util.ArrayList;
import java.util.List;

/**
 * Suit Iterator machine block ported from 1.7.10 BlockSuitIterator.
 * Opens the Suit Iterator GUI on right-click and cycles an ArmorStand suit above it on redstone pulse!
 */
public final class SuitIteratorBlock extends Block
{
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    public SuitIteratorBlock(BlockBehaviour.Properties properties)
    {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        builder.add(POWERED);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit)
    {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer)
        {
            NetworkHooks.openScreen(serverPlayer, new SimpleMenuProvider(
                    (id, inv, p) -> new SuitIteratorMenu(id, inv, pos),
                    Component.translatable("gui.suit_iterator")), pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
            BlockPos neighborPos, boolean movedByPiston)
    {
        if (level.isClientSide) return;
        boolean powered = level.hasNeighborSignal(pos) || level.hasNeighborSignal(pos.above());
        boolean wasPowered = state.getValue(POWERED);
        if (powered != wasPowered)
        {
            level.setBlock(pos, state.setValue(POWERED, powered), 3);
            if (powered)
            {
                cycleArmorStandAbove(level, pos);
            }
        }
    }

    private static void cycleArmorStandAbove(Level level, BlockPos pos)
    {
        List<ArmorStand> stands = level.getEntitiesOfClass(ArmorStand.class, new AABB(pos.above()));
        if (stands.isEmpty()) return;
        ArmorStand stand = stands.get(0);
        HeroIteration current = null;
        for (EquipmentSlot slot : ARMOR_SLOTS)
        {
            HeroIteration it = ItemHeroArmor.getHero(stand.getItemBySlot(slot));
            if (it != null)
            {
                current = it;
                break;
            }
        }
        if (current == null) return;

        List<HeroIteration> all = new ArrayList<>(current.getHero().getIterations().values());
        if (all.size() <= 1) return;
        int idx = all.indexOf(current);
        HeroIteration next = all.get((idx + 1) % all.size());
        for (int i = 0; i < 4; ++i)
        {
            ItemStack piece = stand.getItemBySlot(ARMOR_SLOTS[i]);
            if (ItemHeroArmor.isSuitPiece(piece) && ItemHeroArmor.getHero(piece) != null
                    && ItemHeroArmor.getHero(piece).getHero().equals(current.getHero()))
            {
                ItemStack copy = piece.copy();
                ItemHeroArmor.setHero(copy, next);
                stand.setItemSlot(ARMOR_SLOTS[i], copy);
            }
        }
    }
}
