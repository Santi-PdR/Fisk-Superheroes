extend("fiskheroes:deadpool_xmen");
loadTextures({
    "layer1": "fiskheroes:deadpool_xmen_bob_ross_layer1",
    "layer2": "fiskheroes:deadpool_xmen_bob_ross_layer2",
    "afro": "fiskheroes:deadpool_xmen_bob_ross_afro"
});

var utils = implement("fiskheroes:external/utils");

var afro;

function initEffects(renderer) {
    parent.initEffects(renderer);
    afro = renderer.createEffect("fiskheroes:model");
    afro.setModel(utils.createModel(renderer, "fiskheroes:deadpool_bob_ross_afro", "afro"));
    afro.setOffset(0, 0, -1);
    afro.anchor.set("head");
}

function render(entity, renderLayer, isFirstPersonArm) {
    parent.render(entity, renderLayer, isFirstPersonArm);

    if (!isFirstPersonArm && renderLayer == "HELMET") {
        afro.render();
    }
}
