package com.fiskmods.heroes.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Short, full-bright particles orbit the grabbed entity, as in EntitySHTelekinesisFX. */
final class TelekinesisAuraParticle extends TextureSheetParticle
{
    private static final ResourceLocation[] SPRITES = new ResourceLocation[8];
    static
    {
        for (int i = 0; i < SPRITES.length; ++i) SPRITES[i] = new ResourceLocation("minecraft", "generic_" + i);
    }

    private final Entity target;
    private Vec3 rotation;
    private final Vec3 direction;
    private final float[] start;
    private final float[] end;

    TelekinesisAuraParticle(ClientLevel level, Entity target, Vec3 rotation, Vec3 direction,
            Integer startColor, Integer endColor, float[] fallback)
    {
        super(level, target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ());
        this.target = target;
        this.rotation = rotation;
        this.direction = direction;
        this.start = startColor != null ? rgb(startColor) : fallback;
        this.end = endColor != null ? rgb(endColor) : fallback;
        this.lifetime = (int) (6.0D / (this.random.nextFloat() * 0.4D + 0.6D));
        this.quadSize *= 0.8F + this.random.nextFloat() * 0.2F;
        this.hasPhysics = false;
        updateAppearance();
        updatePosition(false);
    }

    private static float[] rgb(int color)
    {
        return new float[] {(color >> 16 & 255) / 255.0F, (color >> 8 & 255) / 255.0F, (color & 255) / 255.0F};
    }

    @Override
    public void tick()
    {
        if (target.isRemoved() || ++age > lifetime) { remove(); return; }
        xo = x; yo = y; zo = z;
        updatePosition(true);
        updateAppearance();
    }

    private void updatePosition(boolean rotate)
    {
        if (rotate) rotation = rotation.xRot((float) direction.x * 0.4F)
                .yRot((float) direction.y * 0.4F).zRot((float) direction.z * 0.4F);
        double radius = target.getBbWidth() * 1.25D;
        setPos(target.getX() + rotation.x * radius,
                target.getY() + target.getBbHeight() * 0.5D + rotation.y * target.getBbHeight() * 0.75D,
                target.getZ() + rotation.z * radius);
        if (!rotate) { xo = x; yo = y; zo = z; }
    }

    private void updateAppearance()
    {
        float progress = (float) age / lifetime;
        float fade = 1.0F - Math.abs(2.0F * progress - 1.0F);
        setColor(Mth.lerp(fade, start[0], end[0]), Mth.lerp(fade, start[1], end[1]), Mth.lerp(fade, start[2], end[2]));
        alpha = 0.8F - progress * 0.6F;
        int frame = Mth.clamp(7 - age * 8 / lifetime, 0, 7);
        var atlas = (TextureAtlas) Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_PARTICLES);
        setSprite(atlas.getSprite(SPRITES[frame]));
    }

    @Override protected int getLightColor(float partialTicks) { return 0xF000F0; }
    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }
}
