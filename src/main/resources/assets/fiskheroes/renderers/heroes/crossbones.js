extend("fiskheroes:hero_basic");
loadTextures({
    "layer1": "fiskheroes:crossbones_layer1",
    "layer2": "fiskheroes:crossbones_layer2",
    "gauntlets": "fiskheroes:crossbones_gauntlets.tx.json"
});

var right_gauntlet;
var left_gauntlet;

function initEffects(renderer) {
    right_gauntlet = renderer.createEffect("fiskheroes:arm_overlay");
    right_gauntlet.texture.set("gauntlets");
    right_gauntlet.setSide("RIGHT").setOffset(0.0, 1.5, 0.0);
    left_gauntlet = renderer.createEffect("fiskheroes:arm_overlay");
    left_gauntlet.texture.set("gauntlets");
    left_gauntlet.setSide("LEFT");
    left_gauntlet.progress = 0;
}

function render(entity, renderLayer, isFirstPersonArm) {
    if (renderLayer == "CHESTPLATE") {
        right_gauntlet.progress = (1 - entity.getInterpolatedData("fiskheroes:punch_charge")) * entity.getInterpolatedData("fiskheroes:punchmode_timer");
        right_gauntlet.render();

        if (!isFirstPersonArm) {
            left_gauntlet.render();
        }
    }
}
