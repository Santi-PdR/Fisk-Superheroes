function init(hero, super_boost, boostKey, sprintSpeed, tickHandler) {
    super_boost.addKeyBind(hero, "key.boost", boostKey);

    hero.setAttributeProfile(entity => entity.getData("fiskheroes:dyn/flight_super_boost") > 0 ? "SUPER_BOOST" : null);
    hero.addAttributeProfile("SUPER_BOOST", profile => {
        profile.inheritDefaults();
        profile.addAttribute("SPRINT_SPEED", sprintSpeed, 1);
    });

    hero.setTickHandler((entity, manager) => {
        var boost = entity.getData("fiskheroes:dyn/flight_super_boost");
        var flying = boost > 0 || entity.getData("fiskheroes:flying");
        manager.incrementData(entity, "fiskheroes:dyn/booster_timer", 2, flying);
        manager.incrementData(entity, "fiskheroes:dyn/wing_timer", 6, 7, flying);

        super_boost.tick(entity, manager);
        manager.incrementData(entity, "fiskheroes:dyn/flight_super_boost_timer", 4, 14, boost > 0);

        if (typeof tickHandler !== "undefined") {
            tickHandler(entity, manager);
        }
    });
}
