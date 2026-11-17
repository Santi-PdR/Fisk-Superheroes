package com.fiskmods.heroes.pack.js;

import com.fiskmods.heroes.common.data.DataRegistry;
import com.fiskmods.heroes.common.data.var.DataVar;
import net.minecraft.world.entity.Entity;

/** Script-facing neutral display entity for a stored miniature, not its current holder. */
public final class JSMiniatureDisplayEntity extends JSEntity
{
    public JSMiniatureDisplayEntity(Entity holder)
    {
        super(holder);
    }

    @Override
    public Object getData(String key)
    {
        DataVar<?> variable = DataRegistry.INSTANCE.get(key);
        return variable != null ? variable.getDefault() : null;
    }

    @Override
    public float getInterpolatedData(String key)
    {
        Object value = getData(key);
        return value instanceof Number number ? number.floatValue() : 0.0F;
    }

    @Override
    public boolean is(String type)
    {
        return "DISPLAY".equalsIgnoreCase(type) || super.is(type);
    }

    public String getDisplayType()
    {
        return "MINI_SUIT_ITEM";
    }
}
