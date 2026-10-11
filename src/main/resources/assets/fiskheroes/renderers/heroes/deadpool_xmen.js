extend("fiskheroes:hero_basic");
loadTextures({
    "layer1": "fiskheroes:deadpool_xmen_layer1",
    "layer2": "fiskheroes:deadpool_xmen_layer2",
    "scabbard": "fiskheroes:deadpool_xmen_scabbard",
    "katana": "fiskheroes:deadpool_xmen_katana",
    "gun": "fiskheroes:deadpool_xmen_gun"
});

var utils = implement("fiskheroes:external/utils");

var scabbard;

function init(renderer) {
    parent.init(renderer);
}

function initEffects(renderer) {
    scabbard = renderer.createEffect("fiskheroes:model");
    scabbard.setModel(utils.createModel(renderer, "fiskheroes:deadpool_scabbard", "scabbard"));
    scabbard.anchor.set("body");

    utils.addLivery(renderer, "KATANA", "katana");
    utils.addLivery(renderer, "DESERT_EAGLE", "gun");

    renderer.bindProperty("fiskheroes:equipped_item").setItems([
        { "anchor": "body", "scale": 0.535, "offset": [-3.05, 0.52, 3.0], "rotation": [-148.0, 90.0, 0.0] },
        { "anchor": "body", "scale": 0.535, "offset": [3.05, 0.52, 3.0], "rotation": [-148.0, -90.0, 0.0] }
    ]).slotIndex = 0;
    renderer.bindProperty("fiskheroes:equipped_item").setItems([
        { "anchor": "rightLeg", "scale": 0.7, "offset": [-2.4, 0.5, 1.25], "rotation": [90.0, 0.0, 0.0] },
        { "anchor": "leftLeg", "scale": 0.7, "offset": [2.4, 0.5, 1.25], "rotation": [90.0, 0.0, 0.0] }
    ]).slotIndex = 1;
}

function render(entity, renderLayer, isFirstPersonArm) {
    if (!isFirstPersonArm && renderLayer == "CHESTPLATE") {
        scabbard.render();
    }
}
