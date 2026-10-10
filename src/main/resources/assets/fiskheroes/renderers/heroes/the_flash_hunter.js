extend("fiskheroes:hero_basic");
loadTextures({
    "layer1": "fiskheroes:the_flash_hunter_layer1",
    "layer2": "fiskheroes:the_flash_hunter_layer2"
});

var speedster = implement("fiskheroes:external/speedster_utils");

function init(renderer) {
    parent.init(renderer);
    renderer.setItemIcon("HELMET", "the_flash_jay_0");
}

function initEffects(renderer) {
    speedster.init(renderer, "fiskheroes:lightning_gold");
}
