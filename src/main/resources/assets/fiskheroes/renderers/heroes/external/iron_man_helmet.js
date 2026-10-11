function createFolding(renderer, texture, lights, timerFunc) {
    if (typeof timerFunc === "string") {
        var timer = timerFunc;
        timerFunc = entity => entity.getInterpolatedData(timer);
    }

    var model = renderer.createResource("MODEL", "fiskheroes:mk46_helmet");
    model.bindAnimation("fiskheroes:mk46_helmet").setData((entity, data) => data.load(timerFunc(entity)));

    var effect = renderer.createEffect("fiskheroes:model").setModel(model);
    effect.anchor.set("head");
    return {
        model: model,
        effect: effect,
        render: entity => {
            var timer = timerFunc(entity);

            if (timer > 0) {
                if (timer < 1) {
                    model.texture.set(texture, lights);
                }
                else {
                    model.texture.set(texture);
                }

                effect.render();
            }
        }
    };
}

function createFaceplate(renderer, texture, lights) {
    var effect = renderer.createEffect("fiskheroes:opening_mask");
    effect.setOffset(0.0, -3.5, -7.0).setRotation(-80.0, 0.0, 0.0);
    effect.texture.set(texture, lights);
    effect.anchor.set("head");
    return {
        effect: effect,
        render: timer => {
            effect.progress = timer;
            effect.render();
        }
    };
}
