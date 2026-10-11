extend("fiskheroes:zoom");
loadTextures({
    "layer1": "fiskheroes:zoom_death_layer1",
    "layer2": "fiskheroes:zoom_death_layer2"
});

var speedster = implement("fiskheroes:external/speedster_utils");

function init(renderer) {
    parent.init(renderer);
    renderer.setTexture((entity, renderLayer) => renderLayer == "LEGGINGS" ? "layer2" : "layer1");
    renderer.setLights(null);

    renderer.setItemIcon("LEGGINGS", "zoom_2");
    renderer.setItemIcon("BOOTS", "zoom_3");
}

function initEffects(renderer) {
    speedster.init(renderer, "fiskheroes:lightning_red");
}
