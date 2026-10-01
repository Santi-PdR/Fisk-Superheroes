var utils = implement("fiskheroes:external/utils");

function init(hero) {
    hero.setName("hero.fiskheroes.martian_manhunter.name");
    hero.setAliases("mm", "mmcw");
    hero.setTier(8);

    hero.setHelmet("item.superhero_armor.piece.head");
    hero.setChestplate("item.superhero_armor.piece.torso");
    hero.setLeggings("item.superhero_armor.piece.leggings");
    hero.setBoots("item.superhero_armor.piece.boots");

    hero.addPowers("fiskheroes:martian_physiology");
    hero.addAttribute("PUNCH_DAMAGE", 9.0, 0);
    hero.addAttribute("WEAPON_DAMAGE", 2.5, 0);
    hero.addAttribute("SPRINT_SPEED", 0.15, 1);
    hero.addAttribute("JUMP_HEIGHT", 1.5, 0);
    hero.addAttribute("FALL_RESISTANCE", 1.0, 1);

    hero.addKeyBind("SHAPE_SHIFT", "key.shapeShift", 1);
    hero.addKeyBind("SHAPE_SHIFT_RESET", "key.shapeShiftReset", 2);
    hero.addKeyBind("INTANGIBILITY", "key.intangibility", 3);
    hero.addKeyBind("INVISIBILITY", "key.invisibility", 4);

    hero.setDefaultScale(1.1);
    hero.setHasProperty((entity, property) => property == "BREATHE_SPACE");

    hero.setTickHandler((entity, manager) => {
        utils.flightOnIntangibility(entity, manager);
    });
}
