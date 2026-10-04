extend("fiskheroes:deadpool_xmen");
loadTextures({
    "layer1": "fiskheroes:deadpool_xmen_jackman_layer1",
    "mask": "fiskheroes:deadpool_xmen_jackman_mask"
});

var mask;

function init(renderer) {
    parent.init(renderer);
    renderer.setItemIcons("%s_0", "deadpool_xmen_1", "deadpool_xmen_2", "deadpool_xmen_3");
}

function initEffects(renderer) {
    parent.initEffects(renderer);

    var model = renderer.createResource("MODEL", "fiskheroes:paper_mask");
    model.texture.set("mask");
    mask = renderer.createEffect("fiskheroes:model").setModel(model);
    mask.anchor.set("head");
}

function render(entity, renderLayer, isFirstPersonArm) {
    parent.render(entity, renderLayer, isFirstPersonArm);

    if (!isFirstPersonArm && renderLayer == "HELMET") {
        mask.render();
    }
}
