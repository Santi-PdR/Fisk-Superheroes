function create(renderer, texture, lights, icon) {
    if (typeof icon === "undefined") {
        icon = renderer.createResource("ICON", icon);
    }

    var model = renderer.createResource("MODEL", "fiskheroes:black_manta_jetpack");
    model.bindAnimation("fiskheroes:mantapack_flight").setData((entity, data) => data.load(entity.getInterpolatedData("fiskheroes:flight_boost_timer")));
    model.texture.set(texture, lights);

    var jetpack = renderer.createEffect("fiskheroes:model").setModel(model);
    jetpack.anchor.set("body");

    var booster = renderer.createEffect("fiskheroes:booster");
    booster.setIcon(icon).setOffset(0.0, 1.0, 0.0);
    booster.anchor.set("body", model.getCubeOffset("shape10", "shape13"));
    booster.mirror = true;
    booster.opacity = 0.7;

    return {
        model: model,
        jetpack: jetpack,
        booster: booster,
        render: entity => {
            var boost = entity.getInterpolatedData("fiskheroes:flight_boost_timer");
            booster.progress = entity.getInterpolatedData("fiskheroes:dyn/booster_timer");
            booster.speedScale = 0.5 * boost;
            booster.flutter = 1 + boost;

            var f = Math.min(Math.max(boost * 3 - 1.25, 0), 1);
            f = entity.isSprinting() ? 0.5 - Math.cos(2 * f * Math.PI) / 2 : 0;
            booster.setSize(1.75 + f * 2, 3.75 - f * 3);
            booster.render();
            jetpack.render();
        }
    };
}
