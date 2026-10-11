extend("fiskheroes:hero_basic");
loadTextures({
    "layer1": "fiskheroes:shazam_dceu_layer1",
    "layer2": "fiskheroes:shazam_dceu_layer2",
    "lights": "fiskheroes:shazam_dceu_lights",
    "cape": "fiskheroes:shazam_dceu_cape"
});

var utils = implement("fiskheroes:external/utils");
var capes = implement("fiskheroes:external/capes");

var cape;

function init(renderer) {
    parent.init(renderer);
    renderer.setLights((entity, renderLayer) => renderLayer == "CHESTPLATE" ? "lights" : null);

    renderer.showModel("CHESTPLATE", "head", "headwear", "body", "rightArm", "leftArm");
    renderer.fixHatLayer("CHESTPLATE");
}

function initEffects(renderer) {
    var physics = renderer.createResource("CAPE_PHYSICS", null);
    physics.maxFlare = 0.4;
    cape = capes.createDefault(renderer, 17, "fiskheroes:cape_default.mesh.json", physics);
    cape.effect.texture.set("cape");
    cape.effect.width = 12;

    utils.bindTrail(renderer, "fiskheroes:shazam");
    utils.bindBeam(renderer, "fiskheroes:lightning_cast", "fiskheroes:lightning_cast", "rightArm", 0xD9FFFF, [
        { "firstPerson": [-8.0, 4.5, -10.0], "offset": [-0.5, 9.0, 0.0], "size": [0.75, 0.75] }
    ]);
}

function initAnimations(renderer) {
    parent.initAnimations(renderer);
    utils.addFlightAnimation(renderer, "shazam.FLIGHT", "fiskheroes:flight/default.anim.json");
    utils.addHoverAnimation(renderer, "shazam.HOVER", "fiskheroes:flight/idle/default");
}

function render(entity, renderLayer, isFirstPersonArm) {
    if (!isFirstPersonArm && renderLayer == "CHESTPLATE") {
        cape.render(entity);
    }
}
