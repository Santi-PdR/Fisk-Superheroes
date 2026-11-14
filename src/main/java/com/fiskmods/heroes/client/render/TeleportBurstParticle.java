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

/** Full-bright colored mote expands from an endpoint, then contracts over twelve ticks. */
final class TeleportBurstParticle extends TextureSheetParticle
{
    private static final ResourceLocation[] SPRITES = new ResourceLocation[8];
    private static boolean reportedGradient;
    static { for (int i = 0; i < 8; ++i) SPRITES[i] = new ResourceLocation("minecraft", "generic_" + i); }
    private final Vec3 center, offset;
    private final float scale;
    private final CloudColors colors;

    TeleportBurstParticle(ClientLevel level, Vec3 center, Vec3 offset, float scale, CloudColors colors)
    {
        super(level, center.x, center.y, center.z);
        this.center = center;
        this.offset = offset.add(random.nextFloat() * random.nextFloat() * 0.3D,
                random.nextFloat() * random.nextFloat() * 0.3D, random.nextFloat() * random.nextFloat() * 0.3D);
        this.scale = scale;
        this.colors = colors;
        lifetime = 12;
        hasPhysics = false;
        updateAppearance();
    }

    static void clearDiagnostics() { reportedGradient = false; }

    @Override public void tick()
    {
        xo = x; yo = y; zo = z;
        if (++age > lifetime) { remove(); return; }
        float factor = CloudColors.crest((float) age / lifetime);
        Vec3 point = center.add(offset.scale(factor));
        setPos(point.x, point.y, point.z);
        updateAppearance();
        if (Boolean.getBoolean("fiskheroes.ciParticleDiagnostics") && !reportedGradient && age == 6)
        {
            reportedGradient = true;
            com.fiskmods.heroes.FiskHeroes.LOGGER.info("Breach burst animated color: startR={}, startG={}, startB={}, midR={}, midG={}, midB={}",
                    colors.channel(0, 0), colors.channel(1, 0), colors.channel(2, 0),
                    colors.channel(0, 0.5F), colors.channel(1, 0.5F), colors.channel(2, 0.5F));
        }
    }

    private void updateAppearance()
    {
        float factor = CloudColors.crest((float) age / lifetime);
        setColor(colors.atFactor(0, factor), colors.atFactor(1, factor), colors.atFactor(2, factor));
        quadSize = scale * factor * 0.3F;
        int frame = Mth.clamp(Math.round(4 + 3 * factor), 0, 7);
        var atlas = (TextureAtlas) Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_PARTICLES);
        setSprite(atlas.getSprite(SPRITES[frame]));
    }

    @Override protected int getLightColor(float partialTick) { return 0xF000F0; }
    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }
}
