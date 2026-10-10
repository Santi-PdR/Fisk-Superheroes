extend("fiskheroes:hero_basic");
loadTextures({
    "layer1": "fiskheroes:spectre_layer1",
    "layer2": "fiskheroes:spectre_layer2",
    "layer1_cloak": "fiskheroes:spectre_layer1_cloak",
    "layer2_cloak": "fiskheroes:spectre_layer2_cloak",
    "cloak": "fiskheroes:spectre_cloak"
});

var utils = implement("fiskheroes:external/utils");

function init(renderer) {
    parent.init(renderer);
    renderer.setTexture((entity, renderLayer) => {
        if (entity.getWornHelmet().suitType() == $SUIT_NAME) {
            return renderLayer == "HELMET" ? "cloak" : renderLayer == "LEGGINGS" ? "layer2_cloak" : "layer1_cloak";
        }
        return renderLayer == "LEGGINGS" ? "layer2" : "layer1";
    });

    renderer.showModel("HELMET", "head", "headwear", "body", "rightArm", "leftArm", "rightLeg", "leftLeg");
}

function initEffects(renderer) {
    utils.bindCloud(renderer, "fiskheroes:teleportation", "fiskheroes:breach");
    utils.bindBeam(renderer, "fiskheroes:energy_projection", "fiskheroes:energy_projection", "body", 0x0022FF, [
        { "firstPerson": [0.0, 6.0, 0.0], "offset": [0.0, 5.0, -4.0], "size": [4.0, 4.0] }
    ]).setParticles(renderer.createResource("PARTICLE_EMITTER", "fiskheroes:impact_energy_projection"));
}
