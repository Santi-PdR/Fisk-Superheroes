var Opcodes = Java.type('org.objectweb.asm.Opcodes');
var ASMAPI = Java.type('net.minecraftforge.coremod.api.ASMAPI');
var InsnList = Java.type('org.objectweb.asm.tree.InsnList');
var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode');
var InsnNode = Java.type('org.objectweb.asm.tree.InsnNode');
var MethodInsnNode = Java.type('org.objectweb.asm.tree.MethodInsnNode');

// Forge's Size event changes the current bounding box, but vanilla pose checks
// get unscaled dimensions for the prospective pose. Preserve the vanilla call
// and adjust only the dimensions used to build that prospective box.
function initializeCoreMod() {
    return {
        'fiskheroes_scaled_pose_collision': {
            'target': { 'type': 'CLASS', 'name': 'net.minecraft.world.entity.Entity' },
            'transformer': function(classNode) {
                var target = null;
                var descriptor = '(Lnet/minecraft/world/entity/Pose;)Lnet/minecraft/world/phys/AABB;';
                for (var i = 0; i < classNode.methods.size(); ++i) {
                    var method = classNode.methods.get(i);
                    if (String(method.desc) !== descriptor) continue;
                    if (target !== null) throw new Error('Multiple pose bounding-box methods');
                    target = method;
                }
                if (target === null) throw new Error('Pose bounding-box method not found');
                var matches = 0;
                var insns = target.instructions.toArray();
                var dimensionsName = String(ASMAPI.mapMethod('m_6972_'));
                for (var j = 0; j < insns.length; ++j) {
                    var node = insns[j];
                    if (node.getOpcode() !== Opcodes.INVOKEVIRTUAL
                            || String(node.name) !== dimensionsName
                            || String(node.desc) !== '(Lnet/minecraft/world/entity/Pose;)Lnet/minecraft/world/entity/EntityDimensions;') continue;
                    var hook = new InsnList();
                    hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    hook.add(new InsnNode(Opcodes.SWAP));
                    hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                        'com/fiskmods/heroes/common/hero/ability/PlayerSizeHandler', 'poseDimensions',
                        '(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/EntityDimensions;)Lnet/minecraft/world/entity/EntityDimensions;', false));
                    target.instructions.insert(node, hook);
                    ++matches;
                }
                if (matches !== 1) throw new Error('Expected one dimensions call in pose bounding-box method, found ' + matches);
                target.maxStack = Math.max(target.maxStack, 2);
                return classNode;
            }
        }
    };
}
