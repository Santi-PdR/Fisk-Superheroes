function makeSounds(slowMotion, running) {
    return {
        "running": running,
        "slowMotion": {
            "ENABLE": "fiskheroes:slow_motion_enable_" + slowMotion,
            "DISABLE": "fiskheroes:slow_motion_disable_" + slowMotion
        }
    };
}

var SOUNDS_BARRY = makeSounds("cw", {
    "MOVE": "fiskheroes:super_speed_move_loop_cw",
    "SPRINT": "fiskheroes:super_speed_sprint_barry",
    "STOP": "fiskheroes:super_speed_stop_barry"
});
var SOUNDS_TEST = makeSounds("cw", {
    "MOVE": "fiskheroes:super_speed_move_loop_barry_test",
    "SPRINT": "fiskheroes:super_speed_sprint_barry_test",
    "STOP": "fiskheroes:super_speed_stop_barry_test"
});
var SOUNDS_KF = makeSounds("cw", {
    "MOVE": "fiskheroes:super_speed_move_loop_cw",
    "SPRINT": "fiskheroes:super_speed_sprint_wally",
    "STOP": "fiskheroes:super_speed_stop_wally"
});
var SOUNDS_JAY = makeSounds("cw", {
    "MOVE": "fiskheroes:super_speed_move_loop_cw",
    "SPRINT": "fiskheroes:super_speed_sprint_jay",
    "STOP": "fiskheroes:super_speed_stop_jay"
});
var SOUNDS_NEGATIVE = makeSounds("cw", {
    "MOVE": "fiskheroes:super_speed_move_loop_cw",
    "SPRINT": ["fiskheroes:super_speed_sprint_negative", "fiskheroes:super_speed_sprint_negative_roar"],
    "STOP": ["fiskheroes:super_speed_stop_negative", "fiskheroes:super_speed_stop_negative_roar"]
});
var SOUNDS_ZOOM = makeSounds("cw", {
    "MOVE": "fiskheroes:super_speed_move_loop_cw",
    "SPRINT": "fiskheroes:super_speed_sprint_zoom",
    "STOP": "fiskheroes:super_speed_stop_zoom"
});
var SOUNDS_DEATH = makeSounds("cw", {
    "MOVE": "fiskheroes:super_speed_move_loop_cw",
    "SPRINT": ["fiskheroes:super_speed_sprint_zoom_death", "fiskheroes:super_speed_sprint_zoom_death_noise", "fiskheroes:super_speed_sprint_negative_roar"],
    "STOP": ["fiskheroes:super_speed_stop_zoom_death", "fiskheroes:super_speed_stop_zoom_death_noise", "fiskheroes:super_speed_stop_negative_roar", "fiskheroes:super_speed_stop_zoom_death_roar"]
});
var SOUNDS_PINK = makeSounds("cw", {
    "MOVE": "fiskheroes:super_speed_move_loop_zoom_pink",
    "SPRINT": "fiskheroes:super_speed_sprint_zoom_pink",
    "STOP": "fiskheroes:super_speed_stop_zoom_pink"
});
var SOUNDS_SAVITAR = makeSounds("cw", {
    "MOVE": ["fiskheroes:super_speed_move_loop_cw", "fiskheroes:super_speed_move_loop_savitar"],
    "SPRINT": ["fiskheroes:super_speed_sprint_savitar", "fiskheroes:super_speed_sprint_savitar_noise"],
    "STOP": ["fiskheroes:super_speed_stop_savitar", "fiskheroes:super_speed_stop_savitar_noise"]
});
var SOUNDS_RGB = makeSounds("cw", {
    "MOVE": ["fiskheroes:super_speed_move_loop_cw", "fiskheroes:super_speed_move_loop_savitar_rgb"],
    "SPRINT": ["fiskheroes:super_speed_sprint_savitar", "fiskheroes:super_speed_sprint_savitar_noise"],
    "STOP": ["fiskheroes:super_speed_stop_savitar", "fiskheroes:super_speed_stop_savitar_noise"]
});

var SOUNDS_COMICS = makeSounds("comics", {
    "ENABLE": "fiskheroes:flicker_loop_comics",
    "MOVE": ["fiskheroes:super_speed_move_loop_comics", "fiskheroes:super_speed_move_loop_barry_test"],
    "SPRINT": "fiskheroes:super_speed_sprint_comics",
    "STOP": "fiskheroes:super_speed_stop_comics"
});

function tick(entity, manager) {
    manager.incrementData(entity, "fiskheroes:dyn/speed_sprint_timer", 4, entity.isSprinting() && entity.getData("fiskheroes:speed_sprinting") && entity.getData("fiskheroes:speed") > 1);
}

function createSpeedPunch(hero, damageProfile) {
    if (typeof damageProfile === "undefined") {
        damageProfile = {
            "types": {
                "BLUNT": 1.0
            },
            "properties": {
                "HIT_COOLDOWN": 5
            }
        };
    }

    hero.addDamageProfile("SPEED_PUNCH", damageProfile);
    return {
        get: (entity, orElse) => entity.getData("fiskheroes:speeding") ? "SPEED_PUNCH" : orElse
    };
}

function mergeSounds(powerName, sounds, profile) {
    var profile2 = { "powers": {} };
    profile2.powers[powerName] = {
        "fiskheroes:super_speed": sounds.running,
        "fiskheroes:slow_motion": sounds.slowMotion
    };
    if (typeof profile === "undefined") {
        return profile2;
    }
    return merge(profile, profile2);
}

function merge(from, into) {
    if (typeof from !== "object") {
        return from;
    }
    var result = {};
    for (var attrname in into) {
        result[attrname] = into[attrname];
    }
    for (var attrname in from) {
        if (result.hasOwnProperty(attrname)) {
            result[attrname] = merge(from[attrname], result[attrname]);
        }
        else {
            result[attrname] = from[attrname];
        }
    }
    return result;
}
