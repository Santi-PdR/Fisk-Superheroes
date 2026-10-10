package com.fiskmods.heroes.client.render;

import com.fiskmods.heroes.common.entity.arrow.TrickArrowEntity;
import com.fiskmods.heroes.common.hero.HeroIteration;
import com.fiskmods.heroes.common.hero.HeroTracker;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.world.entity.Entity;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

public class TrickArrowRenderer extends ArrowRenderer<TrickArrowEntity>
{
    private static final ResourceLocation TEXTURE = new ResourceLocation("fiskheroes", "textures/heroes/arrow/arrow.png");

    public TrickArrowRenderer(EntityRendererProvider.Context context)
    {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(TrickArrowEntity entity)
    {
        Entity shooter = entity.getOwner();
        HeroIteration iteration = shooter != null ? HeroTracker.getHero(shooter) : null;
        HeroModelData model = iteration != null ? HeroModelRegistry.get(iteration) : null;
        if (model != null)
        {
            JsonObject archery = model.getCustom().get("fiskheroes:archery");
            JsonElement configured = archery != null ? archery.get("textureArrow") : null;
            if (configured != null && configured.isJsonPrimitive() && configured.getAsJsonPrimitive().isString())
            {
                ResourceLocation texture = model.resolveCustomTexture(configured.getAsString(), shooter, -1);
                if (texture != null) return texture;
            }
        }
        return TEXTURE;
    }

    /** The original used a dedicated arrow renderer for each payload (TNT, firework, pearl, etc.).
     * Keep the hero's arrow skin as the shaft, and render the matching payload at its head. */
    @Override
    public void render(TrickArrowEntity arrow, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource buffers, int packedLight)
    {
        super.render(arrow, entityYaw, partialTick, poseStack, buffers, packedLight);

        ItemStack payload = payload(arrow);
        if (payload.isEmpty()) return;

        poseStack.pushPose();
        float yaw = net.minecraft.util.Mth.rotLerp(partialTick, arrow.yRotO, arrow.getYRot());
        float pitch = net.minecraft.util.Mth.lerp(partialTick, arrow.xRotO, arrow.getXRot());
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(pitch));
        poseStack.translate(0.0D, -0.12D, 0.0D);
        poseStack.scale(0.32F, 0.32F, 0.32F);
        net.minecraft.client.Minecraft.getInstance().getItemRenderer().renderStatic(payload,
                net.minecraft.world.item.ItemDisplayContext.GROUND, packedLight,
                OverlayTexture.NO_OVERLAY, poseStack, buffers, arrow.level(), arrow.getId());
        poseStack.popPose();
    }

    private static ItemStack payload(TrickArrowEntity arrow)
    {
        if ("firework".equals(arrow.getArrowType()) && !arrow.getFireworkStack().isEmpty())
        {
            return arrow.getFireworkStack().copy();
        }

        return switch (arrow.getArrowType())
        {
            case "explosive", "triple_explosive", "detonator" -> new ItemStack(Blocks.TNT);
            case "blaze" -> new ItemStack(Items.FIRE_CHARGE);
            case "boxing_glove" -> new ItemStack(Items.RED_WOOL);
            case "cactus" -> new ItemStack(Blocks.CACTUS);
            case "carrot" -> new ItemStack(Items.CARROT);
            case "ender_pearl" -> new ItemStack(Items.ENDER_PEARL);
            case "excessive" -> new ItemStack(Items.GOLDEN_APPLE);
            case "fire_charge", "fireball" -> new ItemStack(Items.FIRE_CHARGE);
            case "firework" -> new ItemStack(Items.FIREWORK_ROCKET);
            case "gross" -> new ItemStack(Items.POTION);
            case "vial" -> arrow.getVialPotion().isEmpty()
                    ? com.fiskmods.heroes.common.item.ItemTrickArrow.getAttachedPotion(
                            com.fiskmods.heroes.common.item.ItemTrickArrow.createStack("vial"))
                    : arrow.getVialPotion();
            case "phantom" -> new ItemStack(Items.PHANTOM_MEMBRANE);
            case "pufferfish", "explosive_pufferfish" -> new ItemStack(Items.PUFFERFISH);
            case "pulse" -> new ItemStack(Items.REDSTONE_TORCH);
            case "slime" -> new ItemStack(Items.SLIME_BALL);
            case "smoke_bomb" -> com.fiskmods.heroes.common.item.ModItems.SMOKE_PELLET.get().getDefaultInstance();
            case "sponge" -> new ItemStack(Blocks.SPONGE);
            case "torch" -> new ItemStack(Blocks.TORCH);
            case "grappling_hook" -> new ItemStack(Items.CHAIN);
            case "tutridium" -> com.fiskmods.heroes.common.item.ModItems.TUTRIDIUM_GEM.get().getDefaultInstance();
            case "vibranium" -> com.fiskmods.heroes.common.item.ModItems.VIBRANIUM_INGOT.get().getDefaultInstance();
            case "vine" -> new ItemStack(Blocks.VINE);
            default -> ItemStack.EMPTY;
        };
    }
}
