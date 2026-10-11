var super_boost = implement("fiskheroes:external/super_boost_with_cooldown");
var falcon_base = implement("fiskheroes:external/falcon_base");

function init(hero) {
    hero.setName("hero.fiskheroes.captain_america_sam.name");
    hero.setAliases("cap_sam", "cap_falcon");
    hero.setTier(5);

    hero.setHelmet("item.superhero_armor.piece.cowl");
    hero.setChestplate("item.superhero_armor.piece.chestpiece");
    hero.setLeggings("item.superhero_armor.piece.pants");
    hero.setBoots("item.superhero_armor.piece.boots");
    hero.addPrimaryEquipment("fiskheroes:captain_americas_shield", true);

    hero.addPowers("fiskheroes:wakandan_flightpack", "fiskheroes:shield_throwing");
    hero.addAttribute("PUNCH_DAMAGE", 6.0, 0);
    hero.addAttribute("WEAPON_DAMAGE", 3.5, 0);
    hero.addAttribute("JUMP_HEIGHT", 1.0, 0);
    hero.addAttribute("FALL_RESISTANCE", 5.0, 0);
    hero.addAttribute("SPRINT_SPEED", 0.2, 1);
    hero.addAttribute("IMPACT_DAMAGE", 0.4, 1);

    hero.addKeyBind("SHIELD_THROW", "key.shieldThrow", 1);

    hero.setKeyBindEnabled(isKeyBindEnabled);
    hero.setHasPermission((entity, permission) => permission == "USE_SHIELD");

    super_boost = super_boost.create(280, 200, 30);
    falcon_base.init(hero, super_boost, 2, 0.3);
    hero.setModifierEnabled((entity, modifier) => super_boost.isModifierEnabled(entity, modifier));
}

function isKeyBindEnabled(entity, keyBind) {
    switch (keyBind) {
    case "SHIELD_THROW":
        return entity.getHeldItem().name() == "fiskheroes:captain_americas_shield";
    default:
        return super_boost.isKeyBindEnabled(entity, keyBind);
    }
}
