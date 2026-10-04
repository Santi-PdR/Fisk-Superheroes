extend("fiskheroes:hero_basic");
loadTextures({
    "layer1": "fiskheroes:august_heart_comics_layer1",
    "layer2": "fiskheroes:august_heart_comics_layer2",
    "pants": "fiskheroes:august_heart_comics_pants"
});

var speedster = implement("fiskheroes:external/speedster_utils");

function init(renderer) {
    parent.init(renderer);
    renderer.setTexture((entity, renderLayer) => {
        if (renderLayer == "LEGGINGS") {
            return entity.getWornChestplate().suitType() == $SUIT_NAME ? "layer2" : "pants";
        }
        return "layer1";
    });
}

function initEffects(renderer) {
    speedster.init(renderer, "fiskheroes:lightning_comics_lime");
}
