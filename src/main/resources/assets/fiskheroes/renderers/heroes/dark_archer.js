extend("fiskheroes:hero_basic");
loadTextures({
    "layer1": "fiskheroes:dark_archer_layer1",
    "layer2": "fiskheroes:dark_archer_layer2",
    "pants_robes": "fiskheroes:dark_archer_pants_robes",
    "boots": "fiskheroes:dark_archer_boots",
    "arrow": "fiskheroes:arrow/dark_archer",
    "quiver": "fiskheroes:quiver/dark_archer"
});

var utils = implement("fiskheroes:external/utils");

function init(renderer) {
    parent.init(renderer);
    renderer.setTexture((entity, renderLayer) => {
        if (renderLayer == "LEGGINGS") {
            return entity.getWornChestplate().suitType() == $SUIT_NAME ? "pants_robes" : "layer2";
        }
        return renderLayer == "BOOTS" ? "boots" : "layer1";
    });

    renderer.showModel("CHESTPLATE", "body", "rightArm", "leftArm", "rightLeg", "leftLeg");
}

function initEffects(renderer) {
    utils.addLivery(renderer, "ARROW", "arrow");
    utils.addLivery(renderer, "QUIVER", "quiver");

    renderer.bindProperty("fiskheroes:equipped_item").setItems([
        { "anchor": "body", "scale": 0.6, "offset": [4.7, 10, -1], "rotation": [150.0, -175.0, -5.0] }
    ]);
}
