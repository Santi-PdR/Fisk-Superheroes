function init(renderer, trailType) {
    var shake = renderer.bindProperty("fiskheroes:camera_shake").setCondition(entity => {
        var speed = entity.getData("fiskheroes:speed");
        shake.factor = speed > 1 && entity.isSprinting() && entity.getData("fiskheroes:speed_sprinting") ? (Math.log(speed - 1) + 1) * 0.5 * Math.sin(Math.PI * entity.getInterpolatedData("fiskheroes:dyn/speed_sprint_timer")) : 0;
        return true;
    });
    shake.intensity = 0.05;

    if (typeof trailType !== "undefined") {
        bindTrail(renderer, trailType);
    }

    var anim = renderer.createResource("ANIMATION", "fiskheroes:speedster_sprint");
    anim.setData((entity, data) => data.load(entity.getInterpolatedData("fiskheroes:dyn/speed_sprint_timer")));
    renderer.addCustomAnimation("speedster.SPRINT", anim);
}

function bindTrail(renderer, trailType) {
    var prop = renderer.bindProperty("fiskheroes:trail");
    prop.setTrail(renderer.createResource("TRAIL", trailType));
    prop.setCondition(entity => entity.getData("fiskheroes:speeding"));
    return prop;
}
