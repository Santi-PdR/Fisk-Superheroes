extend("fiskheroes:hero_basic");
loadTextures({
    "layer1": "fiskheroes:ant_man_layer1",
    "layer2": "fiskheroes:ant_man_layer2",
    "lights": "fiskheroes:ant_man_lights",
    "lights_big": "fiskheroes:ant_man_lights_big"
});

function init(renderer) {
    parent.init(renderer);
    renderer.setLights((entity, renderLayer) => {
        if (renderLayer == "LEGGINGS" || renderLayer == "BOOTS") {
            return null;
        }

        return entity.getData("fiskheroes:dyn/giant_mode") ? "lights_big" : "lights";
    });
}
