package com.fiskmods.heroes.client.render;

import com.fiskmods.heroes.common.util.CloudColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Legacy cloud-smoke lifetime, drift, animated sprite and sampled color endpoints. */
final class CloudSmokeParticle extends TextureSheetParticle
{
    private static final ResourceLocation[] SPRITES = new ResourceLocation[8];
    static { for (int i = 0; i < 8; ++i) SPRITES[i] = new ResourceLocation("minecraft", "generic_" + i); }
    private final CloudColors colors;

    CloudSmokeParticle(ClientLevel level, Vec3 point, Vec3 motion, CloudColors colors, boolean firstPerson)
    {
        super(level, point.x, point.y, point.z);
        this.colors = colors;
        xd = motion.x + (random.nextDouble() * 2 - 1) * 0.05D;
        yd = motion.y + (random.nextDouble() * 2 - 1) * 0.05D;
        zd = motion.z + (random.nextDouble() * 2 - 1) * 0.05D;
        lifetime = (int) (14.0D / (random.nextFloat() * 0.5D + 0.5D)) + 2;
        quadSize = (2.0F + random.nextFloat() * 4.0F) * 0.1F * (firstPerson ? 0.6F : 1.0F);
        hasPhysics = true;
        updateAppearance();
    }

    @Override public void tick()
    {
        xo = x; yo = y; zo = z;
        if (++age > lifetime) { remove(); return; }
        move(xd, yd, zd);
        xd *= 0.9D; yd *= 0.9D; zd *= 0.9D;
        if (onGround) { xd *= 0.7D; zd *= 0.7D; }
        updateAppearance();
    }

    private void updateAppearance()
    {
        float progress = (float) age / lifetime;
        float factor = com.fiskmods.heroes.common.util.CloudColors.crest(progress);
        setColor(colors.atFactor(0, factor), colors.atFactor(1, factor), colors.atFactor(2, factor));
        int frame = Mth.clamp(7 - age * 8 / lifetime, 0, 7);
        var atlas = (TextureAtlas) Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_PARTICLES);
        setSprite(atlas.getSprite(SPRITES[frame]));
    }

    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }
}
