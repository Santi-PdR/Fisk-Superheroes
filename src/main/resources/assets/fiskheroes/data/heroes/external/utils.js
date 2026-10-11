function flightOnIntangibility(entity, manager) {
    if (!entity.isSneaking() && entity.getData("fiskheroes:intangible")) {
        manager.setData(entity, "fiskheroes:flying", true);
    }
}
