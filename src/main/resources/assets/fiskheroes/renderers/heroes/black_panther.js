extend("fiskheroes:hero_basic");
loadTextures({
    "layer1": "fiskheroes:black_panther_layer1",
    "layer2": "fiskheroes:black_panther_layer2",
    "claws": "fiskheroes:black_panther_claws"
});

var overlay;

function initEffects(renderer) {
    overlay = renderer.createEffect("fiskheroes:overlay");
    overlay.texture.set("claws");
}

function render(entity, renderLayer, isFirstPersonArm) {
    if (renderLayer == "CHESTPLATE" && entity.getData("fiskheroes:blade")) {
        overlay.render();
    }
}
