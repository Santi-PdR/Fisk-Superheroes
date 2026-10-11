var speedster_base = implement("fiskheroes:external/speedster_base");

function init(hero) {
    hero.setName("hero.fiskheroes.savitar.name");
    hero.setTier(7);

    hero.setHelmet("item.superhero_armor.piece.helmet");
    hero.setChestplate("item.superhero_armor.piece.chestplate");
    hero.setLeggings("item.superhero_armor.piece.leggings");
    hero.setBoots("item.superhero_armor.piece.boots");
    hero.addEquipment("fiskheroes:flash_ring");

    hero.addPowers("fiskheroes:speed_force", "fiskheroes:savitar_armor");
    hero.addAttribute("PUNCH_DAMAGE", 6.0, 0);
    hero.addAttribute("WEAPON_DAMAGE", 0.5, 0);
    hero.addAttribute("JUMP_HEIGHT", 1.0, 0);
    hero.addAttribute("FALL_RESISTANCE", 4.5, 0);
    hero.addAttribute("BASE_SPEED_LEVELS", 7.0, 0);

    hero.addKeyBind("SUPER_SPEED", "key.superSpeed", 1);
    hero.addKeyBind("SLOW_MOTION", "key.slowMotion", 2);
    hero.addKeyBind("BLADE", "key.spike", 3);

    hero.setDefaultScale(1.2);
    hero.setHasProperty((entity, property) => property == "BREATHE_SPACE");

    hero.setAttributeProfile(entity => entity.getData("fiskheroes:blade") ? "BLADE" : null);
    hero.addAttributeProfile("BLADE", profile => {
        profile.inheritDefaults();
        profile.addAttribute("PUNCH_DAMAGE", 8.0, 0);
    });

    var speedPunch = speedster_base.createSpeedPunch(hero, {
        "types": {
            "BLUNT": 1.0,
            "SHARP": 0.2
        },
        "properties": {
            "HIT_COOLDOWN": 5
        }
    });
    hero.setDamageProfile(entity => entity.getData("fiskheroes:blade") ? "BLADE" : speedPunch.get(entity, null));
    hero.addDamageProfile("BLADE", { "types": { "SHARP": 1.0 } });

    hero.addSoundEvent("LAND", "fiskheroes:savitar_land");
    hero.addSoundEvent("STEP", ["fiskheroes:savitar_walk1", "fiskheroes:savitar_walk2", "fiskheroes:savitar_sprint"]);
    hero.addSoundOverrides("SAVITAR", speedster_base.mergeSounds("fiskheroes:speed_force", speedster_base.SOUNDS_SAVITAR));
    hero.addSoundOverrides("BARRY", speedster_base.mergeSounds("fiskheroes:speed_force", speedster_base.SOUNDS_BARRY));
    hero.addSoundOverrides("RGB", speedster_base.mergeSounds("fiskheroes:speed_force", speedster_base.SOUNDS_RGB));

    hero.setTickHandler((entity, manager) => {
        speedster_base.tick(entity, manager);
    });
}
