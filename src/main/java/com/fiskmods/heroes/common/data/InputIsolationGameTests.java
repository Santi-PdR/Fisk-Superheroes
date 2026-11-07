package com.fiskmods.heroes.common.data;

import java.util.UUID;

import com.fiskmods.heroes.FiskHeroes;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(FiskHeroes.MODID)
@PrefixGameTestTemplate(false)
public final class InputIsolationGameTests
{
    private InputIsolationGameTests() {}

    @GameTest(template = "test_empty")
    public static void integratedClientAndServerHaveIndependentInputs(GameTestHelper helper)
    {
        UUID id = UUID.randomUUID();
        PlayerInputTracker.set(false, id, true, false, 8, -4, true);
        PlayerInputTracker.set(true, id, false, true, -3, 7, false);
        var server = PlayerInputTracker.get(false, id);
        var client = PlayerInputTracker.get(true, id);
        if (!server.jump() || server.sneak() || server.forward() != 1 || server.strafe() != -1 || !server.use()
                || client.jump() || !client.sneak() || client.forward() != -1 || client.strafe() != 1 || client.use())
        { helper.fail("Client key updates must not overwrite authoritative server input for the same UUID"); return; }
        PlayerInputTracker.clear(true, id);
        if (PlayerInputTracker.get(true, id).sneak() || !PlayerInputTracker.get(false, id).jump())
        { helper.fail("Client disconnect must not clear the integrated server's keys"); return; }
        PlayerInputTracker.clear(false, id);
        helper.succeed();
    }
}
