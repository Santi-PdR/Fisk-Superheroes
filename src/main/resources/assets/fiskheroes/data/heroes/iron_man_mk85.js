var landing = implement("fiskheroes:external/superhero_landing");

function init(hero) {
    hero.setName("hero.fiskheroes.iron_man_mk85.name");
    hero.setTier(8);

    hero.setChestplate("item.superhero_armor.piece.arc_reactor");

    hero.addPowers("fiskheroes:mk85_nanites");
    hero.addAttribute("PUNCH_DAMAGE", 8.5, 0);
    hero.addAttribute("WEAPON_DAMAGE", 1.5, 0);
    hero.addAttribute("SPRINT_SPEED", 0.1, 1);
    hero.addAttribute("JUMP_HEIGHT", 0.5, 0);
    hero.addAttribute("FALL_RESISTANCE", 8.0, 0);

    hero.addKeyBind("CHARGED_BEAM", "key.repulsorBeams", 1);
    hero.addKeyBind("BLADE", "key.blade", 2);
    hero.addKeyBind("SHIELD", "key.shield", 2);
    hero.addKeyBind("NANITE_TRANSFORM", "key.naniteTransform", 5);

    hero.setModifierEnabled(isModifierEnabled);
    hero.setKeyBindEnabled(isKeyBindEnabled);
    hero.setHasProperty(hasProperty);
    hero.setTierOverride(getTierOverride);

    hero.addAttributeProfile("INACTIVE", inactiveProfile);
    hero.addAttributeProfile("BLADE", bladeProfile);
    hero.setAttributeProfile(getProfile);
    hero.setDamageProfile(getProfile);
    hero.addDamageProfile("BLADE", {
        "types": {
            "SHARP": 1.0,
            "ENERGY": 0.7
        }
    });

    hero.addSoundEvent("MASK_OPEN", "fiskheroes:mk50_mask_open");
    hero.addSoundEvent("MASK_CLOSE", "fiskheroes:mk50_mask_close");

    hero.setTickHandler((entity, manager) => {
        var flying = entity.getData("fiskheroes:flying");
        manager.incrementData(entity, "fiskheroes:dyn/booster_timer", 2, flying);

        var item = entity.getHeldItem();
        flying &= !entity.getData("fiskheroes:beam_charging") && !entity.as("PLAYER").isUsingItem();
        manager.incrementData(entity, "fiskheroes:dyn/booster_r_timer", 2, flying && item.isEmpty() && !entity.isPunching() && entity.getData("fiskheroes:blade_timer") == 0);
        manager.incrementData(entity, "fiskheroes:dyn/booster_l_timer", 2, flying && !item.doesNeedTwoHands());

        landing.tick(entity, manager);
    });
}

function getTierOverride(entity) {
    return entity.getData("fiskheroes:dyn/nanites") ? 8 : 0;
}

function inactiveProfile(profile) {
    profile.revokeAugments();
}

function bladeProfile(profile) {
    profile.inheritDefaults();
    profile.addAttribute("PUNCH_DAMAGE", 10.5, 0);
    profile.addAttribute("JUMP_HEIGHT", 1.5, 0);
    profile.addAttribute("FALL_RESISTANCE", 9.5, 0);
}

function getProfile(entity) {
    if (!entity.getData("fiskheroes:dyn/nanites")) {
        return "INACTIVE";
    }
    return entity.getData("fiskheroes:blade") ? "BLADE" : null;
}

function isModifierEnabled(entity, modifier) {
    if (modifier.name() != "fiskheroes:transformation" && modifier.name() != "fiskheroes:cooldown" && (!entity.getData("fiskheroes:dyn/nanites") || modifier.name() == "fiskheroes:controlled_flight" && entity.getData("fiskheroes:dyn/nanite_timer") < 1)) {
        return false;
    }

    switch (modifier.name()) {
        case "fiskheroes:blade":
            return entity.getData("fiskheroes:beam_charge") == 0 && !entity.getData("fiskheroes:aiming") && !(entity.getData("fiskheroes:shield") && entity.getData("fiskheroes:blade")) && !(entity.isSprinting() && entity.getData("fiskheroes:flying"));
        case "fiskheroes:shield":
            return entity.getData("fiskheroes:beam_charge") == 0 && !entity.getData("fiskheroes:aiming") && !(entity.getData("fiskheroes:shield") && entity.getData("fiskheroes:blade")) && !entity.getData("fiskheroes:flying");
        case "fiskheroes:regeneration":
            return !entity.getData("fiskheroes:blade");
        case "fiskheroes:water_breathing":
            return !entity.getData("fiskheroes:mask_open") && entity.getData("fiskheroes:dyn/nanites");
        default:
            return true;
    }
}

function isKeyBindEnabled(entity, keyBind) {
    if (keyBind == "NANITE_TRANSFORM") {
        return entity.getData("fiskheroes:mask_open_timer") == 0;
    }
    else if (!entity.getData("fiskheroes:dyn/nanites")) {
        return false;
    }
    else if (entity.isSprinting() && entity.getData("fiskheroes:flying")) {
        return false;
    }

    switch (keyBind) {
        case "SHIELD":
            return (entity.getData("fiskheroes:blade_timer") == 0 || entity.isBookPlayer()) && !entity.getData("fiskheroes:flying");
        case "BLADE":
            return entity.getData("fiskheroes:shield_timer") > 0 || entity.getData("fiskheroes:blade_timer") > 0 || entity.isBookPlayer() || entity.getData("fiskheroes:flying");
        default:
            return true;
    }
}

function hasProperty(entity, property) {
    switch (property) {
        case "MASK_TOGGLE":
            return entity.getData("fiskheroes:dyn/nanite_timer") == 1;
        case "BREATHE_SPACE":
            return !entity.getData("fiskheroes:mask_open") && entity.getData("fiskheroes:dyn/nanites");
        default:
            return false;
    }
}
