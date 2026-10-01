extend("fiskheroes:hero_basic");
loadTextures({
    "jacket": "fiskheroes:heatwave_jacket",
    "pants": "fiskheroes:heatwave_pants",
    "boots": "fiskheroes:heatwave_boots"
});

function init(renderer) {
    parent.init(renderer);
    renderer.setTexture((entity, renderLayer) => renderLayer == "LEGGINGS" ? "pants" : renderLayer == "BOOTS" ? "boots" : "jacket");

    renderer.showModel("CHESTPLATE", "body", "rightArm", "leftArm", "rightLeg", "leftLeg");
}
