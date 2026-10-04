var super_boost = implement("fiskheroes:external/super_boost");

function init(hero) {
    hero.setName("hero.fiskheroes.mysterio.name");
    hero.setTier(3);

    hero.setHelmet("item.superhero_armor.piece.helmet");
    hero.setChestplate("item.superhero_armor.piece.chestplate");
    hero.setLeggings("item.superhero_armor.piece.leggings");
    hero.setBoots("item.superhero_armor.piece.boots");

    hero.addPowers("fiskheroes:drone_illusions");
    hero.addAttribute("PUNCH_DAMAGE", 4.0, 0);
    hero.addAttribute("FALL_RESISTANCE", 6.0, 0);

    hero.addKeyBind("CHARGED_BEAM", "key.chargedBeam", 1);
    hero.addKeyBind("AIM", "key.energyProjection", 2);
    hero.addKeyBind("ENERGY_PROJECTION", "key.energyProjection", 2);
    hero.addKeyBind("SPELL_MENU", "key.illusionMenu", 3);
    super_boost.addKeyBind(hero, "key.boost", 1);

    hero.setModifierEnabled(isModifierEnabled);
    hero.setKeyBindEnabled(isKeyBindEnabled);
    hero.setHasProperty((entity, property) => property == "MASK_TOGGLE");
    hero.supplyFunction("canAim", canAim);

    hero.addSoundEvent("MASK_OPEN", "fiskheroes:mysterio_mask_open");
    hero.addSoundEvent("MASK_CLOSE", "fiskheroes:mysterio_mask_close");
    hero.addSoundEvent("AIM_START", "fiskheroes:mysterio_beam_aim");

    hero.setTickHandler((entity, manager) => {
        super_boost.tick(entity, manager);
    });
}

function isModifierEnabled(entity, modifier) {
    switch (modifier.name()) {
    case "fiskheroes:energy_projection":
        return entity.getHeldItem().isEmpty();
    case "fiskheroes:charged_beam":
        return !(entity.isSprinting() && entity.getData("fiskheroes:flying")) && entity.getHeldItem().isEmpty();
    default:
        return super_boost.isModifierEnabled(entity, modifier);
    }
}

function isKeyBindEnabled(entity, keyBind) {
    switch (keyBind) {
    case "ENERGY_PROJECTION":
        return entity.getHeldItem().isEmpty() && entity.getData("fiskheroes:beam_charge") == 0 && entity.getData("fiskheroes:aimed_timer") >= 1;
    case "AIM":
        return entity.getHeldItem().isEmpty();
    case "CHARGED_BEAM":
        return !(entity.isSprinting() && entity.getData("fiskheroes:flying")) && entity.getHeldItem().isEmpty();
    default:
        return super_boost.isKeyBindEnabled(entity, keyBind);
    }
}

function canAim(entity) {
    return entity.getHeldItem().isEmpty() && entity.getData("fiskheroes:beam_charge") == 0 && entity.getData('fiskheroes:time_since_damaged') > 10;
}
