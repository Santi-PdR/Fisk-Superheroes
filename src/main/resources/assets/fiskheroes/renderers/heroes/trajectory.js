extend("fiskheroes:hero_basic");
loadTextures({
    "layer1": "fiskheroes:trajectory_layer1",
    "layer2": "fiskheroes:trajectory_layer2"
});

var speedster = implement("fiskheroes:external/speedster_utils");

var chest;

function initEffects(renderer) {
    chest = renderer.createEffect("fiskheroes:chest");
    chest.setExtrude(1).setYOffset(1);

    var trail = renderer.bindProperty("fiskheroes:trail");
    trail.setTrail(renderer.createResource("TRAIL", "fiskheroes:lightning_yellow"));
    trail.setCondition(entity => entity.getData("fiskheroes:speeding") && entity.getData("fiskheroes:speed") <= 4);

    trail = renderer.bindProperty("fiskheroes:trail");
    trail.setTrail(renderer.createResource("TRAIL", "fiskheroes:lightning_trajectory"));
    trail.setCondition(entity => entity.getData("fiskheroes:speeding") && entity.getData("fiskheroes:speed") > 4);

    speedster.init(renderer);
}

function render(entity, renderLayer, isFirstPersonArm) {
    if (!isFirstPersonArm && renderLayer == "CHESTPLATE") {
        chest.render();
    }
}
