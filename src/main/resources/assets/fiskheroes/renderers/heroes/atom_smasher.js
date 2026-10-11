extend("fiskheroes:hero_basic");
loadTextures({
    "layer1": "fiskheroes:atom_smasher_layer1",
    "layer2": "fiskheroes:atom_smasher_layer2",
    "mask": "fiskheroes:atom_smasher_mask.tx.json"
});

function init(renderer) {
    parent.init(renderer);
    renderer.setTexture((entity, renderLayer) => renderLayer == "LEGGINGS" ? "layer2" : renderLayer == "HELMET" && entity.getData("fiskheroes:mask_open_timer") > 0 ? "mask" : "layer1");
}
