function init(hero) {
    hero.setName("hero.fiskheroes.chronos.name");
    hero.setTier(5);

    hero.setHelmet("item.superhero_armor.piece.helmet");
    hero.setChestplate("item.superhero_armor.piece.chestplate");
    hero.setLeggings("item.superhero_armor.piece.leggings");
    hero.setBoots("item.superhero_armor.piece.boots");
    hero.addPrimaryEquipment("fiskheroes:chronos_rifle", true);

    hero.addAttribute("PUNCH_DAMAGE", 5.5, 0);
    hero.addAttribute("WEAPON_DAMAGE", 3.0, 0);
    hero.addAttribute("JUMP_HEIGHT", 0.5, 0);
    hero.addAttribute("FALL_RESISTANCE", 3.5, 0);

    hero.addKeyBind("AIM", "key.aim", -1);

    hero.setHasPermission(hasPermission);
    hero.supplyFunction("canAim", canAim);
}

function hasPermission(entity, permission) {
    return permission == "USE_CHRONOS_RIFLE";
}

function canAim(entity) {
    return entity.getHeldItem().name() == "fiskheroes:chronos_rifle";
}
