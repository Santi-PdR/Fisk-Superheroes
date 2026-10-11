function create(renderer, color, icon, map) {
    if (typeof icon === "string") {
        icon = renderer.createResource("ICON", icon);
    }

    var beam = renderer.createResource("BEAM_RENDERER", "fiskheroes:atom_booster");
    var boosters = [ ];
    var bloom = [ ];

    map.boosters.forEach(element => {
        var result = {
            effect: renderer.createEffect("fiskheroes:booster").setIcon(icon),
            rot: [0, 0, 0], size: [1, 1]
        };

        if (element.hasOwnProperty("size") && Array.isArray(element.size)) {
            result.effect.setSize(element.size[0], element.size[1]);
            result.size = element.size;
        }

        loadPropsFromObj(element, result);
        boosters.push(result);
    });

    map.bloom.forEach(element => {
        var result = createBloom(renderer, "body", false, color, beam);
        result.rot = [0, 0, 0];
        result.size = element.hasOwnProperty("size") && Array.isArray(element.size) ? element.size : [1, 1, 1];
        loadPropsFromObj(element, result);
        bloom.push(result);
    });

    var obj = {
        render: entity => {
            var progress = entity.getInterpolatedData("fiskheroes:dyn/booster_timer");
            var boost = entity.getInterpolatedData("fiskheroes:flight_boost_timer");
            var sboost = entity.getInterpolatedData("fiskheroes:dyn/flight_super_boost_timer");
            var f = 0, f1 = 0;

            if (entity.isSprinting())
            {
                f = 0.5 - Math.cos(2 * Math.min(Math.max(boost * 2 - 0.5, 0), 1) * Math.PI) / 2;
            }

            if (entity.getData("fiskheroes:dyn/flight_super_boost") > 0)
            {
                f1 = 0.5 - Math.cos(2 * Math.min(Math.max(sboost * 2 - 0.5, 0), 1) * Math.PI) / 2;
            }

            var f2 = f / 2 + f1;
            boosters.forEach(element => {
                element.effect.speedScale = 0.5 * boost;
                element.effect.flutter = 1 + boost + sboost;
                element.effect.progress = progress;
                element.effect.setRotation(10 * boost + 20 * f + element.rot[0], element.rot[1], element.rot[2]);
                element.effect.setSize(element.size[0] * (1 + f2 * 0.6), element.size[1] * (1 + f2 * 0.6));
                element.effect.render();
            });

            bloom.forEach(element => {
                element.effect.setRotation(10 * boost + 20 * f + element.rot[0], element.rot[1], element.rot[2]);
                element.effect.progress = progress;
                element.effect.opacity = sboost;

                var width = element.size[0] * (1 + f1 + sboost * 0.2);
                var depth = element.size[1] * (1 + f1 + sboost * 0.2);
                var length = element.size[2] * (1 + f1 * 2 + sboost * 0.5);
                element.render(entity, width, depth, length * 2);
            });

        }
    };
    return obj;
}

function loadPropsFromObj(obj, result) {
    if (obj.hasOwnProperty("anchor")) {
        result.effect.anchor.set(obj.anchor);
    }

    if (obj.hasOwnProperty("mirror")) {
        result.effect.mirror = obj.mirror;
    }

    if (obj.hasOwnProperty("offset") && Array.isArray(obj.offset)) {
        result.effect.setOffset(obj.offset[0], obj.offset[1], obj.offset[2]);
    }

    if (obj.hasOwnProperty("rotation") && Array.isArray(obj.rotation)) {
        result.effect.setRotation(obj.rotation[0], obj.rotation[1], obj.rotation[2]);
        result.rot = obj.rotation;
    }
}

function createBloom(renderer, anchor, mirror, color, beam) {
    if (typeof beam === "string") {
        beam = renderer.createResource("BEAM_RENDERER", beam);
    }
    else if (typeof beam === "undefined") {
        beam = renderer.createResource("BEAM_RENDERER", "fiskheroes:atom_booster");
    }

    var shape = renderer.createResource("SHAPE", null);
    var line = shape.bindLine({ "start": [0.0, 0.0, 0.0], "end": [0.0, 0.0, 0.0], "size": [0.0, 0.0] });
    var effect = renderer.createEffect("fiskheroes:lines").setRenderer(beam).setShape(shape);
    effect.setScale(16.0);
    effect.anchor.set(anchor);
    effect.color.set(color);
    effect.mirror = mirror;

    var obj = {
        effect: effect,
        render: (entity, width, depth, length) => {
            var f = Math.PI * 2;
            f = Math.sin((entity.loop(f) * f) % f * 3) / 5;
            line.size.x = (1 + f) * depth;
            line.size.y = (1 + f) * width;
            line.end.y = (1 + f / 4) * length / 16;
            effect.render();
        }
    }
    return obj;
}
