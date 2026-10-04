package com.fiskmods.heroes.common.item;

import java.util.function.Supplier;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.hero.ItemHeroArmor;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Every item registered by the mod. The suit pieces are a single item per armour slot: the hero
 * they belong to lives in the item NBT.
 */
public class ModItems
{
    public static final DeferredRegister<Item> REGISTRY = DeferredRegister.create(ForgeRegistries.ITEMS, FiskHeroes.MODID);

    public static final ArmorMaterial MATERIAL_SUPERHERO = SuperheroArmorMaterial.INSTANCE;

    /* --- Suit pieces --- */
    public static final RegistryObject<Item> HELMET = REGISTRY.register("superhero_helmet",
            () -> new ItemHeroArmor(MATERIAL_SUPERHERO, ArmorItem.Type.HELMET, 0, new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> CHESTPLATE = REGISTRY.register("superhero_chestplate",
            () -> new ItemHeroArmor(MATERIAL_SUPERHERO, ArmorItem.Type.CHESTPLATE, 1, new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> LEGGINGS = REGISTRY.register("superhero_leggings",
            () -> new ItemHeroArmor(MATERIAL_SUPERHERO, ArmorItem.Type.LEGGINGS, 2, new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> BOOTS = REGISTRY.register("superhero_boots",
            () -> new ItemHeroArmor(MATERIAL_SUPERHERO, ArmorItem.Type.BOOTS, 3, new Item.Properties().stacksTo(1)));

    public static ItemHeroArmor[] heroArmor;

    /* --- Crafting materials --- */
    public static final RegistryObject<Item> SUIT_CORE = REGISTRY.register("suit_core", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> SUIT_UPGRADE = REGISTRY.register("suit_upgrade", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> SUIT_DRIVE = REGISTRY.register("suit_drive", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> METAHUMAN_LOG = REGISTRY.register("metahuman_log", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> TUTRIDIUM_GEM = REGISTRY.register("tutridium_gem", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> RAW_VIBRANIUM = REGISTRY.register("raw_vibranium", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> VIBRANIUM_INGOT = REGISTRY.register("vibranium_ingot", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> VIBRANIUM_DISC = REGISTRY.register("vibranium_disc", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> TITANIUM_INGOT = REGISTRY.register("titanium_ingot", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> TITANIUM_NUGGET = REGISTRY.register("titanium_nugget", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> GOLD_TITANIUM_INGOT = REGISTRY.register("gold_titanium_ingot", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> GOLD_TITANIUM_NUGGET = REGISTRY.register("gold_titanium_nugget", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> RAW_DWARF_STAR = REGISTRY.register("raw_dwarf_star", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> DWARF_STAR_ALLOY = REGISTRY.register("dwarf_star_alloy", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> ETERNIUM_SHARD = REGISTRY.register("eternium_shard", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> RAW_TUTRITE = REGISTRY.register("raw_tutrite", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> CRYSTALLINE_TUTRITE = REGISTRY.register("crystalline_tutrite", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> OLIVINE = REGISTRY.register("olivine", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> OLIVINE_DUST = REGISTRY.register("olivine_dust", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> RADIANT_OLIVINE_DUST = REGISTRY.register("radiant_olivine_dust", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> GUN_BASE = REGISTRY.register("gun_base", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> RIFLE_BASE = REGISTRY.register("rifle_base", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> DESERT_EAGLE = REGISTRY.register("desert_eagle",
            () -> new ItemGun(7, 8, 40, 32.0D, 8.0F, new Item.Properties()));
    public static final RegistryObject<Item> BERETTA_93R = REGISTRY.register("beretta_93r",
            () -> new ItemGun(15, 4, 32, 28.0D, 3.5F, new Item.Properties()));
    public static final RegistryObject<Item> SWORD_BLADE = REGISTRY.register("sword_blade", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> HANDLE = REGISTRY.register("handle", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> IRIDESCENT_GOLD = REGISTRY.register("iridescent_gold", () -> new Item(new Item.Properties()));

    /* --- Equipment --- */
    public static final RegistryObject<Item> FLASH_RING = REGISTRY.register("flash_ring", () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final RegistryObject<Item> MINI_SUIT = REGISTRY.register("mini_suit", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> KATANA = REGISTRY.register("katana", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> CHOKUTO = REGISTRY.register("chokuto", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> TACTICAL_TONFA = REGISTRY.register("tactical_tonfa", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> GRAPPLING_GUN = REGISTRY.register("grappling_gun", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> CAPTAIN_AMERICAS_SHIELD = REGISTRY.register("captain_americas_shield", () -> new ItemCaptainAmericaShield(new Item.Properties()));
    public static final RegistryObject<Item> QUIVER = REGISTRY.register("quiver", () -> new ItemQuiver(new Item.Properties()));
    public static final RegistryObject<Item> COMPOUND_BOW = REGISTRY.register("compound_bow", () -> new ItemCompoundBow(new Item.Properties()));
    public static final RegistryObject<Item> TRICK_ARROW = REGISTRY.register("trick_arrow", () -> new ItemTrickArrow(new Item.Properties()));

    /* Non-craftable visual items carried by pack-defined belt gadgets. */
    public static final RegistryObject<Item> BATARANG = registerGadget("batarang");
    public static final RegistryObject<Item> FREEZE_GRENADE = registerGadget("freeze_grenade");
    public static final RegistryObject<Item> SMOKE_PELLET = registerGadget("smoke_pellet");
    public static final RegistryObject<Item> THROWING_STAR = registerGadget("throwing_star");
    public static final RegistryObject<Item> GRENADE = registerGadget("grenade");
    public static final RegistryObject<Item> STICKY_WEB = registerGadget("sticky_web");
    public static final RegistryObject<Item> IMPACT_WEB = registerGadget("impact_web");
    public static final RegistryObject<Item> RAPID_WEBS = registerGadget("rapid_webs");
    public static final RegistryObject<Item> RICOCHET_WEB = registerGadget("ricochet_web");

    private static RegistryObject<Item> registerGadget(String id)
    {
        return REGISTRY.register(id, () -> new Item(new Item.Properties().stacksTo(1)));
    }

    public static ItemStack equipmentGadget(String path)
    {
        return switch (path)
        {
            case "batarang" -> new ItemStack(BATARANG.get());
            case "freeze_grenade" -> new ItemStack(FREEZE_GRENADE.get());
            case "smoke_pellet" -> new ItemStack(SMOKE_PELLET.get());
            case "throwing_star" -> new ItemStack(THROWING_STAR.get());
            case "grenade" -> new ItemStack(GRENADE.get());
            case "sticky_web" -> new ItemStack(STICKY_WEB.get());
            case "impact_web" -> new ItemStack(IMPACT_WEB.get());
            case "rapid_webs" -> new ItemStack(RAPID_WEBS.get());
            case "ricochet_web" -> new ItemStack(RICOCHET_WEB.get());
            default -> ItemStack.EMPTY;
        };
    }

    private static <T extends Item> RegistryObject<T> register(String name, Supplier<T> supplier)
    {
        return REGISTRY.register(name, supplier);
    }

    /** Called once the registry is frozen: builds the slot-indexed armour lookup. */
    public static void resolve()
    {
        heroArmor = new ItemHeroArmor[] {
                (ItemHeroArmor) HELMET.get(),
                (ItemHeroArmor) CHESTPLATE.get(),
                (ItemHeroArmor) LEGGINGS.get(),
                (ItemHeroArmor) BOOTS.get()
        };
    }

    public static CreativeModeTab createSuitTab()
    {
        return CreativeModeTab.builder()
                .title(net.minecraft.network.chat.Component.translatable("itemGroup.fiskheroes.suits"))
                .icon(() -> new net.minecraft.world.item.ItemStack(HELMET.get()))
                .displayItems((params, output) ->
                {
                    for (com.fiskmods.heroes.common.hero.Hero hero : com.fiskmods.heroes.common.hero.Hero.REGISTRY.getSortedHeroes())
                    {
                        for (com.fiskmods.heroes.common.hero.HeroIteration iteration : hero.getIterations().values())
                        {
                            for (ItemHeroArmor item : heroArmor)
                            {
                                if (iteration.getArmorType(item.getSlot()) != null)
                                {
                                    output.accept(ItemHeroArmor.create(iteration, item));
                                }
                            }
                        }
                    }
                })
                .build();
    }

    public static CreativeModeTab createItemTab()
    {
        return CreativeModeTab.builder()
                .title(net.minecraft.network.chat.Component.translatable("itemGroup.fiskheroes.items"))
                .icon(() -> new net.minecraft.world.item.ItemStack(SUIT_CORE.get()))
                .displayItems((params, output) ->
                {
                    output.accept(SUIT_CORE.get());
                    output.accept(SUIT_UPGRADE.get());
                    output.accept(SUIT_DRIVE.get());
                    output.accept(METAHUMAN_LOG.get());
                    output.accept(TUTRIDIUM_GEM.get());
                    output.accept(RAW_VIBRANIUM.get());
                    output.accept(VIBRANIUM_INGOT.get());
                    output.accept(VIBRANIUM_DISC.get());
                    output.accept(TITANIUM_INGOT.get());
                    output.accept(TITANIUM_NUGGET.get());
                    output.accept(GOLD_TITANIUM_INGOT.get());
                    output.accept(GOLD_TITANIUM_NUGGET.get());
                    output.accept(RAW_DWARF_STAR.get());
                    output.accept(DWARF_STAR_ALLOY.get());
                    output.accept(ETERNIUM_SHARD.get());
                    output.accept(RAW_TUTRITE.get());
                    output.accept(CRYSTALLINE_TUTRITE.get());
                    output.accept(OLIVINE.get());
                    output.accept(OLIVINE_DUST.get());
                    output.accept(RADIANT_OLIVINE_DUST.get());
                    output.accept(GUN_BASE.get());
                    output.accept(RIFLE_BASE.get());
                    output.accept(SWORD_BLADE.get());
                    output.accept(HANDLE.get());
                    output.accept(IRIDESCENT_GOLD.get());
                })
                .build();
    }

    public static CreativeModeTab createEquipmentTab()
    {
        return CreativeModeTab.builder()
                .title(net.minecraft.network.chat.Component.translatable("itemGroup.fiskheroes.equipment"))
                .icon(() -> new net.minecraft.world.item.ItemStack(GRAPPLING_GUN.get()))
                .displayItems((params, output) ->
                {
                    output.accept(FLASH_RING.get());
                    output.accept(MINI_SUIT.get());
                    output.accept(KATANA.get());
                    output.accept(CHOKUTO.get());
                    output.accept(TACTICAL_TONFA.get());
                    output.accept(GRAPPLING_GUN.get());
                    output.accept(DESERT_EAGLE.get());
                    output.accept(BERETTA_93R.get());
                    output.accept(CAPTAIN_AMERICAS_SHIELD.get());
                    output.accept(QUIVER.get());
                    output.accept(COMPOUND_BOW.get());
                    for (String type : ItemTrickArrow.TYPES)
                    {
                        output.accept(ItemTrickArrow.createStack(type));
                    }
                })
                .build();
    }
}
