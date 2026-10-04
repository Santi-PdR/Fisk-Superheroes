extend("fiskheroes:hero_basic");
loadTextures({
    "layer1": "fiskheroes:anti_monitor_layer1",
    "layer2": "fiskheroes:anti_monitor_layer2",
    "layer3": "fiskheroes:anti_monitor_layer3",
    "lights": "fiskheroes:anti_monitor_lights",
    "tubes": "fiskheroes:anti_monitor_tubes"
});

var utils = implement("fiskheroes:external/utils");

var tubes;

function init(renderer) {
    parent.init(renderer);
    renderer.setTexture((entity, renderLayer) => {
        if (renderLayer == "HELMET") {
            return entity.getWornChestplate().suitType() == $SUIT_NAME ? "layer3" : "layer2";
        }
        return renderLayer == "BOOTS" ? "layer3" : renderLayer == "LEGGINGS" ? "layer2" : "layer1";
    });
    renderer.setLights((entity, renderLayer) => renderLayer == "CHESTPLATE" ? "lights" : null);

    renderer.showModel("CHESTPLATE", "head", "headwear", "body", "rightArm", "leftArm", "rightLeg", "leftLeg");
    renderer.fixHatLayer("HELMET", "CHESTPLATE");
}

function initEffects(renderer) {
    tubes = renderer.createEffect("fiskheroes:model");
    tubes.setModel(utils.createModel(renderer, "fiskheroes:anti_monitor_tubes", null, "tubes"));
    tubes.anchor.set("rightArm");
    tubes.mirror = true;

    var forcefield = renderer.bindProperty("fiskheroes:forcefield");
    forcefield.color.set(0xFFD3A8);
    forcefield.setShape(36, 18).setOffset(0.0, 6.0, 0.0).setScale(1.25);
    forcefield.setCondition(entity => {
        forcefield.opacity = entity.getInterpolatedData("fiskheroes:shield_blocking_timer") * 0.15;
        return true;
    });

    utils.setOpacityWithData(renderer, 0.0, 1.0, "fiskheroes:shadowform_timer");
    utils.bindCloud(renderer, "fiskheroes:particle_cloud", "fiskheroes:shadow_smoke").setCondition(entity => entity.getData("fiskheroes:shadowform"));
    utils.bindTrail(renderer, "fiskheroes:anti_monitor").setCondition(entity => entity.getData("fiskheroes:size_state") > 0);

    // Antimatter Beam
    utils.bindBeam(renderer, "fiskheroes:energy_projection", "fiskheroes:energy_projection", "body", 0xFF1000, [
        { "firstPerson": [0.0, 6.0, 0.0], "offset": [0.0, 5.0, -4.0], "size": [4.0, 4.0] }
    ]).setParticles(renderer.createResource("PARTICLE_EMITTER", "fiskheroes:impact_energy_projection"));

    // Antimatter Blast
    utils.bindBeam(renderer, "fiskheroes:charged_beam", "fiskheroes:energy_projection", "rightArm", 0xFF1000, [
        { "firstPerson": [-4.5, 3.75, -8.0], "offset": [-0.5, 9.0, 0.0], "size": [3.0, 3.0] }
    ]).setParticles(renderer.createResource("PARTICLE_EMITTER", "fiskheroes:impact_antimatter"));
}

function initAnimations(renderer) {
    parent.initAnimations(renderer);
    renderer.removeCustomAnimation("basic.BLOCKING");
    renderer.removeCustomAnimation("basic.CHARGED_BEAM");

    addAnimationWithData(renderer, "antimonitor.ANTIBLAST", "fiskheroes:aiming", "fiskheroes:beam_charge");
}

function render(entity, renderLayer, isFirstPersonArm) {
    if (renderLayer == "CHESTPLATE") {
        tubes.render();
    }
}
