extend("fiskheroes:hero_basic");
loadTextures({
    "layer1": "fiskheroes:mysterio_layer1",
    "layer2": "fiskheroes:mysterio_layer2",
    "mask": "fiskheroes:mysterio_mask",
    "mask_anim": "fiskheroes:mysterio_mask.tx.json",
    "collar": "fiskheroes:mysterio_collar.tx.json",
    "lights_layer1": "fiskheroes:mysterio_lights_layer1",
    "lights_layer2": "fiskheroes:mysterio_lights_layer2",
    "cape": "fiskheroes:mysterio_cape",
    "cape_lights": "fiskheroes:mysterio_cape_lights"
});

var utils = implement("fiskheroes:external/utils");
var capes = implement("fiskheroes:external/capes");
var glyphs = implement("fiskheroes:external/mysterio_glyph");

var glyph;
var glyph_big;

var cape;
var overlay;

function init(renderer) {
    parent.init(renderer);
    renderer.setTexture((entity, renderLayer) => {
        if (renderLayer == "HELMET" && entity.getData("fiskheroes:mask_open_timer2") > 0) {
            return "collar";
        }
        return renderLayer == "LEGGINGS" ? "layer2" : "layer1";
    });
    renderer.setLights((entity, renderLayer) => {
        if (renderLayer == "HELMET" && entity.getData("fiskheroes:mask_open_timer2") <= 0) {
            return "mask";
        }
        return renderLayer == "LEGGINGS" ? "lights_layer2" : "lights_layer1";
    });
}

function initEffects(renderer) {
    var physics = renderer.createResource("CAPE_PHYSICS", null);
    physics.maxFlare = 0.3;
    physics.flareDegree = 1.5;
    physics.flareFactor = 1.2;
    physics.flareElasticity = 5;
    cape = capes.createDefault(renderer, 24, "fiskheroes:cape_default.mesh.json", physics);
    cape.effect.texture.set("cape", "cape_lights");
    cape.effect.width = 16;

    overlay = renderer.createEffect("fiskheroes:overlay");
    overlay.texture.set(null, "mask_anim");

    var color = 0x66FF47;
    glyph = glyphs.create(renderer, color, "rightArm", "fiskheroes:mysterio_glyph", true);
    glyph.setOffset(1.0, 13.0, 0.0).setRotation(0.0, 0.0, 10.0).setScale(5.2);
    glyph_big = glyphs.create(renderer, color, "head", "fiskheroes:mysterio_glyph_big", false);
    glyph_big.setScale(16);

    renderer.bindProperty("fiskheroes:spellcasting").colorGeneric.set(color);
    utils.bindParticles(renderer, "fiskheroes:mysterio_smoke").setCondition(entity => entity.getData("fiskheroes:dyn/flight_super_boost") > 0);

    // Charged Beam
    var beam = renderer.createResource("BEAM_RENDERER", "fiskheroes:mysterio_beam");
    utils.bindBeam(renderer, "fiskheroes:charged_beam", beam, "head", color, [
        { "offset": [0.0, 5.0, -22.0], "size": [4.0, 4.0] }
    ]).setParticles(renderer.createResource("PARTICLE_EMITTER", "fiskheroes:impact_charged_beam"));

    // Energy Projection
    utils.bindBeam(renderer, "fiskheroes:energy_projection", beam, "rightArm", color, [
        { "firstPerson": [-3.75, 3.0, -8.0], "offset": [-0.5, 12.0, 0.0], "size": [1.5, 1.5] },
        { "firstPerson": [3.75, 3.0, -8.0], "offset": [-0.5, 12.0, 0.0], "size": [1.5, 1.5], "anchor": "leftArm" }
    ]).setParticles(renderer.createResource("PARTICLE_EMITTER", "fiskheroes:impact_energy_projection"));
}

function initAnimations(renderer) {
    parent.initAnimations(renderer);
    renderer.removeCustomAnimation("basic.AIMING");
    renderer.removeCustomAnimation("basic.ENERGY_PROJ");
    renderer.removeCustomAnimation("basic.CHARGED_BEAM");

    addAnimation(renderer, "basic.AIMING", "fiskheroes:dual_aiming").setData((entity, data) => {
        var charge = entity.getInterpolatedData("fiskheroes:beam_charge");
        data.load(Math.max(entity.getInterpolatedData("fiskheroes:aiming_timer"), entity.getData("fiskheroes:beam_charging") ? Math.min(charge * 3, 1) : Math.max(charge * 5 - 4, 0)));
    });

    utils.addFlightAnimation(renderer, "mysterio.FLIGHT", "fiskheroes:flight/default.anim.json");
    utils.addHoverAnimation(renderer, "mysterio.HOVER", "fiskheroes:flight/idle/default");
}

function render(entity, renderLayer, isFirstPersonArm) {
    if (!isFirstPersonArm && renderLayer == "HELMET") {
        overlay.opacity = 1 - entity.getInterpolatedData("fiskheroes:mask_open_timer2");

        if (overlay.opacity < 1) {
            overlay.render();
        }

        renderGlyph(entity, isFirstPersonArm);
    }
    else if (renderLayer == "CHESTPLATE") {
        glyph.render(entity.getInterpolatedData("fiskheroes:aimed_timer"));

        if (isFirstPersonArm) {
            renderGlyph(entity, isFirstPersonArm);
        }
        else {
            cape.render(entity);
        }
    }
}

function renderGlyph(entity, isFirstPersonArm) {
    var charge = entity.getInterpolatedData("fiskheroes:beam_charge");
    charge = entity.getData("fiskheroes:beam_charging") ? Math.min(charge * 2, 1) : Math.max(charge * 3 - 2, 0);

    if (isFirstPersonArm) {
        glyph_big.setOffset(0, 3, -20).setRotation(80, 0, 0);
        glyph_big.ignoreAnchor(true);
    }
    else {
        glyph_big.setOffset(0, 5, -24).setRotation(90, 0, 0);
        glyph_big.ignoreAnchor(false);
    }

    glyph_big.render(charge);
}
