extend("fiskheroes:green_arrow");
loadTextures({
    "layer1": "fiskheroes:green_arrow_earthx_layer1",
    "layer2": "fiskheroes:green_arrow_earthx_layer2",
    "quiver": "fiskheroes:quiver/green_arrow_earthx",
    "arrow": "fiskheroes:arrow/earthx"
});

var utils = implement("fiskheroes:external/utils");

function initEffects(renderer) {
    parent.initEffects(renderer);
    utils.addLivery(renderer, "ARROW", "arrow");
}
