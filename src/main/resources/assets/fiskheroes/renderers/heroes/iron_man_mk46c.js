extend("fiskheroes:iron_man_base");
loadTextures({
    "layer1": "fiskheroes:iron_man_mk46c_layer1",
    "layer2": "fiskheroes:iron_man_mk46c_layer2",
    "lights_layer1": "fiskheroes:iron_man_mk46c_lights_layer1",
    "lights_layer2": "fiskheroes:iron_man_mk46c_lights_layer2",
    "lights_suit": "fiskheroes:iron_man_mk46c_lights_suit.tx.json",
    "suit": "fiskheroes:iron_man_mk46c_suit.tx.json",
    "mask": "fiskheroes:iron_man_mk46c_mask.tx.json"
});

function init(renderer) {
    parent.init(renderer);
}

function hasFoldingHelmet() {
    return false;
}

function hasBootLights() {
    return false;
}
