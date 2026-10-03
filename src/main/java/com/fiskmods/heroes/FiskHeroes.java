package com.fiskmods.heroes;

import com.fiskmods.heroes.common.command.CommandHero;
import com.fiskmods.heroes.common.command.CommandSuit;
import com.fiskmods.heroes.common.config.SHConfig;
import com.fiskmods.heroes.common.data.DataRegistry;
import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.hero.Hero;
import com.fiskmods.heroes.common.hero.attribute.SHAttributes;
import com.fiskmods.heroes.common.item.ModItems;
import com.fiskmods.heroes.common.item.ModMenus;
import com.fiskmods.heroes.common.network.PacketAbility;
import com.fiskmods.heroes.common.network.PacketSyncData;
import com.fiskmods.heroes.common.network.PacketSyncSuit;
import com.fiskmods.heroes.common.network.SHNetwork;
import com.fiskmods.heroes.common.hero.modifier.Modifiers;
import com.fiskmods.heroes.pack.HeroPackEngine;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Fisk's Superheroes for Minecraft 1.20.1 (Forge).
 * <p>
 * This is a functional port of the original 1.7.10 mod: hero packs (JavaScript + JSON) drive the
 * suits, attributes, powers and abilities, all of which are reimplemented on top of the modern
 * Forge APIs.
 */
@Mod(FiskHeroes.MODID)
public class FiskHeroes
{
    public static final String MODID = "fiskheroes";
    public static final String VERSION = "2.4.0";
    public static final String NAME = "Fisk's Superheroes";

    public static final Logger LOGGER = LogManager.getLogger(NAME);

    public static final DeferredRegister<net.minecraft.world.item.CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB, MODID);

    public static final RegistryObject<net.minecraft.world.item.CreativeModeTab> TAB_SUITS = CREATIVE_TABS.register("suits", ModItems::createSuitTab);
    public static final RegistryObject<net.minecraft.world.item.CreativeModeTab> TAB_ITEMS = CREATIVE_TABS.register("items", ModItems::createItemTab);
    public static final RegistryObject<net.minecraft.world.item.CreativeModeTab> TAB_EQUIPMENT = CREATIVE_TABS.register("equipment", ModItems::createEquipmentTab);

    public FiskHeroes(FMLJavaModLoadingContext context)
    {
        IEventBus modBus = context.getModEventBus();

        ModItems.REGISTRY.register(modBus);
        ModMenus.REGISTRY.register(modBus);
        SHAttributes.REGISTRY.register(modBus);
        CREATIVE_TABS.register(modBus);

        modBus.addListener(this::commonSetup);
        modBus.addListener(SHAttributes::onAttributeModification);

        MinecraftForge.EVENT_BUS.register(com.fiskmods.heroes.common.event.CommonEventHandler.class);
        MinecraftForge.EVENT_BUS.register(SHDataCapabilities.Events.class);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SHConfig.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event)
    {
        event.enqueueWork(() ->
        {
            ModItems.resolve();
            Modifiers.registerAll();

            int packets = 0;
            SHNetwork.registerPacket(PacketSyncData.class, PacketSyncData::new, NetworkDirection.PLAY_TO_CLIENT);
            SHNetwork.registerPacket(PacketSyncSuit.class, PacketSyncSuit::new, NetworkDirection.PLAY_TO_CLIENT);
            SHNetwork.registerPacket(PacketAbility.class, PacketAbility::new, NetworkDirection.PLAY_TO_SERVER);
            SHNetwork.registerPacket(com.fiskmods.heroes.common.network.PacketEquipment.class,
                    com.fiskmods.heroes.common.network.PacketEquipment::new, NetworkDirection.PLAY_TO_SERVER);
            SHNetwork.registerPacket(com.fiskmods.heroes.common.network.PacketInput.class,
                    com.fiskmods.heroes.common.network.PacketInput::new, NetworkDirection.PLAY_TO_SERVER);
            SHNetwork.registerPacket(com.fiskmods.heroes.common.network.PacketPlaySound.class,
                    com.fiskmods.heroes.common.network.PacketPlaySound::new, NetworkDirection.PLAY_TO_CLIENT);
            SHNetwork.registerPacket(com.fiskmods.heroes.common.network.PacketSelectArrow.class,
                    com.fiskmods.heroes.common.network.PacketSelectArrow::new, NetworkDirection.PLAY_TO_SERVER);
            SHNetwork.registerPacket(com.fiskmods.heroes.common.network.PacketStopSound.class,
                    com.fiskmods.heroes.common.network.PacketStopSound::new, NetworkDirection.PLAY_TO_CLIENT);

            HeroPackEngine.INSTANCE.setup();
            LOGGER.info("Fisk's Superheroes loaded: {} heroes, {} powers, {} data variables",
                    Hero.REGISTRY.size(), com.fiskmods.heroes.common.hero.power.Power.REGISTRY.size(), DataRegistry.INSTANCE.size());
        });
    }

    public static String namespace(String key)
    {
        return key != null && key.indexOf(':') == -1 ? MODID + ":" + key : key;
    }

    /** Builds a location, leaving namespaced keys alone (pack scripts pass both forms). */
    public static ResourceLocation id(String path)
    {
        return path.indexOf(':') == -1 ? new ResourceLocation(MODID, path) : new ResourceLocation(path);
    }
}
