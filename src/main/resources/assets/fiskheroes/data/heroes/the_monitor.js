function init(hero) {
    hero.setName("hero.fiskheroes.the_monitor.name");
    hero.setTier(10);

    hero.setChestplate("item.superhero_armor.piece.chestplate");
    hero.setLeggings("item.superhero_armor.piece.pants");
    hero.setBoots("item.superhero_armor.piece.boots");

    hero.addPowers("fiskheroes:cosmic_physiology");
    hero.addAttribute("PUNCH_DAMAGE", 13.0, 0);
    hero.addAttribute("WEAPON_DAMAGE", -2.0, 0);
    hero.addAttribute("JUMP_HEIGHT", 1.5, 0);
    hero.addAttribute("FALL_RESISTANCE", 1.0, 1);

    hero.addKeyBind("ENERGY_PROJECTION", "key.cosmicBeam", 1);
    hero.addKeyBind("TELEKINESIS", "key.telekinesis", 2);
    hero.addKeyBind("TELEPORT", "key.teleport", 3);

    hero.setDefaultScale(1.1);
    hero.setModifierEnabled(isModifierEnabled);
    hero.setKeyBindEnabled(isKeyBindEnabled);
    hero.setHasProperty(hasProperty);
}

function isModifierEnabled(entity, modifier) {
    return modifier.name() != "fiskheroes:arrow_catching" && modifier.name() != "fiskheroes:energy_projection" && modifier.name() != "fiskheroes:teleportation" || !entity.getData("fiskheroes:telekinesis");
}

function isKeyBindEnabled(entity, keyBind) {
    return keyBind != "ENERGY_PROJECTION" && keyBind != "TELEPORT" || !entity.getData("fiskheroes:telekinesis");
}

function hasProperty(entity, property) {
    return property == "BREATHE_SPACE";
}
