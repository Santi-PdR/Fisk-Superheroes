extend("fiskheroes:hero_basic");
loadTextures({
    "layer1": "fiskheroes:doctor_strange_layer1",
    "pants": "fiskheroes:doctor_strange_pants",
    "pants_robes": "fiskheroes:doctor_strange_pants_robes",
    "boots": "fiskheroes:doctor_strange_boots",
    "cape": "fiskheroes:doctor_strange_cape"
});

var utils = implement("fiskheroes:external/utils");
var capes = implement("fiskheroes:external/capes");
var mandalas = implement("fiskheroes:external/tao_mandalas");
var eldritch_sword = implement("fiskheroes:external/eldritch_sword");

var physics;
var cape;

var collar;
var shield;
var sword;
var spell;

function init(renderer) {
    parent.init(renderer);
    renderer.setTexture((entity, renderLayer) => {
        if (renderLayer == "LEGGINGS") {
            return entity.getWornChestplate().suitType() == $SUIT_NAME ? "pants_robes" : "pants";
        }
        return renderLayer == "BOOTS" ? "boots" : "layer1";
    });

    renderer.showModel("CHESTPLATE", "head", "headwear", "body", "rightArm", "leftArm", "rightLeg", "leftLeg");
    renderer.fixHatLayer("CHESTPLATE");
}

function initEffects(renderer) {
    physics = renderer.createResource("CAPE_PHYSICS", null);
    physics.weight = 1.2;
    physics.maxFlare = 1;
    physics.flareDegree = 1.5;
    physics.flareFactor = 1.5;
    physics.flareElasticity = 8;
    physics.setTickHandler(entity => {
        var f = 1 - entity.getData("fiskheroes:flight_timer");
        f = 1 - f * f * f;
        physics.headingAngle = 90 - f * 20;
        physics.restAngle = f * 40;
        physics.restFlare = f * 0.7;
        physics.idleFlutter = 0.15 + 0.25 * f;
        physics.flutterSpeed = f * 0.3;
    });

    cape = capes.create(renderer, 24, "fiskheroes:cape_default.mesh.json");
    cape.effect.texture.set("cape");

    collar = renderer.createEffect("fiskheroes:ears");
    collar.anchor.set("head");
    collar.angle = -7;
    collar.inset = -0.065;

    var color = 0xFF4800;
    var tao_mandala = renderer.createResource("SHAPE", "fiskheroes:tao_mandala");
    var beam = renderer.createResource("BEAM_RENDERER", "fiskheroes:line");
    spell = renderer.createEffect("fiskheroes:lines").setShape(tao_mandala).setRenderer(beam);
    spell.color.set(color);
    spell.setOffset(1.0, 8.0, 0.0).setScale(3.2);
    spell.anchor.set("rightArm");
    spell.mirror = true;

    shield = mandalas.create(renderer, color, tao_mandala, beam);
    sword = eldritch_sword.create(renderer, color, beam);
    sword.setOffset(1.0, 9.4, -4.0).setRotation(10.0, 0.0, 0.0).setScale(16.0);

    var magic = renderer.bindProperty("fiskheroes:spellcasting");
    magic.colorGeneric.set(0xFF8000);
    magic.colorEarthCrack.set(0xB366FF);
    magic.colorAtmosphere.set(0xFF8000);
    magic.colorWhip.set(color);
}

function initAnimations(renderer) {
    parent.initAnimations(renderer);
    renderer.removeCustomAnimation("basic.AIMING");
    addAnimationWithData(renderer, "basic.AIMING", "fiskheroes:dual_aiming", "fiskheroes:aiming_timer")
        .priority = 10;

    renderer.removeCustomAnimation("basic.BLOCKING");
    addAnimationWithData(renderer, "basic.BLOCKING", "fiskheroes:dual_aiming", "fiskheroes:shield_blocking_timer")
        .priority = -5;

    addAnimationWithData(renderer, "strange.SWORD_POSE", "fiskheroes:sword_pose", "fiskheroes:blade_timer");

    utils.addHoverAnimation(renderer, "strange.HOVER", "fiskheroes:flight/idle/neutral");
    utils.addFlightAnimation(renderer, "strange.FLIGHT", "fiskheroes:flight/levitate.anim.json", (entity, data) => {
        data.load(entity.getInterpolatedData("fiskheroes:flight_timer"));
    });
}

function render(entity, renderLayer, isFirstPersonArm) {
    if (renderLayer == "CHESTPLATE") {
        spell.progress = entity.getInterpolatedData("fiskheroes:spellcast_timer");
        spell.render();

        shield.render(entity, isFirstPersonArm);
        sword.render(entity.getInterpolatedData("fiskheroes:blade_timer"));

        if (!isFirstPersonArm) {
            collar.render();

            var f = entity.getInterpolatedData("fiskheroes:flight_timer");
            cape.render({
                "wind": 1 + 0.3 * f,
                "windFactor": 1 - 0.7 * f,
                "flutter": physics.getFlutter(entity),
                "flare": physics.getFlare(entity)
            });
        }
    }
}
