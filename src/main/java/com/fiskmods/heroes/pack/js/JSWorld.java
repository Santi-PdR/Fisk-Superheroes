package com.fiskmods.heroes.pack.js;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** World facade exposed to the original hero-pack JavaScript API. */
public final class JSWorld
{
    private final Level level;

    public JSWorld(Level level)
    {
        this.level = level;
    }

    public int getDimension()
    {
        return level.dimension().location().hashCode();
    }

    public String name()
    {
        return level.dimension().location().toString();
    }

    public boolean isDaytime()
    {
        return level.isDay();
    }

    public boolean isRaining()
    {
        return level.isRaining();
    }

    public boolean isThundering()
    {
        return level.isThundering();
    }

    public JSBlock blockAt(int x, int y, int z)
    {
        return new JSBlock(level, new BlockPos(x, y, z));
    }

    public JSBlock blockAt(JSVector position)
    {
        return blockAt(BlockPos.containing(position.x(), position.y(), position.z()));
    }

    public JSBlock blockAt(BlockPos position)
    {
        return new JSBlock(level, position);
    }
}
