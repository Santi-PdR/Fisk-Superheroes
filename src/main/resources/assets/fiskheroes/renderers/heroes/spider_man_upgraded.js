extend("fiskheroes:spider_man");
loadTextures({
    "layer1": "fiskheroes:spider_man_upgraded_layer1",
    "layer2": "fiskheroes:spider_man_upgraded_layer2"
});

function init(renderer) {
    parent.init(renderer);
    renderer.setItemIcon("HELMET", "spider_man_stark_0");
}
