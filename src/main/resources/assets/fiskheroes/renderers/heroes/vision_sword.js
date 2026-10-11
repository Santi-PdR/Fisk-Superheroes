extend("fiskheroes:vision");
loadTextures({
    "layer1": "fiskheroes:vision_sword_layer1",
    "layer2": "fiskheroes:vision_sword_layer2",
    "cape": "fiskheroes:vision_sword_cape",
    "stone": "fiskheroes:vision_sword_stone",
    "lights": "fiskheroes:vision_sword_lights"
});

function init(renderer) {
    parent.init(renderer);
    renderer.setLights((entity, renderLayer) => renderLayer === "HELMET" ? "lights" : null);
}

function getBeamColor() {
    return 0x40E7F9;
}
