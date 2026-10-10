package com.fiskmods.heroes.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/** Recreates the original 1.7 chest extrusion from the suit texture. */
final class ChestSuitRenderer
{
    private ChestSuitRenderer() {}

    static void render(PoseStack pose, MultiBufferSource buffers, int light, Entity entity,
            HeroModelData model, int slot, ModelPart anchor, float extrusion, float yOffset, float opacity)
    {
        if (anchor == null || extrusion == 0.0F || opacity <= 0.0F) return;
        ResourceLocation texture = model.getTexture(slot, entity);
        if (texture == null) return;

        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(texture));
        pose.pushPose();
        try
        {
            anchor.translateAndRotate(pose);
            drawMesh(pose, consumer, light, opacity, extrusion, yOffset, false);

            // The original draws the outer texture row enlarged around the body pivot.
            pose.pushPose();
            try
            {
                pose.scale(1.1F, 1.1F, 1.1F);
                pose.translate(0.0D, -0.5D / 16.0D, 0.0D);
                drawMesh(pose, consumer, light, opacity, extrusion, yOffset, true);
            }
            finally { pose.popPose(); }
        }
        finally { pose.popPose(); }
    }

    private static void drawMesh(PoseStack pose, VertexConsumer out, int light, float opacity,
            float extrusion, float offset, boolean outer)
    {
        float u = 21.0F;
        float v = offset + 20.0F + (outer ? 16.0F : 0.0F);
        float h = 2.0F;
        float front = -2.0F;
        float back = front - extrusion;

        quad(pose,out,light,opacity,  3,offset,front, -3,offset,front, -3,offset+h,back,  3,offset+h,back,
                u+6,v, u,v, u,v+2, u+6,v+2);
        quad(pose,out,light,opacity, -3,offset,front, -4,offset,front, -4,offset+h,front, -3,offset+h,back,
                21,v, 20,v, 20,v+2, 21,v+2);
        quad(pose,out,light,opacity,  3,offset,front,  3,offset+h,back,  4,offset+h,front,  4,offset,front,
                27,v, 27,v+2, 28,v+2, 28,v);

        v += h;
        float y = offset + h;
        quad(pose,out,light,opacity,  3,y,back, -3,y,back, -3,y+h,back,  3,y+h,back,
                u+6,v, u,v, u,v+2, u+6,v+2);
        quad(pose,out,light,opacity, -3,y,back, -4,y,front, -4,y+h,front, -3,y+h,back,
                21,v, 20,v, 20,v+2, 21,v+2);
        quad(pose,out,light,opacity,  3,y,back,  3,y+h,back,  4,y+h,front,  4,y,front,
                27,v, 27,v+2, 28,v+2, 28,v);

        v += h;
        y += h;
        quad(pose,out,light,opacity,  3,y,back, -3,y,back, -3,y+h,back,  3,y+h,back,
                u+6,v, u,v, u,v+2, u+6,v+2);
        quad(pose,out,light,opacity, -4,y,front, -4,y+h,front, -3,y+h,front, -3,y,back,
                20,v, 20,v+2, 21,v+2, 21,v);
        quad(pose,out,light,opacity,  4,y,front,  3,y,back,  3,y+h,front,  4,y+h,front,
                28,v, 27,v, 27,v+2, 28,v+2);
    }

    private static void quad(PoseStack pose, VertexConsumer out, int light, float alpha,
            float ax,float ay,float az,float bx,float by,float bz,float cx,float cy,float cz,float dx,float dy,float dz,
            float au,float av,float bu,float bv,float cu,float cv,float du,float dv)
    {
        float abx = bx - ax, aby = by - ay, abz = bz - az;
        float acx = cx - ax, acy = cy - ay, acz = cz - az;
        float nx = aby * acz - abz * acy;
        float ny = abz * acx - abx * acz;
        float nz = abx * acy - aby * acx;
        float length = (float)Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (length > 0.0F) { nx /= length; ny /= length; nz /= length; }
        vertex(pose,out,light,alpha,ax,ay,az,au,av,nx,ny,nz);
        vertex(pose,out,light,alpha,bx,by,bz,bu,bv,nx,ny,nz);
        vertex(pose,out,light,alpha,cx,cy,cz,cu,cv,nx,ny,nz);
        vertex(pose,out,light,alpha,dx,dy,dz,du,dv,nx,ny,nz);
    }

    private static void vertex(PoseStack pose, VertexConsumer out, int light, float alpha,
            float x,float y,float z,float u,float v,float nx,float ny,float nz)
    {
        out.vertex(pose.last().pose(),x/16.0F,y/16.0F,z/16.0F)
                .color(255,255,255,Math.round(alpha*255.0F)).uv(u/64.0F,v/64.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(pose.last().normal(),nx,ny,nz).endVertex();
    }
}
