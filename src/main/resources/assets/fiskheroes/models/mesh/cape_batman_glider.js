var unfold;

function loadData(entity, data, width, length, res) {
    unfold = data.unfold;

    if (unfold > 0) {
        res.requestResolution(2 * width, length);
    }
}

function evaluate(mesh, x, y, width, length) {
    var ax = Math.abs(x);
    mesh.vertX *= (
        (1 + unfold * y * 5) *
        (1 - unfold * (1 - ax) * 0.2) *
        (1 + unfold * diagonal(ax, y))
    );
    mesh.vertY *= (
        (1 - unfold * (ax * 1.0 + Math.sin(0.5 * ax * Math.PI) * 0.2)) *
        (1 - unfold * Math.sin(Math.min(ax * 1.8, 1) * Math.PI) * 0.1) *
        (1 + unfold * 0.3) *
        (1 + unfold * diagonal(ax, y))
    );
}

function diagonal(x, y) {
    return 0.4 * (
        spike(Math.max(x * 1.15 - 0.15, 0)) +
        spike(Math.min(x * 1.65, 1)) * 0.8 -
        Math.sin(Math.min(x * 1.3, 1) * Math.PI) * 0.2
    );
}

function spike(x) {
    if (x < 0 || x > 1) {
        return 0;
    }
    var f = Math.min(x, Math.abs(x - 1));
    return 8 * f * f * f;
}
