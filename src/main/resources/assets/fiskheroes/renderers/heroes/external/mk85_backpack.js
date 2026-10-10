function create(renderer, texture) {
    var b1 = renderer.createEffect("fiskheroes:shield");
    b1.texture.set(texture);
    b1.anchor.set("body");
    b1.setRotation(-90.0, 45.0, 120.0).setCurve(70.0, 160.0);

    var b2 = renderer.createEffect("fiskheroes:shield");
    b2.texture.set(texture);
    b2.anchor.set("body");
    b2.setRotation(-90.0, -25.0, 120.0).setCurve(70.0, 160.0);

    var b3 = renderer.createEffect("fiskheroes:shield");
    b3.texture.set(texture);
    b3.anchor.set("body");
    b3.setRotation(-90.0, -60.0, 120.0).setCurve(70.0, 160.0);

    b1.mirror = b2.mirror = b3.mirror = true;
    return {
        b1: b1,
        b2: b2,
        b3: b3,
        render: timer => {
            var f = Math.min(timer * 4, 1);
            b1.unfold = b2.unfold = b3.unfold = f;

            f = Math.min(f * 5, 1);
            b1.setOffset(1.5 * f, 5.0, 3.75 * f);
            b2.setOffset(3.5 * f, 5.0 - 1.0 * f, 3.75 * f);
            b3.setOffset(3.0 * f, 5.0 - 3.0 * f, 3.75 * f);
            b1.render();
            b2.render();
            b3.render();
        }
    };
}
