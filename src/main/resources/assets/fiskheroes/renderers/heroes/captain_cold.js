extend("fiskheroes:hero_basic");
loadTextures({
    "jacket": "fiskheroes:captain_cold_jacket",
    "jacket_pants": "fiskheroes:captain_cold_jacket_pants",
    "pants": "fiskheroes:captain_cold_pants",
    "pants_jacket": "fiskheroes:captain_cold_pants_jacket",
    "boots": "fiskheroes:captain_cold_boots",
    "lights": "fiskheroes:captain_cold_lights"
});

var utils = implement("fiskheroes:external/utils");

function init(renderer) {
    parent.init(renderer);
    renderer.setTexture((entity, renderLayer) => {
        if (renderLayer == "CHESTPLATE") {
            return entity.getWornLeggings().suitType() == $SUIT_NAME ? "jacket_pants" : "jacket";
        }
        else if (renderLayer == "LEGGINGS") {
            return entity.getWornChestplate().suitType() == $SUIT_NAME ? "pants_jacket" : "pants";
        }
        return "boots";
    });
    renderer.setLights((entity, renderLayer) => {
        return (renderLayer == "CHESTPLATE" && entity.getWornLeggings().suitType() == $SUIT_NAME ||
            renderLayer == "LEGGINGS" && entity.getWornChestplate().suitType() != $SUIT_NAME) ? "lights" : null
    });

    renderer.showModel("CHESTPLATE", "head", "headwear", "body", "rightArm", "leftArm", "rightLeg", "leftLeg");
    renderer.fixHatLayer("HELMET", "CHESTPLATE");
}

function initEffects(renderer) {
    utils.bindBeam(renderer, "fiskheroes:cold_gun", "fiskheroes:cold_beam", "rightArm", 0x4CB5FF, [
        { "firstPerson": [-4.75, 3.0, -13.0], "offset": [-0.6, 19.0, -1.5], "size": [2.0, 2.0] }
    ]);
}
