function continuePlaying(entity, sound) {
    var vel = entity.motion().length();
    var volume = 0.0;
    var pitch = 0.5;

    if (vel > 0) {
        volume += 0.5 * Math.min(Math.max(vel * vel / 6, 0), 1);
        pitch += 0.05 + 0.3 * Math.min(vel, 1);
    }

    var v = sound.volume();
    var p = sound.pitch();

    if (volume > v) {
        v = volume;
        p = pitch;
    }
    else {
        v += (volume - v) / 8;
        p += (pitch - p) / 8;
    }

    sound.setVolume(v);
    sound.setPitch(p);
    return entity.getData("fiskheroes:speeding") && entity.getData('fiskheroes:moving') && !entity.getData("fiskheroes:flying");
}
