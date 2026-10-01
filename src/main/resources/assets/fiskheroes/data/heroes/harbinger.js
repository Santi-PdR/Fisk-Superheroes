function init(hero) {
    hero.setName("hero.fiskheroes.harbinger.name");
    hero.setTier(9);

    hero.setHelmet("item.superhero_armor.piece.hair");
    hero.setChestplate("item.superhero_armor.piece.tunic");
    hero.setLeggings("item.superhero_armor.piece.pants");
    hero.setBoots("item.superhero_armor.piece.boots");

    hero.addPowers("fiskheroes:cosmic_empowerment");
    hero.addAttribute("PUNCH_DAMAGE", 12.0, 0);
    hero.addAttribute("WEAPON_DAMAGE", 2.5, 0);
    hero.addAttribute("JUMP_HEIGHT", 1.0, 0);
    hero.addAttribute("FALL_RESISTANCE", 11.0, 0);
    hero.addAttribute("SPRINT_SPEED", 0.10, 1);

    hero.addKeyBind("ENERGY_PROJECTION", "key.cosmicBeam", 1);
    hero.addKeyBind("AIM", "key.gravityManip", 2);
    hero.addKeyBind("GRAVITY_MANIPULATION", "key.gravityManip", 2);
    hero.addKeyBind("TELEPORT", "key.teleport", 3);

    hero.setKeyBindEnabled(isKeyBindEnabled);
    hero.setHasProperty(hasProperty);
    hero.supplyFunction("canAim", canAim);
}

function isKeyBindEnabled(entity, keyBind) {
    switch (keyBind) {
    case "ENERGY_PROJECTION":
        return !entity.getData("fiskheroes:gravity_manip");
    case "GRAVITY_MANIPULATION":
        return !entity.getData("fiskheroes:energy_projection");
    case "AIM":
        return !entity.getData("fiskheroes:energy_projection");
    default:
        return true;
    }
}

function hasProperty(entity, property) {
    return property == "BREATHE_SPACE";
}

function canAim(entity) {
    return entity.getHeldItem().isEmpty() && entity.getData("fiskheroes:gravity_manip");
}
