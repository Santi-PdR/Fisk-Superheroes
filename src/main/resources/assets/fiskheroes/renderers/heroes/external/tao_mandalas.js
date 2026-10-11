function create(renderer, color, shape, beam) {
    var shield_idle = renderer.createEffect("fiskheroes:lines").setShape(shape).setRenderer(beam);
    shield_idle.setOffset(1.0, 11.0, 0.0).setRotation(0.0, 0.0, 10.0).setScale(4.8);
    shield_idle.color.set(color);
    shield_idle.anchor.set("rightArm");
    shield_idle.mirror = true;

    var shield_big = renderer.createEffect("fiskheroes:lines").setShape(shape).setRenderer(beam);
    shield_big.color.set(color);
    shield_big.anchor.set("head");

    return {
        render: (entity, isFirstPersonArm) => {
            var shield_timer = entity.getInterpolatedData("fiskheroes:shield_timer");
            var blocking_timer = entity.getInterpolatedData("fiskheroes:shield_blocking_timer");
            shield_idle.progress = shield_timer * (1 - blocking_timer);
            shield_big.progress = shield_timer * blocking_timer;

            if (isFirstPersonArm) {
                shield_big.setOffset(0, 7, -14);
                shield_big.setScale(16).setRotation(80, 0, 0);
                shield_big.anchor.ignoreAnchor(true);
            }
            else {
                var pitch = entity.rotPitch() * Math.PI / 180;
                shield_big.setOffset(0, 2 * Math.cos(pitch), -15 - 12 * Math.pow(Math.sin(Math.max(pitch, 0)), 2));
                shield_big.setScale(12).setRotation(90, 0, 0);
                shield_big.anchor.ignoreAnchor(false);
            }

            shield_idle.render();
            shield_big.render();
        }
    };
}
