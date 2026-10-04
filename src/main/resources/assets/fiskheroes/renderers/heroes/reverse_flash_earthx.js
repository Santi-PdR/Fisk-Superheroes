extend("fiskheroes:reverse_flash");
loadTextures({
    "layer1": "fiskheroes:reverse_flash_earthx_layer1"
});

var vibration;

function init(renderer) {
    parent.init(renderer);
    renderer.setItemIcon("LEGGINGS", "reverse_flash_2");
    renderer.setItemIcon("BOOTS", "reverse_flash_3");
}
