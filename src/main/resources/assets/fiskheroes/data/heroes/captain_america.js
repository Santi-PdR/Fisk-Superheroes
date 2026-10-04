function init(hero) {
    hero.setName("hero.fiskheroes.captain_america.name");
    hero.setAliases("cap");
    hero.setTier(6);

    hero.setHelmet("item.superhero_armor.piece.helmet");
    hero.setChestplate("item.superhero_armor.piece.chestpiece");
    hero.setLeggings("item.superhero_armor.piece.pants");
    hero.setBoots("item.superhero_armor.piece.boots");
    hero.addPrimaryEquipment("fiskheroes:captain_americas_shield", true);

    hero.addPowers("fiskheroes:super_soldier_serum", "fiskheroes:shield_throwing");
    hero.addAttribute("PUNCH_DAMAGE", 7.0, 0);
    hero.addAttribute("WEAPON_DAMAGE", 4.5, 0);
    hero.addAttribute("JUMP_HEIGHT", 2.0, 0);
    hero.addAttribute("FALL_RESISTANCE", 6.0, 0);
    hero.addAttribute("SPRINT_SPEED", 0.4, 1);

    hero.addKeyBind("SHIELD_THROW", "key.shieldThrow", 1);

    hero.setKeyBindEnabled(isKeyBindEnabled);
    hero.setHasPermission(hasPermission);
}

function isKeyBindEnabled(entity, keyBind) {
    return keyBind != "SHIELD_THROW" || entity.getHeldItem().name() == "fiskheroes:captain_americas_shield";
}

function hasPermission(entity, permission) {
    return permission == "USE_SHIELD";
}
