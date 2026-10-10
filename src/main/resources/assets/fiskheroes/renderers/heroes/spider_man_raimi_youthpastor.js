extend("fiskheroes:spider_man_raimi");
loadTextures({
    "layer1": "fiskheroes:spider_man_raimi_youthpastor_layer1",
    "layer2": "fiskheroes:spider_man_raimi_youthpastor_layer2"
});

function init(renderer) {
    parent.init(renderer);
    renderer.setItemIcon("HELMET", "spider_man_raimi_0");
}
