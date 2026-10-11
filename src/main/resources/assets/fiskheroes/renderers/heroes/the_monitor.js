extend("fiskheroes:hero_basic");
loadTextures({
    "chest": "fiskheroes:the_monitor_chest",
    "pants": "fiskheroes:the_monitor_pants",
    "boots": "fiskheroes:the_monitor_boots",
    "cape": "fiskheroes:the_monitor_cape"
});

var utils = implement("fiskheroes:external/utils");
var capes = implement("fiskheroes:external/capes");

var cape;

function init(renderer) {
    parent.init(renderer);
    renderer.setTexture((entity, renderLayer) => renderLayer == "CHESTPLATE" ? "chest" : renderLayer == "LEGGINGS" ? "pants" : "boots");

    renderer.showModel("CHESTPLATE", "head", "headwear", "body", "rightArm", "leftArm", "rightLeg", "leftLeg");
    renderer.fixHatLayer("CHESTPLATE");
}

function initEffects(renderer) {
    var physics = renderer.createResource("CAPE_PHYSICS", null);
    physics.maxFlare = 0.2;
    cape = capes.createDefault(renderer, 21, "fiskheroes:cape_default.mesh.json", physics);
    cape.effect.texture.set("cape");

    utils.bindCloud(renderer, "fiskheroes:telekinesis", "fiskheroes:telekinesis_monitor");
    utils.bindCloud(renderer, "fiskheroes:teleportation", "fiskheroes:breach");
    utils.bindBeam(renderer, "fiskheroes:energy_projection", "fiskheroes:energy_projection", "body", 0xFF2300, [
        { "firstPerson": [0.0, 6.0, 0.0], "offset": [0.0, 5.0, -4.0], "size": [4.0, 4.0] }
    ]).setParticles(renderer.createResource("PARTICLE_EMITTER", "fiskheroes:impact_energy_projection"));
}

function render(entity, renderLayer, isFirstPersonArm) {
    if (!isFirstPersonArm && renderLayer == "CHESTPLATE") {
        cape.render(entity);
    }
}
