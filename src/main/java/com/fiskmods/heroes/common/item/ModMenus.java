package com.fiskmods.heroes.common.item;

import com.fiskmods.heroes.FiskHeroes;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus
{
    public static final DeferredRegister<MenuType<?>> REGISTRY = DeferredRegister.create(ForgeRegistries.MENU_TYPES, FiskHeroes.MODID);
    public static final RegistryObject<MenuType<QuiverMenu>> QUIVER = REGISTRY.register("quiver", () -> IForgeMenuType.create(QuiverMenu::new));

    private ModMenus()
    {
    }
}
