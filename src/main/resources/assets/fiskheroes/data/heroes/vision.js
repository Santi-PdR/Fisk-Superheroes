var utils = implement("fiskheroes:external/utils");

function init(hero) {
    hero.setName("hero.fiskheroes.vision.name");
    hero.setTier(9);

    hero.setHelmet("item.superhero_armor.piece.head");
    hero.setChestplate("item.superhero_armor.piece.torso");
    hero.setLeggings("item.superhero_armor.piece.legs");
    hero.setBoots("item.superhero_armor.piece.boots");

    hero.addPowers("fiskheroes:vibranium_physiology", "fiskheroes:mind_stone");
    hero.addAttribute("PUNCH_DAMAGE", 11.0, 0);
    hero.addAttribute("WEAPON_DAMAGE", 1.5, 0);
    hero.addAttribute("SPRINT_SPEED", 0.15, 1);
    hero.addAttribute("JUMP_HEIGHT", 1.0, 0);
    hero.addAttribute("FALL_RESISTANCE", 1.0, 1);

    hero.addKeyBind("CHARGED_BEAM", "key.mindStoneBlast", 1);
    hero.addKeyBind("INTANGIBILITY", "key.intangibility", 2);

    hero.setTickHandler((entity, manager) => {
        utils.flightOnIntangibility(entity, manager);
    });
}
