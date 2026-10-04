extend("fiskheroes:spider_man_base");
loadTextures({
    "layer1": "fiskheroes:spider_man_raimi_layer1",
    "layer2": "fiskheroes:spider_man_raimi_layer2"
});

function init(renderer) {
    parent.init(renderer);
}

function initEffects(renderer) {
    renderer.bindProperty("fiskheroes:equipment_wheel").color.set(0xE2062B);
}

function initAnimations(renderer) {
    parent.initAnimations(renderer);
    utils.addAnimationEvent(renderer, "WEBSWING_DEFAULT", "fiskheroes:swing_default2");
    utils.addAnimationEvent(renderer, "WEBSWING_TRICK_DEFAULT", "fiskheroes:swing_roll");
}
