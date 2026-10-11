extend("fiskheroes:hero_basic");
loadTextures({
    "layer1": "fiskheroes:black_manta_dceu_layer1",
    "layer2": "fiskheroes:black_manta_dceu_layer2",
    "lights": "fiskheroes:black_manta_dceu_lights",
    "jetpack": "fiskheroes:black_manta_dceu_jetpack",
    "jetpack_lights": "fiskheroes:black_manta_dceu_jetpack_lights.tx.json",
    "helmet": "fiskheroes:black_manta_dceu_helmet",
    "helmet_lights": "fiskheroes:black_manta_dceu_helmet_lights",
    "eyes": "fiskheroes:black_manta_dceu_eyes",
    "blade": "fiskheroes:black_manta_dceu_blade"
});

var utils = implement("fiskheroes:external/utils");
var mantapack = implement("fiskheroes:external/mantapack");

var jetpack;

var blade;
var overlay;
var helmet;
var booster_boots;

function init(renderer) {
    parent.init(renderer);
    renderer.setLights((entity, renderLayer) => renderLayer == "LEGGINGS" ? null : "lights");
}

function initEffects(renderer) {
    blade = renderer.createEffect("fiskheroes:shield");
    blade.texture.set("blade");
    blade.anchor.set("rightArm");
    overlay = renderer.createEffect("fiskheroes:overlay");
    overlay.texture.set(null, "eyes");

    helmet = renderer.createEffect("fiskheroes:model");
    helmet.setModel(utils.createModel(renderer, "fiskheroes:black_manta_helmet", "helmet", "helmet_lights"));
    helmet.anchor.set("head");

    var red_fire = renderer.createResource("ICON", "fiskheroes:red_fire_layer_%s");
    booster_boots = renderer.createEffect("fiskheroes:booster");
    booster_boots.setIcon(red_fire).setOffset(0.0, 8.5, 2.0).setSize(1.5, 4.0);
    booster_boots.anchor.set("rightLeg");
    booster_boots.mirror = true;
    booster_boots.opacity = 0.7;
    jetpack = mantapack.create(renderer, "jetpack", "jetpack_lights", red_fire);

    utils.addCameraShake(renderer, 0.015, 1.5, "fiskheroes:flight_boost_timer");
    utils.bindParticles(renderer, "fiskheroes:black_manta_dceu_eyes").setCondition(entity => entity.getData("fiskheroes:beam_charging") && entity.getData("fiskheroes:beam_charge") < 1);
    utils.bindParticles(renderer, "fiskheroes:black_manta_dceu").setCondition(entity => entity.getData("fiskheroes:flying"));
    utils.bindBeam(renderer, "fiskheroes:charged_beam", "fiskheroes:charged_beam", "head", 0xFF2227, [
        { "firstPerson": [3.0, 0.0, 2.0], "offset": [3.0, -4.0, -4.0], "size": [3.0, 2.0] },
        { "firstPerson": [-3.0, 0.0, 2.0], "offset": [-3.0, -4.0, -4.0], "size": [3.0, 2.0] }
    ]).setParticles(renderer.createResource("PARTICLE_EMITTER", "fiskheroes:impact_charged_beam"));
}

function initAnimations(renderer) {
    parent.initAnimations(renderer);
    utils.addFlightAnimation(renderer, "manta.FLIGHT", "fiskheroes:flight/propelled.anim.json");
    utils.addHoverAnimation(renderer, "manta.HOVER", "fiskheroes:flight/idle/manta");
}

function render(entity, renderLayer, isFirstPersonArm) {
    if (renderLayer == "CHESTPLATE") {
        blade.unfold = entity.getInterpolatedData("fiskheroes:blade_timer");

        var f = Math.min(blade.unfold * 5, 1);
        blade.setOffset(2.9 + 0.35 * f, 6.0 + 2.5 * f, 0.0);
        blade.render();

        if (!isFirstPersonArm) {
            jetpack.render(entity);
        }
    }

    if (!isFirstPersonArm) {
        if (renderLayer == "HELMET") {
            overlay.opacity = entity.getInterpolatedData("fiskheroes:beam_charge");
            overlay.render();
            helmet.render();
        }
        else if (renderLayer == "BOOTS") {
            var boost = entity.getInterpolatedData("fiskheroes:flight_boost_timer");
            booster_boots.progress = entity.getInterpolatedData("fiskheroes:dyn/booster_timer");
            booster_boots.speedScale = 0.5 * boost;
            booster_boots.flutter = 1 + boost;

            booster_boots.setRotation(20 - 10 * boost, 0, 0);
            booster_boots.render();
        }
    }
}
