extend("fiskheroes:deadpool_xmen");
loadTextures({
    "layer1": "fiskheroes:deadpool_xmen_elvis_layer1",
    "layer2": "fiskheroes:deadpool_xmen_elvis_layer2",
    "boots": "fiskheroes:deadpool_xmen_layer1",
    "boots_pants": "fiskheroes:deadpool_xmen_elvis_boots_pants",
    "hair1": "fiskheroes:deadpool_xmen_elvis_hair1",
    "hair2": "fiskheroes:deadpool_xmen_elvis_hair2"
});

var collar;
var hair1;
var hair2;

function init(renderer) {
    parent.init(renderer);
    renderer.setTexture((entity, renderLayer) => {
        if (renderLayer == "BOOTS") {
            return entity.getWornLeggings().suitType() == $SUIT_NAME ? "boots_pants" : "boots";
        }
        return renderLayer == "CHESTPLATE" ? "layer1" : "layer2";
    });

    renderer.setItemIcons("%s_0", "%s_1", "%s_2", "deadpool_xmen_3");
    renderer.showModel("CHESTPLATE", "head", "headwear", "body", "rightArm", "leftArm", "rightLeg", "leftLeg");
    renderer.fixHatLayer("HELMET", "CHESTPLATE");
}

function initEffects(renderer) {
    parent.initEffects(renderer);
    collar = renderer.createEffect("fiskheroes:ears");
    collar.anchor.set("head");
    collar.angle = -10;
    collar.inset = -0.11;

    hair1 = renderer.createEffect("fiskheroes:opening_mask");
    hair1.texture.set("hair1");
    hair1.anchor.set("head");
    hair1.setOffset(0.0, -3.53, -3.77).setRotation(-33.23, 0.0, 0.0);
    hair2 = renderer.createEffect("fiskheroes:opening_mask");
    hair2.texture.set("hair2");
    hair2.anchor.set("head");
    hair2.setOffset(0.0, -6.08, -2.1).setRotation(-33.23, 0.0, 0.0);
}

function render(entity, renderLayer, isFirstPersonArm) {
    parent.render(entity, renderLayer, isFirstPersonArm);

    if (!isFirstPersonArm) {
        if (renderLayer == "HELMET") {
            hair1.render();
            hair2.render();
        }
        else if (renderLayer == "CHESTPLATE") {
            collar.render();
        }
    }
}
