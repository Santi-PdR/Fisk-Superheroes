extend("fiskheroes:hero_basic");
loadTextures({
    "icicle": "fiskheroes:killer_frost_icicle"
});

var utils = implement("fiskheroes:external/utils");

var chest;
var overlay;
var spikeL;
var spikeR;

function init(renderer) {
    parent.init(renderer);
    renderer.showModel("HELMET", "head", "headwear", "body", "rightArm", "leftArm");
}

function initEffects(renderer) {
    chest = renderer.createEffect("fiskheroes:chest");
    chest.setExtrude(0.75).setYOffset(1);

    overlay = renderer.createEffect("fiskheroes:overlay");
    overlay.texture.set(null, "hands");
    overlay.opacity = 0.8;

    spikeL = renderer.createEffect("fiskheroes:shield");
    spikeL.texture.set("icicle");
    spikeL.anchor.set("rightArm");
    spikeL.setRotation(0.0, 0.0, -3.125).setCurve(20.0, 90.0);
    spikeR = renderer.createEffect("fiskheroes:shield");
    spikeR.texture.set("icicle");
    spikeR.anchor.set("rightArm");
    spikeR.setRotation(0.0, 180.0, -3.125).setCurve(20.0, 90.0);

    utils.bindParticles(renderer, "fiskheroes:killer_frost_ice").setCondition(entity => entity.getData("fiskheroes:cryo_charging"));
}

function render(entity, renderLayer, isFirstPersonArm) {
    if (renderLayer == "CHESTPLATE") {
        var charge = entity.getData("fiskheroes:cryo_charge");
        overlay.opacity = 0.8 * charge;
        overlay.render();

        spikeL.unfold = spikeR.unfold = entity.getInterpolatedData("fiskheroes:blade_timer") * (entity.isBookPlayer() ? 1 : Math.sqrt(charge));

        var f = Math.min(spikeL.unfold * 5, 1);
        spikeL.setOffset(2.25 + 1.0 * f, 8.0 + 2.0 * f, 0.0);
        spikeR.setOffset(-0.25 - 1.0 * f, 8.0 + 2.0 * f, 0.0);
        spikeL.render();
        spikeR.render();

        if (!isFirstPersonArm) {
            chest.render();
        }
    }
}
