extend("fiskheroes:hero_basic");
loadTextures({
    "layer1": "fiskheroes:rupture_layer1",
    "layer2": "fiskheroes:rupture_layer2",
    "cape": "fiskheroes:rupture_cape"
});

var utils = implement("fiskheroes:external/utils");
var capes = implement("fiskheroes:external/capes");

var cape;

function initEffects(renderer) {
    var physics = renderer.createResource("CAPE_PHYSICS", null);
    physics.maxFlare = 0.4;
    cape = capes.createDefault(renderer, 20, "fiskheroes:cape_default.mesh.json", physics);
    cape.effect.texture.set("cape");
    cape.effect.width = 10;

    utils.bindBeam(renderer, "fiskheroes:energy_manipulation", "fiskheroes:energy_discharge", "rightArm", 0xFF0000, [
        { "firstPerson": [-2.5, 0.0, -7.0], "offset": [-0.5, 19.0, -12.0], "size": [1.5, 1.5] }
    ]);

    renderer.bindProperty("fiskheroes:equipped_item").setItems([
        { "anchor": "body", "scale": 0.55, "offset": [0.5, 4.5, 3.0], "rotation": [0.0, -90.0, 35.0] }
    ]);
}

function render(entity, renderLayer, isFirstPersonArm) {
    if (!isFirstPersonArm && renderLayer == "CHESTPLATE") {
        cape.render(entity);
    }
}
