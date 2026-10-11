function createHands(renderer, icon, mirror) {
    if (typeof icon === "string") {
        icon = renderer.createResource("ICON", icon);
    }

    var front = renderer.createEffect("fiskheroes:booster").setIcon(icon);
    front.setOffset(1.2, 10.0, 0.15).setRotation(-12.0, 5.0, 173.0).setSize(4.75, 2.0);
    front.anchor.set("rightArm");

    var back = renderer.createEffect("fiskheroes:booster").setIcon(icon);
    back.setOffset(1.2, 10.0, -0.15).setRotation(12.0, -5.0, 173.0).setSize(4.75, 2.0);
    back.anchor.set("rightArm");

    var bottom = renderer.createEffect("fiskheroes:booster").setIcon(icon);
    bottom.setOffset(-1.0, 8.4, 0.0).setRotation(0.0, 0.0, 83.0).setSize(4.0, 1.5);
    bottom.anchor.set("rightArm");

    front.opacity = back.opacity = bottom.opacity = 0.7;
    front.flutter = back.flutter = bottom.flutter = 0.25;
    front.speedScale = back.speedScale = bottom.speedScale = 0;
    front.mirror = back.mirror = bottom.mirror = mirror;

    return {
        front: front,
        back: back,
        bottom: bottom,
        render: timer => {
            front.progress = back.progress = bottom.progress = timer;
            front.render();
            back.render();
            bottom.render();
        }
    };
}

function createHead(renderer, icon) {
    if (typeof icon === "string") {
        icon = renderer.createResource("ICON", icon);
    }

    var base = renderer.createEffect("fiskheroes:booster").setIcon(icon);
    base.setOffset(0.0, 1.0, 0.0).setRotation(0.0, 0.0, 180.0);
    base.anchor.set("head");

    var top = renderer.createEffect("fiskheroes:booster").setIcon(icon);
    top.setOffset(0.0, -5.0, 0.0).setRotation(-25.0, 0.0, 180.0).setSize(7.5, 1.75);
    top.anchor.set("head");

    base.opacity = 0.4;
    top.opacity = 0.3;
    base.flutter = top.flutter = 0.25;
    base.speedScale = top.speedScale = 0;
    return {
        base: base,
        top: top,
        render: timer => {
            base.progress = top.progress = timer;
            base.setSize(8 + 2 * timer, 1.75);
            base.render();
            top.render();
        }
    };
}
