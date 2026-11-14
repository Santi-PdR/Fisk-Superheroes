package com.fiskmods.heroes.client.render;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.data.var.Vars;
import com.fiskmods.heroes.common.util.ParticleEmitterMath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Short-lived chain motes follow the grabbed target along the caster's view curve. */
final class TelekinesisChainParticle extends TextureSheetParticle
{
    private static final ResourceLocation[] SPRITES = new ResourceLocation[8];
    private static final boolean DIAGNOSTICS = Boolean.getBoolean("fiskheroes.ciParticleDiagnostics");
    private static boolean reportedMovement;
    static
    {
        for (int i = 0; i < SPRITES.length; ++i) SPRITES[i] = new ResourceLocation("minecraft", "generic_" + i);
    }

    private final Player caster;
    private final Entity target;
    private final Vec3 start;
    private final double progress;
    private final Vec3 initialTarget;
    private final Vec3 initialPoint;

    TelekinesisChainParticle(ClientLevel level, Player caster, Entity target, Vec3 start, double progress, float[] color)
    {
        super(level, start.x, start.y, start.z);
        this.caster = caster;
        this.target = target;
        this.start = start;
        this.progress = progress;
        this.initialTarget = target.position();
        this.initialPoint = point();
        this.lifetime = ((int) (14.0D / (this.random.nextFloat() * 0.5D + 0.5D)) + 2) / 2;
        this.quadSize = (2.0F + random.nextFloat() * 4.0F) * 0.035F;
        this.hasPhysics = false;
        setColor(color[0], color[1], color[2]);
        setPos(initialPoint.x, initialPoint.y, initialPoint.z);
        xo = x; yo = y; zo = z;
        updateAppearance();
    }

    static void clearDiagnostics() { reportedMovement = false; }

    private Vec3 point()
    {
        Vec3 end = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
        return ParticleEmitterMath.telekinesisChainPoint(start, caster.getEyePosition(), caster.getViewVector(1.0F), end, progress);
    }

    @Override
    public void tick()
    {
        if (++age > lifetime) { remove(); return; }
        xo = x; yo = y; zo = z;
        var data = SHDataCapabilities.getPlayer(caster);
        if (!target.isRemoved() && data != null && data.getData().get(Vars.TELEKINESIS)
                && data.getData().get(Vars.GRAB_ID) == target.getId())
        {
            Vec3 next = point();
            setPos(next.x, next.y, next.z);
            if (DIAGNOSTICS && !reportedMovement && progress >= 0.4D && progress <= 0.6D
                    && target.position().distanceToSqr(initialTarget) > 1.0D)
            {
                reportedMovement = true;
                FiskHeroes.LOGGER.info("Telekinesis chain particle tracks moving target: targetDelta={}, pointDelta={}, progress={}",
                        target.position().distanceTo(initialTarget), next.distanceTo(initialPoint), progress);
            }
        }
        updateAppearance();
    }

    private void updateAppearance()
    {
        alpha = age < 2 ? 0.0F : Mth.clamp(1.2F - 0.6F * age / lifetime, 0.0F, 1.0F);
        int frame = Mth.clamp(10 - age, 0, 7);
        var atlas = (TextureAtlas) Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_PARTICLES);
        setSprite(atlas.getSprite(SPRITES[frame]));
    }

    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }
}
