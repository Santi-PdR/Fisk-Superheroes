function continuePlaying(entity, sound) {
    var vel = entity.motion().length();
    var f = sound.fadeProgress();
    var volume = 0.5;
    var pitch = 0.5 + f * 0.2;

    if (vel >= 0.01) {
        volume += 0.5 * Math.min(Math.max(vel * vel / 8, 0), 1);
        pitch += 0.3 * Math.min(vel, 1);
    }

    sound.setVolume(volume);
    sound.setPitch(pitch);
    return sound.ticksPlaying() < 10 || entity.isSprinting() && entity.getData("fiskheroes:flying") && entity.getData("fiskheroes:dyn/flight_super_boost") > 0;
}
