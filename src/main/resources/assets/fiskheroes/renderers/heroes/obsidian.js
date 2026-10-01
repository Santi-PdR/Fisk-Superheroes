extend("fiskheroes:hero_basic");
loadTextures({
    "layer1": "fiskheroes:obsidian_layer1",
    "layer2": "fiskheroes:obsidian_layer2",
    "hood": "fiskheroes:obsidian_hood",
    "cape": "fiskheroes:obsidian_cape"
});

var utils = implement("fiskheroes:external/utils");
var capes = implement("fiskheroes:external/capes");

var cape;

function init(renderer) {
    parent.init(renderer);
    renderer.setTexture((entity, renderLayer) => {
        if (renderLayer == "HELMET") {
            return entity.getWornChestplate().suitType() == $SUIT_NAME ? "layer2" : "hood";
        }
        return renderLayer == "LEGGINGS" ? "layer2" : "layer1";
    });

    renderer.showModel("CHESTPLATE", "head", "headwear", "body", "rightArm", "leftArm");
    renderer.fixHatLayer("HELMET", "CHESTPLATE");
}

function initEffects(renderer) {
    var physics = renderer.createResource("CAPE_PHYSICS", null);
    physics.maxFlare = 0.2;
    cape = capes.createDefault(renderer, 24, "fiskheroes:cape_default.mesh.json", physics);
    cape.effect.texture.set("cape");

    var chain = utils.bindCloud(renderer, "fiskheroes:telekinesis_chain", "fiskheroes:shadow_smoke");
    chain.anchor.set("rightArm");
    chain.setOffset(-0.5, 10.0, 0.0);
    chain.setFirstPerson(-4.75, 4.0, -8.5);

    utils.setOpacityWithData(renderer, 0.0, 1.0, "fiskheroes:shadowform_timer");
    utils.bindCloud(renderer, "fiskheroes:particle_cloud", "fiskheroes:shadow_smoke").setCondition(entity => entity.getData("fiskheroes:shadowform"));
}

function render(entity, renderLayer, isFirstPersonArm) {
    if (!isFirstPersonArm && renderLayer == "CHESTPLATE") {
        cape.render(entity);
    }
}
