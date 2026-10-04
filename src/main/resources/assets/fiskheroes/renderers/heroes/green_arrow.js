extend("fiskheroes:hero_basic");
loadTextures({
    "layer1": "fiskheroes:green_arrow_layer1",
    "layer2": "fiskheroes:green_arrow_layer2",
    "quiver": "fiskheroes:quiver/green_arrow"
});

var utils = implement("fiskheroes:external/utils");

function initEffects(renderer) {
    utils.addLivery(renderer, "QUIVER", "quiver");
}
