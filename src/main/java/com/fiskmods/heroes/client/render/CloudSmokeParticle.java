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
    private static boolean reportedGradient;
    private final CloudColors colors;
    private final ResourceLocation cloudId;

    CloudSmokeParticle(ClientLevel level, Vec3 point, Vec3 motion, CloudColors colors, boolean firstPerson, ResourceLocation cloudId)
    {
        super(level, point.x, point.y, point.z);
        this.colors = colors;
        this.cloudId = cloudId;
        xd = motion.x + (random.nextDouble() * 2 - 1) * 0.05D;
        yd = motion.y + (random.nextDouble() * 2 - 1) * 0.05D;
        zd = motion.z + (random.nextDouble() * 2 - 1) * 0.05D;
        lifetime = (int) (14.0D / (random.nextFloat() * 0.5D + 0.5D)) + 2;
        quadSize = (2.0F + random.nextFloat() * 4.0F) * 0.1F * (firstPerson ? 0.6F : 1.0F);
        hasPhysics = true;
        updateAppearance();
    }

    static void clearDiagnostics() { reportedGradient = false; }

    @Override public void tick()
    {
        xo = x; yo = y; zo = z;
        if (++age > lifetime) { remove(); return; }
        move(xd, yd, zd);
        xd *= 0.9D; yd *= 0.9D; zd *= 0.9D;
        if (onGround) { xd *= 0.7D; zd *= 0.7D; }
        updateAppearance();
        if (Boolean.getBoolean("fiskheroes.ciParticleDiagnostics") && !reportedGradient
                && cloudId.toString().equals("fiskheroes:breach") && age == lifetime / 2)
        {
            reportedGradient = true;
            com.fiskmods.heroes.FiskHeroes.LOGGER.info("Breach cloud animated color: startR={}, startG={}, startB={}, midR={}, midG={}, midB={}",
                    colors.channel(0, 0), colors.channel(1, 0), colors.channel(2, 0),
                    colors.channel(0, 0.5F), colors.channel(1, 0.5F), colors.channel(2, 0.5F));
        }
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
