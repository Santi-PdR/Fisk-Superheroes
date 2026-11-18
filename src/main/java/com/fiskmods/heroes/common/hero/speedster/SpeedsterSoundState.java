package com.fiskmods.heroes.common.hero.speedster;

import java.util.function.Consumer;

/** Per-player transition tracker matching the original SpeedsterSoundHandler. */
public final class SpeedsterSoundState
{
    private boolean moving;
    private boolean sprinting;
    private java.util.List<net.minecraft.resources.ResourceLocation> moveIds = java.util.List.of();

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
    public java.util.List<net.minecraft.resources.ResourceLocation> getMoveIds() { return moveIds; }
    public void setMoveIds(java.util.List<net.minecraft.resources.ResourceLocation> ids) { moveIds = java.util.List.copyOf(ids); }

    public void reset()
    {
        moving = false;
        sprinting = false;
        moveIds = java.util.List.of();
    }
}
