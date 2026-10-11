extend("fiskheroes:deadpool_xmen");
loadTextures({
    "layer1": "fiskheroes:deadpool_xmen_russia_layer1",
    "layer2": "fiskheroes:deadpool_xmen_russia_layer2",
    "boots": "fiskheroes:deadpool_xmen_layer1",
    "pants": "fiskheroes:deadpool_xmen_layer2"
});

function init(renderer) {
    parent.init(renderer);
    renderer.setTexture((entity, renderLayer) => {
        if (renderLayer == "LEGGINGS") {
            return entity.getWornChestplate().suitType() == $SUIT_NAME ? "layer2" : "pants";
        }
        return renderLayer == "CHESTPLATE" ? "layer1" : renderLayer == "BOOTS" ? "boots" : "layer2";
    });

    renderer.setItemIcons("%s_0", "%s_1", "deadpool_xmen_2", "deadpool_xmen_3");
    renderer.showModel("CHESTPLATE", "head", "headwear", "body", "rightArm", "leftArm", "rightLeg", "leftLeg");
    renderer.fixHatLayer("HELMET", "CHESTPLATE");
}
