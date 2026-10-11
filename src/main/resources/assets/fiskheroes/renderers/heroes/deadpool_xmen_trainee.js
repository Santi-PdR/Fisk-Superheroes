extend("fiskheroes:deadpool_xmen");
loadTextures({
    "layer1": "fiskheroes:deadpool_xmen_trainee_layer1"
});

function init(renderer) {
    parent.init(renderer);
    renderer.setItemIcons("deadpool_xmen_0", "%s_1", "deadpool_xmen_2", "deadpool_xmen_3");
}
