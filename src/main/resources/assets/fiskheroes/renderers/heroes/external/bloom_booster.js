function create(renderer, anchor, mirror, color, icon, beam) {
    if (typeof icon === "string") {
        icon = renderer.createResource("ICON", icon);
    }

    var booster = renderer.createEffect("fiskheroes:booster").setIcon(icon);
    booster.anchor.set(anchor);
    booster.mirror = mirror;

    if (typeof beam === "string") {
        beam = renderer.createResource("BEAM_RENDERER", beam);
    }

    var shape = renderer.createResource("SHAPE", null);
    var line = shape.bindLine({ "start": [0.0, 0.0, 0.0], "end": [0.0, 0.0, 0.0], "size": [0.0, 0.0] });
    var bloom = renderer.createEffect("fiskheroes:lines").setRenderer(beam).setShape(shape);
    bloom.anchor.set(anchor);
    bloom.color.set(color);
    bloom.mirror = mirror;
    bloom.setScale(16.0);

    var size = { x: 1.0, y: 1.0 };
    var obj = {
        booster: booster,
        bloom: bloom,
        setSize: (x, y) => {
            booster.setSize(x, y);
            size.x = x;
            size.y = y;
            return obj;
        },
        setOffset: (x, y, z) => {
            booster.setOffset(x, y, z);
            bloom.setOffset(x, y, z);
            return obj;
        },
        setRotation: (x, y, z) => {
            booster.setRotation(x, y, z);
            bloom.setRotation(x, y, z);
            return obj;
        },
        render: (entity, progress) => {
            booster.progress = progress;
            booster.render();

            var f = Math.PI * 2;
            f = Math.sin((entity.loop(f) * f) % f * 3) / 5;
            line.size.x = line.size.y = (1 + f * booster.flutter) * size.x;
            line.end.y = (1 + f * booster.flutter / 4) * size.x * size.y / 8;

            bloom.progress = bloom.opacity = progress;
            bloom.render();
        }
    };
    return obj;
}
