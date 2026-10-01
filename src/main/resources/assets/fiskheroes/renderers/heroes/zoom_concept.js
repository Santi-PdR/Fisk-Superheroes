extend("fiskheroes:zoom");
loadTextures({
    "eyes": "fiskheroes:zoom_concept_eyes"
});

function init(renderer) {
    parent.init(renderer);
    renderer.setLights((entity, renderLayer) => renderLayer == "HELMET" && entity.getData("fiskheroes:speeding") ? "eyes" : "null");

    renderer.setItemIcons("%s_0", "zoom_1", "zoom_2", "zoom_3");
}

function initEffects(renderer) {
    parent.initEffects(renderer);
    vibration = renderer.createEffect("fiskheroes:vibration");
}

function render(entity, renderLayer, isFirstPersonArm) {
    if (entity.getData("fiskheroes:speeding")) {
        vibration.render();
    }
}
