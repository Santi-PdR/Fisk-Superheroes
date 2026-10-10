extend("fiskheroes:the_flash");
loadTextures({
    "layer1": "fiskheroes:the_flash_test_layer1",
    "layer2": "fiskheroes:the_flash_test_layer2",
    "layer3": "fiskheroes:the_flash_test_layer3"
});

var speedster = implement("fiskheroes:external/speedster_utils");

function init(renderer) {
    parent.init(renderer);
    renderer.setTexture((entity, renderLayer) => renderLayer == "LEGGINGS" ? "layer2" : renderLayer == "BOOTS" ? "layer3" : "layer1");

    renderer.showModel("CHESTPLATE", "body", "rightArm", "leftArm", "rightLeg", "leftLeg");
}

function initEffects(renderer) {
    speedster.init(renderer, "fiskheroes:blur_red");
}

function initAnimations(renderer) {
    parent.initAnimations(renderer);
    renderer.removeCustomAnimation("flash.MASK");
}
