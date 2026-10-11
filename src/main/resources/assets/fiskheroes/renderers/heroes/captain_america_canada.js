extend("fiskheroes:captain_america");
loadTextures({
    "layer1": "fiskheroes:captain_america_canada_layer1",
    "layer2": "fiskheroes:captain_america_canada_layer2",
    "shield": "fiskheroes:captain_america_canada_shield"
});

var utils = implement("fiskheroes:external/utils");

function init(renderer) {
    parent.init(renderer);
    renderer.setItemIcon("BOOTS", "captain_america_museum_3");
}

function initEffects(renderer) {
    parent.initEffects(renderer);
    utils.addLivery(renderer, "SHIELD", "shield");
}
