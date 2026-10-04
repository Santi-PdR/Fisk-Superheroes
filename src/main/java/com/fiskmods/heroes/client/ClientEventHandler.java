package com.fiskmods.heroes.client;

import com.fiskmods.heroes.client.hud.SuitHud;
import com.fiskmods.heroes.client.keybinds.SHKeyBinds;
import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.data.var.Vars;
import com.fiskmods.heroes.common.hero.ability.AbilityHandler;
import com.fiskmods.heroes.common.hero.HeroTracker;
import com.fiskmods.heroes.common.network.PacketAbility;
import com.fiskmods.heroes.common.network.PacketCastSpell;
import com.fiskmods.heroes.common.network.PacketSelectArrow;
import com.fiskmods.heroes.common.network.SHNetwork;
import com.fiskmods.heroes.common.item.ItemQuiver;
import com.fiskmods.heroes.common.item.ModItems;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-side input handling. Ability keys are reported to the server, which validates them against
 * the worn hero; the client never applies an ability on its own.
 */
@Mod.EventBusSubscriber(modid = com.fiskmods.heroes.FiskHeroes.MODID, value = Dist.CLIENT)
public class ClientEventHandler
{
    /** Ability indices are 1-based to match the pack scripts; 0 is the mask toggle. */
    private static final int[] ABILITY_INDICES = { 1, 2, 3, 4, 5 };
    private static final boolean[] abilityKeysDown = new boolean[SHKeyBinds.ABILITY_COUNT];
    private static boolean weaponKeyHeld;
    private static boolean maskKeyDown;
    private static boolean attackKeyDown;
    private static final boolean[] spellDirectionDown = new boolean[4];
    private static final StringBuilder spellSequence = new StringBuilder();
    private static boolean spellMenuWasDown;

    /** Prevent a hold-to-open weapon wheel from reopening after it is dismissed. */
    public static void onEquipmentWheelClosed()
    {
        weaponKeyHeld = false;
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event)
    {
        Minecraft mc = Minecraft.getInstance();
        if (SHKeyBinds.WEAPON.matches(event.getKey(), event.getScanCode()))
        {
            if (event.getAction() == org.lwjgl.glfw.GLFW.GLFW_PRESS
                    && (mc.screen == null || mc.screen instanceof com.fiskmods.heroes.client.gui.EquipmentWheelScreen)) weaponKeyHeld = true;
            else if (event.getAction() == org.lwjgl.glfw.GLFW.GLFW_RELEASE) weaponKeyHeld = false;
        }

        LocalPlayer player = mc.player;

        if (player != null && (event.getAction() == org.lwjgl.glfw.GLFW.GLFW_PRESS
                || event.getAction() == org.lwjgl.glfw.GLFW.GLFW_RELEASE))
        {
            for (int i = 0; i < SHKeyBinds.ABILITY_COUNT; ++i)
            {
                if (SHKeyBinds.ABILITIES[i].matches(event.getKey(), event.getScanCode()))
                {
                    boolean pressed = event.getAction() == org.lwjgl.glfw.GLFW.GLFW_PRESS && mc.screen == null;
                    setAbilityKeyState(player, i, pressed);
                }
            }
        }

        if (player == null || mc.screen != null)
        {
            return;
        }

    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event)
    {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.screen != null) return;

        com.fiskmods.heroes.common.hero.Hero hero = HeroTracker.getHeroType(player);
        SHPlayerData data = SHDataCapabilities.getPlayer(player);
        if (data == null) return;

        if (hero != null && hero.isKeyPressed(player, "GRAVITY_MANIPULATION"))
        {
            int direction = event.getScrollDelta() > 0.0D ? 1 : -1;
            SHNetwork.sendToServer(new com.fiskmods.heroes.common.network.PacketGravityAmount(direction));
            event.setCanceled(true);
            return;
        }

        if (hero != null && hero.isKeyPressed(player, "UTILITY_BELT")
                && com.fiskmods.heroes.common.hero.equipment.EquipmentHelper.getUtilityBelt(hero, player, data) != null)
        {
            int direction = event.getScrollDelta() > 0.0D ? 1 : -1;
            SHNetwork.sendToServer(new com.fiskmods.heroes.common.network.PacketCycleUtilityBelt(direction));
            event.setCanceled(true);
            return;
        }

        if (!player.getMainHandItem().is(ModItems.COMPOUND_BOW.get())
                || ItemQuiver.findQuiver(player).isEmpty()
                || !player.isShiftKeyDown() && (hero == null || !hero.isKeyPressed(player, "QUIVER_CYCLE"))) return;

        int current = Byte.toUnsignedInt(data.getData().get(Vars.SELECTED_ARROW)) % 5;
        int step = event.getScrollDelta() > 0.0D ? -1 : 1;
        byte selected = (byte) Math.floorMod(current + step, 5);
        data.getData().set(Vars.SELECTED_ARROW, selected);
        SHNetwork.sendToServer(new PacketSelectArrow(selected));
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END)
        {
            return;
        }

        LocalPlayer player = Minecraft.getInstance().player;

        if (player == null)
        {
            java.util.Arrays.fill(abilityKeysDown, false);
            maskKeyDown = false;
            attackKeyDown = false;
            spellMenuWasDown = false;
            spellSequence.setLength(0);
            java.util.Arrays.fill(spellDirectionDown, false);
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        updateSpellInput(mc, player);
        for (int i = 0; i < SHKeyBinds.ABILITY_COUNT; ++i)
        {
            boolean down = mc.screen == null && SHKeyBinds.ABILITIES[i].isDown();
            setAbilityKeyState(player, i, down);
        }

        // In the original mod, key index -1 means the player's primary attack input. Several
        // packs bind AIM/SHOOT to this input rather than one of the five suit keys.
        boolean attackDown = mc.screen == null && mc.options.keyAttack.isDown();
        AbilityHandler.setClientKeyState(player, -1, attackDown);
        if (attackDown != attackKeyDown)
        {
            attackKeyDown = attackDown;
            SHNetwork.sendToServer(new PacketAbility(-1, attackDown));
        }

        boolean maskDown = mc.screen == null && SHKeyBinds.MASK.isDown();
        if (maskDown != maskKeyDown)
        {
            maskKeyDown = maskDown;
            SHNetwork.sendToServer(new PacketAbility(0, maskDown));
        }

        if (mc.screen instanceof com.fiskmods.heroes.client.gui.EquipmentWheelScreen)
        {
            if (!weaponKeyHeld) mc.setScreen(null);
        }
        else if (mc.screen == null && weaponKeyHeld)
        {
            com.fiskmods.heroes.client.gui.EquipmentWheelScreen wheel =
                    com.fiskmods.heroes.client.gui.EquipmentWheelScreen.createForCurrentPlayer();
            if (wheel != null) mc.setScreen(wheel);
        }

        com.fiskmods.heroes.client.sound.SHSoundPlayer.tick();

        boolean jump = Minecraft.getInstance().options.keyJump.isDown();
        boolean sneak = Minecraft.getInstance().options.keyShift.isDown();
        com.fiskmods.heroes.common.data.PlayerInputTracker.set(player, jump, sneak);

        if (!com.fiskmods.heroes.common.network.PacketInput.lastSent
                || jump != com.fiskmods.heroes.common.network.PacketInput.lastJump
                || sneak != com.fiskmods.heroes.common.network.PacketInput.lastSneak)
        {
            com.fiskmods.heroes.common.network.PacketInput.lastSent = true;
            com.fiskmods.heroes.common.network.PacketInput.lastJump = jump;
            com.fiskmods.heroes.common.network.PacketInput.lastSneak = sneak;
            SHNetwork.sendToServer(new com.fiskmods.heroes.common.network.PacketInput(jump, sneak));
        }

        SHPlayerData data = SHDataCapabilities.getPlayer(player);

        if (data == null)
        {
            return;
        }

        // Interpolate the values the HUD animates
        interpolate(data, Vars.MASK_OPEN_TIMER2, data.getData().get(Vars.MASK_OPEN) ? 1.0F : 0.0F);
        interpolate(data, Vars.BOOSTER_TIMER, data.getData().get(Vars.FLYING) ? 1.0F : 0.0F);
    }

    /** Sends only transitions, including very short presses received between client ticks. */
    private static void setAbilityKeyState(LocalPlayer player, int ability, boolean down)
    {
        if (ability < 0 || ability >= abilityKeysDown.length || abilityKeysDown[ability] == down) return;

        abilityKeysDown[ability] = down;
        int index = ABILITY_INDICES[ability];
        AbilityHandler.setClientKeyState(player, index, down);
        SHNetwork.sendToServer(new PacketAbility(index, down));

        if (down)
        {
            com.fiskmods.heroes.common.hero.Hero hero = HeroTracker.getHeroType(player);
            if (hero != null)
            {
                if (hero.getKeyBinding("SHAPE_SHIFT") == index
                        && hero.isKeyBindEnabled(player, "SHAPE_SHIFT"))
                {
                    Minecraft.getInstance().setScreen(new com.fiskmods.heroes.client.gui.ShapeShiftScreen());
                }
                else if (hero.getKeyBinding("SHAPE_SHIFT_RESET") == index
                        && hero.isKeyBindEnabled(player, "SHAPE_SHIFT_RESET"))
                {
                    SHNetwork.sendToServer(new com.fiskmods.heroes.common.network.PacketSetDisguise(""));
                }
            }
        }
    }

    /** Capture WASD sequences while the spell-menu key is held and request a matching spell. */
    private static void updateSpellInput(Minecraft mc, LocalPlayer player)
    {
        boolean menuDown = mc.screen == null && SHKeyBinds.SPELL_MENU.isDown();
        if (!menuDown)
        {
            if (spellMenuWasDown) spellSequence.setLength(0);
            spellMenuWasDown = false;
            java.util.Arrays.fill(spellDirectionDown, false);
            return;
        }

        if (!spellMenuWasDown) spellSequence.setLength(0);
        spellMenuWasDown = true;

        boolean[] down = {
                mc.options.keyUp.isDown(), mc.options.keyLeft.isDown(),
                mc.options.keyDown.isDown(), mc.options.keyRight.isDown()
        };
        char[] letters = { 'w', 'a', 's', 'd' };
        for (int i = 0; i < down.length; ++i)
        {
            if (down[i] && !spellDirectionDown[i])
            {
                spellDirectionDown[i] = true;
                spellSequence.append(letters[i]);
                matchSpellSequence(player);
            }
            else if (!down[i])
            {
                spellDirectionDown[i] = false;
            }
        }
    }

    private static void matchSpellSequence(LocalPlayer player)
    {
        if (spellSequence.length() > 16)
        {
            spellSequence.delete(0, spellSequence.length() - 16);
        }

        SHPlayerData data = SHDataCapabilities.getPlayer(player);
        com.fiskmods.heroes.common.hero.HeroIteration iteration = data != null ? data.getHero() : null;
        com.fiskmods.heroes.common.hero.Hero hero = iteration != null ? iteration.getHero() : null;
        if (hero == null || data == null) return;

        for (com.fiskmods.heroes.common.hero.power.ModifierEntry entry : hero.getPowerContainer().getEntries())
        {
            if (!(entry.getModifier() instanceof com.fiskmods.heroes.common.spell.ModifierSpellcasting)
                    || !entry.isEnabled() || !entry.isModifierEnabled(player, data)) continue;

            com.fiskmods.heroes.common.spell.SpellSet spells = entry.get(com.fiskmods.heroes.common.hero.power.PowerProperty.SPELLS);
            for (int i = 0; i < spells.size(); ++i)
            {
                com.fiskmods.heroes.common.spell.SpellDefinition spell = spells.get(i);
                if (spell != null && !spell.sequence().isEmpty() && spell.sequence().contentEquals(spellSequence))
                {
                    SHNetwork.sendToServer(new PacketCastSpell(i));
                    spellSequence.setLength(0);
                    return;
                }
            }
        }
    }

    @SubscribeEvent
    public static void onMouseInput(InputEvent.MouseButton event)
    {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player != null && mc.screen == null && event.getAction() == org.lwjgl.glfw.GLFW.GLFW_PRESS
                && mc.options.keyAttack.matchesMouse(event.getButton())
                && mc.player.getMainHandItem().is(com.fiskmods.heroes.common.item.ModItems.CAPTAIN_AMERICAS_SHIELD.get())
                && !mc.player.getCooldowns().isOnCooldown(com.fiskmods.heroes.common.item.ModItems.CAPTAIN_AMERICAS_SHIELD.get())
                && AbilityHandler.isKeyPressed(mc.player, "SHIELD_THROW"))
        {
            var hero = com.fiskmods.heroes.common.hero.HeroTracker.getHeroType(mc.player);
            var entry = hero != null ? AbilityHandler.findModifier(hero, "SHIELD_THROW") : null;
            int cooldown = entry != null
                    ? entry.getInt(com.fiskmods.heroes.common.hero.power.PowerProperty.COOLDOWN_TIME) : 10;
            SHNetwork.sendToServer(new com.fiskmods.heroes.common.network.PacketThrowShield());
            mc.player.getCooldowns().addCooldown(com.fiskmods.heroes.common.item.ModItems.CAPTAIN_AMERICAS_SHIELD.get(), Math.max(1, cooldown));
        }

        if (mc.player != null && mc.options.keyAttack.matchesMouse(event.getButton()))
        {
            boolean down = event.getAction() == org.lwjgl.glfw.GLFW.GLFW_PRESS && mc.screen == null;
            if (down != attackKeyDown)
            {
                attackKeyDown = down;
                AbilityHandler.setClientKeyState(mc.player, -1, down);
                SHNetwork.sendToServer(new PacketAbility(-1, down));
            }
        }

        if (!SHKeyBinds.WEAPON.matchesMouse(event.getButton())) return;
        if (event.getAction() == org.lwjgl.glfw.GLFW.GLFW_PRESS
                && (mc.screen == null || mc.screen instanceof com.fiskmods.heroes.client.gui.EquipmentWheelScreen)) weaponKeyHeld = true;
        else if (event.getAction() == org.lwjgl.glfw.GLFW.GLFW_RELEASE) weaponKeyHeld = false;
    }

    private static void interpolate(SHPlayerData data, com.fiskmods.heroes.common.data.var.DataVar<Float> var, float target)
    {
        float value = data.getData().get(var);
        data.getData().set(var, value + (target - value) * 0.15F);
    }

    /** The current frame's partial tick, kept by the render events. */
    public static float partialTicks()
    {
        return net.minecraft.client.Minecraft.getInstance().getFrameTime();
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event)
    {
        com.fiskmods.heroes.common.data.RenderTickInfo.set(com.fiskmods.heroes.client.ClientEventHandler.partialTicks());

        SuitHud.render(event.getGuiGraphics(), event.getPartialTick());
    }
}
