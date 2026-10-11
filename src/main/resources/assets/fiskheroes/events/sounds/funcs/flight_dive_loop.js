function continuePlaying(entity, sound) {
    var vel = entity.motion().length();
    var f = sound.fadeProgress();
    var volume = 0.0;
    var pitch = 0.5 + f * 0.2;

    if (vel >= 0.01) {
        volume += 0.6 * Math.min(Math.max(vel * vel / 4, 0), 1);
        pitch += 0.5 * Math.min(vel, 1);
    }

    sound.setVolume(volume);
    sound.setPitch(pitch);
    return !entity.getData("fiskheroes:flying") && !entity.isSneaking() && !entity.isOnGround();
}