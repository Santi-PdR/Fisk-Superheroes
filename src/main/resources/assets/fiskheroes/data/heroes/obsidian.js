function init(hero) {
    hero.setName("hero.fiskheroes.obsidian.name");
    hero.setTier(5);

    hero.setHelmet("item.superhero_armor.piece.hood");
    hero.setChestplate("item.superhero_armor.piece.chestpiece");
    hero.setLeggings("item.superhero_armor.piece.pants");
    hero.setBoots("item.superhero_armor.piece.boots");

    hero.addPowers("fiskheroes:umbrakinesis");
    hero.addAttribute("PUNCH_DAMAGE", 5.0, 0);
    hero.addAttribute("WEAPON_DAMAGE", 3.0, 0);
    hero.addAttribute("JUMP_HEIGHT", 1.0, 0);
    hero.addAttribute("FALL_RESISTANCE", 4.0, 0);
    hero.addAttribute("SPRINT_SPEED", 0.1, 1);

    hero.addKeyBind("SHADOWFORM", "key.shadowForm", 1);
    hero.addKeyBind("AIM", "key.shadowChain", 2);
    hero.addKeyBind("TELEKINESIS", "key.shadowChain", 2);
    hero.addKeyBind("SHADOWDOME", "key.shadowDome", 3);

    hero.setModifierEnabled(isModifierEnabled);
    hero.setKeyBindEnabled(isKeyBindEnabled);
    hero.supplyFunction("canAim", canAim);
}

function isModifierEnabled(entity, modifier) {
    switch (modifier.name()) {
    case "fiskheroes:flight":
        return entity.getData("fiskheroes:shadowform");
    case "fiskheroes:telekinesis":
        return entity.getHeldItem().isEmpty() && !entity.getData("fiskheroes:shadowform") && !entity.getData("fiskheroes:lightsout");
    default:
        return true;
    }
}

function isKeyBindEnabled(entity, keyBind) {
    if (entity.getData("fiskheroes:shadowform")) {
        return keyBind == "SHADOWFORM";
    }
    else if (entity.getData("fiskheroes:lightsout")) {
        return keyBind == "SHADOWDOME";
    }
    else if (entity.getData("fiskheroes:telekinesis")) {
        return keyBind == "TELEKINESIS" || keyBind == "AIM";
    }

    return keyBind != "TELEKINESIS" && keyBind != "AIM" || entity.getHeldItem().isEmpty();
}

function canAim(entity) {
    return entity.getHeldItem().isEmpty() && entity.getData("fiskheroes:grab_id") > -1;
}
