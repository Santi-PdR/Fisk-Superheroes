package com.fiskmods.heroes.common.block;

import java.util.List;

import com.fiskmods.heroes.FiskHeroes;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Registered solid mineral blocks ported from the original block registry. */
public final class ModBlocks
{
    public static final DeferredRegister<Block> REGISTRY = DeferredRegister.create(ForgeRegistries.BLOCKS, FiskHeroes.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, FiskHeroes.MODID);

    public static final RegistryObject<Block> TUTRIDIUM_STONE = register("tutridium_stone", 1.5F, 10.0F, false, 0);
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
    public static final RegistryObject<Block> ETERNIUM_STONE = register("eternium_stone", 5.0F, 3000.0F, false, 0);

    public static final List<RegistryObject<Block>> ALL = List.of(TUTRIDIUM_STONE, TUTRIDIUM_BLOCK,
            CRYSTALLINE_TUTRITE_BLOCK, IRIDESCENT_GOLD_BLOCK, VIBRANIUM_BLOCK, TITANIUM_BLOCK,
            GOLD_TITANIUM_BLOCK, DWARF_STAR_BLOCK, OLIVINE_BLOCK, PACKED_OLIVINE, ETERNIUM_BLOCK, ETERNIUM_STONE);

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

    public static CreativeModeTab createBlockTab()
    {
        return CreativeModeTab.builder()
                .title(net.minecraft.network.chat.Component.translatable("itemGroup.fiskheroes.blocks"))
                .icon(() -> new net.minecraft.world.item.ItemStack(TUTRIDIUM_BLOCK.get()))
                .displayItems((params, output) -> ALL.forEach(block -> output.accept(block.get())))
                .build();
    }
}
