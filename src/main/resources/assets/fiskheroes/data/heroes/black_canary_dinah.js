function init(hero) {
    hero.setName("hero.fiskheroes.black_canary_dinah.name");
    hero.setTier(4);

    hero.setHelmet("item.superhero_armor.piece.mask");
    hero.setChestplate("item.superhero_armor.piece.chestpiece");
    hero.setLeggings("item.superhero_armor.piece.pants");
    hero.setBoots("item.superhero_armor.piece.boots");
    hero.addPrimaryEquipment("fiskheroes:bo_staff", true);

    hero.addPowers("fiskheroes:canary_cry");
    hero.addAttribute("PUNCH_DAMAGE", 5.0, 0);
    hero.addAttribute("WEAPON_DAMAGE", 3.5, 0);
    hero.addAttribute("JUMP_HEIGHT", 1.0, 0);
    hero.addAttribute("FALL_RESISTANCE", 4.0, 0);
    hero.addAttribute("SPRINT_SPEED", 0.15, 1);

    hero.addKeyBind("SONIC_WAVES", "key.canaryCry", 1);
}
