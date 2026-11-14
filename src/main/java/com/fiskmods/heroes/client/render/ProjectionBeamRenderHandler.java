package com.fiskmods.heroes.client.render;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.data.SHDataCapabilities;
import com.fiskmods.heroes.common.data.SHPlayerData;
import com.fiskmods.heroes.common.data.var.Vars;
import com.fiskmods.heroes.common.hero.HeroIteration;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Draws the short-lived energy beams described by the wearer's hero model. */
@Mod.EventBusSubscriber(modid = FiskHeroes.MODID, value = Dist.CLIENT)
public final class ProjectionBeamRenderHandler
{
    private static final java.util.Set<net.minecraft.resources.ResourceLocation> LOGGED_CHARGED_BEAMS = new java.util.HashSet<>();

    private ProjectionBeamRenderHandler()
    {
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event)
    {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES)
        {
            return;
        }

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null)
        {
            return;
        }

        var buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        Vec3 camera = event.getCamera().getPosition();
        boolean rendered = false;

        for (Player player : level.players())
        {
            SHPlayerData data = SHDataCapabilities.getPlayer(player);
            if (data == null)
            {
                continue;
            }

            HeroIteration iteration = data.getHero();
            HeroModelData model = HeroModelRegistry.get(iteration);
            if (model == null)
            {
                continue;
            }

            float chargedActivity = Mth.clamp(data.getData().get(Vars.BEAM_SHOOTING_TIMER), 0.0F, 1.0F);
            float activity = Math.max(chargedActivity,
                    Mth.clamp(data.getData().get(Vars.ENERGY_PROJECTION_TIMER), 0.0F, 1.0F));
            double length = data.getData().get(Vars.HEAT_VISION_LENGTH);
            if (activity <= 0.0F || length <= 0.0D)
            {
                continue;
            }

            // A suit such as Mysterio has both effects: use the charged beam's head anchor
            // while it is firing, rather than the energy projection's arm anchor.
            JsonObject effect = chargedActivity > 0.0F ? model.getCustom().get("fiskheroes:charged_beam") : null;
            if (effect == null) effect = model.getCustom().get("fiskheroes:energy_projection");
            if (effect == null) effect = model.getCustom().get("fiskheroes:charged_beam");
            if (effect == null)
            {
                // Black Lightning and Shazam use the same hitscan ability, but their model
                // packs describe its visual as lightning_attack instead of energy_projection.
                effect = model.getCustom().get("fiskheroes:lightning_attack");
            }
            if (effect == null) effect = model.getCustom().get("fiskheroes:cold_gun");
            if (effect == null) effect = model.getCustom().get("fiskheroes:repulsor_blast");
            if (effect == null) effect = model.getCustom().get("fiskheroes:energy_manipulation");
            if (effect == null)
            {
                continue;
            }

            PoseStack poseStack = event.getPoseStack();
            poseStack.pushPose();
            BeamConstellationRenderer.render(poseStack, buffers, camera, player, effect,
                    length, activity, event.getPartialTick(), false, parseColor(effect));
            if (chargedActivity > 0.0F && effect == model.getCustom().get("fiskheroes:charged_beam")
                    && LOGGED_CHARGED_BEAMS.add(iteration.getRegistryName()))
                FiskHeroes.LOGGER.info("Charged beam render path: hero={}, length={}", iteration.getRegistryName(), length);
            poseStack.popPose();
            rendered = true;
        }

        if (rendered)
        {
            buffers.endBatch(RenderType.lightning());
        }
    }

    private static int parseColor(JsonObject effect)
    {
        if (effect.has("color"))
        {
            try
            {
                return (int) Long.decode(effect.get("color").getAsString()).longValue();
            }
            catch (NumberFormatException ignored)
            {
                // Use the energy-beam default if an optional pack color is malformed.
            }
        }
        return 0xFF9A24;
    }

 }
