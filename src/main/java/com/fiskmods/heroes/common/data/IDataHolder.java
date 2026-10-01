package com.fiskmods.heroes.common.data;

import com.fiskmods.heroes.common.data.var.DataContainer;
import com.fiskmods.heroes.common.data.var.DataVar;

/**
 * Implemented by the entity capability which stores hero pack data variables.
 */
public interface IDataHolder
{
    DataContainer getData();

    default <T> T get(DataVar<T> var)
    {
        return getData().get(var);
    }

    default <T> void set(DataVar<T> var, T value)
    {
        getData().set(var, value);
    }

    /** Interpolated accessor used by client-side rendering code. */
    default float getInterpolated(DataVar<Float> var)
    {
        return getData().get(var);
    }
}
