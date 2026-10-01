function init(hero) {
    hero.setName("hero.fiskheroes.atom_smasher.name");
    hero.setTier(6);

    hero.setHelmet("item.superhero_armor.piece.helmet");
    hero.setChestplate("item.superhero_armor.piece.chestpiece");
    hero.setLeggings("item.superhero_armor.piece.pants");
    hero.setBoots("item.superhero_armor.piece.boots");

    hero.addPowers("fiskheroes:radiokinesis", "fiskheroes:water_breathing");
    hero.addAttribute("PUNCH_DAMAGE", 7.5, 0);
    hero.addAttribute("JUMP_HEIGHT", 1.0, 0);
    hero.addAttribute("FALL_RESISTANCE", 8.0, 0);
    hero.addAttribute("SPRINT_SPEED", 0.3, 1);

    hero.addKeyBind("SIZE_MANIPULATION", "key.sizeManipulation", 1);

    hero.setDefaultScale(1.1);
    hero.setModifierEnabled(isModifierEnabled);
    hero.setHasProperty(hasProperty);
}

function isModifierEnabled(entity, modifier) {
    return modifier.name() != "fiskheroes:water_breathing" || !entity.getData("fiskheroes:mask_open");
}

function hasProperty(entity, property) {
    return property == "MASK_TOGGLE" || property == "BREATHE_SPACE" && !entity.getData("fiskheroes:mask_open");
}
