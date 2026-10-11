function init(hero) {
    hero.setName("hero.fiskheroes.deadpool_xmen.name");
    hero.setVersion("item.superhero_armor.version.xmen");
    hero.setAliases("dp");
    hero.setTier(5);

    hero.setHelmet("item.superhero_armor.piece.mask");
    hero.setChestplate("item.superhero_armor.piece.chestpiece");
    hero.setLeggings("item.superhero_armor.piece.pants");
    hero.setBoots("item.superhero_armor.piece.boots");
    hero.addPrimaryEquipment("fiskheroes:katana{Dual:1}", true, item => item.nbt().getBoolean("Dual"));
    hero.addPrimaryEquipment("fiskheroes:desert_eagle{Dual:1}", true, item => item.nbt().getBoolean("Dual"));

    hero.addPowers("fiskheroes:healing_factor");
    hero.addAttribute("PUNCH_DAMAGE", 5.0, 0);
    hero.addAttribute("WEAPON_DAMAGE", 5.0, 0);
    hero.addAttribute("JUMP_HEIGHT", 1.0, 0);
    hero.addAttribute("FALL_RESISTANCE", 4.5, 0);
    hero.addAttribute("SPRINT_SPEED", 0.15, 1);

    hero.addKeyBind("AIM", "key.aim", -1);
    hero.addKeyBind("GUN_RELOAD", "key.reload", 1);

    hero.setKeyBindEnabled((entity, keyBind) => keyBind != "GUN_RELOAD" || entity.getHeldItem().isGun() && !entity.getData("fiskheroes:aiming"));
    hero.setHasPermission((entity, permission) => permission == "USE_GUN");
    hero.supplyFunction("canAim", entity => entity.getHeldItem().isGun());
}
