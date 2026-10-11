extend("fiskheroes:hero_basic");
loadTextures({
    "layer1": "fiskheroes:agent_liberty_layer1",
    "layer2": "fiskheroes:agent_liberty_layer2",
    "helmet": "fiskheroes:agent_liberty_helmet",
    "blade": "fiskheroes:agent_liberty_blade"
});

var blade;

function init(renderer) {
    parent.init(renderer);
    renderer.setTexture((entity, renderLayer) => renderLayer == "HELMET" ? "helmet": renderLayer == "LEGGINGS" ? "layer2" : "layer1");

    renderer.showModel("CHESTPLATE", "head", "headwear", "body", "rightArm", "leftArm");
    renderer.fixHatLayer("HELMET", "CHESTPLATE");
}

function initEffects(renderer) {
    blade = renderer.createEffect("fiskheroes:shield");
    blade.texture.set("blade");
    blade.anchor.set("rightArm");
}

function render(entity, renderLayer, isFirstPersonArm) {
    if (renderLayer == "CHESTPLATE") {
        blade.unfold = entity.getInterpolatedData("fiskheroes:blade_timer");

        var f = Math.min(blade.unfold * 5, 1);
        blade.setOffset(2.9 + 0.1 * f, 6.0 + 3.0 * f, 0.0);
        blade.render();
    }
}
