extend("fiskheroes:hero_basic");
loadTextures({
    "layer1": "fiskheroes:black_canary_layer1",
    "layer2": "fiskheroes:black_canary_layer2",
    "mask": "fiskheroes:black_canary_mask",
    "chest": "fiskheroes:black_canary_chest"
});

var chest;

function init(renderer) {
    parent.init(renderer);
    renderer.setTexture((entity, renderLayer) => {
        if (renderLayer == "CHESTPLATE") {
            return entity.getWornHelmet().suitType() == $SUIT_NAME ? "layer1" : "chest";
        }
        return renderLayer == "HELMET" ? "mask" : renderLayer == "LEGGINGS" ? "layer2" : "layer1";
    });

    renderer.showModel("HELMET", "head", "headwear", "body");
}

function initEffects(renderer) {
    chest = renderer.createEffect("fiskheroes:chest");
    chest.setExtrude(1).setYOffset(1);

    initEquipped(renderer);
}

function initEquipped(renderer) {
    renderer.bindProperty("fiskheroes:equipped_item").setItems([
        { "anchor": "rightLeg", "scale": 1.0, "offset": [-2.5, 1.0, 0.5], "rotation": [90.0, 180.0, -2.0] },
        { "anchor": "leftLeg", "scale": 1.0, "offset": [2.5, 1.0, 0.5], "rotation": [90.0, 180.0, 2.0] }
    ]);
}

function render(entity, renderLayer, isFirstPersonArm) {
    if (!isFirstPersonArm && renderLayer == "CHESTPLATE") {
        chest.render();
    }
}
