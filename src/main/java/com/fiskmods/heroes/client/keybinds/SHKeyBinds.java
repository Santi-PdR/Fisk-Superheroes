package com.fiskmods.heroes.client.keybinds;

import org.lwjgl.glfw.GLFW;

import com.fiskmods.heroes.FiskHeroes;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;

/**
 * The mod's key mappings. The original registered a mask key, an "equip item" key and five suit
 * ability keys; the same seven keys are used here. Hero packs bind their abilities to the ability
 * keys by index.
 */
public class SHKeyBinds
{
    public static final String CATEGORY = "key.categories.fiskheroes";
    public static final int ABILITY_COUNT = 5;

    public static final KeyMapping MASK = new KeyMapping("key.openMask", KeyConflictContext.IN_GAME, KeyModifier.NONE, InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_X), CATEGORY);
    public static final KeyMapping WEAPON = new KeyMapping("key.equipItem", KeyConflictContext.IN_GAME, KeyModifier.NONE, InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_R), CATEGORY);

    public static final KeyMapping[] ABILITIES = new KeyMapping[ABILITY_COUNT];

    static
    {
        int[] defaults = { GLFW.GLFW_KEY_C, GLFW.GLFW_KEY_V, GLFW.GLFW_KEY_B, GLFW.GLFW_KEY_G, GLFW.GLFW_KEY_H };

        for (int i = 0; i < ABILITY_COUNT; ++i)
        {
            ABILITIES[i] = new KeyMapping("key.suitAbility" + (i + 1), KeyConflictContext.IN_GAME, KeyModifier.NONE,
                    InputConstants.Type.KEYSYM.getOrCreate(defaults[i]), CATEGORY);
        }
    }

    public static KeyMapping[] all()
    {
        KeyMapping[] mappings = new KeyMapping[2 + ABILITY_COUNT];
        mappings[0] = MASK;
        mappings[1] = WEAPON;
        System.arraycopy(ABILITIES, 0, mappings, 2, ABILITY_COUNT);
        return mappings;
    }

    /** The key mapping of the given ability index (1..5), or the mask key for index 0. */
    public static KeyMapping get(int index)
    {
        if (index == 0)
        {
            return MASK;
        }

        if (index == 1)
        {
            return WEAPON;
        }

        int ability = index - 2;

        if (ability >= 0 && ability < ABILITY_COUNT)
        {
            return ABILITIES[ability];
        }

        return null;
    }

    public static void register(net.minecraftforge.client.event.RegisterKeyMappingsEvent event)
    {
        for (KeyMapping mapping : all())
        {
            event.register(mapping);
        }
    }

    public static String describe(int abilityIndex)
    {
        KeyMapping mapping = ABILITIES[Math.max(0, Math.min(ABILITY_COUNT - 1, abilityIndex))];
        return mapping.getTranslatedKeyMessage().getString();
    }

    static
    {
        FiskHeroes.LOGGER.debug("Fisk's Superheroes key mappings registered");
    }
}
