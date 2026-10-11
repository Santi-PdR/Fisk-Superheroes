function init(hero) {
    hero.setName("hero.fiskheroes.black_manta_dceu.name");
    hero.setVersion("item.superhero_armor.version.dceu");
    hero.setAliases("manta");
    hero.setTier(7);

    hero.setHelmet("item.superhero_armor.piece.helmet");
    hero.setChestplate("item.superhero_armor.piece.chestplate");
    hero.setLeggings("item.superhero_armor.piece.leggings");
    hero.setBoots("item.superhero_armor.piece.boots");

    hero.addPowers("fiskheroes:manta_armor");
    hero.addAttribute("PUNCH_DAMAGE", 7.5, 0);
    hero.addAttribute("WEAPON_DAMAGE", 4.0, 0);
    hero.addAttribute("JUMP_HEIGHT", 0.5, 0);
    hero.addAttribute("FALL_RESISTANCE", 7.0, 0);
    hero.addAttribute("IMPACT_DAMAGE", 0.3, 1);

    hero.addKeyBind("CHARGED_BEAM", "key.mantaRays", 1);
    hero.addKeyBind("BLADE", "key.blade", 2);

    hero.setKeyBindEnabled(isKeyBindEnabled);

    hero.addAttributeProfile("BLADE", bladeProfile);
    hero.setAttributeProfile(getProfile);
    hero.setDamageProfile(getProfile);
    hero.addDamageProfile("BLADE", {
        "types": {
            "SHARP": 1.0,
            "ATLANTEAN_STEEL": 1.0
        }
    });

    hero.addSoundEvent("STEP", "fiskheroes:manta_walk");

    hero.setTickHandler((entity, manager) => {
        manager.incrementData(entity, "fiskheroes:dyn/booster_timer", 2, entity.getData("fiskheroes:flying"));
    });
}

function bladeProfile(profile) {
    profile.inheritDefaults();
    profile.addAttribute("PUNCH_DAMAGE", 9.5, 0);
}

function getProfile(entity) {
    return entity.getData("fiskheroes:blade") ? "BLADE" : null;
}

function isKeyBindEnabled(entity, keyBind) {
    if (entity.isSprinting() && entity.getData("fiskheroes:flying")) {
        return keyBind != "CHARGED_BEAM";
    }
    return true;
}
