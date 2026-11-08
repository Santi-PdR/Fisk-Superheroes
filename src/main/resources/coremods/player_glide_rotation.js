var Opcodes = Java.type('org.objectweb.asm.Opcodes');
var InsnList = Java.type('org.objectweb.asm.tree.InsnList');
var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode');
var MethodInsnNode = Java.type('org.objectweb.asm.tree.MethodInsnNode');

// Apply the gliding body rotation after vanilla's yaw and ordinary player rotations,
// before skin, armor, attachments and held items are drawn.
function initializeCoreMod() {
    return {
        'fiskheroes_player_glide_rotation': {
            'target': { 'type': 'CLASS', 'name': 'net.minecraft.client.renderer.entity.player.PlayerRenderer' },
            'transformer': function(classNode) {
                var target = null;
                for (var i = 0; i < classNode.methods.size(); ++i) {
                    var method = classNode.methods.get(i);
                    if (String(method.desc) === '(Lnet/minecraft/client/player/AbstractClientPlayer;Lcom/mojang/blaze3d/vertex/PoseStack;FFF)V') {
                        if (target !== null) throw new Error('Multiple player rotation methods');
                        target = method;
                    }
                }
                if (target === null) throw new Error('Player rotation method not found');
                var returns = 0;
                var instructions = target.instructions.toArray();
                for (var j = 0; j < instructions.length; ++j) {
                    if (instructions[j].getOpcode() !== Opcodes.RETURN) continue;
                    var hook = new InsnList();
                    hook.add(new VarInsnNode(Opcodes.ALOAD, 1));
                    hook.add(new VarInsnNode(Opcodes.ALOAD, 2));
                    hook.add(new VarInsnNode(Opcodes.FLOAD, 5));
                    hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                        'com/fiskmods/heroes/client/render/GlidingBodyRenderer', 'apply',
                        '(Lnet/minecraft/client/player/AbstractClientPlayer;Lcom/mojang/blaze3d/vertex/PoseStack;F)V', false));
                    target.instructions.insertBefore(instructions[j], hook);
                    ++returns;
                }
                if (returns === 0) throw new Error('Player rotation method has no return');
                return classNode;
            }
        }
    };
}
