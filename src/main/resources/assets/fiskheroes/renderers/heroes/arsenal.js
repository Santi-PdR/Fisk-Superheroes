extend("fiskheroes:hero_basic");
loadTextures({
    "layer1": "fiskheroes:arsenal_layer1",
    "layer2": "fiskheroes:arsenal_layer2",
    "arrow": "fiskheroes:arrow/arsenal",
    "quiver": "fiskheroes:quiver/arsenal"
});

var utils = implement("fiskheroes:external/utils");

function initEffects(renderer) {
    utils.addLivery(renderer, "ARROW", "arrow");
    utils.addLivery(renderer, "QUIVER", "quiver");
}
