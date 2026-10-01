extend("fiskheroes:hero_basic");
loadTextures({
    "layer1": "fiskheroes:rip_hunter_layer1",
    "layer2": "fiskheroes:rip_hunter_pants",
    "pants_trenchcoat": "fiskheroes:rip_hunter_pants_trenchcoat",
    "boots": "fiskheroes:rip_hunter_boots",
    "boots_trenchcoat": "fiskheroes:rip_hunter_boots_trenchcoat"
});

function init(renderer) {
    parent.init(renderer);
    renderer.setTexture((entity, renderLayer) => {
        if (renderLayer == "LEGGINGS") {
            return entity.getWornChestplate().suitType() == $SUIT_NAME ? "pants_trenchcoat" : "layer2";
        }
        else if (renderLayer == "BOOTS") {
            return entity.getWornChestplate().suitType() == $SUIT_NAME ? "boots_trenchcoat" : "boots";
        }
        return "layer1";
    });

    renderer.showModel("CHESTPLATE", "head", "headwear", "body", "rightArm", "leftArm", "rightLeg", "leftLeg");
    renderer.fixHatLayer("CHESTPLATE");
}

function initEffects(renderer) {
    renderer.bindProperty("fiskheroes:energy_bolt").color.set(0x005EFF);
}
