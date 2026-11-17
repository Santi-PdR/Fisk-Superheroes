package com.fiskmods.heroes.client.render;

import com.fiskmods.heroes.common.hero.HeroIteration;
import com.fiskmods.heroes.common.hero.ItemHeroArmor;
import com.fiskmods.heroes.common.item.ItemFlashRing;
import com.fiskmods.heroes.common.item.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Miniature 3D hero suit preview renderer ported from RenderItemMiniSuit.
 * Renders the stored hero's actual 3D suit textures at miniature scale!
 */
public final class MiniSuitItemRenderer extends BlockEntityWithoutLevelRenderer
{
    private static MiniSuitItemRenderer instance;
    private static final java.util.Set<String> DIAGNOSTICS = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final PlayerModel<?> playerModel;

    private MiniSuitItemRenderer()
    {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        this.playerModel = new PlayerModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
        this.playerModel.young = false;
    }

    public static MiniSuitItemRenderer getInstance()
    {
        if (instance == null) instance = new MiniSuitItemRenderer();
        return instance;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
            MultiBufferSource buffers, int light, int overlay)
    {
        ItemStack[] stored = ItemFlashRing.getContainedArmor(stack);
        if (stored == null)
        {
            // An empty miniature cannot deploy a suit: do not depict a fully equipped Atom.
            // Render a neutral icon instead of silently producing an invisible item.
            pose.pushPose();
            pose.translate(0.5D, 0.5D, 0.5D);
            Minecraft.getInstance().getItemRenderer().renderStatic(new ItemStack(ModItems.SUIT_CORE.get()),
                    context, light, overlay, pose, buffers, Minecraft.getInstance().level, 0);
            pose.popPose();
            return;
        }

        if (Boolean.getBoolean("fiskheroes.ciMiniSuitDiagnostics"))
        {
            StringBuilder pieces = new StringBuilder();
            for (int slot = 0; slot < stored.length; ++slot)
            {
                HeroIteration hero = ItemHeroArmor.getHero(stored[slot]);
                if (hero != null && stored[slot].getItem() instanceof ItemHeroArmor armor
                        && armor.getSlot() == slot && hero.hasPieceOfSet(slot))
                    pieces.append(slot).append('=').append(hero.getRegistryName()).append(';');
            }
            String key = context + "|" + pieces;
            if (DIAGNOSTICS.add(key))
                com.fiskmods.heroes.FiskHeroes.LOGGER.info("Mini suit preview: context={}, pieces={}", context, pieces);
        }
        pose.pushPose();
        switch (context)
        {
            case GUI ->
            {
                pose.translate(0.5D, 0.85D, 0.5D);
                pose.mulPose(Axis.XP.rotationDegrees(20.0F));
                pose.mulPose(Axis.YP.rotationDegrees(210.0F));
                pose.scale(0.46F, -0.46F, -0.46F);
            }
            case FIRST_PERSON_RIGHT_HAND, FIRST_PERSON_LEFT_HAND ->
            {
                pose.translate(context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND ? 0.42D : 0.58D, 0.94D, 0.45D);
                pose.mulPose(Axis.YP.rotationDegrees(180.0F));
                // A player model is taller than an ordinary item; keep the entire
                // miniature inside the first-person viewport, including its legs.
                pose.scale(0.18F, -0.18F, -0.18F);
            }
            default ->
            {
                pose.translate(0.5D, 0.72D, 0.5D);
                pose.scale(0.32F, -0.32F, -0.32F);
            }
        }

        boolean rendered = false;
        try
        {
            for (int slot = 0; slot < stored.length; ++slot)
            {
                ItemStack piece = stored[slot];
                HeroIteration iter = ItemHeroArmor.getHero(piece);
                if (!(piece.getItem() instanceof ItemHeroArmor armor) || armor.getSlot() != slot
                        || iter == null || !iter.hasPieceOfSet(slot)) continue;
                HeroModelData modelData = HeroModelRegistry.get(iter);
                if (modelData == null) continue;
                setVisibleForSlot(slot, modelData);
                if (Boolean.getBoolean("fiskheroes.ciMiniSuitDiagnostics"))
                {
                    String key = "mask|" + context + "|" + iter.getRegistryName() + "|" + slot;
                    if (DIAGNOSTICS.add(key))
                        com.fiskmods.heroes.FiskHeroes.LOGGER.info(
                                "Mini suit mask: context={}, hero={}, slot={}, head={}, body={}, rightArm={}, leftArm={}, rightLeg={}, leftLeg={}",
                                context, iter.getRegistryName(), slot, playerModel.head.visible, playerModel.body.visible,
                                playerModel.rightArm.visible, playerModel.leftArm.visible,
                                playerModel.rightLeg.visible, playerModel.leftLeg.visible);
                }
                ResourceLocation baseTex = modelData.getMiniatureTexture(slot, Minecraft.getInstance().player, stored, false);
                if (Boolean.getBoolean("fiskheroes.ciMiniSuitDiagnostics"))
                {
                    var holder = Minecraft.getInstance().player;
                    Object holderMask = com.fiskmods.heroes.pack.js.JSEntity.read(holder, "fiskheroes:mask_open");
                    Object holderTimer = com.fiskmods.heroes.pack.js.JSEntity.read(holder, "fiskheroes:mask_open_timer2");
                    boolean holderMaskAnimating = holderTimer instanceof Number n && n.floatValue() > 0.0F;
                    String key = "texture|" + context + "|" + iter.getRegistryName() + "|" + slot + "|" + baseTex + "|" + holderMask + "|" + holderMaskAnimating;
                    if (DIAGNOSTICS.add(key)) com.fiskmods.heroes.FiskHeroes.LOGGER.info(
                            "Mini suit texture: context={}, hero={}, slot={}, texture={}, holderMaskOpen={}, holderMaskTimer={}",
                            context, iter.getRegistryName(), slot, baseTex, holderMask, holderTimer);
                }
                if (baseTex != null)
                {
                    playerModel.renderToBuffer(pose, buffers.getBuffer(RenderType.entityTranslucent(baseTex)),
                            light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
                    rendered = true;
                }
                ResourceLocation lightsTex = modelData.getMiniatureTexture(slot, Minecraft.getInstance().player, stored, true);
                if (lightsTex != null)
                {
                    playerModel.renderToBuffer(pose, buffers.getBuffer(RenderType.eyes(lightsTex)),
                            LightTexture.FULL_BRIGHT, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
                }
            }
        }
        finally
        {
            playerModel.setAllVisible(true);
        }
        if (!rendered)
        {
            // Unknown hero models and non-suit contents still have a visible, truthful icon.
            for (ItemStack piece : stored)
                if (!piece.isEmpty())
                {
                    Minecraft.getInstance().getItemRenderer().renderStatic(
                            piece, ItemDisplayContext.GUI, light, overlay, pose, buffers, Minecraft.getInstance().level, 0);
                    break;
                }
        }
        pose.popPose();
    }

    private void setVisibleForSlot(int slot, HeroModelData data)
    {
        // Some suits (for example Iron Man Mk 50) use one chest item for the
        // entire body. Follow the pack's per-piece mask just like the worn suit.
        playerModel.setAllVisible(false);
        playerModel.head.visible = data.showsModelPart(slot, "head");
        playerModel.hat.visible = playerModel.head.visible && data.showsModelPart(slot, "headwear")
                && !data.shouldFixHatLayer(slot);
        playerModel.body.visible = data.showsModelPart(slot, "body");
        playerModel.jacket.visible = playerModel.body.visible;
        playerModel.rightArm.visible = data.showsModelPart(slot, "rightArm");
        playerModel.rightSleeve.visible = playerModel.rightArm.visible;
        playerModel.leftArm.visible = data.showsModelPart(slot, "leftArm");
        playerModel.leftSleeve.visible = playerModel.leftArm.visible;
        playerModel.rightLeg.visible = data.showsModelPart(slot, "rightLeg");
        playerModel.rightPants.visible = playerModel.rightLeg.visible;
        playerModel.leftLeg.visible = data.showsModelPart(slot, "leftLeg");
        playerModel.leftPants.visible = playerModel.leftLeg.visible;
    }
}
