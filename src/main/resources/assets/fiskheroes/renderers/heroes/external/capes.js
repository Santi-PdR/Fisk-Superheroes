function create(renderer, length, mesh) {
    if (typeof mesh === "string") {
        mesh = renderer.createResource("MESH", mesh);
    }

    var effect = renderer.createEffect("fiskheroes:cape").setMesh(mesh);
    effect.length = length;
    return {
        effect: effect,
        render: data => {
            mesh.setData(data);
            effect.render();
        }
    };
}

function createDefault(renderer, length, mesh, physics) {
    if (typeof mesh === "string") {
        mesh = renderer.createResource("MESH", mesh);
    }

    var effect = renderer.createEffect("fiskheroes:cape").setMesh(mesh);
    effect.length = length;
    return {
        effect: effect,
        render: entity => {
            mesh.setData({
                "flutter": physics.getFlutter(entity),
                "flare": physics.getFlare(entity)
            });
            effect.render();
        }
    };
}

function createGlider(renderer, length, mesh, physics) {
    if (typeof mesh === "string") {
        mesh = renderer.createResource("MESH", mesh);
    }

    var effect = renderer.createEffect("fiskheroes:cape").setMesh(mesh);
    effect.length = length;
    return {
        effect: effect,
        render: (entity, f) => {
            mesh.setData({
                "unfold": f,
                "wind": 1 - f,
                "flutter": physics.getFlutter(entity),
                "flare": physics.getFlare(entity) * (1 - f)
            });
            effect.rigid = f;
            effect.render();
        }
    };
}
