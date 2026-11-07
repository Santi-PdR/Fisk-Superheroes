var ASMAPI = Java.type('net.minecraftforge.coremod.api.ASMAPI');
var Opcodes = Java.type('org.objectweb.asm.Opcodes');
var InsnList = Java.type('org.objectweb.asm.tree.InsnList');
var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode');
var MethodInsnNode = Java.type('org.objectweb.asm.tree.MethodInsnNode');
var JumpInsnNode = Java.type('org.objectweb.asm.tree.JumpInsnNode');
var LabelNode = Java.type('org.objectweb.asm.tree.LabelNode');
var FrameNode = Java.type('org.objectweb.asm.tree.FrameNode');
var InsnNode = Java.type('org.objectweb.asm.tree.InsnNode');

// Replace vanilla travel only while an enabled suit is actually gliding. The original mod
// used the same hook before vanilla travel, avoiding double gravity and duplicate movement.
function initializeCoreMod() {
    return {
        'fiskheroes_gliding_travel': {
            'target': { 'type': 'CLASS', 'name': 'net.minecraft.world.entity.LivingEntity' },
            'transformer': function(classNode) {
                var target = null;
                var name = ASMAPI.mapMethod('m_7023_');
                for (var i = 0; i < classNode.methods.size(); ++i) {
                    var method = classNode.methods.get(i);
                    if (String(method.name) === String(name) && String(method.desc) === '(Lnet/minecraft/world/phys/Vec3;)V') {
                        if (target !== null) throw new Error('Multiple LivingEntity travel methods');
                        target = method;
                    }
                }
                if (target === null) throw new Error('LivingEntity travel method not found');
                var vanilla = new LabelNode();
                var hook = new InsnList();
                hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
                hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                    'com/fiskmods/heroes/common/hero/ability/GlidingTravel', 'travel',
                    '(Lnet/minecraft/world/entity/LivingEntity;)Z', false));
                hook.add(new JumpInsnNode(Opcodes.IFEQ, vanilla));
                hook.add(new InsnNode(Opcodes.RETURN));
                hook.add(vanilla);
                hook.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
                target.instructions.insert(hook);
                return classNode;
            }
        }
    };
}
