extend("fiskheroes:doctor_strange");
loadTextures({
    "layer1": "fiskheroes:doctor_strange_eye_layer1"
});

function init(renderer) {
    parent.init(renderer);
    renderer.setItemIcons("doctor_strange_0", "%s_1", "doctor_strange_2", "doctor_strange_3");
}
