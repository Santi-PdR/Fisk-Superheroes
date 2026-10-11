extend("fiskheroes:iron_man_mk46c");
loadTextures({
    "layer1": "fiskheroes:iron_man_mk47c_layer1",
    "layer2": "fiskheroes:iron_man_mk47c_layer2",
    "suit": "fiskheroes:iron_man_mk47c_suit.tx.json"
});

function init(renderer) {
    parent.init(renderer);
    renderer.setItemIcon("HELMET", "iron_man_mk46c_0");
    renderer.setItemIcon("BOOTS", "iron_man_mk46c_3");
}
