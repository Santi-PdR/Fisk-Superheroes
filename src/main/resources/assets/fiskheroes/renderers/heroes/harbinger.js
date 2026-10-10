extend("fiskheroes:hero_basic");
loadTextures({
    "chest": "fiskheroes:harbinger_chest",
    "pants": "fiskheroes:harbinger_pants",
    "boots": "fiskheroes:harbinger_boots"
});

var utils = implement("fiskheroes:external/utils");

var chest;
var glow;

function init(renderer) {
    parent.init(renderer);
    renderer.setTexture((entity, renderLayer) => renderLayer == "LEGGINGS" ? "pants" : renderLayer == "BOOTS" ? "boots" : "chest");

    renderer.showModel("CHESTPLATE", "body", "rightArm", "leftArm", "rightLeg", "leftLeg");
}

function initEffects(renderer) {
    chest = renderer.createEffect("fiskheroes:chest");
    chest.setExtrude(1).setYOffset(1);

    glow = renderer.createEffect("fiskheroes:glowerlay");
    glow.includeEffects(chest);
    glow.color.set(0xFFFFFF);

    utils.bindParticles(renderer, "fiskheroes:harbinger_glow");
    utils.bindBeam(renderer, "fiskheroes:energy_projection", "fiskheroes:energy_projection", "body", 0x00A5FF, [
        { "firstPerson": [0.0, 6.0, 0.0], "offset": [0.0, 5.0, -4.0], "size": [4.0, 4.0] }
    ]).setParticles(renderer.createResource("PARTICLE_EMITTER", "fiskheroes:impact_energy_projection"));

    renderer.bindProperty("fiskheroes:gravity_manipulation").color.set(0x00A5FF);
}

function render(entity, renderLayer, isFirstPersonArm) {
    if (!isFirstPersonArm && renderLayer == "CHESTPLATE") {
        chest.render();
    }

    glow.opacity = entity.getInterpolatedData("fiskheroes:teleport_timer");
    glow.render();
}
