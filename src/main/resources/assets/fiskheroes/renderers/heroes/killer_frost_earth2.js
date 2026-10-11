extend("fiskheroes:killer_frost_base");
loadTextures({
    "layer1": "fiskheroes:killer_frost_earth2_layer1",
    "layer2": "fiskheroes:killer_frost_earth2_layer2",
    "chest": "fiskheroes:killer_frost_earth2_chest",
    "hands": "fiskheroes:killer_frost_earth2_hands",
    "hair": "fiskheroes:killer_frost_hair"
});

function init(renderer) {
    parent.init(renderer);
    renderer.setTexture((entity, renderLayer) => {
        if (renderLayer == "CHESTPLATE") {
            return entity.getWornHelmet().suitType() == $SUIT_NAME ? "layer1" : "chest";
        }
        return renderLayer == "HELMET" ? "hair" : renderLayer == "LEGGINGS" ? "layer2" : "layer1"
    });

    renderer.setItemIcon("HELMET", "killer_frost_0");
}
