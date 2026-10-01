function init(hero) {
    hero.setName("hero.fiskheroes.citizen_steel.name");
    hero.setAliases("steel");
    hero.setTier(5);

    hero.setHelmet("item.superhero_armor.piece.helmet");
    hero.setChestplate("item.superhero_armor.piece.chestpiece");
    hero.setLeggings("item.superhero_armor.piece.pants");
    hero.setBoots("item.superhero_armor.piece.boots");

    hero.addPowers("fiskheroes:steel_transformation");
    hero.addAttribute("PUNCH_DAMAGE", 5.5, 0);
    hero.addAttribute("JUMP_HEIGHT", 0.5, 0);
    hero.addAttribute("FALL_RESISTANCE", 4.0, 0);

    hero.addKeyBind("STEEL_TRANSFORM", "key.steelTransform", 1);

    hero.addAttributeProfile("STEEL", steelProfile);
    hero.setAttributeProfile(getAttributeProfile);
    hero.setModifierEnabled(isModifierEnabled);
    hero.setTierOverride(getTierOverride);
}

function getTierOverride(entity) {
    return entity.getData("fiskheroes:dyn/steeled") ? 7 : 5;
}

function steelProfile(profile) {
    profile.addAttribute("PUNCH_DAMAGE", 9.0, 0);
    profile.addAttribute("JUMP_HEIGHT", 1.0, 0);
    profile.addAttribute("FALL_RESISTANCE", 5.0, 0);
    profile.addAttribute("SPRINT_SPEED", 0.15, 1);
}

function getAttributeProfile(entity) {
    return entity.getData("fiskheroes:dyn/steeled") ? "STEEL" : null;
}

function isModifierEnabled(entity, modifier) {
    return modifier.name() == "fiskheroes:transformation" || modifier.name() == "fiskheroes:cooldown" || entity.getData("fiskheroes:dyn/steeled");
}
