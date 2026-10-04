extend("fiskheroes:hero_basic");
loadTextures({
    "layer1": "fiskheroes:rip_hunter_time_master",
    "layer2": "fiskheroes:rip_hunter_pants"
});

function init(renderer) {
    parent.init(renderer);
    renderer.setItemIcons("rip_hunter_0", "%s_1", "rip_hunter_2", "rip_hunter_3")
}

function initEffects(renderer) {
    renderer.bindProperty("fiskheroes:energy_bolt").color.set(0x005EFF);
    renderer.bindProperty("fiskheroes:equipped_item").setItems([
        { "anchor": "rightLeg", "scale": 0.325, "offset": [-2.0, 0.0, 1.5], "rotation": [90.0, 0.0, -7.0] }
    ]);
}
