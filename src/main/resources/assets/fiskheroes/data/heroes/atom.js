function init(hero) {
    hero.setName("hero.fiskheroes.atom.name");
    hero.setTier(6);

    hero.setHelmet("item.superhero_armor.piece.helmet");
    hero.setChestplate("item.superhero_armor.piece.chestplate");
    hero.setLeggings("item.superhero_armor.piece.leggings");
    hero.setBoots("item.superhero_armor.piece.boots");

    hero.addPowers("fiskheroes:atom_exosuit");
    hero.addAttribute("PUNCH_DAMAGE", 7.0, 0);
    hero.addAttribute("SPRINT_SPEED", 0.15, 1);
    hero.addAttribute("JUMP_HEIGHT", 0.5, 0);
    hero.addAttribute("FALL_RESISTANCE", 7.0, 0);

    hero.addKeyBind("SIZE_MANIPULATION", "key.sizeManipulation", 1);
    hero.addKeyBind("MINIATURIZE_SUIT", "key.miniaturizeSuit", 3);

    hero.setModifierEnabled(isModifierEnabled);
    hero.setHasProperty(hasProperty);

    hero.setTickHandler((entity, manager) => {
        manager.incrementData(entity, "fiskheroes:dyn/booster_timer", 2, entity.getData("fiskheroes:flying"));
        manager.incrementData(entity, "fiskheroes:dyn/shrink_timer", 3, entity.getData("fiskheroes:size_state") != 0);
    });
}

function isModifierEnabled(entity, modifier) {
    return modifier.name() != "fiskheroes:water_breathing" || entity.getData("fiskheroes:mask_open");
}

function hasProperty(entity, property) {
    return property == "MASK_TOGGLE" || property == "BREATHE_SPACE" && entity.getData("fiskheroes:mask_open");
}
