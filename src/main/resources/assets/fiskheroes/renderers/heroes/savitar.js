extend("fiskheroes:hero_basic");
loadTextures({
    "layer1": "fiskheroes:savitar_layer1",
    "layer2": "fiskheroes:savitar_layer2",
    "layer1_lights": "fiskheroes:savitar_layer1_lights",
    "layer2_lights": "fiskheroes:savitar_layer2_lights",
    "metal": "fiskheroes:savitar_metal",
    "spike": "fiskheroes:savitar_spike"
});

var speedster = implement("fiskheroes:external/speedster_utils");

var spike;
var metal_heat;

function init(renderer) {
    parent.init(renderer);
    renderer.setLights((entity, renderLayer) => renderLayer == "LEGGINGS" ? "layer2_lights" : "layer1_lights");
}

function initEffects(renderer) {
    spike = renderer.createEffect("fiskheroes:shield");
    spike.texture.set("spike");
    spike.anchor.set("rightArm");
    spike.setRotation(0.0, 0.0, 10.0).setCurve(40.0, 160.0);

    metal_heat = renderer.createEffect("fiskheroes:metal_heat");
    metal_heat.includeEffects(spike);
    metal_heat.texture = "metal";

    speedster.init(renderer, getTrailType());
}

function getTrailType() {
    return "fiskheroes:lightning_white";
}

function render(entity, renderLayer, isFirstPersonArm) {
    if (renderLayer == "CHESTPLATE") {
        spike.unfold = entity.getInterpolatedData("fiskheroes:blade_timer");

        var f = Math.min(spike.unfold * 5, 1);
        spike.setOffset(2.9 + 0.6 * f, 6.0 + 2.0 * f, 0.0);
        spike.render();
    }

    metal_heat.opacity = entity.getInterpolatedData("fiskheroes:metal_heat");
    metal_heat.render();
}
