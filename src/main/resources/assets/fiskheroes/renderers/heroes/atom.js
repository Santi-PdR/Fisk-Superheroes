extend("fiskheroes:hero_basic");
loadTextures({
    "layer1": "fiskheroes:atom_layer1",
    "layer2": "fiskheroes:atom_layer2",
    "lights_layer1": "fiskheroes:atom_lights_layer1",
    "lights_layer2": "fiskheroes:atom_lights_layer2",
    "visor": "fiskheroes:atom_visor.tx.json"
});

var utils = implement("fiskheroes:external/utils");
var boosters = implement("fiskheroes:external/bloom_booster");
var body_lines = implement("fiskheroes:external/body_lines");

var visor;
var booster_boots;
var booster_back;

var shrink_lights;

function init(renderer) {
    parent.init(renderer);
    renderer.setLights((entity, renderLayer) => renderLayer == "LEGGINGS" ? "lights_layer2" : renderLayer != "HELMET" ? "lights_layer1" : null);
}

function initEffects(renderer) {
    visor = renderer.createEffect("fiskheroes:overlay");
    visor.texture.set("visor");
    visor.opacity = 0.85;

    shrink_lights = body_lines.create(renderer, "fiskheroes:atom_shrink", 0x005EFF, [
        { anchor: "body", renderLayer: "CHESTPLATE", mirror: true, entries: [
            { "start": [3.5, 0.0, -1.5], "end": [7.5, -6.0, -3.5], "size": [0.5, 0.5] },
            { "start": [4.0, 5.5, -2.0], "end": [8.0, 4.5, -6.0], "size": [0.5, 0.5] },
            { "start": [3.5, 9.5, -1.5], "end": [7.5, 12.5, -4.5], "size": [0.5, 0.5] }
        ]},
        { anchor: "rightArm", renderLayer: "CHESTPLATE", mirror: true, entries: [
            { "start": [-3.5, 1.0, 0.5], "end": [-9.0, -1.0, 1.5], "size": [0.5, 0.5] },
            { "start": [-3.0, 4.5, 0.5], "end": [-8.0, 5.5, 1.5], "size": [0.5, 0.5] }
        ]},
        { anchor: "rightLeg", renderLayer: "LEGGINGS", mirror: true, entries: [
            { "start": [-2.0, 2.2, 2.0], "end": [-4.0, 5.2, 4.0], "size": [0.5, 0.5] }
        ]}
    ]);

    var beam = renderer.createResource("BEAM_RENDERER", "fiskheroes:atom_booster");
    var blue_fire = renderer.createResource("ICON", "fiskheroes:deep_blue_fire_layer_%s");
    var color = 0x0033FF;
    booster_boots = boosters.create(renderer, "rightLeg", true, color, blue_fire, beam).setOffset(0.0, 7.0, 2.0).setSize(1.5, 3.0);
    booster_back = boosters.create(renderer, "body", true, color, blue_fire, beam).setOffset(1.5, 6.0, 1.75).setRotation(15.0, 0.0, 5.0);

    utils.addCameraShake(renderer, 0.015, 1.5, "fiskheroes:flight_boost_timer");
}

function initAnimations(renderer) {
    parent.initAnimations(renderer);
    utils.addHoverAnimation(renderer, "atom.HOVER", "fiskheroes:flight/idle/default_back");
    utils.addFlightAnimation(renderer, "atom.FLIGHT", "fiskheroes:flight/propelled.anim.json", (entity, data) => {
        data.load(0, entity.getInterpolatedData("fiskheroes:flight_timer"));
        data.load(1, entity.getInterpolatedData("fiskheroes:flight_boost_timer"));
        data.load(2, entity.getInterpolatedData("fiskheroes:scale"));
    });
}

function render(entity, renderLayer, isFirstPersonArm) {
    if (!isFirstPersonArm) {
        if (renderLayer == "CHESTPLATE" || renderLayer == "LEGGINGS") {
            shrink_lights.opacity = shrink_lights.progress = entity.getInterpolatedData("fiskheroes:dyn/shrink_timer");
            shrink_lights.progress /= Math.sqrt(entity.getInterpolatedData("fiskheroes:scale")) * 2;
            shrink_lights.render(renderLayer);
        }

        if (renderLayer == "HELMET") {
            visor.render();
        }
        else if (renderLayer == "CHESTPLATE") {
            var boost = entity.getInterpolatedData("fiskheroes:flight_boost_timer");
            booster_back.speedScale = 0.5 * boost;
            booster_back.flutter = 1 + boost;

            var f = Math.min(Math.max(boost * 3 - 1.25, 0), 1);
            f = entity.isSprinting() ? 0.5 - Math.cos(2 * f * Math.PI) / 2 : 0;
            booster_back.setSize(1.5 + f * 2, 3.5 - f * 2);
            booster_back.render(entity, entity.getInterpolatedData("fiskheroes:dyn/booster_timer"));
        }
        else if (renderLayer == "BOOTS") {
            var boost = entity.getInterpolatedData("fiskheroes:flight_boost_timer");
            booster_boots.speedScale = 0.5 * boost;
            booster_boots.flutter = 1 + boost;

            booster_boots.setRotation(15 - 10 * boost, 0, 0);
            booster_boots.render(entity, entity.getInterpolatedData("fiskheroes:dyn/booster_timer"));
        }
    }
}
