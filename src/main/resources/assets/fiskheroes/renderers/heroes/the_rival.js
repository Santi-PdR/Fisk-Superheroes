extend("fiskheroes:hero_basic");
loadTextures({
    "layer1": "fiskheroes:the_rival_layer1",
    "layer2": "fiskheroes:the_rival_layer2"
});

var speedster = implement("fiskheroes:external/speedster_utils");

var ears;

function initEffects(renderer) {
    ears = renderer.createEffect("fiskheroes:ears");
    ears.anchor.set("head");
    ears.angle = 20;
    ears.inset = 0.075;

    speedster.init(renderer, "fiskheroes:lightning_red");
}

function render(entity, renderLayer, isFirstPersonArm) {
    if (!isFirstPersonArm && renderLayer == "HELMET") {
        ears.render();
    }
}
