extend("fiskheroes:falcon");
loadTextures({
    "layer1": "fiskheroes:falcon_civil_war_layer1",
    "layer2": "fiskheroes:falcon_civil_war_layer2",
    "wings": "fiskheroes:falcon_civil_war_wings",
    "jetpack": "fiskheroes:falcon_civil_war_jetpack",
    "jetpack_lights": "fiskheroes:falcon_civil_war_jetpack_lights"
});

function initBoosters(renderer, utils, falcon_boosters) {
    utils.bindParticles(renderer, "fiskheroes:falcon_cw").setCondition(entity => entity.getData("fiskheroes:flying"));
    return falcon_boosters.create(renderer, 0xFF0000, "fiskheroes:red_fire_layer_%s", {
        boosters: [
            { anchor: "body", offset: [2.4, 5.0, 2.6], rotation: [0.0, -18.0, 0.0], size: [1.75, 3.0], mirror: true }
        ],
        bloom: [
            { anchor: "body", offset: [2.4, 5.0, 2.6], rotation: [0.0, -18.0, 0.0], size: [1.75, 1.75, 6.0], mirror: true }
        ]
    });
}

function getFlightAnimation() {
    return "fiskheroes:flight/falcon_with_arms.anim.json";
}
