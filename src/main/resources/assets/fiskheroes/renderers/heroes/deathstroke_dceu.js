extend("fiskheroes:hero_basic");
loadTextures({
    "layer1": "fiskheroes:deathstroke_dceu_layer1",
    "layer2": "fiskheroes:deathstroke_dceu_layer2",
    "scabbard": "fiskheroes:deathstroke_dceu_scabbard",
    "katana": "fiskheroes:deathstroke_dceu_katana",
    "gun": "fiskheroes:deathstroke_dceu_gun",
    "ammo_bag": "fiskheroes:deathstroke_dceu_ammo_bag"
});

var utils = implement("fiskheroes:external/utils");

var scabbard;

function initEffects(renderer) {
    scabbard = renderer.createEffect("fiskheroes:model");
    scabbard.setModel(utils.createModel(renderer, "fiskheroes:deadpool_scabbard", "scabbard"));
    scabbard.anchor.set("body");

    utils.addLivery(renderer, "KATANA", "katana");
    utils.addLivery(renderer, "DESERT_EAGLE", "gun");
    utils.addLivery(renderer, "AMMO_BAG", "ammo_bag");

    renderer.bindProperty("fiskheroes:equipped_item").setItems([
        { "anchor": "body", "scale": 0.535, "offset": [-3.05, 0.52, 3.0], "rotation": [-148.0, 90.0, 0.0] },
        { "anchor": "body", "scale": 0.535, "offset": [3.05, 0.52, 3.0], "rotation": [-148.0, -90.0, 0.0] }
    ]).slotIndex = 0;
    renderer.bindProperty("fiskheroes:equipped_item").setItems([
        { "anchor": "rightLeg", "scale": 0.7, "offset": [-2.4, 0.5, 1.25], "rotation": [90.0, 0.0, 0.0] }
    ]).slotIndex = 1;
}

function render(entity, renderLayer, isFirstPersonArm) {
    if (!isFirstPersonArm && renderLayer == "CHESTPLATE") {
        scabbard.render();
    }
}
