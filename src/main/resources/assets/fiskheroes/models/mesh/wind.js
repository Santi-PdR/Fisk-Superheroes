var flutter, wind;

function loadData(entity, data) {
    if (entity.is("DISPLAY")) {
        flutter = wind = 0;
    }
    else {
        var speed = entity.motionInterpolated().length();

        if (data.hasOwnProperty("windFactor")) {
            speed *= data.windFactor;
        }

        speed *= speed;
        flutter = data.flutter;
        wind = (1.2 + Math.sin(entity.loop(20 * Math.PI) / 20) / 3) / 8 * (0.4 + 0.6 * Math.min(speed * 4, 1) - 0.4 * Math.min(speed / 10, 1));

        if (data.hasOwnProperty("wind")) {
            wind *= data.wind;
        }
    }
}

function evaluate(mesh, x, y, width, length) {
    mesh.vertX += y * Math.sin(flutter / 3 + x - y * 4) / 2 * wind;
    mesh.vertZ += y * Math.sin(flutter + x - y * 4) * wind;
}
