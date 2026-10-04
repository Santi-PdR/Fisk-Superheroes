function init(hero) {
    hero.setName("hero.fiskheroes.rip_hunter.name");
    hero.setAliases("rip");
    hero.setTier(1);

    hero.setChestplate("item.superhero_armor.piece.trenchcoat");
    hero.setLeggings("item.superhero_armor.piece.pants");
    hero.setBoots("item.superhero_armor.piece.boots");
    hero.addPrimaryEquipment("fiskheroes:rip_hunters_gun", true);

    hero.addAttribute("PUNCH_DAMAGE", 5.0, 0);
    hero.addAttribute("WEAPON_DAMAGE", 2.5, 0);
    hero.addAttribute("JUMP_HEIGHT", 0.5, 0);
    hero.addAttribute("FALL_RESISTANCE", 3.5, 0);
    hero.addAttribute("SPRINT_SPEED", 0.1, 1);

    hero.addKeyBind("AIM", "key.aim", -1);

    hero.setHasPermission(hasPermission);
    hero.supplyFunction("canAim", canAim);
}

function hasPermission(entity, permission) {
    return permission == "USE_RIPS_GUN";
}

function canAim(entity) {
    return entity.getHeldItem().name() == "fiskheroes:rip_hunters_gun";
}
