extend("fiskheroes:hero_basic");
loadTextures({
    "layer1": "fiskheroes:wild_dog_layer1",
    "layer2": "fiskheroes:wild_dog_layer2"
});

function initEffects(renderer) {
    renderer.bindProperty("fiskheroes:equipped_item").setItems([
        { "anchor": "rightLeg", "scale": 0.7, "offset": [-1.8, 0.75, -0.5], "rotation": [110.0, 0.0, 0.0] },
        { "anchor": "leftLeg", "scale": 0.7, "offset": [1.8, 0.75, -0.5], "rotation": [110.0, 0.0, 0.0] }
    ]);
}
