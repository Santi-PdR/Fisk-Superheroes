var firestorm = implement("fiskheroes:external/firestorm_base");

function init(hero) {
    hero.setName("hero.fiskheroes.firestorm_jax.name");
    hero.setAliases("jax");
    hero.setTier(5);

    hero.setChestplate("item.superhero_armor.piece.chestpiece");
    hero.setLeggings("item.superhero_armor.piece.pants");
    hero.setBoots("item.superhero_armor.piece.boots");

    hero.addPowers("fiskheroes:firestorm_matrix");
    hero.addAttribute("PUNCH_DAMAGE", 6.0, 0);
    hero.addAttribute("SPRINT_SPEED", 0.15, 1);
    hero.addAttribute("JUMP_HEIGHT", 0.5, 0);
    hero.addAttribute("FALL_RESISTANCE", 5.5, 0);

    firestorm.init(hero);
}
