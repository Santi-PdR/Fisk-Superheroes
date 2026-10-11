extend("fiskheroes:hero_basic");
loadTextures({
    "layer1": "fiskheroes:captain_america_layer1",
    "layer2": "fiskheroes:captain_america_layer2"
});

function init(renderer) {
    parent.init(renderer);
}

function initEffects(renderer) {
    var equipped = renderer.bindProperty("fiskheroes:equipped_item");
    equipped.setItems([
        { "anchor": "body", "scale": 1.0, "offset": [0.0, 5.0, 2.75], "rotation": [90.0, -180.0, 0.0] }
    ]);
    equipped.addOffset("QUIVER", -0.5, 0.0, 2.36);
}
