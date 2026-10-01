function addKeyBind(hero, boostName, boostKey) {
    hero.addKeyBindFunc("func_BOOST", (player, manager) => {
        manager.setData(player, "fiskheroes:dyn/flight_super_boost", 1);
        return true;
    }, boostName, boostKey);
}

function tick(entity, manager) {
    if (entity.getData("fiskheroes:dyn/flight_super_boost") == 1) {
        manager.setData(entity, "fiskheroes:dyn/flight_super_boost", 2);
        manager.setData(entity, "fiskheroes:flying", true);
        manager.setData(entity, "fiskheroes:flight_timer", entity.getData("fiskheroes:prev_flight_timer"));
        manager.setData(entity, "fiskheroes:flight_boost_timer", entity.getData("fiskheroes:prev_flight_boost_timer"));
    }
    else if (!(entity.isSprinting() && entity.getData("fiskheroes:flying")) && entity.getData("fiskheroes:dyn/flight_super_boost") > 0) {
        manager.setData(entity, "fiskheroes:dyn/flight_super_boost", 0);
    }
}

function isModifierEnabled(entity, modifier) {
    if (modifier.name() == "fiskheroes:controlled_flight") {
        var boost = entity.getData("fiskheroes:dyn/flight_super_boost");
        return boost != 1 && (modifier.id() == "boosted") == (boost > 0);
    }
    return true;
}

function isKeyBindEnabled(entity, keyBind) {
    if (keyBind == "func_BOOST") {
        return entity.isSprinting() && entity.getData("fiskheroes:flying") && entity.getData("fiskheroes:dyn/flight_super_boost") == 0;
    }
    return true;
}
