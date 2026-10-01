var lengths = [1.9, 2.55, 2.675, 2.8, 2.9];

function create(renderer, anchor, energyColor) {
    var c1 = renderer.createEffect("fiskheroes:shield");
    c1.texture.set("cannon1", "cannon1_lights");
    c1.anchor.set(anchor);
    c1.setRotation(3.0, 90.0, 3.0).setCurve(30.0, 136.0);
    c1.large = true;

    var c2 = renderer.createEffect("fiskheroes:shield");
    c2.texture.set("cannon2", "cannon2_lights");
    c2.anchor.set(anchor);
    c2.setRotation(-7.0, -90.0, 7.0).setCurve(60.0, 136.0);
    c2.large = true;

    var c3 = renderer.createEffect("fiskheroes:shield");
    c3.texture.set(null, "cannon_inner");
    c3.anchor.set(anchor);
    c3.setRotation(0.0, 0.0, -90.0).setCurve(0.0, -77.0);

    var shape = renderer.createResource("SHAPE", null);
    var energy = renderer.createEffect("fiskheroes:lines");
    energy.anchor.set(anchor);
    energy.color.set(energyColor);
    energy.setOffset(1.0, 13.75, -0.25);

    for (var i = 0; i < lengths.length; ++i) {
        var y = i * 0.5;
        shape.bindLine({
            "start": [0.0, y, lengths[i]],
            "end": [0.0, y, -lengths[i]],
            "size": [1.0, 1.0]
        });
    }

    energy.setShape(shape);
    energy.setRenderer(renderer.createResource("BEAM_RENDERER", "fiskheroes:crab_cannon"));
    return {
        c1: c1,
        c2: c2,
        c3: c3,
        energy: energy,
        render: timer => {
            var f = Math.min(timer * 5, 1);
            c1.unfold = c2.unfold = timer;
            c1.setOffset(1.25, 6.3 + f * 2.0, 1.67 + f);
            c2.setOffset(1.25, 6.9 + f * 2.0, -1.67 - f);
            c3.unfold = Math.max(1 - (1 - timer) * 2, 0);
            f = Math.min(timer * 5, 1);
            c3.setOffset(2.25, 8.05 + f * 5.0, -0.25);
            c1.render();
            c2.render();
            c3.render();

            energy.progress = c3.unfold;
            energy.render();
        }
    };
}
