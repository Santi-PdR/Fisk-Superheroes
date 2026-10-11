extend("fiskheroes:spider_man_base");
loadTextures({
    "layer1": "fiskheroes:spider_man_webb_layer1",
    "layer2": "fiskheroes:spider_man_webb_layer2"
});

function initEffects(renderer) {
    renderer.bindProperty("fiskheroes:equipment_wheel").color.set(0x00102A);
}
