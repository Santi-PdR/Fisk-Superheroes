package com.fiskmods.heroes.common.world;

import java.util.List;

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

    private OreGenerationBiomeModifier() {}

    @Override
    public void modify(Holder<Biome> biome, Phase phase, Builder builder)
    {
        if (phase != Phase.ADD) return;
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
        builder.getGenerationSettings().addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, Holder.direct(feature));
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
}
