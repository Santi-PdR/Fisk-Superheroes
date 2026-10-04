function init(hero) {
    hero.setName("hero.fiskheroes.doctor_octopus.name");
    hero.setAliases("doc_ock", "ock");
    hero.setTier(2);

    hero.setHelmet("item.superhero_armor.piece.glasses");
    hero.setChestplate("item.superhero_armor.piece.trenchcoat");
    hero.setLeggings("item.superhero_armor.piece.pants");
    hero.setBoots("item.superhero_armor.piece.shoes");

    hero.addPowers("fiskheroes:mechanical_smart_arms");
    hero.addAttribute("FALL_RESISTANCE", 2.5, 0);
    hero.addAttribute("PUNCH_DAMAGE", 3.5, 0);

    hero.addKeyBind("TENTACLE_JAB", "key.tentacleJab", 1);
    hero.addKeyBind("TENTACLE_GRAB", "key.tentacleGrab", 2);
    hero.addKeyBind("TENTACLE_STRIKE", "key.tentacleStrike", 3);
    hero.addKeyBind("TENTACLES", "key.tentacles", 5);

    hero.setKeyBindEnabled(isKeyBindEnabled);
}

function isKeyBindEnabled(entity, keyBind) {
    return keyBind == "TENTACLES" || entity.getData("fiskheroes:dyn/tentacles_active");
}
