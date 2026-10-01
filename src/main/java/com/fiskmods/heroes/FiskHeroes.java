package com.fiskmods.heroes;

import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(FiskHeroes.MODID)
public class FiskHeroes
{
    public static final String MODID = "fiskheroes";
    public static final String VERSION = "2.4.0";
    public static final String NAME = "Fisk's Superheroes";

    public static final Logger LOGGER = LogManager.getLogger(NAME);

    public FiskHeroes()
    {
    }

    public static String namespace(String key)
    {
        return key != null && key.indexOf(':') == -1 ? MODID + ":" + key : key;
    }
}
