function init(hero) {
    hero.setName("hero.fiskheroes.anti_monitor.name");
    hero.setTier(10);

    hero.setHelmet("item.superhero_armor.piece.head");
    hero.setChestplate("item.superhero_armor.piece.chestplate");
    hero.setLeggings("item.superhero_armor.piece.pants");
    hero.setBoots("item.superhero_armor.piece.boots");

    hero.addPowers("fiskheroes:antimatter_physiology");
    hero.addAttribute("PUNCH_DAMAGE", 13.0, 0);
    hero.addAttribute("WEAPON_DAMAGE", -2.0, 0);
    hero.addAttribute("JUMP_HEIGHT", 1.5, 0);
    hero.addAttribute("FALL_RESISTANCE", 1.0, 1);

    hero.addKeyBind("ENERGY_PROJECTION", "key.antimatterBeam", 1);
    hero.addKeyBind("CHARGED_BEAM", "key.antimatterBlast", 1);
    hero.addKeyBind("SHADOWFORM", "key.shadowForm", 2);
    hero.addKeyBindFunc("func_GIANT_MODE", giantModeKey, "key.giantMode", 3);
    hero.addKeyBind("SHIELD", "key.forcefield", 4);

    hero.setDefaultScale(1.1);
    hero.addAttributeProfile("SHIELD", shieldProfile);
    hero.setAttributeProfile(getAttributeProfile);
    hero.setModifierEnabled(isModifierEnabled);
    hero.setKeyBindEnabled(isKeyBindEnabled);
    hero.setHasProperty(hasProperty);

    hero.addSoundEvent("LAND", "fiskheroes:anti_land");
    hero.addSoundEvent("PUNCH", "fiskheroes:anti_punch");
    hero.addSoundEvent("STEP", "fiskheroes:anti_walk");
}

function giantModeKey(player, manager) {
    var flag = player.getData("fiskheroes:dyn/giant_mode");
    manager.setData(player, "fiskheroes:dyn/giant_mode", !flag);
    manager.setData(player, "fiskheroes:size_state", flag ? -1 : 1);
    return true;
}

function shieldProfile(profile) {
    profile.inheritDefaults();
    profile.addAttribute("BASE_SPEED", -0.75, 1);
    profile.addAttribute("JUMP_HEIGHT", -2.0, 1);
}

function getAttributeProfile(entity) {
    return entity.getData("fiskheroes:shield_blocking") ? "SHIELD" : null;
}

function isModifierEnabled(entity, modifier) {
    switch (modifier.name()) {
    case "fiskheroes:arrow_catching":
        return entity.getData("fiskheroes:scale") <= 1.11 && !entity.getData("fiskheroes:shield_blocking");
    case "fiskheroes:energy_projection":
        return entity.getData("fiskheroes:scale") <= 1.11;
    case "fiskheroes:charged_beam":
        return entity.getData("fiskheroes:scale") > 1.11;
    case "fiskheroes:flight":
        return entity.getData("fiskheroes:shadowform");
    case "fiskheroes:leaping":
        return !entity.getData("fiskheroes:shadowform") && !entity.getData("fiskheroes:shield_blocking");
    default:
        return true;
    }
}

function isKeyBindEnabled(entity, keyBind) {
    if (keyBind != "SHADOWFORM" && entity.getData("fiskheroes:shadowform") || keyBind != "SHIELD" && entity.getData("fiskheroes:shield_blocking")) {
        return false;
    }

    switch (keyBind) {
    case "func_GIANT_MODE":
        var timer = entity.getData("fiskheroes:dyn/giant_mode_timer");
        return timer == 0 || timer == 1;
    case "CHARGED_BEAM":
        return entity.getHeldItem().isEmpty() && entity.getData("fiskheroes:scale") > 1.11 && entity.getData("fiskheroes:size_state") == 0;
    case "SHIELD":
        return entity.getHeldItem().isEmpty() && entity.getData("fiskheroes:scale") <= 1.11;
    default:
        return entity.getData("fiskheroes:scale") <= 1.11;
    }
}

function hasProperty(entity, property) {
    return property == "BREATHE_SPACE";
}
