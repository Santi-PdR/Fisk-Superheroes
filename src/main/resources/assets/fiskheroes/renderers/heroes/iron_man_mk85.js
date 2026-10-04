extend("fiskheroes:hero_basic");
loadTextures({
    "base": "fiskheroes:iron_man_mk85",
    "suit": "fiskheroes:iron_man_mk85_suit.tx.json",
    "mask": "fiskheroes:iron_man_mk85_mask.tx.json",
    "mask_lights": "fiskheroes:iron_man_mk85_mask_lights",
    "lights": "fiskheroes:iron_man_mk85_lights",
    "reactor": "fiskheroes:iron_man_mk85_reactor",
    "reactor_lights": "fiskheroes:iron_man_mk85_reactor_lights",
    "shield": "fiskheroes:iron_man_mk85_shield",
    "shield_handle": "fiskheroes:iron_man_mk85_shield_handle",
    "shield_handle_lights": "fiskheroes:iron_man_mk85_shield_handle_lights",
    "blade": "fiskheroes:iron_man_mk85_blade",
    "blade_lights": "fiskheroes:iron_man_mk85_blade_lights",
    "backpack": "fiskheroes:iron_man_mk85_backpack",
    "repulsor": "fiskheroes:iron_man_repulsor",
    "repulsor_left": "fiskheroes:iron_man_repulsor_left",
    "repulsor_boots": "fiskheroes:iron_man_repulsor_boots"
});

var utils = implement("fiskheroes:external/utils");
var mk85_backpack = implement("fiskheroes:external/mk85_backpack");
var iron_man_boosters = implement("fiskheroes:external/iron_man_boosters");

var backpack;
var boosters;

var repulsor;
var blade;
var shield;
var shield_handle;
var metal_heat;

function init(renderer) {
    parent.init(renderer);
    renderer.setTexture((entity, renderLayer) => {
        if (entity.getData("fiskheroes:mask_open_timer2") > 0) {
            return "mask";
        }
        else if (!entity.is("DISPLAY") || entity.as("DISPLAY").getDisplayType() === "BOOK_PREVIEW") {
            var timer = entity.getInterpolatedData("fiskheroes:dyn/nanite_timer");
            return timer == 0 ? "reactor" : timer < 1 ? "suit" : "base";
        }
        return "base";
    });
    renderer.setLights((entity, renderLayer) => {
        if (entity.getData("fiskheroes:mask_open_timer2") > 0) {
            return "mask_lights";
        }
        return (!entity.is("DISPLAY") || entity.as("DISPLAY").getDisplayType() === "BOOK_PREVIEW") && entity.getInterpolatedData("fiskheroes:dyn/nanite_timer") < 1 ? "reactor_lights" : "lights";
    });

    renderer.showModel("CHESTPLATE", "head", "headwear", "body", "rightArm", "leftArm", "rightLeg", "leftLeg");
    renderer.fixHatLayer("CHESTPLATE");
}

function initEffects(renderer) {
    repulsor = renderer.createEffect("fiskheroes:overlay");

    blade = renderer.createEffect("fiskheroes:shield");
    blade.texture.set("blade", "blade_lights");
    blade.anchor.set("rightArm");
    blade.setOffset(1.5, 8.0, 0.0);
    blade.large = true;

    shield = renderer.createEffect("fiskheroes:shield");
    shield.texture.set(null, "shield");
    shield.anchor.set("rightArm");
    shield.setRotation(55.0, -20.0, -10.0).setCurve(15.0, 50.0);
    shield.large = true;
    shield_handle = renderer.createEffect("fiskheroes:shield");
    shield_handle.texture.set("shield_handle", "shield_handle_lights");
    shield_handle.anchor.set("rightArm");
    shield_handle.setOffset(3.5, 7.0, -3.0).setRotation(55.0, -20.0, 20.0).setCurve(100.0, 45.0);

    backpack = mk85_backpack.create(renderer, "backpack");
    boosters = iron_man_boosters.create(renderer, "fiskheroes:repulsor_layer_%s", true);

    metal_heat = renderer.createEffect("fiskheroes:metal_heat");
    metal_heat.includeEffects(blade, shield_handle, backpack.b1, backpack.b2, backpack.b3);

    utils.addCameraShake(renderer, 0.015, 1.5, "fiskheroes:flight_boost_timer");
    var shake = renderer.bindProperty("fiskheroes:camera_shake").setCondition(entity => {
        shake.factor = entity.isSprinting() && entity.getData("fiskheroes:flying") ? 0.3 * Math.sin(Math.PI * entity.getInterpolatedData("fiskheroes:flight_boost_timer")) : 0;
        return true;
    });
    shake.intensity = 0.05;

    utils.bindParticles(renderer, "fiskheroes:iron_man").setCondition(entity => entity.getData("fiskheroes:flying"));
    utils.bindBeam(renderer, "fiskheroes:charged_beam", "fiskheroes:charged_beam", "body", 0xFFC462, [
        { "offset": [6.75, 10.0, 3.0], "size": [2.0, 2.0] },
        { "offset": [10.0, 0.5, 3.0], "size": [2.0, 2.0] },
        { "offset": [6.5, -4.5, 3.0], "size": [2.0, 2.0] },
        { "offset": [-6.75, 10.0, 3.0], "size": [2.0, 2.0] },
        { "offset": [-10.0, 0.5, 3.0], "size": [2.0, 2.0] },
        { "offset": [-6.5, -4.5, 3.0], "size": [2.0, 2.0] }
    ]).setParticles(renderer.createResource("PARTICLE_EMITTER", "fiskheroes:impact_charged_beam"));
}

function initAnimations(renderer) {
    parent.initAnimations(renderer);
    addAnimation(renderer, "basic.CHARGED_BEAM", "fiskheroes:dual_aiming").setData((entity, data) => data.load(Math.max(entity.getInterpolatedData("fiskheroes:beam_charge") * 5 - 4, 0)));

    utils.addFlightAnimationWithLanding(renderer, "iron_man.FLIGHT", "fiskheroes:flight/iron_man.anim.json");
    utils.addHoverAnimation(renderer, "iron_man.HOVER", "fiskheroes:flight/idle/iron_man");
    utils.addAnimationEvent(renderer, "FLIGHT_DIVE", "fiskheroes:iron_man_dive");

    addAnimationWithData(renderer, "iron_man.LAND", "fiskheroes:superhero_landing", "fiskheroes:dyn/superhero_landing_timer")
        .priority = -8;

    addAnimationWithData(renderer, "iron_man.ROLL", "fiskheroes:flight/barrel_roll", "fiskheroes:barrel_roll_timer")
        .priority = 10;
}

function render(entity, renderLayer, isFirstPersonArm) {
    repulsor.opacity = entity.getInterpolatedData("fiskheroes:dyn/booster_r_timer");
    repulsor.texture.set(null, "repulsor");
    repulsor.render();
    repulsor.opacity = entity.getInterpolatedData("fiskheroes:dyn/booster_l_timer");
    repulsor.texture.set(null, "repulsor_left");
    repulsor.render();
    repulsor.opacity = entity.getInterpolatedData("fiskheroes:dyn/booster_timer");
    repulsor.texture.set(null, "repulsor_boots");
    repulsor.render();

    blade.unfold = entity.getInterpolatedData("fiskheroes:blade_timer");
    blade.render();

    shield.unfold = entity.getInterpolatedData("fiskheroes:shield_blocking_timer");
    shield.opacity = 0.25 + 0.25 * shield.unfold * shield.unfold;
    shield.setOffset(3.75 + 2.25 * shield.unfold, 8.75 + 1.5 * shield.unfold, -0.75 * shield.unfold);
    shield.render();

    var shield_timer = entity.getInterpolatedData("fiskheroes:shield_timer");
    shield_handle.unfold = entity.getData("fiskheroes:shield") ? Math.min(shield_timer * 2, 1) : Math.max(shield_timer * 2 - 1, 0);
    shield_handle.render();

    backpack.render(entity.getInterpolatedData("fiskheroes:beam_charge"));
    boosters.render(entity, renderLayer, isFirstPersonArm, true);

    metal_heat.opacity = entity.getInterpolatedData("fiskheroes:metal_heat");
    metal_heat.render();
}
