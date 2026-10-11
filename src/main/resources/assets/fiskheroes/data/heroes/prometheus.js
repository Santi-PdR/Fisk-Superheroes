function init(hero) {
    hero.setName("hero.fiskheroes.prometheus.name");
    hero.setTier(5);

    hero.setHelmet("item.superhero_armor.piece.hood");
    hero.setChestplate("item.superhero_armor.piece.chestpiece");
    hero.setLeggings("item.superhero_armor.piece.pants");
    hero.setBoots("item.superhero_armor.piece.boots");
    hero.addPrimaryEquipment("fiskheroes:chokuto", true, item => !item.nbt().getBoolean("Dual"));
    hero.addEquipment("fiskheroes:compound_bow");
    hero.addEquipment("fiskheroes:quiver");

    hero.addPowers("fiskheroes:archery", "fiskheroes:throwing_stars");
    hero.addAttribute("PUNCH_DAMAGE", 6.0, 0);
    hero.addAttribute("WEAPON_DAMAGE", 5.0, 0);
    hero.addAttribute("JUMP_HEIGHT", 1.0, 0);
    hero.addAttribute("FALL_RESISTANCE", 5.0, 0);
    hero.addAttribute("SPRINT_SPEED", 0.15, 1);
    hero.addAttribute("BOW_DRAWBACK", 0.55, 1);

    hero.addKeyBind("QUIVER_CYCLE", "key.quiverCycle", 1);
    hero.addKeyBind("UTILITY_BELT", "key.throwingStars", 2);
    hero.addKeyBind("HORIZONTAL_BOW", "key.horizontalBow", 3);

    hero.setKeyBindEnabled(isKeyBindEnabled);
}

function isKeyBindEnabled(entity, keyBind) {
    return keyBind != "QUIVER_CYCLE" || entity.getHeldItem().name() === "fiskheroes:compound_bow" && entity.getData("fiskheroes:equipped_quiver") != null;
}
