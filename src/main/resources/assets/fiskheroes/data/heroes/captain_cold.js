function init(hero) {
    hero.setName("hero.fiskheroes.captain_cold.name");
    hero.setAliases("cap_cold");
    hero.setTier(1);

    hero.setHelmet("item.superhero_armor.piece.goggles");
    hero.setChestplate("item.superhero_armor.piece.jacket");
    hero.setLeggings("item.superhero_armor.piece.pants");
    hero.setBoots("item.superhero_armor.piece.boots");
    hero.addPrimaryEquipment("fiskheroes:cold_gun", true);

    hero.addPowers("fiskheroes:cold_resistance");
    hero.addAttribute("PUNCH_DAMAGE", 4.0, 0);
    hero.addAttribute("WEAPON_DAMAGE", 2.0, 0);
    hero.addAttribute("JUMP_HEIGHT", 0.5, 0);
    hero.addAttribute("FALL_RESISTANCE", 3.0, 0);

    hero.addKeyBind("AIM", "key.aim", -1);

    hero.setHasPermission(hasPermission);
    hero.supplyFunction("canAim", canAim);

    hero.addSoundEvent("AIM_START", ["fiskheroes:cold_gun_aim", "fiskheroes:cold_gun_static"]);
    //hero.addSoundEvent("AIM_STOP", "fiskheroes:repulsor_powerdown");
}

function hasPermission(entity, permission) {
    return permission == "USE_COLD_GUN";
}

function canAim(entity) {
    return entity.getHeldItem().name() == "fiskheroes:cold_gun";
}
