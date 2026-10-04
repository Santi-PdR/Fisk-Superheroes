extend("fiskheroes:hero_basic");
loadTextures({
    "layer1": "fiskheroes:citizen_steel_layer1",
    "layer2": "fiskheroes:citizen_steel_layer2",
    "metal": "fiskheroes:citizen_steel_metal"
});

var overlay;
var metal_heat;

function initEffects(renderer) {
    overlay = renderer.createEffect("fiskheroes:overlay");
    overlay.texture.set("metal");

    metal_heat = renderer.createEffect("fiskheroes:metal_heat");
    metal_heat.texture.set("metal");
}

function render(entity, renderLayer, isFirstPersonArm) {
    if (renderLayer == "HELMET" || renderLayer == "CHESTPLATE") {
        overlay.opacity = entity.getInterpolatedData("fiskheroes:dyn/steel_timer");
        overlay.render();

        metal_heat.opacity = entity.getInterpolatedData("fiskheroes:metal_heat");
        metal_heat.render();
    }
}
