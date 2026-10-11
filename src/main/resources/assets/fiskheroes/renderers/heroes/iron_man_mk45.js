extend("fiskheroes:iron_man_base");
loadTextures({
    "layer1": "fiskheroes:iron_man_mk45_layer1",
    "layer2": "fiskheroes:iron_man_mk45_layer2",
    "lights": "fiskheroes:iron_man_mk45_lights",
    "suit": "fiskheroes:iron_man_mk45_suit.tx.json",
    "mask": "fiskheroes:iron_man_mk45_mask.tx.json"
});

function init(renderer) {
    parent.init(renderer);
    renderer.setLights((entity, renderLayer) => {
        if (renderLayer == "HELMET") {
            return entity.getData('fiskheroes:mask_open_timer') == 0 ? "lights" : null;
        }
        return renderLayer == "CHESTPLATE" ? "lights" : null;
    });
}

function hasFoldingHelmet() {
    return false;
}
