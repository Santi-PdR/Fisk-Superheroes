extend("fiskheroes:deadpool_xmen");
loadTextures({
    "layer1": "fiskheroes:deadpool_xmen_emmy_layer1",
    "layer2": "fiskheroes:deadpool_xmen_emmy_layer2"
});

function init(renderer) {
    parent.init(renderer);
    renderer.setItemIcons("deadpool_xmen_0", "%s_1", "%s_2", "%s_3");
}
