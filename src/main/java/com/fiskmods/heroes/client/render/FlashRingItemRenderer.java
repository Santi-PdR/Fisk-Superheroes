package com.fiskmods.heroes.client.render;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.item.ItemDualSword;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import com.fiskmods.heroes.common.hero.HeroIteration;
import com.fiskmods.heroes.common.hero.ItemHeroArmor;
import com.fiskmods.heroes.common.item.ItemFlashRing;
import com.fiskmods.heroes.common.item.ModItems;

/** Original 3D Flash Ring mesh + floating stored suit icon ported from ModelFlashRing & RenderItemFlashRing. */
public final class FlashRingItemRenderer extends BlockEntityWithoutLevelRenderer
{
    private static final ResourceLocation TEXTURE = FiskHeroes.id("textures/models/the_flash_ring.png");
    private static FlashRingItemRenderer instance;
    private final ModelPart ring1;
    private final ModelPart upperRingSide1;
    private final ModelPart upperRingSide2;
    private final ModelPart upperRingBase1;
    private final ModelPart topRing1;
    private final ModelPart lightningBolt1;

    private FlashRingItemRenderer()
    {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        ModelPart baked = createModel();
        this.ring1 = baked.getChild("ring1");
        this.upperRingSide1 = baked.getChild("upperRingSide1");
        this.upperRingSide2 = baked.getChild("upperRingSide2");
        this.upperRingBase1 = baked.getChild("upperRingBase1");
        this.topRing1 = baked.getChild("topRing1");
        this.lightningBolt1 = baked.getChild("lightningBolt1");
    }

    public static FlashRingItemRenderer getInstance()
    {
        if (instance == null) instance = new FlashRingItemRenderer();
        return instance;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
            MultiBufferSource buffers, int light, int overlay)
    {
        HeroIteration contained = ItemFlashRing.getContainedHero(stack);
        pose.pushPose();
        switch (context)
        {
            case GUI ->
            {
                pose.translate(0.5D, 0.34D, 0.5D);
                pose.mulPose(Axis.YP.rotationDegrees(45.0F));
                pose.mulPose(Axis.XP.rotationDegrees(35.0F));
                pose.mulPose(Axis.YP.rotationDegrees(180.0F));
                pose.scale(1.45F, -1.45F, -1.45F);
            }
            case GROUND, FIXED ->
            {
                pose.translate(0.5D, 0.42D, 0.5D);
                pose.scale(0.85F, -0.85F, -0.85F);
            }
            case FIRST_PERSON_RIGHT_HAND, FIRST_PERSON_LEFT_HAND ->
            {
                pose.translate(context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND ? 0.62D : 0.38D, 0.46D, 0.46D);
                pose.mulPose(Axis.XP.rotationDegrees(25.0F));
                pose.mulPose(Axis.YP.rotationDegrees(180.0F));
                pose.scale(0.85F, -0.85F, -0.85F);
            }
            case THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND ->
            {
                pose.translate(0.5D, 0.48D, 0.5D);
                pose.mulPose(Axis.XP.rotationDegrees(25.0F));
                pose.scale(0.55F, -0.55F, -0.55F);
            }
            default ->
            {
                pose.translate(0.5D, 0.42D, 0.5D);
                pose.scale(0.85F, -0.85F, -0.85F);
            }
        }

        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        renderScaled(pose, consumer, light, overlay, upperRingSide2, 1.0F, 1.0F, 0.8F);
        renderScaled(pose, consumer, light, overlay, upperRingBase1, 0.6F, 0.55F, 0.6F);
        renderScaled(pose, consumer, light, overlay, topRing1, 0.55F, 0.5F, 0.55F);
        ring1.render(pose, consumer, light, overlay);
        renderScaled(pose, consumer, light, overlay, upperRingSide1, 1.0F, 1.0F, 0.8F);
        if (contained == null)
        {
            renderScaled(pose, consumer, light, overlay, lightningBolt1, 0.4F, 0.5F, 0.9F);
        }
        pose.popPose();

        if (contained != null && ModItems.heroArmor != null)
        {
            int slot = Math.max(0, contained.getHero().getCorePieceOfSet());
            ItemStack icon = ItemHeroArmor.create(contained, ModItems.heroArmor[slot]);
            pose.pushPose();
            pose.translate(0.5D, context == ItemDisplayContext.GUI ? 0.66D : 0.62D, 0.5D);
            float iconScale = context == ItemDisplayContext.GUI ? 0.52F : 0.35F;
            pose.scale(iconScale, iconScale, iconScale);
            Minecraft.getInstance().getItemRenderer().renderStatic(
                    icon, ItemDisplayContext.GUI, light, overlay, pose, buffers, Minecraft.getInstance().level, 0);
            pose.popPose();
        }
    }

    private static void renderScaled(PoseStack pose, VertexConsumer consumer, int light, int overlay,
            ModelPart part, float sx, float sy, float sz)
    {
        pose.pushPose();
        pose.translate(part.x * 0.0625F, part.y * 0.0625F, part.z * 0.0625F);
        pose.scale(sx, sy, sz);
        pose.translate(-part.x * 0.0625F, -part.y * 0.0625F, -part.z * 0.0625F);
        part.render(pose, consumer, light, overlay);
        pose.popPose();
    }

    private static ModelPart createModel()
    {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition meshRoot = mesh.getRoot();
        meshRoot.addOrReplaceChild("upperRingSide2", CubeListBuilder.create()
                .texOffs(0, 15).mirror(true).addBox(-0.5F, 0.0F, -1.0F, 1.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(1.58F, -0.95F, 0.0F, 0.0F, 0.0F, -0.5235988F));
        PartDefinition p_upperRingBase1 = meshRoot.addOrReplaceChild("upperRingBase1", CubeListBuilder.create()
                .texOffs(0, 10).mirror(false).addBox(-2.5F, -1.0F, -2.0F, 5.0F, 1.0F, 4.0F), PartPose.offset(0.0F, -0.4F, 0.0F));
        p_upperRingBase1.addOrReplaceChild("upperRingBase2", CubeListBuilder.create()
                .texOffs(0, 4).mirror(false).addBox(-2.0F, -1.0F, -2.5F, 4.0F, 1.0F, 5.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition p_topRing1 = meshRoot.addOrReplaceChild("topRing1", CubeListBuilder.create()
                .texOffs(9, 0).mirror(false).addBox(-1.5F, -3.65F, -1.0F, 3.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(0.0F, -0.7F, 0.0F, 1.5707964F, 0.0F, 0.0F));
        p_topRing1.addOrReplaceChild("topRing7", CubeListBuilder.create()
                .texOffs(9, 0).mirror(false).addBox(-1.5F, -3.65F, -1.0F, 3.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.5707964F));
        p_topRing1.addOrReplaceChild("topRing5", CubeListBuilder.create()
                .texOffs(9, 0).mirror(false).addBox(-1.5F, -3.65F, -1.0F, 3.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, (float) Math.PI));
        p_topRing1.addOrReplaceChild("topRing3", CubeListBuilder.create()
                .texOffs(9, 0).mirror(false).addBox(-1.5F, -3.65F, -1.0F, 3.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.5707964F));
        p_topRing1.addOrReplaceChild("topRing4", CubeListBuilder.create()
                .texOffs(9, 0).mirror(false).addBox(-1.5F, -3.65F, -1.0F, 3.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 2.3561945F));
        p_topRing1.addOrReplaceChild("topRing6", CubeListBuilder.create()
                .texOffs(9, 0).mirror(false).addBox(-1.5F, -3.65F, -1.0F, 3.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.7853982F));
        p_topRing1.addOrReplaceChild("topRing8", CubeListBuilder.create()
                .texOffs(9, 0).mirror(false).addBox(-1.5F, -3.65F, -1.0F, 3.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -2.3561945F));
        p_topRing1.addOrReplaceChild("topRing2", CubeListBuilder.create()
                .texOffs(9, 0).mirror(false).addBox(-1.5F, -3.65F, -1.0F, 3.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.7853982F));
        PartDefinition p_ring1 = meshRoot.addOrReplaceChild("ring1", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-1.5F, -3.65F, -1.5F, 3.0F, 1.0F, 3.0F), PartPose.offset(0.0F, 3.0F, 0.0F));
        p_ring1.addOrReplaceChild("ring2", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-1.5F, -3.65F, -1.5F, 3.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.7853982F));
        p_ring1.addOrReplaceChild("ring4", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-1.5F, -3.65F, -1.5F, 3.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 2.3561945F));
        p_ring1.addOrReplaceChild("ring8", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-1.5F, -3.65F, -1.5F, 3.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -2.3561945F));
        p_ring1.addOrReplaceChild("ring3", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-1.5F, -3.65F, -1.5F, 3.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.5707964F));
        p_ring1.addOrReplaceChild("ring5", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-1.5F, -3.65F, -1.5F, 3.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, (float) Math.PI));
        p_ring1.addOrReplaceChild("ring7", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-1.5F, -3.65F, -1.5F, 3.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.5707964F));
        p_ring1.addOrReplaceChild("ring6", CubeListBuilder.create()
                .texOffs(0, 0).mirror(false).addBox(-1.5F, -3.65F, -1.5F, 3.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.7853982F));
        meshRoot.addOrReplaceChild("upperRingSide1", CubeListBuilder.create()
                .texOffs(0, 15).mirror(false).addBox(-0.5F, 0.0F, -1.0F, 1.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(-1.58F, -0.95F, 0.0F, 0.0F, 0.0F, 0.5235988F));
        PartDefinition p_lightningBolt1 = meshRoot.addOrReplaceChild("lightningBolt1", CubeListBuilder.create()
                .texOffs(6, 15).mirror(false).addBox(-0.5F, -0.5F, -1.0F, 1.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(0.0F, -1.0F, 0.0F, 0.0F, 0.5235988F, 0.0F));
        p_lightningBolt1.addOrReplaceChild("lightningBolt2", CubeListBuilder.create()
                .texOffs(6, 15).mirror(false).addBox(0.3F, -0.5F, -0.2F, 1.0F, 1.0F, 2.0F), PartPose.offset(0.0F, 0.0F, 0.8F));
        p_lightningBolt1.addOrReplaceChild("lightningBolt3", CubeListBuilder.create()
                .texOffs(6, 15).mirror(false).addBox(-1.3F, -0.5F, -1.8F, 1.0F, 1.0F, 2.0F), PartPose.offset(0.0F, 0.0F, -0.8F));
        return LayerDefinition.create(mesh, 32, 32).bakeRoot();
    }
}
