package com.fiskmods.heroes.common.block;

import java.util.List;

import com.fiskmods.heroes.FiskHeroes;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.util.RandomSource;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Registered solid mineral blocks ported from the original block registry. */
public final class ModBlocks
{
    public static final DeferredRegister<Block> REGISTRY = DeferredRegister.create(ForgeRegistries.BLOCKS, FiskHeroes.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, FiskHeroes.MODID);

    public static final RegistryObject<Block> TUTRIDIUM_STONE = registerTutridiumStone();
    public static final RegistryObject<Block> TUTRIDIUM_BLOCK = register("tutridium_block", 5.0F, 10.0F, true, 2);
    public static final RegistryObject<Block> CRYSTALLINE_TUTRITE_BLOCK = register("crystalline_tutrite_block", 5.0F, 10.0F, true, 2);
    public static final RegistryObject<Block> IRIDESCENT_GOLD_BLOCK = register("iridescent_gold_block", 4.0F, 10.0F, true, 2);
    public static final RegistryObject<Block> VIBRANIUM_BLOCK = register("vibranium_block", 5.0F, 4000.0F, true, 2);
    public static final RegistryObject<Block> TITANIUM_BLOCK = register("titanium_block", 10.0F, 100.0F, true, 2);
    public static final RegistryObject<Block> GOLD_TITANIUM_BLOCK = register("gold_titanium_block", 8.0F, 200.0F, true, 2);
    public static final RegistryObject<Block> DWARF_STAR_BLOCK = register("dwarf_star_block", 17.5F, 4000.0F, true, 3);
    public static final RegistryObject<Block> OLIVINE_BLOCK = register("olivine_block", 3.0F, 5.0F, false, 1);
    public static final RegistryObject<Block> PACKED_OLIVINE = register("packed_olivine", 2.0F, 5.0F, false, 1);
    public static final RegistryObject<Block> ETERNIUM_BLOCK = register("eternium_block", 7.5F, 6000.0F, true, 3);
    public static final RegistryObject<Block> SUPERCHARGED_ETERNIUM = registerSuperchargedEternium();
    public static final RegistryObject<Block> NEXUS_BRICKS = register("nexus_bricks", 3.0F, 100.0F, false, 0);
    public static final RegistryObject<Block> NEXUS_SOIL = registerNexusSoil();
    public static final RegistryObject<Block> NEXUS_BRICK_STAIRS = registerNexusBrickStairs();
    public static final RegistryObject<Block> NEXUS_BRICK_SLAB = registerNexusBrickSlab();
    public static final RegistryObject<Block> NEXUS_BRICK_DOUBLE_SLAB = register("nexus_brick_double_slab", 3.0F, 100.0F, false, 0);
    public static final RegistryObject<Block> ETERNIUM_STONE = register("eternium_stone", 5.0F, 3000.0F, false, 0);
    public static final RegistryObject<Block> TUTRIDIUM_ORE = ore("tutridium_ore", 3.0F, 5.0F, 2, 3, 7);
    public static final RegistryObject<Block> TITANIUM_ORE = ore("titanium_ore", 4.0F, 100.0F, 2, 0, 0);
    public static final RegistryObject<Block> DWARF_STAR_ORE = ore("dwarf_star_ore", 15.0F, 3000.0F, 3, 3, 7);
    public static final RegistryObject<Block> OLIVINE_ORE = ore("olivine_ore", 2.0F, 5.0F, 1, 0, 2);
    public static final RegistryObject<Block> ETERNIUM_ORE = ore("eternium_ore", 5.0F, 3000.0F, 3, 0, 0);
    public static final RegistryObject<Block> TUTRITE_ORE = ore("tutrite_ore", 5.0F, 20.0F, 3, 1, 3);
    public static final RegistryObject<Block> LUNAR_IRON_ORE = ore("lunar_iron_ore", 3.0F, 5.0F, 1, 0, 0);
    public static final RegistryObject<Block> LUNAR_TITANIUM_ORE = ore("lunar_titanium_ore", 4.0F, 100.0F, 2, 0, 0);
    public static final RegistryObject<Block> LUNAR_OLIVINE_ORE = ore("lunar_olivine_ore", 2.0F, 5.0F, 1, 0, 2);
    public static final RegistryObject<Block> VIBRANIUM_ORE = registerVibraniumOre();
    public static final RegistryObject<Block> LUNAR_ROCK = register("lunar_rock", 1.5F, 10.0F, false, 0);
    public static final RegistryObject<Block> COBBLED_LUNAR_ROCK = register("cobbled_lunar_rock", 2.0F, 10.0F, false, 0);
    /** Invisible, short-lived redstone source used by trick pulse arrows. */
    public static final RegistryObject<Block> PULSE_SIGNAL = REGISTRY.register("pulse_signal", () ->
            new PulseSignalBlock(BlockBehaviour.Properties.of().strength(-1.0F, 3600000.0F)
                    .noLootTable().noOcclusion().noCollission()));

    public static final List<RegistryObject<Block>> ALL = List.of(TUTRIDIUM_STONE, TUTRIDIUM_BLOCK,
            CRYSTALLINE_TUTRITE_BLOCK, IRIDESCENT_GOLD_BLOCK, VIBRANIUM_BLOCK, TITANIUM_BLOCK,
            GOLD_TITANIUM_BLOCK, DWARF_STAR_BLOCK, OLIVINE_BLOCK, PACKED_OLIVINE, ETERNIUM_BLOCK, SUPERCHARGED_ETERNIUM, ETERNIUM_STONE,
            NEXUS_BRICKS, NEXUS_SOIL, NEXUS_BRICK_STAIRS, NEXUS_BRICK_SLAB,
            TUTRIDIUM_ORE, TITANIUM_ORE, DWARF_STAR_ORE, OLIVINE_ORE, ETERNIUM_ORE, TUTRITE_ORE,
            LUNAR_IRON_ORE, LUNAR_TITANIUM_ORE, LUNAR_OLIVINE_ORE, VIBRANIUM_ORE, LUNAR_ROCK, COBBLED_LUNAR_ROCK);

    private ModBlocks() {}

    private static RegistryObject<Block> register(String name, float hardness, float resistance, boolean metal, int harvestLevel)
    {
        RegistryObject<Block> block = REGISTRY.register(name, () ->
        {
            BlockBehaviour.Properties properties = BlockBehaviour.Properties.of()
                    .mapColor(metal ? MapColor.METAL : MapColor.STONE)
                    .strength(hardness, resistance).sound(metal ? SoundType.METAL : SoundType.STONE);
            if (harvestLevel > 0) properties = properties.requiresCorrectToolForDrops();
            return new Block(properties);
        });
        ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static RegistryObject<Block> ore(String name, float hardness, float resistance, int harvestLevel, int xpMin, int xpMax)
    {
        RegistryObject<Block> block = REGISTRY.register(name, () ->
        {
            BlockBehaviour.Properties properties = BlockBehaviour.Properties.of().mapColor(MapColor.STONE)
                    .strength(hardness, resistance).sound(SoundType.STONE).requiresCorrectToolForDrops();
            return new OreBlock(properties, xpMin, xpMax);
        });
        ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static RegistryObject<Block> registerVibraniumOre()
    {
        RegistryObject<Block> block = REGISTRY.register("vibranium_ore", () ->
        {
            BlockBehaviour.Properties properties = BlockBehaviour.Properties.of().mapColor(MapColor.STONE)
                    .strength(3.0F, 2000.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()
                    .lightLevel(state -> 8);
            return new OreBlock(properties, 2, 5);
        });
        ITEMS.register("vibranium_ore", () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static RegistryObject<Block> registerSuperchargedEternium()
    {
        RegistryObject<Block> block = REGISTRY.register("supercharged_eternium", () ->
                new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
                        .strength(20.0F, 6000.0F).sound(SoundType.METAL).lightLevel(state -> 15)
                        .noOcclusion().requiresCorrectToolForDrops()));
        ITEMS.register("supercharged_eternium", () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static RegistryObject<Block> registerNexusSoil()
    {
        RegistryObject<Block> block = REGISTRY.register("nexus_soil", () ->
                new NexusSoilBlock(BlockBehaviour.Properties.of().mapColor(MapColor.DIRT)
                        .strength(0.9F).sound(SoundType.GRAVEL).requiresCorrectToolForDrops()));
        ITEMS.register("nexus_soil", () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static RegistryObject<Block> registerNexusBrickStairs()
    {
        RegistryObject<Block> block = REGISTRY.register("nexus_brick_stairs", () ->
                new StairBlock(() -> NEXUS_BRICKS.get().defaultBlockState(),
                        BlockBehaviour.Properties.of().mapColor(MapColor.STONE)
                                .strength(3.0F, 100.0F).sound(SoundType.STONE)));
        ITEMS.register("nexus_brick_stairs", () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static RegistryObject<Block> registerNexusBrickSlab()
    {
        RegistryObject<Block> block = REGISTRY.register("nexus_brick_slab", () ->
                new SlabBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE)
                        .strength(3.0F, 100.0F).sound(SoundType.STONE)));
        ITEMS.register("nexus_brick_slab", () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static RegistryObject<Block> registerTutridiumStone()
    {
        RegistryObject<Block> block = REGISTRY.register("tutridium_stone", () ->
                new TutridiumStoneBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE)
                        .strength(1.5F, 10.0F).sound(SoundType.STONE)));
        ITEMS.register("tutridium_stone", () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static final class OreBlock extends Block
    {
        private final int xpMin;
        private final int xpMax;

        private OreBlock(BlockBehaviour.Properties properties, int xpMin, int xpMax)
        {
            super(properties);
            this.xpMin = xpMin;
            this.xpMax = xpMax;
        }

        @Override
        public int getExpDrop(BlockState state, LevelReader level, RandomSource random, BlockPos pos, int fortune, int silkTouch)
        {
            if (silkTouch > 0) return 0;
            return xpMax > xpMin ? xpMin + random.nextInt(xpMax - xpMin + 1) : xpMin;
        }
    }

    private static final class TutridiumStoneBlock extends Block
    {
        private TutridiumStoneBlock(BlockBehaviour.Properties properties)
        {
            super(properties);
        }

        @Override
        public int getExpDrop(BlockState state, LevelReader level, RandomSource random, BlockPos pos, int fortune, int silkTouch)
        {
            return silkTouch > 0 ? 0 : random.nextInt(3) > 0 ? 1 : 0;
        }
    }

    /** Nexus soil's low collision top and entangling/fire response, ported from BlockNexusSoil. */
    private static final class NexusSoilBlock extends Block
    {
        private static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 12.0D, 16.0D);

        private NexusSoilBlock(BlockBehaviour.Properties properties)
        {
            super(properties);
        }

        @Override
        public VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level,
                BlockPos pos, CollisionContext context)
        {
            return SHAPE;
        }

        @Override
        public VoxelShape getCollisionShape(BlockState state, net.minecraft.world.level.BlockGetter level,
                BlockPos pos, CollisionContext context)
        {
            return SHAPE;
        }

        @Override
        public void entityInside(BlockState state, net.minecraft.world.level.Level level, BlockPos pos,
                net.minecraft.world.entity.Entity entity)
        {
            RandomSource random = level.getRandom();
            Vec3 movement = entity.getDeltaMovement();
            entity.setDeltaMovement(movement.x + (random.nextFloat() * 2.0D - 1.0D) * 0.75D,
                    movement.y - 100.0D,
                    movement.z + (random.nextFloat() * 2.0D - 1.0D) * 0.75D);
            entity.makeStuckInBlock(state, new Vec3(0.25D, 0.05D, 0.25D));

            if (!level.isClientSide && level.isEmptyBlock(pos.above()))
            {
                level.setBlock(pos.above(), Blocks.FIRE.defaultBlockState(), 3);
            }
        }
    }

    public static CreativeModeTab createBlockTab()
    {
        return CreativeModeTab.builder()
                .title(net.minecraft.network.chat.Component.translatable("itemGroup.fiskheroes.blocks"))
                .icon(() -> new net.minecraft.world.item.ItemStack(TUTRIDIUM_BLOCK.get()))
                .displayItems((params, output) -> ALL.forEach(block -> output.accept(block.get())))
                .build();
    }
}
