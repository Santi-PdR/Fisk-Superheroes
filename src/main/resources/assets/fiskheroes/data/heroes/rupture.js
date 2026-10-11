function init(hero) {
    hero.setName("hero.fiskheroes.rupture.name");
    hero.setTier(3);

    hero.setHelmet("item.superhero_armor.piece.mask");
    hero.setChestplate("item.superhero_armor.piece.chestpiece");
    hero.setLeggings("item.superhero_armor.piece.pants");
    hero.setBoots("item.superhero_armor.piece.boots");
    hero.addPrimaryEquipment("fiskheroes:ruptures_scythe", true);

    hero.addPowers("fiskheroes:energy_manipulation");
    hero.addAttribute("PUNCH_DAMAGE", 4.5, 0);
    hero.addAttribute("WEAPON_DAMAGE", 3.5, 0);
    hero.addAttribute("JUMP_HEIGHT", 0.5, 0);
    hero.addAttribute("FALL_RESISTANCE", 3.0, 0);
    hero.addAttribute("SPRINT_SPEED", 0.1, 1);

    hero.addKeyBind("CHARGE_ENERGY", "key.chargeEnergy", 1);

    hero.setModifierEnabled(isModifierEnabled);
    hero.setKeyBindEnabled(isKeyBindEnabled);
    hero.supplyFunction("canDischargeEnergy", isHoldingScythe);
}

function isModifierEnabled(entity, modifier) {
    return modifier.name() != "fiskheroes:energy_manipulation" || isHoldingScythe(entity);
}

function isKeyBindEnabled(entity, keyBind) {
    return keyBind != "CHARGE_ENERGY" || isHoldingScythe(entity);
}

function isHoldingScythe(entity) {
    return entity.getHeldItem().name() == "fiskheroes:ruptures_scythe";
}
