extend("fiskheroes:hero_basic");
loadTextures({
    "layer1": "fiskheroes:reverse_flash_layer1",
    "layer2": "fiskheroes:reverse_flash_layer2",
    "eyes": "fiskheroes:reverse_flash_eyes"
});

var speedster = implement("fiskheroes:external/speedster_utils");

var vibration;

function init(renderer) {
    parent.init(renderer);
    renderer.setLights((entity, renderLayer) => renderLayer == "HELMET" && entity.getData("fiskheroes:mask_open") ? "eyes" : null);
}

function initEffects(renderer) {
    vibration = renderer.createEffect("fiskheroes:vibration");

    speedster.init(renderer, "fiskheroes:lightning_red");
}

function render(entity, renderLayer, isFirstPersonArm) {
    if ((!entity.is("DISPLAY") || entity.as("DISPLAY").getDisplayType() === "BOOK_PREVIEW") && entity.getData("fiskheroes:mask_open")) {
        vibration.render();
    }
}
