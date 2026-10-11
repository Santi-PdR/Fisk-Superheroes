function init(hero) {
    hero.setName("hero.fiskheroes.senor_cactus.name");
    hero.setTier(5);

    hero.setHelmet("item.superhero_armor.piece.hat");
    hero.setChestplate("item.superhero_armor.piece.chestpiece");
    hero.setLeggings("item.superhero_armor.piece.pants");
    hero.setBoots("item.superhero_armor.piece.boots");

    hero.addPowers("fiskheroes:cactus_physiology");
    hero.addAttribute("PUNCH_DAMAGE", 5.0, 0);
    hero.addAttribute("WEAPON_DAMAGE", 2.0, 0);
    hero.addAttribute("JUMP_HEIGHT", 1.0, 0);
    hero.addAttribute("FALL_RESISTANCE", 4.5, 0);
    hero.addAttribute("SPRINT_SPEED", 0.1, 1);
    hero.addAttribute("STEP_HEIGHT", 0.5, 0);

    hero.addKeyBind("AIM", "key.point", 1);
    hero.addKeyBind("SHOOT_SPIKES", "key.shootSpikes", 2);

    hero.setModifierEnabled(isModifierEnabled);
    hero.setTierOverride(getTierOverride);
    hero.supplyFunction("canAim", canAim);

    hero.setDamageProfile(getDamageProfile);
    hero.addDamageProfile("PUNCH", {
        "types": {
            "BLUNT": 1.0,
            "CACTUS": 0.2
        }
    });
}

function getTierOverride(entity) {
    return entity.isWet() ? 2 : 5;
}

function getDamageProfile(entity) {
    return !entity.getHeldItem().isWeapon() ? "PUNCH" : null;
}

function isModifierEnabled(entity, modifier) {
    return modifier.name() != "fiskheroes:regeneration" || modifier.id() == "wet" == entity.isWet();
}

function canAim(entity) {
    return entity.getHeldItem().isEmpty();
}
