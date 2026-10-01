extend("fiskheroes:hero_basic");
loadTextures({
    "layer1": "fiskheroes:kid_flash_layer1",
    "layer2": "fiskheroes:kid_flash_layer2"
});

var speedster = implement("fiskheroes:external/speedster_utils");

function init(renderer) {
    parent.init(renderer);
    renderer.setTexture((entity, renderLayer) => {
        if (renderLayer == "HELMET" && (entity.is("DISPLAY") && entity.as("DISPLAY").isStatic() ? entity.getData("fiskheroes:mask_open") : entity.getData("fiskheroes:mask_open_timer2") > 0.35)) {
            return "layer2";
        }
        return renderLayer == "LEGGINGS" ? "layer2" : "layer1";
    });
}

function initEffects(renderer) {
    speedster.init(renderer, "fiskheroes:lightning_amber");
}

function initAnimations(renderer) {
    parent.initAnimations(renderer);
    addAnimation(renderer, "flash.MASK", "fiskheroes:remove_cowl")
        .setData((entity, data) => {
            var f = entity.getInterpolatedData("fiskheroes:mask_open_timer2");
            data.load(f < 1 ? f : 0);
        });
}
