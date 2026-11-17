package com.fiskmods.heroes.common.hero.speedster;

import java.util.function.Consumer;

/** Per-player transition tracker matching the original SpeedsterSoundHandler. */
public final class SpeedsterSoundState
{
    private boolean moving;
    private boolean sprinting;

    public void update(boolean speeding, boolean movingNow, boolean sprintingNow, Consumer<String> dispatch)
    {
        if (!speeding)
        {
            if (moving) dispatch.accept("STOP");
            reset();
            return;
        }
        if (sprintingNow && !sprinting) dispatch.accept("SPRINT");
        sprinting = sprintingNow;
        if (moving != movingNow) dispatch.accept(movingNow ? "MOVE" : "STOP");
        moving = movingNow;
    }

    public boolean isMoving() { return moving; }

    public void reset()
    {
        moving = false;
        sprinting = false;
    }
}
