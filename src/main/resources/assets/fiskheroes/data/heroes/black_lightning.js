function init(hero) {
    hero.setName("hero.fiskheroes.black_lightning.name");
    hero.setAliases("bl");
    hero.setTier(6);

    hero.setHelmet("item.superhero_armor.piece.mask");
    hero.setChestplate("item.superhero_armor.piece.chestplate");
    hero.setLeggings("item.superhero_armor.piece.leggings");
    hero.setBoots("item.superhero_armor.piece.boots");

    hero.addPowers("fiskheroes:electrokinesis");
    hero.addAttribute("PUNCH_DAMAGE", 6.5, 0);
    hero.addAttribute("WEAPON_DAMAGE", 2.0, 0);
    hero.addAttribute("JUMP_HEIGHT", 1.0, 0);
    hero.addAttribute("FALL_RESISTANCE", 6.0, 0);

    hero.setHasProperty((entity, property) => property == "MASK_TOGGLE");
}
