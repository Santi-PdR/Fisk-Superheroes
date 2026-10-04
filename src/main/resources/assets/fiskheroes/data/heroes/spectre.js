function init(hero) {
    hero.setName("hero.fiskheroes.spectre.name");
    hero.setTier(10);

    hero.setHelmet("item.superhero_armor.piece.cloak");
    hero.setChestplate("item.superhero_armor.piece.chestpiece");
    hero.setLeggings("item.superhero_armor.piece.pants");
    hero.setBoots("item.superhero_armor.piece.boots");

    hero.addPowers("fiskheroes:spirit_physiology");
    hero.addAttribute("PUNCH_DAMAGE", 13.0, 0);
    hero.addAttribute("WEAPON_DAMAGE", -1.0, 0);
    hero.addAttribute("JUMP_HEIGHT", 1.0, 0);
    hero.addAttribute("FALL_RESISTANCE", 1.0, 1);

    hero.addKeyBind("ENERGY_PROJECTION", "key.energyProjection", 1);
    hero.addKeyBind("TELEPORT", "key.teleport", 2);

    hero.setHasProperty(hasProperty);
}

function hasProperty(entity, property) {
    return property == "BREATHE_SPACE";
}
