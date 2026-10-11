function init(hero) {
    hero.setName("hero.fiskheroes.guardian.name");
    hero.setTier(5);

    hero.setHelmet("item.superhero_armor.piece.helmet");
    hero.setChestplate("item.superhero_armor.piece.chestplate");
    hero.setLeggings("item.superhero_armor.piece.leggings");
    hero.setBoots("item.superhero_armor.piece.boots");

    hero.addPowers("fiskheroes:retractable_shield");
    hero.addAttribute("PUNCH_DAMAGE", 5.5, 0);
    hero.addAttribute("WEAPON_DAMAGE", 2.5, 0);
    hero.addAttribute("JUMP_HEIGHT", 0.5, 0);
    hero.addAttribute("FALL_RESISTANCE", 4.0, 0);

    hero.addKeyBind("SHIELD", "key.shield", 1);

    hero.addAttributeProfile("SHIELD", shieldProfile);
    hero.setAttributeProfile(getProfile);
    hero.setDamageProfile(getProfile);
    hero.addDamageProfile("SHIELD", {
        "types": {
            "BLUNT": 0.7,
            "SHARP": 0.3
        }
    });
}

function shieldProfile(profile) {
    profile.inheritDefaults();
    profile.addAttribute("PUNCH_DAMAGE", 6.5, 0);
}

function getProfile(entity) {
    return entity.getData("fiskheroes:shield") ? "SHIELD" : null;
}
