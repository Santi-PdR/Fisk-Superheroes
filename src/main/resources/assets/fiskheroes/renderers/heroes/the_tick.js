extend("fiskheroes:hero_basic");
loadTextures({
    "layer1": "fiskheroes:the_tick_layer1",
    "layer2": "fiskheroes:the_tick_layer2"
});

var antennae;

function initEffects(renderer) {
    antennae = renderer.createEffect("fiskheroes:antennae").setSegments(8);
    antennae.anchor.set("head");
    antennae.angle = 90.0;
    antennae.offset = 2.0;
}

function render(entity, renderLayer, isFirstPersonArm) {
    if (!isFirstPersonArm && renderLayer == "HELMET") {
        antennae.render();
    }
}
