extend("fiskheroes:zoom");
loadTextures({
    "layer1": "fiskheroes:zoom_pink_layer1",
    "layer2": "fiskheroes:zoom_pink_layer2"
});

var speedster = implement("fiskheroes:external/speedster_utils");

function initEffects(renderer) {
    speedster.init(renderer, "fiskheroes:lightning_zoom_pink");
}
