extend("fiskheroes:iron_man_base");
loadTextures({
    "layer1": "fiskheroes:iron_man_mk47_layer1",
    "layer2": "fiskheroes:iron_man_mk47_layer2",
    "lights_layer1": "fiskheroes:iron_man_mk47_lights_layer1",
    "lights_layer2": "fiskheroes:iron_man_mk47_lights_layer2",
    "lights_suit": "fiskheroes:iron_man_mk47_lights_suit.tx.json",
    "suit": "fiskheroes:iron_man_mk47_suit.tx.json",
    "mask": "fiskheroes:iron_man_mk47_mask.tx.json"
});

function init(renderer) {
    parent.init(renderer);
    renderer.setItemIcon("HELMET", "iron_man_0");
}

function hasFoldingHelmet() {
    return false;
}

function hasBootLights() {
    return false;
}
