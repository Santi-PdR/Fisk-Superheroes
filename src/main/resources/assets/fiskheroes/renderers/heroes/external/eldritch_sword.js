function create(renderer, color, beam) {
    if (typeof beam === "string") {
        beam = renderer.createResource("BEAM_RENDERER", beam);
    }

    var sword_shape = renderer.createResource("SHAPE", "fiskheroes:eldritch_sword");
    var handle_shape = renderer.createResource("SHAPE", "fiskheroes:eldritch_sword_handle");

    var sword = renderer.createEffect("fiskheroes:lines").setShape(sword_shape).setRenderer(beam);
    sword.color.set(color);
    sword.anchor.set("rightArm");

    var handle = renderer.createEffect("fiskheroes:lines").setShape(handle_shape).setRenderer(beam);
    handle.color.set(color);
    handle.anchor.set("rightArm");

    var obj = {
        sword: sword,
        handle: handle,
        setScale: scale => {
            sword.setScale(scale);
            handle.setScale(scale);
            return obj;
        },
        setOffset: (x, y, z) => {
            sword.setOffset(x, y, z);
            handle.setOffset(x, y, z);
            return obj;
        },
        setRotation: (x, y, z) => {
            sword.setRotation(x, y, z);
            handle.setRotation(x, y, z);
            return obj;
        },
        render: timer => {
            sword.progress = handle.progress = timer;
            sword.render();
            handle.render();
        }
    };
    return obj;
}
