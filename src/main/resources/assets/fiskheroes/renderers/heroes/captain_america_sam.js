extend("fiskheroes:hero_basic");
loadTextures({
    "layer1": "fiskheroes:captain_america_sam_layer1",
    "layer2": "fiskheroes:captain_america_sam_layer2",
    "wings": "fiskheroes:captain_america_sam_wings",
    "jetpack": "fiskheroes:captain_america_sam_jetpack",
    "jetpack_lights": "fiskheroes:captain_america_sam_jetpack_lights"
});

var utils = implement("fiskheroes:external/utils");
var wing_utils = implement("fiskheroes:external/wing_utils");
var falcon_boosters = implement("fiskheroes:external/falcon_boosters");

var wings;
var jetpack;

var boosters;

function initEffects(renderer) {
    wings = wing_utils.create(renderer, "wings", null, wing_utils.PRESET_CONTROLLED_FLIGHT);
    wings.effect.setYOffset(-1);

    jetpack = renderer.createEffect("fiskheroes:model");
    jetpack.setModel(utils.createModel(renderer, "fiskheroes:falcon_cap_jetpack", "jetpack", "jetpack_lights"));
    jetpack.anchor.set("body");

    boosters = falcon_boosters.create(renderer, 0x0033FF, "fiskheroes:blue_fire_layer_%s", {
        boosters: [
            { anchor: "body", offset: [0.0, 5.0, 2.6], size: [1.75, 3.0] },
            { anchor: "body", offset: [0.75, 5.0, 2.6], size: [1.5, 2.0], mirror: true }
        ],
        bloom: [
            { anchor: "body", offset: [0.0, 5.0, 2.6], size: [3.0, 1.75, 5.5] }
        ]
    });

    utils.addCameraShake(renderer, 0.015, 0.75, "fiskheroes:flight_boost_timer");
    utils.addCameraShake(renderer, 0.015, 0.75, "fiskheroes:dyn/flight_super_boost_timer");
    var shake = renderer.bindProperty("fiskheroes:camera_shake").setCondition(entity => {
        shake.factor = entity.isSprinting() && entity.getData("fiskheroes:flying") ? 0.15 * Math.sin(Math.PI * entity.getInterpolatedData("fiskheroes:flight_boost_timer")) : 0;

        if (entity.getData("fiskheroes:dyn/flight_super_boost") > 0) {
            shake.factor += 0.15 * Math.sin(Math.PI * entity.getInterpolatedData("fiskheroes:dyn/flight_super_boost_timer"));
        }
        return true;
    });
    shake.intensity = 0.05;
    utils.bindParticles(renderer, "fiskheroes:cap_falcon").setCondition(entity => entity.getData("fiskheroes:flying"));
    renderer.bindProperty("fiskheroes:equipped_item").setItems([
        { "anchor": "body", "scale": 1.0, "offset": [0.0, 4.5, 3.75], "rotation": [95.0, -180.0, 0.0] }
    ]).addOffset("QUIVER", -0.3, 1.0, 1.4);
}

function initAnimations(renderer) {
    parent.initAnimations(renderer);
    utils.addHoverAnimation(renderer, "falcon.HOVER", "fiskheroes:flight/idle/falcon");
    utils.addFlightAnimation(renderer, "falcon.FLIGHT", "fiskheroes:flight/cap_falcon.anim.json", (entity, data) => {
        data.load(0, entity.getInterpolatedData("fiskheroes:flight_timer"));
        data.load(1, entity.getInterpolatedData("fiskheroes:flight_boost_timer"));
        data.load(3, entity.getHeldItem().name() == "fiskheroes:captain_americas_shield" && !entity.as("PLAYER").isBlocking() ? entity.getInterpolatedData("fiskheroes:dyn/wing_timer") : 0);
    });

    renderer.reprioritizeDefaultAnimation("BLOCK_CAPS_SHIELD", -9);

    utils.addAnimationEvent(renderer, "FLIGHT_DIVE", "fiskheroes:iron_man_dive");
    utils.addAnimationEvent(renderer, "FLIGHT_DIVE_ROLL", "fiskheroes:falcon_dive_roll");

    addAnimationWithData(renderer, "falcon.ROLL", "fiskheroes:flight/barrel_roll", "fiskheroes:barrel_roll_timer")
        .priority = 10;
}

function render(entity, renderLayer, isFirstPersonArm) {
    if (!isFirstPersonArm && renderLayer == "CHESTPLATE") {
        wings.render(entity, 0);
        jetpack.render();
        boosters.render(entity);
    }
}
