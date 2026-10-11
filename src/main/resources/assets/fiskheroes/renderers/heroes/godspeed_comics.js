extend("fiskheroes:hero_basic");
loadTextures({
    "layer1": "fiskheroes:godspeed_comics_layer1",
    "layer2": "fiskheroes:godspeed_comics_layer2",
    "layer1_on": "fiskheroes:godspeed_comics_layer1_on",
    "layer2_on": "fiskheroes:godspeed_comics_layer2_on",
    "lights": "fiskheroes:godspeed_comics_lights",
    "lights_layer1": "fiskheroes:godspeed_comics_lights_layer1",
    "lights_layer2": "fiskheroes:godspeed_comics_lights_layer2"
});

var speedster = implement("fiskheroes:external/speedster_utils");

var ears;

function init(renderer) {
    parent.init(renderer);
    renderer.setTexture((entity, renderLayer) => (renderLayer == "LEGGINGS" ? "layer2" : "layer1") + (entity.getData("fiskheroes:speeding") ? "_on" : ""));
    renderer.setLights((entity, renderLayer) => {
        if (entity.getData("fiskheroes:speeding")) {
            return renderLayer == "LEGGINGS" ? "lights_layer2" : "lights_layer1";
        }
        return renderLayer == "CHESTPLATE" ? "lights" : null;
    });
}

function initEffects(renderer) {
    ears = renderer.createEffect("fiskheroes:ears");
    ears.anchor.set("head");
    ears.angle = 15;
    ears.inset = 0.025;

    speedster.init(renderer, "fiskheroes:lightning_comics_yellow");
}

function render(entity, renderLayer, isFirstPersonArm) {
    if (!isFirstPersonArm && renderLayer == "HELMET") {
        ears.render();
    }
}
