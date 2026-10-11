extend("fiskheroes:spider_man_base");
loadTextures({
    "layer1": "fiskheroes:spider_man_layer1",
    "layer2": "fiskheroes:spider_man_layer2",
    "web_wings": "fiskheroes:spider_man_wings"
});

var web_wings;

function init(renderer) {
    parent.init(renderer);
}

function initEffects(renderer) {
    web_wings = renderer.createEffect("fiskheroes:wingsuit");
    web_wings.texture.set("web_wings");
    web_wings.opacity = 0.99;

    renderer.bindProperty("fiskheroes:equipment_wheel").color.set(0x00DAFF);
}

function render(entity, renderLayer, isFirstPersonArm) {
    if (!isFirstPersonArm && renderLayer == "CHESTPLATE") {
        web_wings.unfold = entity.getInterpolatedData("fiskheroes:wing_animation_timer");
        web_wings.render();
    }
}
