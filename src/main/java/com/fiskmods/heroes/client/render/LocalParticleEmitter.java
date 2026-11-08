package com.fiskmods.heroes.client.render;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.data.var.Vars;
import com.fiskmods.heroes.common.hero.HeroTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Tick first-person emitters even if no player model or hand is rendered on screen. */
@Mod.EventBusSubscriber(modid = FiskHeroes.MODID, value = Dist.CLIENT)
public final class LocalParticleEmitter
{
    private static PlayerModel<AbstractClientPlayer> classic;
    private static PlayerModel<AbstractClientPlayer> slim;

    private LocalParticleEmitter() { }

    static void clear()
    {
        classic = null;
        slim = null;
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event)
    {
        ParticleEmitterRenderer.clear();
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        AbstractClientPlayer player = mc.player;
        if (player == null || mc.level == null || mc.isPaused() || !mc.options.getCameraType().isFirstPerson()) return;
        var iteration = HeroTracker.getWornSuit(player);
        var data = SHDataCapabilities.getPlayer(player);
        if (iteration == null && data != null) iteration = data.getHero();
        if (iteration == null) return;
        HeroModelData suit = HeroModelRegistry.get(iteration);
        if (suit == null || suit.getCustom().keySet().stream().noneMatch(key ->
                key.startsWith("fiskheroes:particles") || key.equals("fiskheroes:particle_cloud")
                || key.equals("fiskheroes:teleportation") || key.equals("fiskheroes:telekinesis")
                || key.equals("fiskheroes:telekinesis_chain"))) return;

        // Use a private model; modifying the shared renderer model during a tick could
        // otherwise leak a pose into another player or a first-person hand render.
        boolean isSlim = "slim".equals(player.getModelName());
        if (isSlim && slim == null) slim = new PlayerModel<>(mc.getEntityModels().bakeLayer(ModelLayers.PLAYER_SLIM), true);
        if (!isSlim && classic == null) classic = new PlayerModel<>(mc.getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
        PlayerModel<AbstractClientPlayer> model = isSlim ? slim : classic;
        model.attackTime = player.getAttackAnim(1.0F);
        model.riding = player.isPassenger();
        model.crouching = player.isCrouching();
        var main = player.getMainHandItem().isEmpty() ? net.minecraft.client.model.HumanoidModel.ArmPose.EMPTY
                : net.minecraft.client.model.HumanoidModel.ArmPose.ITEM;
        var off = player.getOffhandItem().isEmpty() ? net.minecraft.client.model.HumanoidModel.ArmPose.EMPTY
                : net.minecraft.client.model.HumanoidModel.ArmPose.ITEM;
        model.rightArmPose = player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT ? main : off;
        model.leftArmPose = player.getMainArm() == net.minecraft.world.entity.HumanoidArm.LEFT ? main : off;
        model.young = player.isBaby();
        float swing = player.walkAnimation.position();
        float amount = player.walkAnimation.speed();
        model.prepareMobModel(player, swing, amount, 1.0F);
        model.setupAnim(player, swing, amount, player.tickCount,
                Mth.wrapDegrees(player.getYHeadRot() - player.yBodyRot), player.getXRot());
        ParticleEmitterRenderer.tick(player, model, suit, Vars.getScale(player));
    }
}
