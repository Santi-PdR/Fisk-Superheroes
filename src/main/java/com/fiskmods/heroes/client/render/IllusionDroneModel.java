package com.fiskmods.heroes.client.render;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.spell.IllusionDroneEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/** Faithful conversion of the original paired-half illusion drone model and 128x64 UV map. */
public final class IllusionDroneModel extends EntityModel<IllusionDroneEntity>
{
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(FiskHeroes.id("illusion_drone"), "main");
    private final ModelPart left;
    private final ModelPart right;
    private float alpha = 1.0F;

    public IllusionDroneModel(ModelPart root)
    {
        left = root.getChild("lHalf1");
        right = root.getChild("rHalf1");
    }

    public static LayerDefinition createBodyLayer()
    {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition part_lHalf1 = root.addOrReplaceChild("lHalf1", CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, -3.02F, -2.2F, 2.0F, 6.0F, 14.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition part_lHalf20 = part_lHalf1.addOrReplaceChild("lHalf20", CubeListBuilder.create().texOffs(60, 0).addBox(0.0F, 0.0F, 0.0F, 1.0F, 5.0F, 4.0F), PartPose.offset(7.0F, -2.5F, 4.0F));
        PartDefinition part_lHalf21 = part_lHalf20.addOrReplaceChild("lHalf21", CubeListBuilder.create().texOffs(70, 0).addBox(-1.0F, 0.0F, -4.0F, 1.0F, 5.0F, 4.0F), PartPose.offsetAndRotation(1.0F, 0.0F, 0.0F, 0.0F, 0.2617994F, 0.0F));
        PartDefinition part_lHalf22 = part_lHalf21.addOrReplaceChild("lHalf22", CubeListBuilder.create().texOffs(80, 0).addBox(-1.0F, 0.0F, -4.0F, 1.0F, 5.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 0.0F, -4.0F, 0.0F, 0.2617994F, 0.0F));
        PartDefinition part_lHalf23 = part_lHalf22.addOrReplaceChild("lHalf23", CubeListBuilder.create().texOffs(50, 9).addBox(-1.0F, 0.0F, 0.0F, 1.0F, 3.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 1.0F, -4.0F, 0.0F, 0.2268928F, 0.0F));
        PartDefinition part_lHalf24 = part_lHalf23.addOrReplaceChild("lHalf24", CubeListBuilder.create().texOffs(58, 9).addBox(-1.0F, 0.0F, 0.0F, 1.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 3.0F, 0.0F, -0.2268928F, 0.0F));
        PartDefinition part_lHalf25 = part_lHalf24.addOrReplaceChild("lHalf25", CubeListBuilder.create().texOffs(64, 9).addBox(-1.0F, 0.0F, 0.0F, 1.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 2.0F, 0.0F, -0.15707964F, 0.0F));
        PartDefinition part_lHalf26 = part_lHalf25.addOrReplaceChild("lHalf26", CubeListBuilder.create().texOffs(70, 9).addBox(-1.0F, 0.0F, 0.0F, 1.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 2.0F, 0.0F, -0.17453292F, 0.0F));
        PartDefinition part_lHalf40 = part_lHalf20.addOrReplaceChild("lHalf40", CubeListBuilder.create().texOffs(50, 0).mirror().addBox(0.0F, 0.0F, -4.0F, 1.0F, 5.0F, 4.0F), PartPose.offsetAndRotation(1.0F, 0.0F, 4.0F, 0.0F, 2.8797932F, 0.0F));
        PartDefinition part_lHalf41 = part_lHalf40.addOrReplaceChild("lHalf41", CubeListBuilder.create().texOffs(40, 1).mirror().addBox(0.0F, 0.0F, -4.0F, 1.0F, 5.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 0.0F, -4.0F, 0.0F, -0.2617994F, 0.0F));
        PartDefinition part_lHalf42 = part_lHalf41.addOrReplaceChild("lHalf42", CubeListBuilder.create().texOffs(29, 17).mirror().addBox(0.0F, 0.0F, 0.0F, 1.0F, 3.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 1.0F, -4.0F, 0.0F, -0.2268928F, 0.0F));
        PartDefinition part_lHalf43 = part_lHalf42.addOrReplaceChild("lHalf43", CubeListBuilder.create().texOffs(38, 10).mirror().addBox(0.0F, 0.0F, 0.0F, 1.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 3.0F, 0.0F, 0.2268928F, 0.0F));
        PartDefinition part_lHalf44 = part_lHalf43.addOrReplaceChild("lHalf44", CubeListBuilder.create().texOffs(44, 10).mirror().addBox(0.0F, 0.0F, 0.0F, 1.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 2.0F, 0.0F, 0.15707964F, 0.0F));
        PartDefinition part_lHalf45 = part_lHalf44.addOrReplaceChild("lHalf45", CubeListBuilder.create().texOffs(34, 15).mirror().addBox(0.0F, 0.0F, 0.0F, 1.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 2.0F, 0.0F, 0.17453292F, 0.0F));
        PartDefinition part_lHalf27 = part_lHalf20.addOrReplaceChild("lHalf27", CubeListBuilder.create().texOffs(18, 33).addBox(0.0F, 0.0F, 0.0F, 2.0F, 5.0F, 4.0F), PartPose.offset(1.0F, 0.0F, 0.0F));
        PartDefinition part_lHalf28 = part_lHalf27.addOrReplaceChild("lHalf28", CubeListBuilder.create().texOffs(0, 51).addBox(-2.0F, 0.0F, -4.0F, 2.0F, 1.0F, 4.0F), PartPose.offsetAndRotation(2.0F, 0.01F, 0.0F, 0.0F, 0.4171337F, 0.0F));
        PartDefinition part_lHalf29 = part_lHalf28.addOrReplaceChild("lHalf29", CubeListBuilder.create().texOffs(8, 46).addBox(-2.0F, 0.0F, -1.0F, 2.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, -4.0F, 0.0F, 0.33161256F, 0.0F));
        PartDefinition part_lHalf30 = part_lHalf29.addOrReplaceChild("lHalf30", CubeListBuilder.create().texOffs(12, 51).addBox(-1.0F, 0.0F, -3.0F, 1.0F, 1.0F, 4.0F), PartPose.offset(0.0F, 0.0F, -2.0F));
        PartDefinition part_lHalf31 = part_lHalf27.addOrReplaceChild("lHalf31", CubeListBuilder.create().texOffs(0, 41).mirror().addBox(0.0F, 0.0F, -4.0F, 2.0F, 1.0F, 4.0F), PartPose.offsetAndRotation(2.0F, 0.01F, 4.0F, 0.0F, -3.5587263F, 0.0F));
        PartDefinition part_lHalf32 = part_lHalf31.addOrReplaceChild("lHalf32", CubeListBuilder.create().texOffs(8, 41).mirror().addBox(0.0F, 0.0F, -1.0F, 2.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.01F, -4.0F, 0.0F, -0.33161256F, 0.0F));
        PartDefinition part_lHalf33 = part_lHalf32.addOrReplaceChild("lHalf33", CubeListBuilder.create().texOffs(12, 41).mirror().addBox(0.0F, 0.0F, -3.0F, 1.0F, 1.0F, 4.0F), PartPose.offset(0.0F, 0.0F, -2.0F));
        PartDefinition part_lHalf37 = part_lHalf27.addOrReplaceChild("lHalf37", CubeListBuilder.create().texOffs(0, 46).mirror().addBox(0.0F, 0.0F, -4.0F, 2.0F, 1.0F, 4.0F), PartPose.offsetAndRotation(2.0F, 3.99F, 4.0F, 0.0F, -3.5587263F, 0.0F));
        PartDefinition part_lHalf38 = part_lHalf37.addOrReplaceChild("lHalf38", CubeListBuilder.create().texOffs(8, 43).mirror().addBox(0.0F, 0.0F, -1.0F, 2.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0F, -0.01F, -4.0F, 0.0F, -0.33161256F, 0.0F));
        PartDefinition part_lHalf39 = part_lHalf38.addOrReplaceChild("lHalf39", CubeListBuilder.create().texOffs(12, 46).mirror().addBox(0.0F, 0.0F, -3.0F, 1.0F, 1.0F, 4.0F), PartPose.offset(0.0F, 0.0F, -2.0F));
        PartDefinition part_lHalf34 = part_lHalf27.addOrReplaceChild("lHalf34", CubeListBuilder.create().texOffs(0, 56).addBox(-2.0F, 0.0F, -4.0F, 2.0F, 2.0F, 4.0F), PartPose.offsetAndRotation(2.0F, 4.0F, 0.0F, 0.0F, 0.4171337F, 0.0F));
        PartDefinition part_lHalf35 = part_lHalf34.addOrReplaceChild("lHalf35", CubeListBuilder.create().texOffs(12, 56).addBox(-2.0F, 0.0F, -3.0F, 2.0F, 2.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, -4.0F, 0.0F, 0.33161256F, 0.0F));
        PartDefinition part_lHalf36 = part_lHalf35.addOrReplaceChild("lHalf36", CubeListBuilder.create().texOffs(15, 33).addBox(-1.0F, 0.0F, -3.0F, 1.0F, 1.0F, 2.0F), PartPose.offset(0.0F, 0.0F, -2.0F));
        PartDefinition part_lHalf12 = part_lHalf1.addOrReplaceChild("lHalf12", CubeListBuilder.create().texOffs(10, 0).addBox(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F), PartPose.offset(0.0F, -3.5F, -3.98F));
        PartDefinition part_lHalf13 = part_lHalf12.addOrReplaceChild("lHalf13", CubeListBuilder.create().texOffs(0, 33).addBox(0.0F, 0.0F, 0.0F, 6.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(1.0F, 0.02F, 0.0F, 0.0F, -1.0471976F, 0.0F));
        PartDefinition part_lHalf14 = part_lHalf13.addOrReplaceChild("lHalf14", CubeListBuilder.create().texOffs(0, 27).addBox(0.0F, 0.0F, 0.0F, 3.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(6.0F, -0.01F, 0.0F, 0.0F, -0.17453292F, 0.0F));
        PartDefinition part_lHalf15 = part_lHalf14.addOrReplaceChild("lHalf15", CubeListBuilder.create().texOffs(18, 20).addBox(0.0F, 0.0F, 0.0F, 4.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(3.0F, -0.01F, 0.0F, 0.0F, -0.34906584F, 0.0F));
        PartDefinition part_lHalf16 = part_lHalf15.addOrReplaceChild("lHalf16", CubeListBuilder.create().texOffs(18, 24).addBox(0.0F, 0.0F, 0.0F, 3.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(4.0F, 0.01F, 0.0F, 0.0F, -0.34906584F, 0.0F));
        PartDefinition part_lHalf17 = part_lHalf16.addOrReplaceChild("lHalf17", CubeListBuilder.create().texOffs(0, 37).addBox(0.0F, 0.0F, 0.0F, 6.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(3.0F, 0.01F, 0.0F, 0.0F, -0.17453292F, 0.0F));
        PartDefinition part_lHalf18 = part_lHalf17.addOrReplaceChild("lHalf18", CubeListBuilder.create().texOffs(10, 2).addBox(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(6.0F, -0.02F, 0.0F, 0.0F, -1.0471976F, 0.0F));
        PartDefinition part_lHalf19 = part_lHalf12.addOrReplaceChild("lHalf19", CubeListBuilder.create().texOffs(0, 20).addBox(2.0F, 0.0F, 0.0F, 3.0F, 1.0F, 12.0F), PartPose.offset(-2.0F, 0.03F, 4.0F));
        PartDefinition part_lHalf2 = part_lHalf1.addOrReplaceChild("lHalf2", CubeListBuilder.create().texOffs(32, 10).addBox(0.0F, 0.0F, 0.0F, 1.0F, 3.0F, 2.0F), PartPose.offset(0.0F, 0.0F, -4.0F));
        PartDefinition part_lHalf3 = part_lHalf2.addOrReplaceChild("lHalf3", CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, 0.0F, 0.0F, 1.0F, 3.0F, 1.0F), PartPose.offset(0.0F, -3.0F, 0.3F));
        PartDefinition part_lHalf4 = part_lHalf2.addOrReplaceChild("lHalf4", CubeListBuilder.create().texOffs(0, 10).addBox(0.0F, 0.0F, 0.0F, 6.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(1.0F, -3.0F, 0.0F, 0.0F, -1.0471976F, 0.0F));
        PartDefinition part_lHalf5 = part_lHalf4.addOrReplaceChild("lHalf5", CubeListBuilder.create().texOffs(0, 25).addBox(-5.0F, -1.0F, 0.0F, 5.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(6.0F, 2.0F, 0.0F, 0.0F, 0.0F, -0.2443461F));
        PartDefinition part_lHalf6 = part_lHalf4.addOrReplaceChild("lHalf6", CubeListBuilder.create().texOffs(36, 0).addBox(-2.0F, 0.0F, 0.0F, 2.0F, 4.0F, 1.0F), PartPose.offset(2.0F, 2.0F, 0.0F));
        PartDefinition part_lHalf7 = part_lHalf4.addOrReplaceChild("lHalf7", CubeListBuilder.create().texOffs(0, 20).addBox(0.0F, 0.0F, 0.0F, 3.0F, 2.0F, 3.0F), PartPose.offsetAndRotation(6.0F, -0.01F, 0.0F, 0.0F, -0.17453292F, 0.0F));
        PartDefinition part_lHalf8 = part_lHalf7.addOrReplaceChild("lHalf8", CubeListBuilder.create().texOffs(18, 9).addBox(0.0F, 0.0F, 0.0F, 4.0F, 2.0F, 3.0F), PartPose.offsetAndRotation(3.0F, -0.01F, 0.0F, 0.0F, -0.34906584F, 0.0F));
        PartDefinition part_lHalf9 = part_lHalf8.addOrReplaceChild("lHalf9", CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, 0.0F, 0.0F, 3.0F, 6.0F, 4.0F), PartPose.offsetAndRotation(4.0F, 0.01F, 0.0F, 0.0F, -0.34906584F, 0.0F));
        PartDefinition part_lHalf10 = part_lHalf9.addOrReplaceChild("lHalf10", CubeListBuilder.create().texOffs(18, 0).addBox(0.0F, 0.0F, 0.0F, 6.0F, 6.0F, 3.0F), PartPose.offsetAndRotation(3.0F, 0.01F, 0.0F, 0.0F, -0.17453292F, 0.0F));
        PartDefinition part_lHalf11 = part_lHalf10.addOrReplaceChild("lHalf11", CubeListBuilder.create().texOffs(36, 5).addBox(0.0F, 0.0F, 0.0F, 1.0F, 6.0F, 1.0F), PartPose.offsetAndRotation(6.0F, -0.02F, 0.0F, 0.0F, -1.0471976F, 0.0F));
        PartDefinition part_lHalf62 = part_lHalf1.addOrReplaceChild("lHalf62", CubeListBuilder.create().texOffs(28, 0).mirror(), PartPose.offset(1.0F, 1.0F, 2.5F));
        PartDefinition part_lHalf63 = part_lHalf62.addOrReplaceChild("lHalf63", CubeListBuilder.create().texOffs(20, 32).mirror().addBox(-1.0F, 0.0F, 0.0F, 1.0F, 1.0F, 10.0F), PartPose.offsetAndRotation(3.0F, -2.0F, -9.5F, 0.0F, 0.0F, 0.61086524F));
        PartDefinition part_lHalf64 = part_lHalf63.addOrReplaceChild("lHalf64", CubeListBuilder.create().texOffs(18, 43).addBox(-1.0F, 0.0F, 0.0F, 1.0F, 1.0F, 10.0F), PartPose.offset(0.0F, 0.5F, 0.0F));
        PartDefinition part_lHalf65 = part_lHalf63.addOrReplaceChild("lHalf65", CubeListBuilder.create().texOffs(19, 54).mirror().addBox(0.0F, 0.0F, -0.3F, 2.0F, 2.0F, 3.0F), PartPose.offsetAndRotation(-1.5F, 0.0F, 10.6F, -1.5707964F, 0.0F, 0.0F));
        PartDefinition part_lHalf68 = part_lHalf62.addOrReplaceChild("lHalf68", CubeListBuilder.create().texOffs(23, 54).mirror().addBox(-2.5F, -0.5F, 0.0F, 3.0F, 3.0F, 6.0F), PartPose.offset(5.0F, 3.5F, -4.5F));
        PartDefinition part_lHalf72 = part_lHalf68.addOrReplaceChild("lHalf72", CubeListBuilder.create().texOffs(56, 14).mirror().addBox(-2.0F, 0.0F, 0.0F, 2.0F, 2.0F, 3.0F), PartPose.offset(1.0F, 1.0F, 1.0F));
        PartDefinition part_lHalf69 = part_lHalf68.addOrReplaceChild("lHalf69", CubeListBuilder.create().texOffs(34, 24).mirror().addBox(-0.5F, 0.1F, 0.0F, 1.0F, 1.0F, 5.0F), PartPose.offset(-1.0F, 1.0F, -5.0F));
        PartDefinition part_lHalf71 = part_lHalf68.addOrReplaceChild("lHalf71", CubeListBuilder.create().texOffs(34, 24).addBox(-0.5F, 0.1F, 0.0F, 1.0F, 1.0F, 5.0F), PartPose.offsetAndRotation(-1.0F, 1.0F, -5.0F, 0.0F, 0.0F, 2.0943952F));
        PartDefinition part_lHalf70 = part_lHalf68.addOrReplaceChild("lHalf70", CubeListBuilder.create().texOffs(34, 24).mirror().addBox(-0.5F, 0.1F, 0.0F, 1.0F, 1.0F, 5.0F), PartPose.offsetAndRotation(-1.0F, 1.0F, -5.0F, 0.0F, 0.0F, -2.0943952F));
        PartDefinition part_lHalf73 = part_lHalf68.addOrReplaceChild("lHalf73", CubeListBuilder.create().texOffs(8, 51).mirror().addBox(-2.0F, 0.0F, 0.0F, 2.0F, 2.0F, 2.0F), PartPose.offset(0.0F, 0.0F, 6.0F));
        PartDefinition part_lHalf74 = part_lHalf68.addOrReplaceChild("lHalf74", CubeListBuilder.create().texOffs(24, 24).mirror().addBox(0.0F, 0.0F, 0.0F, 2.0F, 2.0F, 6.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 3.5F, 0.7853982F, -1.5707964F, 0.0F));
        PartDefinition part_lHalf66 = part_lHalf62.addOrReplaceChild("lHalf66", CubeListBuilder.create().texOffs(20, 32).addBox(-1.0F, -1.0F, 0.0F, 1.0F, 1.0F, 10.0F), PartPose.offsetAndRotation(3.0F, 2.5F, -9.5F, 0.0F, 0.0F, -0.61086524F));
        PartDefinition part_lHalf67 = part_lHalf66.addOrReplaceChild("lHalf67", CubeListBuilder.create().texOffs(18, 43).addBox(-1.0F, 0.0F, 0.0F, 1.0F, 1.0F, 10.0F), PartPose.offset(0.0F, -1.5F, 0.0F));
        PartDefinition part_lHalf46 = part_lHalf1.addOrReplaceChild("lHalf46", CubeListBuilder.create().texOffs(16, 25), PartPose.offsetAndRotation(4.7F, 0.0F, 4.4F, 0.0F, 0.17453292F, 0.0F));
        PartDefinition part_lHalf52 = part_lHalf46.addOrReplaceChild("lHalf52", CubeListBuilder.create().texOffs(45, 15).addBox(0.0F, 0.0F, -4.0F, 1.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(0.5F, -2.0F, 1.0F, -0.61086524F, 0.0F, 0.0F));
        PartDefinition part_lHalf55 = part_lHalf52.addOrReplaceChild("lHalf55", CubeListBuilder.create().texOffs(40, 15).addBox(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 3.0F), PartPose.offset(-0.2F, -0.2F, -2.0F));
        PartDefinition part_lHalf56 = part_lHalf52.addOrReplaceChild("lHalf56", CubeListBuilder.create().texOffs(37, 19).addBox(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 3.0F), PartPose.offset(-0.2F, 0.2F, -2.0F));
        PartDefinition part_lHalf53 = part_lHalf52.addOrReplaceChild("lHalf53", CubeListBuilder.create().texOffs(45, 19).addBox(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 3.0F), PartPose.offset(0.2F, 0.2F, -2.0F));
        PartDefinition part_lHalf54 = part_lHalf52.addOrReplaceChild("lHalf54", CubeListBuilder.create().texOffs(48, 15).addBox(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 3.0F), PartPose.offset(0.2F, -0.2F, -2.0F));
        PartDefinition part_lHalf57 = part_lHalf46.addOrReplaceChild("lHalf57", CubeListBuilder.create().texOffs(45, 15).addBox(0.0F, 0.0F, -4.0F, 1.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(0.5F, -2.5F, 3.0F, -0.6981317F, 0.0F, 0.0F));
        PartDefinition part_lHalf61 = part_lHalf57.addOrReplaceChild("lHalf61", CubeListBuilder.create().texOffs(37, 19).addBox(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 3.0F), PartPose.offset(-0.2F, 0.2F, -2.0F));
        PartDefinition part_lHalf58 = part_lHalf57.addOrReplaceChild("lHalf58", CubeListBuilder.create().texOffs(45, 19).addBox(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 3.0F), PartPose.offset(0.2F, 0.2F, -2.0F));
        PartDefinition part_lHalf60 = part_lHalf57.addOrReplaceChild("lHalf60", CubeListBuilder.create().texOffs(40, 15).addBox(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 3.0F), PartPose.offset(-0.2F, -0.2F, -2.0F));
        PartDefinition part_lHalf59 = part_lHalf57.addOrReplaceChild("lHalf59", CubeListBuilder.create().texOffs(48, 15).addBox(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 3.0F), PartPose.offset(0.2F, -0.2F, -2.0F));
        PartDefinition part_lHalf47 = part_lHalf46.addOrReplaceChild("lHalf47", CubeListBuilder.create().texOffs(45, 15).addBox(0.0F, 0.0F, -4.0F, 1.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(0.5F, -1.5F, -1.0F, -0.4886922F, 0.0F, 0.0F));
        PartDefinition part_lHalf51 = part_lHalf47.addOrReplaceChild("lHalf51", CubeListBuilder.create().texOffs(37, 19).addBox(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 3.0F), PartPose.offset(-0.2F, 0.2F, -2.0F));
        PartDefinition part_lHalf48 = part_lHalf47.addOrReplaceChild("lHalf48", CubeListBuilder.create().texOffs(45, 19).addBox(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 3.0F), PartPose.offset(0.2F, 0.2F, -2.0F));
        PartDefinition part_lHalf50 = part_lHalf47.addOrReplaceChild("lHalf50", CubeListBuilder.create().texOffs(40, 15).addBox(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 3.0F), PartPose.offset(-0.2F, -0.2F, -2.0F));
        PartDefinition part_lHalf49 = part_lHalf47.addOrReplaceChild("lHalf49", CubeListBuilder.create().texOffs(48, 15).addBox(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 3.0F), PartPose.offset(0.2F, -0.2F, -2.0F));
        PartDefinition part_rHalf1 = root.addOrReplaceChild("rHalf1", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-2.0F, -3.02F, -2.2F, 2.0F, 6.0F, 14.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition part_rHalf12 = part_rHalf1.addOrReplaceChild("rHalf12", CubeListBuilder.create().texOffs(10, 0).mirror().addBox(-1.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F), PartPose.offset(0.0F, -3.5F, -3.98F));
        PartDefinition part_rHalf19 = part_rHalf12.addOrReplaceChild("rHalf19", CubeListBuilder.create().texOffs(0, 20).mirror().addBox(-5.0F, 0.0F, 0.0F, 3.0F, 1.0F, 12.0F), PartPose.offset(2.0F, 0.03F, 4.0F));
        PartDefinition part_rHalf13 = part_rHalf12.addOrReplaceChild("rHalf13", CubeListBuilder.create().texOffs(0, 33).mirror().addBox(-6.0F, 0.0F, 0.0F, 6.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(-1.0F, 0.02F, 0.0F, 0.0F, 1.0471976F, 0.0F));
        PartDefinition part_rHalf14 = part_rHalf13.addOrReplaceChild("rHalf14", CubeListBuilder.create().texOffs(0, 27).mirror().addBox(-3.0F, 0.0F, 0.0F, 3.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(-6.0F, -0.01F, 0.0F, 0.0F, 0.17453292F, 0.0F));
        PartDefinition part_rHalf15 = part_rHalf14.addOrReplaceChild("rHalf15", CubeListBuilder.create().texOffs(18, 20).mirror().addBox(-4.0F, 0.0F, 0.0F, 4.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(-3.0F, -0.01F, 0.0F, 0.0F, 0.34906584F, 0.0F));
        PartDefinition part_rHalf16 = part_rHalf15.addOrReplaceChild("rHalf16", CubeListBuilder.create().texOffs(18, 24).mirror().addBox(-3.0F, 0.0F, 0.0F, 3.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(-4.0F, 0.01F, 0.0F, 0.0F, 0.34906584F, 0.0F));
        PartDefinition part_rHalf17 = part_rHalf16.addOrReplaceChild("rHalf17", CubeListBuilder.create().texOffs(0, 37).mirror().addBox(-6.0F, 0.0F, 0.0F, 6.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(-3.0F, 0.01F, 0.0F, 0.0F, 0.17453292F, 0.0F));
        PartDefinition part_rHalf18 = part_rHalf17.addOrReplaceChild("rHalf18", CubeListBuilder.create().texOffs(10, 2).mirror().addBox(-1.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(-6.0F, -0.02F, 0.0F, 0.0F, 1.0471976F, 0.0F));
        PartDefinition part_rHalf20 = part_rHalf1.addOrReplaceChild("rHalf20", CubeListBuilder.create().texOffs(60, 0).mirror().addBox(-1.0F, 0.0F, 0.0F, 1.0F, 5.0F, 4.0F), PartPose.offset(-7.0F, -2.5F, 4.0F));
        PartDefinition part_rHalf27 = part_rHalf20.addOrReplaceChild("rHalf27", CubeListBuilder.create().texOffs(18, 33).mirror().addBox(-2.0F, 0.0F, 0.0F, 2.0F, 5.0F, 4.0F), PartPose.offset(-1.0F, 0.0F, 0.0F));
        PartDefinition part_rHalf28 = part_rHalf27.addOrReplaceChild("rHalf28", CubeListBuilder.create().texOffs(0, 51).mirror().addBox(0.0F, 0.0F, -4.0F, 2.0F, 1.0F, 4.0F), PartPose.offsetAndRotation(-2.0F, 0.01F, 0.0F, 0.0F, -0.4171337F, 0.0F));
        PartDefinition part_rHalf29 = part_rHalf28.addOrReplaceChild("rHalf29", CubeListBuilder.create().texOffs(8, 46).mirror().addBox(0.0F, 0.0F, -1.0F, 2.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, -4.0F, 0.0F, -0.33161256F, 0.0F));
        PartDefinition part_rHalf30 = part_rHalf29.addOrReplaceChild("rHalf30", CubeListBuilder.create().texOffs(12, 51).mirror().addBox(0.0F, 0.0F, -3.0F, 1.0F, 1.0F, 4.0F), PartPose.offset(0.0F, 0.0F, -2.0F));
        PartDefinition part_rHalf37 = part_rHalf27.addOrReplaceChild("rHalf37", CubeListBuilder.create().texOffs(0, 46).addBox(-2.0F, 0.0F, -4.0F, 2.0F, 1.0F, 4.0F), PartPose.offsetAndRotation(-2.0F, 3.99F, 4.0F, 0.0F, 3.5587263F, 0.0F));
        PartDefinition part_rHalf38 = part_rHalf37.addOrReplaceChild("rHalf38", CubeListBuilder.create().texOffs(8, 43).addBox(-2.0F, 0.0F, -1.0F, 2.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0F, -0.01F, -4.0F, 0.0F, 0.33161256F, 0.0F));
        PartDefinition part_rHalf39 = part_rHalf38.addOrReplaceChild("rHalf39", CubeListBuilder.create().texOffs(12, 46).addBox(-1.0F, 0.0F, -3.0F, 1.0F, 1.0F, 4.0F), PartPose.offset(0.0F, 0.0F, -2.0F));
        PartDefinition part_rHalf31 = part_rHalf27.addOrReplaceChild("rHalf31", CubeListBuilder.create().texOffs(0, 41).addBox(-2.0F, 0.0F, -4.0F, 2.0F, 1.0F, 4.0F), PartPose.offsetAndRotation(-2.0F, 0.01F, 4.0F, 0.0F, 3.5587263F, 0.0F));
        PartDefinition part_rHalf32 = part_rHalf31.addOrReplaceChild("rHalf32", CubeListBuilder.create().texOffs(8, 41).addBox(-2.0F, 0.0F, -1.0F, 2.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.01F, -4.0F, 0.0F, 0.33161256F, 0.0F));
        PartDefinition part_rHalf33 = part_rHalf32.addOrReplaceChild("rHalf33", CubeListBuilder.create().texOffs(12, 41).addBox(-1.0F, 0.0F, -3.0F, 1.0F, 1.0F, 4.0F), PartPose.offset(0.0F, 0.0F, -2.0F));
        PartDefinition part_rHalf34 = part_rHalf27.addOrReplaceChild("rHalf34", CubeListBuilder.create().texOffs(0, 56).mirror().addBox(0.0F, 0.0F, -4.0F, 2.0F, 2.0F, 4.0F), PartPose.offsetAndRotation(-2.0F, 4.0F, 0.0F, 0.0F, -0.4171337F, 0.0F));
        PartDefinition part_rHalf35 = part_rHalf34.addOrReplaceChild("rHalf35", CubeListBuilder.create().texOffs(12, 56).mirror().addBox(0.0F, 0.0F, -3.0F, 2.0F, 2.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, -4.0F, 0.0F, -0.33161256F, 0.0F));
        PartDefinition part_rHalf36 = part_rHalf35.addOrReplaceChild("rHalf36", CubeListBuilder.create().texOffs(15, 33).mirror().addBox(0.0F, 0.0F, -3.0F, 1.0F, 1.0F, 2.0F), PartPose.offset(0.0F, 0.0F, -2.0F));
        PartDefinition part_rHalf21 = part_rHalf20.addOrReplaceChild("rHalf21", CubeListBuilder.create().texOffs(70, 0).mirror().addBox(0.0F, 0.0F, -4.0F, 1.0F, 5.0F, 4.0F), PartPose.offsetAndRotation(-1.0F, 0.0F, 0.0F, 0.0F, -0.2617994F, 0.0F));
        PartDefinition part_rHalf22 = part_rHalf21.addOrReplaceChild("rHalf22", CubeListBuilder.create().texOffs(80, 0).mirror().addBox(0.0F, 0.0F, -4.0F, 1.0F, 5.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 0.0F, -4.0F, 0.0F, -0.2617994F, 0.0F));
        PartDefinition part_rHalf23 = part_rHalf22.addOrReplaceChild("rHalf23", CubeListBuilder.create().texOffs(50, 9).mirror().addBox(0.0F, 0.0F, 0.0F, 1.0F, 3.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 1.0F, -4.0F, 0.0F, -0.2268928F, 0.0F));
        PartDefinition part_rHalf24 = part_rHalf23.addOrReplaceChild("rHalf24", CubeListBuilder.create().texOffs(58, 9).mirror().addBox(0.0F, 0.0F, 0.0F, 1.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 3.0F, 0.0F, 0.2268928F, 0.0F));
        PartDefinition part_rHalf25 = part_rHalf24.addOrReplaceChild("rHalf25", CubeListBuilder.create().texOffs(64, 9).mirror().addBox(0.0F, 0.0F, 0.0F, 1.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 2.0F, 0.0F, 0.15707964F, 0.0F));
        PartDefinition part_rHalf26 = part_rHalf25.addOrReplaceChild("rHalf26", CubeListBuilder.create().texOffs(70, 9).mirror().addBox(0.0F, 0.0F, 0.0F, 1.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 2.0F, 0.0F, 0.17453292F, 0.0F));
        PartDefinition part_rHalf40 = part_rHalf20.addOrReplaceChild("rHalf40", CubeListBuilder.create().texOffs(50, 0).addBox(-1.0F, 0.0F, -4.0F, 1.0F, 5.0F, 4.0F), PartPose.offsetAndRotation(-1.0F, 0.0F, 4.0F, 0.0F, -2.8797932F, 0.0F));
        PartDefinition part_rHalf41 = part_rHalf40.addOrReplaceChild("rHalf41", CubeListBuilder.create().texOffs(40, 1).addBox(-1.0F, 0.0F, -4.0F, 1.0F, 5.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 0.0F, -4.0F, 0.0F, 0.2617994F, 0.0F));
        PartDefinition part_rHalf42 = part_rHalf41.addOrReplaceChild("rHalf42", CubeListBuilder.create().texOffs(29, 17).addBox(-1.0F, 0.0F, 0.0F, 1.0F, 3.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 1.0F, -4.0F, 0.0F, 0.2268928F, 0.0F));
        PartDefinition part_rHalf43 = part_rHalf42.addOrReplaceChild("rHalf43", CubeListBuilder.create().texOffs(38, 10).addBox(-1.0F, 0.0F, 0.0F, 1.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 3.0F, 0.0F, -0.2268928F, 0.0F));
        PartDefinition part_rHalf44 = part_rHalf43.addOrReplaceChild("rHalf44", CubeListBuilder.create().texOffs(44, 10).addBox(-1.0F, 0.0F, 0.0F, 1.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 2.0F, 0.0F, -0.15707964F, 0.0F));
        PartDefinition part_rHalf45 = part_rHalf44.addOrReplaceChild("rHalf45", CubeListBuilder.create().texOffs(34, 15).addBox(-1.0F, 0.0F, 0.0F, 1.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 2.0F, 0.0F, -0.17453292F, 0.0F));
        PartDefinition part_rHalf46 = part_rHalf1.addOrReplaceChild("rHalf46", CubeListBuilder.create().texOffs(16, 25).mirror(), PartPose.offsetAndRotation(-4.7F, 0.0F, 4.4F, 0.0F, -0.17453292F, 0.0F));
        PartDefinition part_rHalf47 = part_rHalf46.addOrReplaceChild("rHalf47", CubeListBuilder.create().texOffs(45, 15).mirror().addBox(-1.0F, 0.0F, -4.0F, 1.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(-0.5F, -1.5F, -1.0F, -0.4886922F, 0.0F, 0.0F));
        PartDefinition part_rHalf50 = part_rHalf47.addOrReplaceChild("rHalf50", CubeListBuilder.create().texOffs(40, 15).mirror().addBox(-1.0F, 0.0F, 0.0F, 1.0F, 1.0F, 3.0F), PartPose.offset(0.2F, -0.2F, -2.0F));
        PartDefinition part_rHalf48 = part_rHalf47.addOrReplaceChild("rHalf48", CubeListBuilder.create().texOffs(45, 19).mirror().addBox(-1.0F, 0.0F, 0.0F, 1.0F, 1.0F, 3.0F), PartPose.offset(-0.2F, 0.2F, -2.0F));
        PartDefinition part_rHalf49 = part_rHalf47.addOrReplaceChild("rHalf49", CubeListBuilder.create().texOffs(48, 15).mirror().addBox(-1.0F, 0.0F, 0.0F, 1.0F, 1.0F, 3.0F), PartPose.offset(-0.2F, -0.2F, -2.0F));
        PartDefinition part_rHalf51 = part_rHalf47.addOrReplaceChild("rHalf51", CubeListBuilder.create().texOffs(37, 19).mirror().addBox(-1.0F, 0.0F, 0.0F, 1.0F, 1.0F, 3.0F), PartPose.offset(0.2F, 0.2F, -2.0F));
        PartDefinition part_rHalf52 = part_rHalf46.addOrReplaceChild("rHalf52", CubeListBuilder.create().texOffs(45, 15).mirror().addBox(-1.0F, 0.0F, -4.0F, 1.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(-0.5F, -2.0F, 1.0F, -0.61086524F, 0.0F, 0.0F));
        PartDefinition part_rHalf54 = part_rHalf52.addOrReplaceChild("rHalf54", CubeListBuilder.create().texOffs(48, 15).mirror().addBox(-1.0F, 0.0F, 0.0F, 1.0F, 1.0F, 3.0F), PartPose.offset(-0.2F, -0.2F, -2.0F));
        PartDefinition part_rHalf55 = part_rHalf52.addOrReplaceChild("rHalf55", CubeListBuilder.create().texOffs(40, 15).mirror().addBox(-1.0F, 0.0F, 0.0F, 1.0F, 1.0F, 3.0F), PartPose.offset(0.2F, -0.2F, -2.0F));
        PartDefinition part_rHalf56 = part_rHalf52.addOrReplaceChild("rHalf56", CubeListBuilder.create().texOffs(37, 19).mirror().addBox(-1.0F, 0.0F, 0.0F, 1.0F, 1.0F, 3.0F), PartPose.offset(0.2F, 0.2F, -2.0F));
        PartDefinition part_rHalf53 = part_rHalf52.addOrReplaceChild("rHalf53", CubeListBuilder.create().texOffs(45, 19).mirror().addBox(-1.0F, 0.0F, 0.0F, 1.0F, 1.0F, 3.0F), PartPose.offset(-0.2F, 0.2F, -2.0F));
        PartDefinition part_rHalf57 = part_rHalf46.addOrReplaceChild("rHalf57", CubeListBuilder.create().texOffs(45, 15).mirror().addBox(-1.0F, 0.0F, -4.0F, 1.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(-0.5F, -2.5F, 3.0F, -0.6981317F, 0.0F, 0.0F));
        PartDefinition part_rHalf58 = part_rHalf57.addOrReplaceChild("rHalf58", CubeListBuilder.create().texOffs(45, 19).mirror().addBox(-1.0F, 0.0F, 0.0F, 1.0F, 1.0F, 3.0F), PartPose.offset(-0.2F, 0.2F, -2.0F));
        PartDefinition part_rHalf61 = part_rHalf57.addOrReplaceChild("rHalf61", CubeListBuilder.create().texOffs(37, 19).mirror().addBox(-1.0F, 0.0F, 0.0F, 1.0F, 1.0F, 3.0F), PartPose.offset(0.2F, 0.2F, -2.0F));
        PartDefinition part_rHalf59 = part_rHalf57.addOrReplaceChild("rHalf59", CubeListBuilder.create().texOffs(48, 15).mirror().addBox(-1.0F, 0.0F, 0.0F, 1.0F, 1.0F, 3.0F), PartPose.offset(-0.2F, -0.2F, -2.0F));
        PartDefinition part_rHalf60 = part_rHalf57.addOrReplaceChild("rHalf60", CubeListBuilder.create().texOffs(40, 15).mirror().addBox(-1.0F, 0.0F, 0.0F, 1.0F, 1.0F, 3.0F), PartPose.offset(0.2F, -0.2F, -2.0F));
        PartDefinition part_rHalf2 = part_rHalf1.addOrReplaceChild("rHalf2", CubeListBuilder.create().texOffs(32, 10).mirror().addBox(-1.0F, 0.0F, 0.0F, 1.0F, 3.0F, 2.0F), PartPose.offset(0.0F, 0.0F, -4.0F));
        PartDefinition part_rHalf4 = part_rHalf2.addOrReplaceChild("rHalf4", CubeListBuilder.create().texOffs(0, 10).mirror().addBox(-6.0F, 0.0F, 0.0F, 6.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(-1.0F, -3.0F, 0.0F, 0.0F, 1.0471976F, 0.0F));
        PartDefinition part_rHalf6 = part_rHalf4.addOrReplaceChild("rHalf6", CubeListBuilder.create().texOffs(36, 0).mirror().addBox(0.0F, 0.0F, 0.0F, 2.0F, 4.0F, 1.0F), PartPose.offset(-2.0F, 2.0F, 0.0F));
        PartDefinition part_rHalf5 = part_rHalf4.addOrReplaceChild("rHalf5", CubeListBuilder.create().texOffs(0, 25).mirror().addBox(0.0F, -1.0F, 0.0F, 5.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(-6.0F, 2.0F, 0.0F, 0.0F, 0.0F, 0.2443461F));
        PartDefinition part_rHalf7 = part_rHalf4.addOrReplaceChild("rHalf7", CubeListBuilder.create().texOffs(0, 20).mirror().addBox(-3.0F, 0.0F, 0.0F, 3.0F, 2.0F, 3.0F), PartPose.offsetAndRotation(-6.0F, -0.01F, 0.0F, 0.0F, 0.17453292F, 0.0F));
        PartDefinition part_rHalf8 = part_rHalf7.addOrReplaceChild("rHalf8", CubeListBuilder.create().texOffs(18, 9).mirror().addBox(-4.0F, 0.0F, 0.0F, 4.0F, 2.0F, 3.0F), PartPose.offsetAndRotation(-3.0F, -0.01F, 0.0F, 0.0F, 0.34906584F, 0.0F));
        PartDefinition part_rHalf9 = part_rHalf8.addOrReplaceChild("rHalf9", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-3.0F, 0.0F, 0.0F, 3.0F, 6.0F, 4.0F), PartPose.offsetAndRotation(-4.0F, 0.01F, 0.0F, 0.0F, 0.34906584F, 0.0F));
        PartDefinition part_rHalf10 = part_rHalf9.addOrReplaceChild("rHalf10", CubeListBuilder.create().texOffs(18, 0).mirror().addBox(-6.0F, 0.0F, 0.0F, 6.0F, 6.0F, 3.0F), PartPose.offsetAndRotation(-3.0F, 0.01F, 0.0F, 0.0F, 0.17453292F, 0.0F));
        PartDefinition part_rHalf11 = part_rHalf10.addOrReplaceChild("rHalf11", CubeListBuilder.create().texOffs(36, 5).mirror().addBox(-1.0F, 0.0F, 0.0F, 1.0F, 6.0F, 1.0F), PartPose.offsetAndRotation(-6.0F, -0.02F, 0.0F, 0.0F, 1.0471976F, 0.0F));
        PartDefinition part_rHalf3 = part_rHalf2.addOrReplaceChild("rHalf3", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-1.0F, 0.0F, 0.0F, 1.0F, 3.0F, 1.0F), PartPose.offset(0.0F, -3.0F, 0.3F));
        PartDefinition part_rHalf62 = part_rHalf1.addOrReplaceChild("rHalf62", CubeListBuilder.create().texOffs(28, 0), PartPose.offset(-1.0F, 1.0F, 2.5F));
        PartDefinition part_rHalf66 = part_rHalf62.addOrReplaceChild("rHalf66", CubeListBuilder.create().texOffs(20, 32).mirror().addBox(0.0F, -1.0F, 0.0F, 1.0F, 1.0F, 10.0F), PartPose.offsetAndRotation(-3.0F, 2.5F, -9.5F, 0.0F, 0.0F, 0.61086524F));
        PartDefinition part_rHalf67 = part_rHalf66.addOrReplaceChild("rHalf67", CubeListBuilder.create().texOffs(18, 43).mirror().addBox(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 10.0F), PartPose.offset(0.0F, -1.5F, 0.0F));
        PartDefinition part_rHalf63 = part_rHalf62.addOrReplaceChild("rHalf63", CubeListBuilder.create().texOffs(20, 32).addBox(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 10.0F), PartPose.offsetAndRotation(-3.0F, -2.0F, -9.5F, 0.0F, 0.0F, -0.61086524F));
        PartDefinition part_rHalf65 = part_rHalf63.addOrReplaceChild("rHalf65", CubeListBuilder.create().texOffs(19, 54).addBox(-2.0F, 0.0F, -0.3F, 2.0F, 2.0F, 3.0F), PartPose.offsetAndRotation(1.5F, 0.0F, 10.6F, -1.5707964F, 0.0F, 0.0F));
        PartDefinition part_rHalf64 = part_rHalf63.addOrReplaceChild("rHalf64", CubeListBuilder.create().texOffs(18, 43).mirror().addBox(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 10.0F), PartPose.offset(0.0F, 0.5F, 0.0F));
        PartDefinition part_rHalf68 = part_rHalf62.addOrReplaceChild("rHalf68", CubeListBuilder.create().texOffs(23, 54).addBox(-0.5F, -0.5F, 0.0F, 3.0F, 3.0F, 6.0F), PartPose.offset(-5.0F, 3.5F, -4.5F));
        PartDefinition part_rHalf72 = part_rHalf68.addOrReplaceChild("rHalf72", CubeListBuilder.create().texOffs(56, 14).addBox(0.0F, 0.0F, 0.0F, 2.0F, 2.0F, 3.0F), PartPose.offset(-1.0F, 1.0F, 1.0F));
        PartDefinition part_rHalf69 = part_rHalf68.addOrReplaceChild("rHalf69", CubeListBuilder.create().texOffs(34, 24).addBox(-0.5F, 0.1F, 0.0F, 1.0F, 1.0F, 5.0F), PartPose.offset(1.0F, 1.0F, -5.0F));
        PartDefinition part_rHalf71 = part_rHalf68.addOrReplaceChild("rHalf71", CubeListBuilder.create().texOffs(34, 24).mirror().addBox(-0.5F, 0.1F, 0.0F, 1.0F, 1.0F, 5.0F), PartPose.offsetAndRotation(1.0F, 1.0F, -5.0F, 0.0F, 0.0F, -2.0943952F));
        PartDefinition part_rHalf74 = part_rHalf68.addOrReplaceChild("rHalf74", CubeListBuilder.create().texOffs(24, 24).addBox(-2.0F, 0.0F, 0.0F, 2.0F, 2.0F, 6.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 3.5F, 0.7853982F, 1.5707964F, 0.0F));
        PartDefinition part_rHalf73 = part_rHalf68.addOrReplaceChild("rHalf73", CubeListBuilder.create().texOffs(8, 51).addBox(0.0F, 0.0F, 0.0F, 2.0F, 2.0F, 2.0F), PartPose.offset(0.0F, 0.0F, 6.0F));
        PartDefinition part_rHalf70 = part_rHalf68.addOrReplaceChild("rHalf70", CubeListBuilder.create().texOffs(34, 24).addBox(-0.5F, 0.1F, 0.0F, 1.0F, 1.0F, 5.0F), PartPose.offsetAndRotation(1.0F, 1.0F, -5.0F, 0.0F, 0.0F, 2.0943952F));
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void setupAnim(IllusionDroneEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)
    {
        float spin = entity.isShooting() ? 0.12F : 0.035F;
        left.yRot = Mth.sin(ageInTicks * spin) * 0.06F;
        right.yRot = -left.yRot;
        left.xRot = right.xRot = 0.0F;
        left.zRot = right.zRot = 0.0F;
        alpha = entity.isTargetless() ? 0.35F : 1.0F;
    }

    @Override
    public void renderToBuffer(com.mojang.blaze3d.vertex.PoseStack pose, com.mojang.blaze3d.vertex.VertexConsumer vertex, int light, int overlay, float red, float green, float blue, float ignoredAlpha)
    {
        left.render(pose, vertex, light, overlay, red, green, blue, alpha * ignoredAlpha);
        right.render(pose, vertex, light, overlay, red, green, blue, alpha * ignoredAlpha);
    }
}

