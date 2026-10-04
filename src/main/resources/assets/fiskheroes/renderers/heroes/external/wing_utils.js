var PRESET_GLIDING = {
    timerFunc: entity => entity.getInterpolatedData("fiskheroes:wing_animation_timer"),
    apply: effect => {
        effect.bindAnimation("fiskheroes:wings/gliding").setData((entity, data) => {
            data.load(entity.getPunchTimerInterpolated());
        });
    }
};

var PRESET_CONTROLLED_GLIDING = {
    timerFunc: entity => entity.getInterpolatedData("fiskheroes:wing_animation_timer"),
    apply: effect => {
        effect.bindAnimation("fiskheroes:wings/controlled_gliding").setData((entity, data) => {
            data.load(0, Math.max(1 - PRESET_CONTROLLED_GLIDING.timerFunc(entity), entity.getInterpolatedData("fiskheroes:gliding_timer")));
            data.load(1, entity.getPunchTimerInterpolated());
        });
    }
};

var PRESET_CONTROLLED_FLIGHT = {
    timerFunc: entity => entity.getInterpolatedData("fiskheroes:dyn/wing_timer"),
    apply: effect => {
        effect.bindAnimation("fiskheroes:wings/controlled_flight").setData((entity, data) => {
            data.load(0, Math.max(1 - entity.getInterpolatedData("fiskheroes:flight_timer"), entity.getInterpolatedData("fiskheroes:flight_boost_timer")));
            data.load(1, entity.getPunchTimerInterpolated());
            data.load(2, entity.getInterpolatedData("fiskheroes:barrel_roll_timer"));
            data.load(3, entity.getInterpolatedData("fiskheroes:dyn/flight_super_boost_timer"));
        });
    }
};

function create(renderer, texture, lights, type) {
    var effect = renderer.createEffect("fiskheroes:wings");
    effect.texture.set(texture, lights);
    effect.anchor.set("body");
    type.apply(effect);

    return {
        effect: effect,
        render: (entity, shieldUnfold) => {
            effect.unfold = type.timerFunc(entity);
            effect.shieldUnfold = shieldUnfold;
            effect.render();
        }
    };
}
