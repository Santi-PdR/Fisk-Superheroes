extend("fiskheroes:killer_frost_base");
loadTextures({
    "hair": "fiskheroes:killer_frost_hair",
    "coat": "fiskheroes:killer_frost_coat",
    "coat_hair": "fiskheroes:killer_frost_coat_hair",
    "pants": "fiskheroes:killer_frost_pants",
    "pants_coat": "fiskheroes:killer_frost_pants_coat",
    "boots": "fiskheroes:killer_frost_boots",
    "hands": "fiskheroes:killer_frost_hands"
});

function init(renderer) {
    parent.init(renderer);
    renderer.setTexture((entity, renderLayer) => {
        if (renderLayer == "CHESTPLATE") {
            return entity.getWornHelmet().suitType() == $SUIT_NAME ? "coat_hair" : "coat";
        }
        else if (renderLayer == "LEGGINGS") {
            return entity.getWornChestplate().suitType() == $SUIT_NAME ? "pants_coat" : "pants";
        }
        return renderLayer == "HELMET" ? "hair" : "boots"
    });

    renderer.showModel("CHESTPLATE", "body", "rightArm", "leftArm", "rightLeg", "leftLeg");
}
