var speedster_base = implement("fiskheroes:external/speedster_base");

function init(hero) {
    hero.setName("hero.fiskheroes.zoom.name");
    hero.setTier(5);

    hero.setHelmet("item.superhero_armor.piece.cowl");
    hero.setChestplate("item.superhero_armor.piece.chestpiece");
    hero.setLeggings("item.superhero_armor.piece.pants");
    hero.setBoots("item.superhero_armor.piece.boots");
    hero.addEquipment("fiskheroes:flash_ring");

    hero.addPowers("fiskheroes:speed_force");
    hero.addAttribute("PUNCH_DAMAGE", 5.0, 0);
    hero.addAttribute("WEAPON_DAMAGE", 1.5, 0);
    hero.addAttribute("JUMP_HEIGHT", 1.0, 0);
    hero.addAttribute("FALL_RESISTANCE", 4.0, 0);
    hero.addAttribute("BASE_SPEED_LEVELS", 6.0, 0);

    hero.addKeyBind("SUPER_SPEED", "key.superSpeed", 1);
    hero.addKeyBind("SLOW_MOTION", "key.slowMotion", 2);

    var speedPunch = speedster_base.createSpeedPunch(hero);
    hero.setDamageProfile(entity => speedPunch.get(entity, null));

    hero.addSoundEvent("EQUIP", ["fiskheroes:flicker_loop_zoom", "fiskheroes:flicker_loop_zoom_pink"]);
    hero.addSoundOverrides("ZOOM", speedster_base.mergeSounds("fiskheroes:speed_force", speedster_base.SOUNDS_ZOOM));
    hero.addSoundOverrides("DEATH", speedster_base.mergeSounds("fiskheroes:speed_force", speedster_base.SOUNDS_DEATH));
    hero.addSoundOverrides("PINK", speedster_base.mergeSounds("fiskheroes:speed_force", speedster_base.SOUNDS_PINK));
    hero.addSoundOverrides("CONCEPT", speedster_base.mergeSounds("fiskheroes:speed_force", speedster_base.SOUNDS_ZOOM, {
        "powers": {
            "fiskheroes:speed_force": {
                "fiskheroes:super_speed": {
                    "ENABLE": ["fiskheroes:zoom_concept_vibration_on", "fiskheroes:zoom_concept_vibration_loop"]
                }
            }
        }
    }));

    hero.setTickHandler((entity, manager) => {
        speedster_base.tick(entity, manager);
    });
}
