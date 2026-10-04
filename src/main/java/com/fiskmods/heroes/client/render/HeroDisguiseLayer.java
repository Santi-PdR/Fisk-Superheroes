package com.fiskmods.heroes.client.render;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.data.var.Vars;
import com.fiskmods.heroes.common.hero.Hero;
import com.fiskmods.heroes.common.hero.HeroTracker;
import com.fiskmods.heroes.common.hero.modifier.ModifierShapeShifting;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/** Draws the selected Minecraft skin beneath the Martian Manhunter suit overlay. */
public final class HeroDisguiseLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>
{
    private static final java.util.Map<UUID, ResourceLocation> SKINS = new ConcurrentHashMap<>();
    private static final Set<UUID> REQUESTS = ConcurrentHashMap.newKeySet();

    public HeroDisguiseLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent)
    {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
            float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch)
    {
        SHPlayerData data = SHDataCapabilities.getPlayer(player);
        Hero hero = HeroTracker.getHeroType(player);
        if (data == null || hero == null || !hasShapeShiftingPower(player, data, hero)) return;

        String name = data.getData().get(Vars.DISGUISE);
        String profileId = data.getData().get(Vars.DISGUISE_UUID);
        if (name == null || name.isBlank() || profileId == null || profileId.isBlank()) return;

        UUID uuid;
        try
        {
            uuid = UUID.fromString(profileId);
        }
        catch (IllegalArgumentException ignored)
        {
            return;
        }

        ResourceLocation skin = SKINS.get(uuid);
        if (skin == null)
        {
            requestSkin(uuid, name);
            return;
        }

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(skin));
        getParentModel().renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY,
                1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static boolean hasShapeShiftingPower(LivingEntity entity, SHPlayerData data, Hero hero)
    {
        return hero.getPowerContainer().getEntries().stream()
                .anyMatch(entry -> entry.getModifier() instanceof ModifierShapeShifting
                        && entry.isEnabled() && entry.isModifierEnabled(entity, data));
    }

    private static void requestSkin(UUID uuid, String name)
    {
        if (!REQUESTS.add(uuid)) return;

        GameProfile profile = new GameProfile(uuid, name);
        Minecraft.getInstance().getSkinManager().registerSkins(profile, (type, location, texture) ->
        {
            if (type == com.mojang.authlib.minecraft.MinecraftProfileTexture.Type.SKIN)
            {
                SKINS.put(uuid, location);
            }
            else if (type == com.mojang.authlib.minecraft.MinecraftProfileTexture.Type.CAPE)
            {
                REQUESTS.remove(uuid);
            }
        }, true);
    }
}
