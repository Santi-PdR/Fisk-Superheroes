extend("fiskheroes:spider_man_base");
loadTextures({
    "layer1": "fiskheroes:spider_man_ps4_layer1",
    "layer2": "fiskheroes:spider_man_ps4_layer2"
});

function initEffects(renderer) {
    renderer.bindProperty("fiskheroes:equipment_wheel").color.set(0x00DAFF);
}

function initAnimations(renderer) {
    parent.initAnimations(renderer);
    utils.addAnimationEvent(renderer, "WEBSWING_TRICK_RIGHT", [
        "fiskheroes:swing_rotate_right", "fiskheroes:swing_rotate_right1"
    ]);
    utils.addAnimationEvent(renderer, "WEBSWING_TRICK_LEFT", [
        "fiskheroes:swing_rotate_left", "fiskheroes:swing_rotate_left1"
    ]);
}
