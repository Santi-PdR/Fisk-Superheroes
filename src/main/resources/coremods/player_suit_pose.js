var Opcodes = Java.type('org.objectweb.asm.Opcodes');
var InsnList = Java.type('org.objectweb.asm.tree.InsnList');
var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode');
var MethodInsnNode = Java.type('org.objectweb.asm.tree.MethodInsnNode');

// PlayerModel.setupAnim runs before the skin, armor, held items and suit layers are rendered.
// Inject into the typed overload only, avoiding the synthetic Entity bridge's second call.
function initializeCoreMod() {
    return {
        'fiskheroes_player_suit_pose': {
            'target': { 'type': 'CLASS', 'name': 'net.minecraft.client.model.PlayerModel' },
            'transformer': function(classNode) {
                var target = null;
                for (var i = 0; i < classNode.methods.size(); ++i) {
                    var method = classNode.methods.get(i);
                    if (String(method.desc) === '(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V') {
                        if (target !== null) throw new Error('Multiple PlayerModel setup methods');
                        target = method;
                    }
                }
                if (target === null) throw new Error('PlayerModel setup method not found');
                var returns = 0;
                var instructions = target.instructions.toArray();
                for (var j = 0; j < instructions.length; ++j) {
                    if (instructions[j].getOpcode() !== Opcodes.RETURN) continue;
                    var hook = new InsnList();
                    hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    hook.add(new VarInsnNode(Opcodes.ALOAD, 1));
                    hook.add(new VarInsnNode(Opcodes.FLOAD, 3));
                    hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                        'com/fiskmods/heroes/client/render/SuitArmAnimationRenderer', 'onPlayerModelSetup',
                        '(Lnet/minecraft/client/model/PlayerModel;Lnet/minecraft/world/entity/LivingEntity;F)V', false));
                    target.instructions.insertBefore(instructions[j], hook);
                    ++returns;
                }
                if (returns === 0) throw new Error('PlayerModel setup has no return');
                return classNode;
            }
        }
    };
}
