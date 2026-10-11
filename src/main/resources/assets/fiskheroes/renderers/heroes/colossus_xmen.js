extend("fiskheroes:hero_basic");
loadTextures({
    "layer1": "fiskheroes:colossus_xmen_layer1",
    "layer2": "fiskheroes:colossus_xmen_layer2",
    "metal": "fiskheroes:colossus_xmen_metal"
});

var metal_heat;

function initEffects(renderer) {
    metal_heat = renderer.createEffect("fiskheroes:metal_heat");
    metal_heat.texture.set("metal");
}

function render(entity, renderLayer, isFirstPersonArm) {
    if (renderLayer == "HELMET" || renderLayer == "CHESTPLATE") {
        metal_heat.opacity = entity.getInterpolatedData("fiskheroes:metal_heat");
        metal_heat.render();
    }
}
