package com.fiskmods.heroes.common.hero.modifier;

import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.data.var.Vars;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** Cleans up grab ownership on suit/key lifecycle changes and recovers stale saved marks. */
public final class TelekinesisState
{
    public static final String GRABBED_BY_TAG = "FiskHeroesGrabbedBy";
    private TelekinesisState() {}

    public static void release(LivingEntity owner, SHPlayerData data)
    {
        int id = data.getData().get(Vars.GRAB_ID);
        Entity target = id >= 0 ? owner.level().getEntity(id) : null;
        if (target != null && target.getPersistentData().contains(GRABBED_BY_TAG)
                && target.getPersistentData().getInt(GRABBED_BY_TAG) == owner.getId())
            target.getPersistentData().remove(GRABBED_BY_TAG);
        data.getData().set(Vars.GRAB_ID, -1);
        data.getData().set(Vars.GRAB_DISTANCE, 0.0F);
    }

    public static boolean canClaim(LivingEntity owner, Entity target)
    {
        var tag = target.getPersistentData();
        if (!tag.contains(GRABBED_BY_TAG)) return true;
        int id = tag.getInt(GRABBED_BY_TAG);
        if (id == owner.getId()) return true;
        Entity previous = owner.level().getEntity(id);
        SHPlayerData data = previous != null && previous.isAlive() ? SHDataCapabilities.getPlayer(previous) : null;
        if (data != null && data.getData().get(Vars.TELEKINESIS) && data.getData().get(Vars.GRAB_ID) == target.getId())
            return false;
        // Entity ids are not persistent across reloads; stale marks must never lock a target forever.
        tag.remove(GRABBED_BY_TAG);
        return true;
    }
}
