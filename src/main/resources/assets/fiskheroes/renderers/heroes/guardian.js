extend("fiskheroes:hero_basic");
loadTextures({
    "layer1": "fiskheroes:guardian_layer1",
    "layer2": "fiskheroes:guardian_layer2",
    "shield": "fiskheroes:guardian_shield"
});

var shield;

function initEffects(renderer) {
    shield = renderer.createEffect("fiskheroes:shield");
    shield.texture.set("shield");
    shield.anchor.set("rightArm");
    shield.setCurve(25.0, 35.0);
}

function render(entity, renderLayer, isFirstPersonArm) {
    if (renderLayer == "CHESTPLATE") {
        shield.unfold = entity.getInterpolatedData("fiskheroes:shield_timer");
        shield.setOffset(2.9 + 1.35 * Math.min(shield.unfold * 5, 1), 5.0, 0.0);
        shield.render();
    }
}
