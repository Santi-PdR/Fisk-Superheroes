function create(renderer, color, anchor, beam, mirror) {
    if (typeof beam === "string") {
        beam = renderer.createResource("BEAM_RENDERER", beam);
    }

    var eye_shape = renderer.createResource("SHAPE", "fiskheroes:mysterio_eye");
    var triangle_shape = renderer.createResource("SHAPE", "fiskheroes:mysterio_glyph2");
    var currScale = 1;

    var eye = renderer.createEffect("fiskheroes:lines").setShape(eye_shape).setRenderer(beam);
    eye.color.set(color);
    eye.anchor.set(anchor);

    var triangle = renderer.createEffect("fiskheroes:lines").setShape(triangle_shape).setRenderer(beam);
    triangle.color.set(color);
    triangle.anchor.set(anchor);

    eye.mirror = triangle.mirror = mirror;
    var obj = {
        eye: eye,
        triangle: triangle,
        setScale: scale => {
            currScale = scale;
            eye.setScale(scale * 0.4);
            triangle.setScale(scale);
            return obj;
        },
        setOffset: (x, y, z) => {
            eye.setOffset(x, y, z);
            triangle.setOffset(x, y, z);
            return obj;
        },
        setRotation: (x, y, z) => {
            eye.setRotation(x, y, z);
            triangle.setRotation(x, y, z);
            return obj;
        },
        ignoreAnchor: ignore => {
            triangle.anchor.ignoreAnchor(ignore);
            eye.anchor.ignoreAnchor(ignore);
            return obj;
        },
        render: (f) => {
            var timer = Math.min(f * 1.5 - 0.5, 1);
            var timerEye = Math.min(f * 2 - 0.25, 1);
            eye.opacity = timerEye;
            eye.setScale(currScale * 0.4 * (0.8 + timer * 0.2));
            eye.render();
            triangle.opacity = timer;
            timer = 1 - timer;
            timer *= timer;
            timer = 1 - timer;
            //triangle.progress = f;
            triangle.setScale(currScale * (0.2 + timer * 0.8));
            triangle.render();
        }
    };
    return obj;
}
