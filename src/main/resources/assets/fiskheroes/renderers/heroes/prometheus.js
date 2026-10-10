extend("fiskheroes:hero_basic");
loadTextures({
    "layer1": "fiskheroes:prometheus_layer1",
    "layer2": "fiskheroes:prometheus_layer2",
    "sheath": "fiskheroes:prometheus_sheath",
    "arrow": "fiskheroes:arrow/prometheus",
    "quiver": "fiskheroes:quiver/prometheus"
});

var utils = implement("fiskheroes:external/utils");

var sheath;

function initEffects(renderer) {
    sheath = renderer.createEffect("fiskheroes:model");
    sheath.setModel(utils.createModel(renderer, "fiskheroes:prometheus_sheath", "sheath"));
    sheath.anchor.set("body");

    utils.addLivery(renderer, "ARROW", "arrow");
    utils.addLivery(renderer, "QUIVER", "quiver");

    renderer.bindProperty("fiskheroes:equipped_item").setItems([
        { "anchor": "body", "scale": 0.535, "offset": [-5.03, 1.28, 2.43], "rotation": [-150.0, 90.0, 0.0] }
    ]);
}

function render(entity, renderLayer, isFirstPersonArm) {
    if (!isFirstPersonArm && renderLayer == "CHESTPLATE") {
        sheath.render();
    }
}
