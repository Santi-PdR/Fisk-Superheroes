package com.fiskmods.heroes.client;

import com.fiskmods.heroes.client.hud.SuitHud;
import com.fiskmods.heroes.client.keybinds.SHKeyBinds;
import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.data.var.Vars;
import com.fiskmods.heroes.common.hero.ability.AbilityHandler;
import com.fiskmods.heroes.common.hero.HeroTracker;
import com.fiskmods.heroes.common.network.PacketAbility;
import com.fiskmods.heroes.common.network.PacketGunFire;
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
    private static boolean abilityClickMode;
    private static long nextGunShotTick = Long.MIN_VALUE;
    private static int hudAbilityHeld = Integer.MIN_VALUE;
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
        if (mc.player != null && mc.screen == null && event.getAction() == org.lwjgl.glfw.GLFW.GLFW_PRESS
                && SHKeyBinds.ABILITY_CLICK_MODE.matches(event.getKey(), event.getScanCode()))
        {
            abilityClickMode = !abilityClickMode;
            if (abilityClickMode) mc.mouseHandler.releaseMouse();
            else mc.mouseHandler.grabMouse();
            event.setCanceled(true);
            return;
        }

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

            if (event.getAction() == org.lwjgl.glfw.GLFW.GLFW_PRESS && mc.screen == null
                    && mc.options.keyAttack.matches(event.getKey(), event.getScanCode()))
            {
                requestGunShot(player);
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
            abilityClickMode = false;
            nextGunShotTick = Long.MIN_VALUE;
            hudAbilityHeld = Integer.MIN_VALUE;
            spellMenuWasDown = false;
            spellSequence.setLength(0);
            java.util.Arrays.fill(spellDirectionDown, false);
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        // Vanilla GUI screens own the pointer while open and re-capture it when they close.
        if (mc.screen != null) abilityClickMode = false;
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

        // Firearms shoot from primary attack input. Send an explicit request so the server can
        // validate the held item, hero permission, ammunition, reload state and cadence.
        if (attackDown)
        {
            requestGunShot(player);
        }
        else
        {
            nextGunShotTick = Long.MIN_VALUE;
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
        setAbilityInput(player, index, down);
    }

    /** Keeps keyboard input and clickable HUD controls on the same local and network path. */
    private static void setAbilityInput(LocalPlayer player, int index, boolean down)
    {
        AbilityHandler.setClientKeyState(player, index, down);
        if (down)
        {
            com.fiskmods.heroes.FiskHeroes.LOGGER.info("Ability input sent from client: index={}", index);
        }
        SHNetwork.sendToServer(new PacketAbility(index, down));

        if (down)
        {
            com.fiskmods.heroes.common.hero.Hero hero = HeroTracker.getHeroType(player);
            if (hero != null)
            {
                if (hero.hasKeyBind("SHAPE_SHIFT") && hero.getKeyBinding("SHAPE_SHIFT") == index
                        && hero.isKeyBindEnabled(player, "SHAPE_SHIFT"))
                {
                    Minecraft.getInstance().setScreen(new com.fiskmods.heroes.client.gui.ShapeShiftScreen());
                }
                else if (hero.hasKeyBind("SHAPE_SHIFT_RESET") && hero.getKeyBinding("SHAPE_SHIFT_RESET") == index
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
        com.fiskmods.heroes.common.hero.Hero hero = HeroTracker.getHeroType(player);
        int spellMenuIndex = hero != null ? hero.getKeyBinding("SPELL_MENU") : Integer.MIN_VALUE;
        boolean hasHeroSpellBinding = hero != null && hero.getKeyBind("SPELL_MENU") != null;
        boolean menuBindingEnabled = !hasHeroSpellBinding || hero.isKeyBindEnabled(player, "SPELL_MENU");
        boolean menuKeyDown = hasHeroSpellBinding && spellMenuIndex >= 1 && spellMenuIndex <= SHKeyBinds.ABILITY_COUNT
                ? SHKeyBinds.ABILITIES[spellMenuIndex - 1].isDown() || hudAbilityHeld == spellMenuIndex
                : SHKeyBinds.SPELL_MENU.isDown();
        boolean menuDown = mc.screen == null && menuBindingEnabled && menuKeyDown;
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
    public static void onMouseInput(InputEvent.MouseButton.Pre event)
    {
        Minecraft mc = Minecraft.getInstance();

        // The original HUD presents these rows as controls. Let a click on a row trigger the same
        // server-validated keybind as its displayed keyboard key, including AIM (-1).
        if (mc.player != null && event.getButton() == org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_1)
        {
            if (event.getAction() == org.lwjgl.glfw.GLFW.GLFW_PRESS && mc.screen == null)
            {
                // Convert MouseHandler's raw window coordinates the same way vanilla converts
                // them before dispatching clicks to screens. getWidth()/getHeight() are framebuffer
                // dimensions and produce a shifted hit position on HiDPI or scaled displays.
                double x = mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth() / mc.getWindow().getScreenWidth();
                double y = mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight() / mc.getWindow().getScreenHeight();
                int index = SuitHud.findKeyBindAt(x, y);
                if (index != Integer.MIN_VALUE && index >= -1 && index < 16)
                {
                    hudAbilityHeld = index;
                    setAbilityInput(mc.player, index, true);
                    event.setCanceled(true);
                    return;
                }
            }
            else if (event.getAction() == org.lwjgl.glfw.GLFW.GLFW_RELEASE && hudAbilityHeld != Integer.MIN_VALUE)
            {
                int index = hudAbilityHeld;
                hudAbilityHeld = Integer.MIN_VALUE;
                setAbilityInput(mc.player, index, false);
                event.setCanceled(true);
                return;
            }
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
            if (down) requestGunShot(mc.player);
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

    /** Sends one rate-limited shot for both quick presses and held primary-attack input. */
    private static void requestGunShot(LocalPlayer player)
    {
        if (!com.fiskmods.heroes.common.item.ItemGun.isGun(player.getMainHandItem()))
        {
            return;
        }

        long now = player.level().getGameTime();
        if (now < nextGunShotTick)
        {
            return;
        }

        var gun = (com.fiskmods.heroes.common.item.ItemGun) player.getMainHandItem().getItem();
        SHNetwork.sendToServer(new PacketGunFire());
        nextGunShotTick = now + gun.getShotCooldownTicks(player.getMainHandItem());
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
