function create(renderer, beam, color, map) {
    if (typeof beam === "string") {
        beam = renderer.createResource("BEAM_RENDERER", beam);
    }

    var effects = [];

    for (var i = 0; i < map.length; ++i) {
        var shape = renderer.createResource("SHAPE", null);
        var effect = renderer.createEffect("fiskheroes:lines").setShape(shape);
        var scale = [16.0, 16.0, 16.0];
        var lines = [];

        if (map[i].hasOwnProperty("anchor")) {
            effect.anchor.set(map[i].anchor);
        }

        if (map[i].hasOwnProperty("mirror")) {
            effect.mirror = map[i].mirror;
        }

        if (map[i].hasOwnProperty("offset") && Array.isArray(map[i].offset)) {
            effect.setOffset(map[i].offset[0], map[i].offset[1], map[i].offset[2]);
        }

        if (map[i].hasOwnProperty("rotation") && Array.isArray(map[i].rotation)) {
            effect.setRotation(map[i].rotation[0], map[i].rotation[1], map[i].rotation[2]);
        }

        if (map[i].hasOwnProperty("scale")) {
            if (Array.isArray(map[i].scale)) {
                scale = map[i].scale;
            }
            else if (typeof map[i].scale === "number") {
                scale = [map[i].scale, map[i].scale, map[i].scale];
            }
        }

        if (Array.isArray(map[i].entries)) {
            for (var j = 0; j < map[i].entries.length; ++j) {
                var entry = map[i].entries[j];

                if (entry.hasOwnProperty("start") && Array.isArray(entry.start)) {
                    for (var k = 0; k < entry.start.length; ++k) {
                        entry.start[k] = entry.start[k] / scale[k];
                    }
                }

                if (entry.hasOwnProperty("end") && Array.isArray(entry.end)) {
                    for (var k = 0; k < entry.end.length; ++k) {
                        entry.end[k] = entry.end[k] / scale[k];
                    }
                }

                lines[j] = shape.bindLine(entry);
            }
        }

        effect.color.set(color);
        effect.setScale(scale[0], scale[1], scale[2]);
        effect.setRenderer(beam);
        effects[i] = {
            renderLayer: map[i].renderLayer,
            effect: effect,
            lines: lines
        };
    }

    var obj = {
        opacity: 1.0,
        progress: 1.0,
        effects: effects,
        render: renderLayer => {
            effects.forEach(element => {
                if (element.renderLayer == renderLayer) {
                    element.effect.opacity = obj.opacity;
                    element.effect.progress = obj.progress;
                    element.effect.render();
                }
            });
        }
    };
    return obj;
}
