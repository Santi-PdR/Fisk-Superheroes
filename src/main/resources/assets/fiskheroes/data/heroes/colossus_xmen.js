function init(hero) {
    hero.setName("hero.fiskheroes.colossus_xmen.name");
    hero.setVersion("item.superhero_armor.version.xmen");
    hero.setTier(7);

    hero.setHelmet("item.superhero_armor.piece.head");
    hero.setChestplate("item.superhero_armor.piece.torso");
    hero.setLeggings("item.superhero_armor.piece.pants");
    hero.setBoots("item.superhero_armor.piece.boots");

    hero.addPowers("fiskheroes:organic_steel");
    hero.addAttribute("PUNCH_DAMAGE", 9.0, 0);
    hero.addAttribute("JUMP_HEIGHT", 1.0, 0);
    hero.addAttribute("FALL_RESISTANCE", 8.0, 0);
    hero.addAttribute("STEP_HEIGHT", 0.5, 0);
    hero.addAttribute("KNOCKBACK", 1.0, 0);

    hero.setDefaultScale(1.3);
}
