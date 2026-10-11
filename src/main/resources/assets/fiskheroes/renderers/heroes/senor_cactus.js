extend("fiskheroes:hero_basic");
loadTextures({
    "layer1": "fiskheroes:senor_cactus_layer1",
    "layer2": "fiskheroes:senor_cactus_layer2",
    "cape": "fiskheroes:senor_cactus_cape",
    "hat": "fiskheroes:senor_cactus_sombrero"
});

var capes = implement("fiskheroes:external/capes");

var cape;
var sombrero;

function initEffects(renderer) {
    var physics = renderer.createResource("CAPE_PHYSICS", null);
    physics.maxFlare = 0.2;
    cape = capes.createDefault(renderer, 24, "fiskheroes:cape_default.mesh.json", physics);
    cape.effect.texture.set("cape");

    var model = renderer.createResource("MODEL", "fiskheroes:sombrero");
    model.bindAnimation("fiskheroes:hat_tip_sombrero").setData((entity, data) => data.load(entity.getInterpolatedData("fiskheroes:hat_tip")));
    model.texture.set("hat");
    sombrero = renderer.createEffect("fiskheroes:model").setModel(model);
    sombrero.anchor.set("head");
}

function initAnimations(renderer) {
    parent.initAnimations(renderer);
    addAnimationWithData(renderer, "senor_cactus.HAT_TIP", "fiskheroes:hat_tip_player", "fiskheroes:hat_tip");
}

function render(entity, renderLayer, isFirstPersonArm) {
    if (!isFirstPersonArm) {
        if (renderLayer == "HELMET") {
            sombrero.render();
        }
        else if (renderLayer == "CHESTPLATE") {
            cape.render(entity);
        }
    }
}
