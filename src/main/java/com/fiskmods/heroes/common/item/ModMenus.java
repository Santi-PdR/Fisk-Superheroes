package com.fiskmods.heroes.common.item;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.block.AlloySmelteryMenu;
import com.fiskmods.heroes.common.block.SuitFabricatorMenu;
import com.fiskmods.heroes.common.block.SuitIteratorMenu;
import com.fiskmods.heroes.common.block.SuitAugmentorMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus
{
    public static final DeferredRegister<MenuType<?>> REGISTRY = DeferredRegister.create(ForgeRegistries.MENU_TYPES, FiskHeroes.MODID);
    public static final RegistryObject<MenuType<QuiverMenu>> QUIVER =
            REGISTRY.register("quiver", () -> IForgeMenuType.create(QuiverMenu::new));
    public static final RegistryObject<MenuType<AlloySmelteryMenu>> ALLOY_SMELTERY =
            REGISTRY.register("alloy_smeltery", () -> IForgeMenuType.create(AlloySmelteryMenu::new));
    public static final RegistryObject<MenuType<SuitIteratorMenu>> SUIT_ITERATOR =
            REGISTRY.register("suit_iterator", () -> IForgeMenuType.create(SuitIteratorMenu::new));
    public static final RegistryObject<MenuType<SuitAugmentorMenu>> SUIT_AUGMENTOR =
            REGISTRY.register("suit_augmentor", () -> IForgeMenuType.create(SuitAugmentorMenu::new));
    public static final RegistryObject<MenuType<SuitFabricatorMenu>> SUIT_FABRICATOR =
            REGISTRY.register("suit_fabricator", () -> IForgeMenuType.create(SuitFabricatorMenu::new));

    private ModMenus()
    {
    }
}
