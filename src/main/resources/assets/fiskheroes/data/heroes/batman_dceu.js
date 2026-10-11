function init(hero) {
    hero.setName("hero.fiskheroes.batman_dceu.name");
    hero.setVersion("item.superhero_armor.version.dceu");
    hero.setTier(6);

    hero.setHelmet("item.superhero_armor.piece.cowl");
    hero.setChestplate("item.superhero_armor.piece.chestpiece");
    hero.setLeggings("item.superhero_armor.piece.pants");
    hero.setBoots("item.superhero_armor.piece.boots");
    hero.addPrimaryEquipment("fiskheroes:grappling_gun", true);

    hero.addPowers("fiskheroes:batsuit");
    hero.addAttribute("PUNCH_DAMAGE", 6.5, 0);
    hero.addAttribute("WEAPON_DAMAGE", 3.5, 0);
    hero.addAttribute("JUMP_HEIGHT", 1.0, 0);
    hero.addAttribute("FALL_RESISTANCE", 6.0, 0);
    hero.addAttribute("SPRINT_SPEED", 0.2, 1);
    hero.addAttribute("IMPACT_DAMAGE", 0.5, 1);

    hero.addKeyBind("UTILITY_BELT", "key.utilityBelt", 1);

    hero.setHasPermission((entity, permission) => permission == "USE_GRAPPLING_GUN");
}
