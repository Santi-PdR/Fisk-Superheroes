var speedster_base = implement("fiskheroes:external/speedster_base");

function init(hero) {
    hero.setName("hero.fiskheroes.trajectory.name");
    hero.setTier(3);

    hero.setHelmet("item.superhero_armor.piece.mask");
    hero.setChestplate("item.superhero_armor.piece.chestpiece");
    hero.setLeggings("item.superhero_armor.piece.pants");
    hero.setBoots("item.superhero_armor.piece.boots");
    hero.addEquipment("fiskheroes:flash_ring");

    hero.addPowers("fiskheroes:velocity_nine");
    hero.addAttribute("PUNCH_DAMAGE", 3.5, 0);
    hero.addAttribute("JUMP_HEIGHT", 1.0, 0);
    hero.addAttribute("FALL_RESISTANCE", 3.5, 0);
    hero.addAttribute("BASE_SPEED_LEVELS", 6.0, 0);

    hero.addKeyBind("SUPER_SPEED", "key.superSpeed", 1);
    hero.addKeyBind("SLOW_MOTION", "key.slowMotion", 2);

    hero.setModifierEnabled(isModifierEnabled);

    var speedPunch = speedster_base.createSpeedPunch(hero);
    hero.setDamageProfile(entity => speedPunch.get(entity, null));

    hero.setTickHandler((entity, manager) => {
        speedster_base.tick(entity, manager);
    });
}

function isModifierEnabled(entity, modifier) {
    return modifier.name() != "fiskheroes:speed_disintegration" || entity.getData("fiskheroes:speeding") && entity.getData("fiskheroes:speed") > 4;
}
