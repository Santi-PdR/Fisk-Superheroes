function loadData(entity, data, width, length, res) {
    if (entity.is("DISPLAY")) {
        res.requestResolution(1, 1);
    }
    else {
        res.requestResolution(width, length);
    }
}

function evaluate(mesh, x, y, width, length) {
    mesh.vertX = x * width;
    mesh.vertY = y * length;
    mesh.vertZ = 0;
}
