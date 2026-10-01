var firestorm = implement("fiskheroes:external/firestorm_base");

function init(hero) {
    hero.setName("hero.fiskheroes.firestorm.name");
    hero.setTier(4);

    hero.setChestplate("item.superhero_armor.piece.jacket");
    hero.setLeggings("item.superhero_armor.piece.pants");
    hero.setBoots("item.superhero_armor.piece.shoes");

    hero.addPowers("fiskheroes:firestorm_matrix");
    hero.addAttribute("PUNCH_DAMAGE", 5.0, 0);
    hero.addAttribute("JUMP_HEIGHT", 0.5, 0);
    hero.addAttribute("FALL_RESISTANCE", 5.0, 0);

    firestorm.init(hero);
}
