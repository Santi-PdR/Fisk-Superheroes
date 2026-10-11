function init(hero) {
    hero.setName("hero.fiskheroes.doctor_strange.name");
    hero.setAliases("strange");
    hero.setTier(2);

    hero.setChestplate("item.superhero_armor.piece.robes");
    hero.setLeggings("item.superhero_armor.piece.pants");
    hero.setBoots("item.superhero_armor.piece.boots");

    hero.addPowers("fiskheroes:cloak_of_levitation", "fiskheroes:eldritch_magic");
    hero.addAttribute("PUNCH_DAMAGE", 1.5, 0);
    hero.addAttribute("WEAPON_DAMAGE", 2.0, 0);
    hero.addAttribute("JUMP_HEIGHT", 0.5, 0);
    hero.addAttribute("FALL_RESISTANCE", 3.5, 0);

    hero.addKeyBind("AIM", "key.aim", -1);
    hero.addKeyBind("BLADE", "key.blade", 1);
    hero.addKeyBind("SHIELD", "key.shield", 1);
    hero.addKeyBind("SPELL_MENU", "key.spellMenu", 2);

    hero.setModifierEnabled(isModifierEnabled);
    hero.setKeyBindEnabled(isKeyBindEnabled);
    hero.supplyFunction("canAim", canAim);

    hero.addAttributeProfile("BLADE", bladeProfile);
    hero.setAttributeProfile(getProfile);
    hero.setDamageProfile(getProfile);
    hero.addDamageProfile("BLADE", {
        "types": {
            "SHARP": 1.0,
            "FIRE": 0.2
        }
    });

    hero.setTickHandler((entity, manager) => {
        if (!entity.isSneaking() && !entity.isOnGround() && entity.motionY() < -0.8) {
            manager.setData(entity, "fiskheroes:flying", true);
        }
    });
}

function bladeProfile(profile) {
    profile.inheritDefaults();
    profile.addAttribute("PUNCH_DAMAGE", 7.0, 0);
}

function getProfile(entity) {
    return entity.getData("fiskheroes:blade") ? "BLADE" : null;
}

function isModifierEnabled(entity, modifier) {
    return modifier.name() != "fiskheroes:shield" || !entity.getData("fiskheroes:blade");
}

function isKeyBindEnabled(entity, keyBind) {
    switch (keyBind) {
    case "SHIELD":
        return !entity.getData("fiskheroes:shield") || !(entity.getData("fiskheroes:shield") && entity.getData("fiskheroes:aiming"));
    case "BLADE":
        return entity.getData("fiskheroes:shield") && entity.getData("fiskheroes:aiming") || entity.getData("fiskheroes:blade") || entity.isBookPlayer();
    case "SPELL_MENU":
        return !entity.getData("fiskheroes:blade") && !entity.getData("fiskheroes:shield");
    default:
        return true;
    }
}

function canAim(entity) {
    return entity.getData("fiskheroes:shield") && entity.getData("fiskheroes:shield_blocking_timer") == 0;
}
