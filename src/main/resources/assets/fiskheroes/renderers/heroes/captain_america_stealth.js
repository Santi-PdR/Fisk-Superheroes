extend("fiskheroes:captain_america");
loadTextures({
    "layer1": "fiskheroes:captain_america_stealth_layer1",
    "layer2": "fiskheroes:captain_america_stealth_layer2",
    "shield": "fiskheroes:captain_america_stealth_shield"
});

var utils = implement("fiskheroes:external/utils");

function initEffects(renderer) {
    parent.initEffects(renderer);
    utils.addLivery(renderer, "SHIELD", "shield");
}
