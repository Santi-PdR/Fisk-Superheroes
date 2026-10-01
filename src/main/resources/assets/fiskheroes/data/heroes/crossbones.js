function init(hero) {
    hero.setName("hero.fiskheroes.crossbones.name");
    hero.setTier(5);

    hero.setHelmet("item.superhero_armor.piece.helmet");
    hero.setChestplate("item.superhero_armor.piece.chestpiece");
    hero.setLeggings("item.superhero_armor.piece.pants");
    hero.setBoots("item.superhero_armor.piece.boots");

    hero.addPowers("fiskheroes:charged_punch", "fiskheroes:pain_resistance");
    hero.addAttribute("PUNCH_DAMAGE", 6.0, 0);
    hero.addAttribute("WEAPON_DAMAGE", 3.0, 0);
    hero.addAttribute("JUMP_HEIGHT", 0.5, 0);
    hero.addAttribute("FALL_RESISTANCE", 4.0, 0);
    hero.addAttribute("MAX_HEALTH", 2.0, 0);

    hero.addKeyBind("CHARGED_PUNCH", "key.punchToggle", 1);

    hero.addAttributeProfile("PUNCH", punchProfile);
    hero.addAttributeProfile("BLUNT", bluntProfile);
    hero.setAttributeProfile(getAttributeProfile);
}

function punchProfile(profile) {
    profile.inheritDefaults();
    profile.addAttribute("PUNCH_DAMAGE", 8.5, 0);
    profile.addAttribute("KNOCKBACK", 2.5, 0);
}

function bluntProfile(profile) {
    profile.inheritDefaults();
    profile.addAttribute("PUNCH_DAMAGE", 5.5, 0);
    profile.addAttribute("FALL_RESISTANCE", 3.5, 0);
    profile.addAttribute("BASE_SPEED", -0.2, 1);
}

function getAttributeProfile(entity) {
    return entity.getData("fiskheroes:punchmode") ? entity.getData("fiskheroes:punch_charged") ? "PUNCH" : "BLUNT" : null;
}
