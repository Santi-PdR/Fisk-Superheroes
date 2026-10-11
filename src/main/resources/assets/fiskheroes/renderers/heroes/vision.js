extend("fiskheroes:hero_basic");
loadTextures({
    "layer1": "fiskheroes:vision_layer1",
    "layer2": "fiskheroes:vision_layer2",
    "cape": "fiskheroes:vision_cape",
    "stone": "fiskheroes:vision_stone"
});

var utils = implement("fiskheroes:external/utils");
var capes = implement("fiskheroes:external/capes");

var cape;
var overlay;

function init(renderer) {
    parent.init(renderer);
}

function initEffects(renderer) {
    var physics = renderer.createResource("CAPE_PHYSICS", null);
    physics.weight = 0.9;
    physics.maxFlare = 0.5;
    cape = capes.createDefault(renderer, 24, "fiskheroes:cape_default.mesh.json", physics);
    cape.effect.texture.set("cape");
    cape.effect.opacity = 0.85;

    overlay = renderer.createEffect("fiskheroes:overlay");
    overlay.texture.set(null, "stone");

    utils.setOpacityWithData(renderer, 0.5, 1.0, "fiskheroes:intangibility_timer");
    utils.bindBeam(renderer, "fiskheroes:charged_beam", "fiskheroes:charged_beam", "head", getBeamColor(), [
        { "firstPerson": [0.0, -6.15, 0.0], "offset": [0.0, -6.15, -4.0], "size": [1.0, 0.5] }
    ]).setParticles(renderer.createResource("PARTICLE_EMITTER", "fiskheroes:impact_charged_beam"));
}

function initAnimations(renderer) {
    parent.initAnimations(renderer);
    utils.addFlightAnimation(renderer, "vision.FLIGHT", "fiskheroes:flight/default.anim.json");
    utils.addHoverAnimation(renderer, "vision.HOVER", "fiskheroes:flight/idle/neutral");
}

function getBeamColor() {
    return 0xFFFF6D;
}

function render(entity, renderLayer, isFirstPersonArm) {
    if (!isFirstPersonArm) {
        if (renderLayer == "HELMET") {
            overlay.opacity = entity.getInterpolatedData("fiskheroes:beam_charge");
            overlay.render();
        }
        else if (renderLayer == "CHESTPLATE") {
            cape.render(entity);
        }
    }
}
