function init(hero) {
    hero.setName("hero.fiskheroes.the_tick.name");
    hero.setTier(8);

    hero.setHelmet("item.superhero_armor.piece.cowl");
    hero.setChestplate("item.superhero_armor.piece.chestpiece");
    hero.setLeggings("item.superhero_armor.piece.pants");
    hero.setBoots("item.superhero_armor.piece.boots");

    hero.addPowers("fiskheroes:near_invulnerability", "fiskheroes:leaping");
    hero.addAttribute("PUNCH_DAMAGE", 10.0, 0);
    hero.addAttribute("WEAPON_DAMAGE", -1.0, 0);
    hero.addAttribute("JUMP_HEIGHT", 1.0, 0);
    hero.addAttribute("FALL_RESISTANCE", 8.0, 0);
    hero.addAttribute("SPRINT_SPEED", 0.2, 1);
    hero.addAttribute("STEP_HEIGHT", 0.5, 0);

    hero.setDefaultScale(1.1);
}
