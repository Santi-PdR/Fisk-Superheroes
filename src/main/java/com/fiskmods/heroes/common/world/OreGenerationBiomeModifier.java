package com.fiskmods.heroes.common.world;

import java.util.List;
import java.util.Random;

import com.fiskmods.heroes.common.block.ModBlocks;
import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ModifiableBiomeInfo.BiomeInfo.Builder;

/** Keeps the original ore frequencies, height bands, and biome climate gates. */
public final class OreGenerationBiomeModifier implements BiomeModifier
{
    public static final OreGenerationBiomeModifier INSTANCE = new OreGenerationBiomeModifier();
    public static final Codec<OreGenerationBiomeModifier> CODEC = Codec.unit(INSTANCE);
    private static final Feature<NoneFeatureConfiguration> TUTRIDIUM_VEIN = new TutridiumVeinFeature();
    private static final Feature<NoneFeatureConfiguration> OLIVINE_VEIN = new OlivineVeinFeature();
    private static final Feature<NoneFeatureConfiguration> ETERNITY_NEXUS = new NexusStructureFeature();

    private OreGenerationBiomeModifier() {}

    @Override
    public void modify(Holder<Biome> biome, Phase phase, Builder builder)
    {
        if (phase != Phase.ADD) return;
        addNexus(builder);
        Biome.ClimateSettings climate = biome.value().getModifiedClimateSettings();
        float temperature = climate.temperature();
        float downfall = climate.downfall();
        add(builder, create(TUTRIDIUM_VEIN, NoneFeatureConfiguration.INSTANCE, 6, 0, 32));
        add(builder, ore(ModBlocks.TITANIUM_ORE.get().defaultBlockState(), 8, 2, 0, 32));
        add(builder, ore(ModBlocks.ETERNIUM_ORE.get().defaultBlockState(), 4, 3, 0, 5));
        if (temperature >= 2.0F)
        {
            add(builder, ore(ModBlocks.DWARF_STAR_ORE.get().defaultBlockState(), 3, 1, 0, 16));
        }
        if (temperature >= 0.9F && temperature <= 1.5F && downfall <= 0.0F)
        {
            add(builder, ore(ModBlocks.VIBRANIUM_ORE.get().defaultBlockState(), 6, 2, 32, 48));
        }
        int olivineCount = temperature >= 0.9F && downfall >= 0.8F ? 4 : 1;
        for (int i = 0; i < olivineCount; ++i)
        {
            add(builder, create(OLIVINE_VEIN, NoneFeatureConfiguration.INSTANCE, 1, 0, 48));
        }
    }

    @Override
    public Codec<? extends BiomeModifier> codec()
    {
        return CODEC;
    }

    private static void add(Builder builder, PlacedFeature feature)
    {
        add(builder, feature, GenerationStep.Decoration.UNDERGROUND_ORES);
    }

    private static void add(Builder builder, PlacedFeature feature, GenerationStep.Decoration step)
    {
        builder.getGenerationSettings().addFeature(step, Holder.direct(feature));
    }

    private static void addNexus(Builder builder)
    {
        ConfiguredFeature<NoneFeatureConfiguration, Feature<NoneFeatureConfiguration>> configured =
                new ConfiguredFeature<>(ETERNITY_NEXUS, NoneFeatureConfiguration.INSTANCE);
        PlacedFeature placed = new PlacedFeature(Holder.direct(configured), List.of(CountPlacement.of(1), BiomeFilter.biome()));
        add(builder, placed, GenerationStep.Decoration.SURFACE_STRUCTURES);
    }

    private static PlacedFeature ore(BlockState state, int veinSize, int count, int minY, int maxY)
    {
        OreConfiguration config = new OreConfiguration(List.of(
                OreConfiguration.target(new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES), state),
                OreConfiguration.target(new TagMatchTest(BlockTags.DEEPSLATE_ORE_REPLACEABLES), state)), veinSize);
        return create(Feature.ORE, config, count, minY, maxY);
    }

    private static <C extends net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration> PlacedFeature create(
            Feature<C> feature, C config, int count, int minY, int maxY)
    {
        ConfiguredFeature<C, Feature<C>> configured = new ConfiguredFeature<>(feature, config);
        return new PlacedFeature(Holder.direct(configured), List.of(
                CountPlacement.of(count), InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.aboveBottom(minY), VerticalAnchor.aboveBottom(maxY - 1)),
                BiomeFilter.biome()));
    }

    private static final class TutridiumVeinFeature extends Feature<NoneFeatureConfiguration>
    {
        private TutridiumVeinFeature() { super(NoneFeatureConfiguration.CODEC); }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context)
        {
            BlockPos origin = context.origin();
            int minX = origin.getX() & ~15;
            int minZ = origin.getZ() & ~15;
            return grow(context.level(), context.random(), origin, 0, minX, minX + 16, minZ, minZ + 16);
        }

        private boolean grow(WorldGenLevel level, RandomSource random, BlockPos pos, int depth,
                int minX, int maxX, int minZ, int maxZ)
        {
            if (pos.getX() < minX || pos.getX() >= maxX || pos.getZ() < minZ || pos.getZ() >= maxZ
                    || !level.getBlockState(pos).is(BlockTags.STONE_ORE_REPLACEABLES)) return false;
            level.setBlock(pos, depth == 0 || random.nextInt(4) == 0
                    ? ModBlocks.TUTRIDIUM_ORE.get().defaultBlockState()
                    : ModBlocks.TUTRIDIUM_STONE.get().defaultBlockState(), 2);
            for (Direction direction : Direction.values())
            {
                if (depth < random.nextFloat() * 1.5F)
                {
                    grow(level, random, pos.relative(direction), depth + 1, minX, maxX, minZ, maxZ);
                }
            }
            return true;
        }
    }

    private static final class OlivineVeinFeature extends Feature<NoneFeatureConfiguration>
    {
        private OlivineVeinFeature() { super(NoneFeatureConfiguration.CODEC); }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context)
        {
            RandomSource random = context.random();
            BlockPos origin = context.origin();
            int size = 1;
            float angle = random.nextFloat() * (float) Math.PI;
            double x0 = origin.getX() + 8 + Mth.sin(angle) * size / 8.0F;
            double x1 = origin.getX() + 8 - Mth.sin(angle) * size / 8.0F;
            double z0 = origin.getZ() + 8 + Mth.cos(angle) * size / 8.0F;
            double z1 = origin.getZ() + 8 - Mth.cos(angle) * size / 8.0F;
            double y0 = origin.getY() + random.nextInt(3) - 2;
            double y1 = origin.getY() + random.nextInt(3) - 2;
            for (int step = 0; step <= size; ++step)
            {
                double x = x0 + (x1 - x0) * step / size;
                double y = y0 + (y1 - y0) * step / size;
                double z = z0 + (z1 - z0) * step / size;
                double radius = random.nextDouble() * size / 16.0;
                double horizontal = (Mth.sin((float) (step * Math.PI / size)) + 1.0F) * radius + 1.0;
                double vertical = horizontal;
                for (int bx = Mth.floor(x - horizontal / 2); bx <= Mth.floor(x + horizontal / 2); ++bx)
                for (int by = Mth.floor(y - vertical / 2); by <= Mth.floor(y + vertical / 2); ++by)
                for (int bz = Mth.floor(z - horizontal / 2); bz <= Mth.floor(z + horizontal / 2); ++bz)
                {
                    double dx = (bx + 0.5 - x) / (horizontal / 2);
                    double dy = (by + 0.5 - y) / (vertical / 2);
                    double dz = (bz + 0.5 - z) / (horizontal / 2);
                    BlockPos pos = new BlockPos(bx, by, bz);
                    if (dx * dx + dy * dy + dz * dz < 1.0 && random.nextFloat() * 25.0F < 1.0F
                            && context.level().getBlockState(pos).is(BlockTags.STONE_ORE_REPLACEABLES))
                    {
                        context.level().setBlock(pos, ModBlocks.OLIVINE_ORE.get().defaultBlockState(), 2);
                    }
                }
            }
            return true;
        }
    }

    /** Recreates the original deterministic Eternity Nexus spacing and block layout. */
    private static final class NexusStructureFeature extends Feature<NoneFeatureConfiguration>
    {
        private static final int MIN_DISTANCE = 48;
        private static final int MAX_DISTANCE = 64;
        private static final long X_SEED_MULTIPLIER = 341873128712L;
        private static final long Z_SEED_MULTIPLIER = 132897987541L;

        private NexusStructureFeature()
        {
            super(NoneFeatureConfiguration.CODEC);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context)
        {
            WorldGenLevel level = context.level();
            BlockPos origin = context.origin();
            int centerX = (origin.getX() & ~15) + 8;
            int centerZ = (origin.getZ() & ~15) + 8;
            long worldSeed = level.getLevel().getSeed();

            if (!isNexusChunk(worldSeed, centerX, centerZ))
            {
                return false;
            }

            Random random = new Random(structureSeed(worldSeed, centerX, centerZ));
            int centerY = random.nextInt(32);
            new NexusBuilder(level, centerX, centerY, centerZ, random).generate();
            return true;
        }

        private static boolean isNexusChunk(long worldSeed, int centerX, int centerZ)
        {
            int gridX = Math.floorDiv(centerX, MAX_DISTANCE);
            int gridZ = Math.floorDiv(centerZ, MAX_DISTANCE);
            Random random = new Random(structureSeed(worldSeed, gridX, gridZ));
            int targetX = gridX * MAX_DISTANCE + random.nextInt(MAX_DISTANCE - MIN_DISTANCE);
            int targetZ = gridZ * MAX_DISTANCE + random.nextInt(MAX_DISTANCE - MIN_DISTANCE);
            return centerX == targetX && centerZ == targetZ;
        }

        private static long structureSeed(long worldSeed, int x, int z)
        {
            return (long) x * X_SEED_MULTIPLIER + (long) z * Z_SEED_MULTIPLIER + worldSeed + 235785655L;
        }
    }

    private static final class NexusBuilder
    {
        private final WorldGenLevel level;
        private final int centerX;
        private final int centerY;
        private final int centerZ;
        private final Random random;
        private boolean mirrorX;
        private boolean mirrorZ;

        private NexusBuilder(WorldGenLevel level, int x, int y, int z, Random random)
        {
            this.level = level;
            centerX = x;
            centerY = y;
            centerZ = z;
            this.random = random;
        }

        private void generate()
        {
            int height = 7 + random.nextInt(2);
            int xRadius = 7 + random.nextInt(3);
            int zRadius = 7 + random.nextInt(3);
            int minX = -xRadius - 1;
            int minZ = -zRadius - 1;
            int maxX = xRadius + 1;
            int maxZ = zRadius + 1;
            int wall = 5;

            for (int x = minX - wall; x <= maxX + wall; ++x)
            for (int y = -1 - wall; y <= height + wall; ++y)
            for (int z = minZ - wall; z <= maxZ + wall; ++z)
            {
                boolean outside = x < minX || y < -1 || z < minZ || x > maxX || y > height || z > maxZ;
                if (x == minX || y == -1 || z == minZ || x == maxX || y == height || z == maxZ)
                {
                    if (!outside) setBlock(x, y, z, ModBlocks.ETERNIUM_STONE.get(), 0);
                }
                else if (outside)
                {
                    int distance = Math.max(Math.max(Math.max(minX - x, 0), -1 - y), Math.max(minZ - z, 0));
                    distance = Math.max(Math.max(distance, x - maxX), Math.max(y - height, z - maxZ));
                    if (distance <= random.nextInt(wall)) setBlock(x, y, z, ModBlocks.ETERNIUM_STONE.get(), 0);
                }
                else
                {
                    setBlock(x, y, z, Blocks.AIR, 0);
                }
            }

            for (int i = 0; i <= height - 7; ++i)
            {
                setBlock(0, 3 + i, 0, ModBlocks.SUPERCHARGED_ETERNIUM.get(), 0);
            }
            for (int x = minX + 1; x < maxX; ++x)
            for (int z = minZ + 1; z < maxZ; ++z)
            {
                setBlock(x, 0, z, ModBlocks.NEXUS_SOIL.get(), 0);
                setBlock(x, height - 1, z, ModBlocks.NEXUS_BRICK_SLAB.get(), 8);
                if (x != 0 && z != 0) continue;
                setBlock(x, 1, z, ModBlocks.NEXUS_BRICKS.get(), 0);
                setBlock(x, height - 1, z, ModBlocks.NEXUS_BRICKS.get(), 0);
                if (x == 0)
                {
                    mirror(true, false);
                    setBlock(x + 1, 1, z, ModBlocks.NEXUS_BRICK_STAIRS.get(), 1);
                }
                else
                {
                    mirror(false, true);
                    setBlock(x, 1, z + 1, ModBlocks.NEXUS_BRICK_STAIRS.get(), 3);
                }
                mirror(false, false);
            }

            for (int i = 0; i < 5; ++i)
            for (int j = 0; j < 5; ++j)
            {
                setBlock(i - 2, 1, j - 2, ModBlocks.NEXUS_BRICKS.get(), 0);
                setBlock(i - 2, height - 1, j - 2, ModBlocks.NEXUS_BRICKS.get(), 0);
            }

            setBlock(0, 2, 0, ModBlocks.NEXUS_BRICKS.get(), 0);
            mirror(true, true);
            setBlock(1, 2, 0, ModBlocks.NEXUS_BRICKS.get(), 0);
            setBlock(1, 2, 1, ModBlocks.NEXUS_BRICK_SLAB.get(), 0);
            setBlock(2, 2, 0, ModBlocks.NEXUS_BRICK_STAIRS.get(), 1);
            setBlock(4, 1, 4, ModBlocks.NEXUS_BRICK_SLAB.get(), 0);
            setBlock(4, 1, 3, ModBlocks.NEXUS_BRICK_STAIRS.get(), 0);
            setBlock(3, 1, 4, ModBlocks.NEXUS_BRICK_STAIRS.get(), 2);
            setBlock(6, 1, 3, ModBlocks.NEXUS_BRICK_STAIRS.get(), 1);
            setBlock(3, 1, 6, ModBlocks.NEXUS_BRICK_STAIRS.get(), 3);
            for (int i = 1; i < height; ++i)
            {
                setBlock(5, i, 3, ModBlocks.NEXUS_BRICKS.get(), 0);
                setBlock(3, i, 5, ModBlocks.NEXUS_BRICKS.get(), 0);
            }
            mirror(true, false);
            for (int i = 1; i < height; ++i)
            {
                setBlock(xRadius, height - i, zRadius, Blocks.LAVA, 0);
                setBlock(xRadius, height - i, -zRadius, Blocks.LAVA, 0);
            }
            for (int i = 0; i <= 4; i += 4)
            {
                int y = i > 0 ? height - 1 : 1;
                setBlock(xRadius - 1, y, zRadius, ModBlocks.NEXUS_BRICK_STAIRS.get(), i);
                setBlock(xRadius, y, zRadius - 1, ModBlocks.NEXUS_BRICK_STAIRS.get(), i + 2);
                setBlock(xRadius - 1, y, -zRadius, ModBlocks.NEXUS_BRICK_STAIRS.get(), i);
                setBlock(xRadius, y, -zRadius + 1, ModBlocks.NEXUS_BRICK_STAIRS.get(), i + 3);
            }
        }

        private void mirror(boolean x, boolean z)
        {
            mirrorX = x;
            mirrorZ = z;
        }

        private void setBlock(int x, int y, int z, net.minecraft.world.level.block.Block block, int metadata)
        {
            if (mirrorX && mirrorZ)
            {
                if (x != 0 || z != 0)
                {
                    place(x + z, y, -x, block, mirrorX(block, rotate(block, metadata)));
                    place(-z, y, x, block, mirrorZ(block, rotate(block, metadata)));
                    place(-x, y, -z, block, mirrorXZ(block, metadata));
                }
            }
            else
            {
                if (mirrorX && x != 0) place(-x, y, z, block, mirrorX(block, metadata));
                if (mirrorZ && z != 0) place(x, y, -z, block, mirrorZ(block, metadata));
            }
            place(x, y, z, block, metadata);
        }

        private void place(int x, int y, int z, net.minecraft.world.level.block.Block block, int metadata)
        {
            if (block == ModBlocks.NEXUS_BRICKS.get() && metadata == 0 && random.nextFloat() < 0.15F)
            {
                if (random.nextFloat() < 0.25F)
                {
                    block = ModBlocks.NEXUS_BRICK_SLAB.get();
                    metadata = random.nextBoolean() ? 0 : 8;
                }
                else
                {
                    block = ModBlocks.NEXUS_BRICK_STAIRS.get();
                    metadata = random.nextInt(8);
                }
            }
            else if (block == ModBlocks.NEXUS_BRICK_STAIRS.get() && random.nextFloat() < 0.15F)
            {
                block = ModBlocks.NEXUS_BRICK_SLAB.get();
                metadata = (metadata & 4) == 4 ? 8 : 0;
            }

            BlockPos pos = new BlockPos(centerX + x, centerY + y, centerZ + z);
            BlockState current = level.getBlockState(pos);
            BlockState desired = toBlockState(block, metadata);
            if (!current.equals(desired) && current.getDestroySpeed(level, pos) != -1.0F)
            {
                level.setBlock(pos, desired, 2);
            }
        }

        private static BlockState toBlockState(net.minecraft.world.level.block.Block block, int metadata)
        {
            BlockState state = block.defaultBlockState();
            if (block instanceof StairBlock)
            {
                net.minecraft.core.Direction facing = switch (metadata & 3)
                {
                    case 0 -> net.minecraft.core.Direction.EAST;
                    case 1 -> net.minecraft.core.Direction.WEST;
                    case 2 -> net.minecraft.core.Direction.SOUTH;
                    default -> net.minecraft.core.Direction.NORTH;
                };
                state = state.setValue(StairBlock.FACING, facing)
                        .setValue(StairBlock.HALF, (metadata & 4) != 0 ? Half.TOP : Half.BOTTOM);
            }
            else if (block instanceof SlabBlock)
            {
                state = state.setValue(SlabBlock.TYPE, (metadata & 8) != 0 ? SlabType.TOP : SlabType.BOTTOM);
            }
            return state;
        }

        private static int rotate(net.minecraft.world.level.block.Block block, int metadata)
        {
            if (!(block instanceof StairBlock)) return metadata;
            return switch (metadata & 7)
            {
                case 0 -> 3; case 1 -> 2; case 2 -> 1; case 3 -> 0;
                case 4 -> 7; case 5 -> 6; case 6 -> 5; case 7 -> 4;
                default -> metadata;
            };
        }

        private static int mirrorX(net.minecraft.world.level.block.Block block, int metadata)
        {
            if (!(block instanceof StairBlock)) return metadata;
            return switch (metadata & 7)
            {
                case 0 -> 1; case 1 -> 0; case 4 -> 5; case 5 -> 4;
                default -> metadata;
            };
        }

        private static int mirrorZ(net.minecraft.world.level.block.Block block, int metadata)
        {
            if (!(block instanceof StairBlock)) return metadata;
            return switch (metadata & 7)
            {
                case 2 -> 3; case 3 -> 2; case 6 -> 7; case 7 -> 6;
                default -> metadata;
            };
        }

        private static int mirrorXZ(net.minecraft.world.level.block.Block block, int metadata)
        {
            if (!(block instanceof StairBlock)) return metadata;
            return switch (metadata & 7)
            {
                case 0 -> 1; case 1 -> 0; case 2 -> 3; case 3 -> 2;
                case 4 -> 5; case 5 -> 4; case 6 -> 7; case 7 -> 6;
                default -> metadata;
            };
        }
    }
}
