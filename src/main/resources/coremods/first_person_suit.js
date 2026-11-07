var Opcodes = Java.type('org.objectweb.asm.Opcodes');
var InsnList = Java.type('org.objectweb.asm.tree.InsnList');
var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode');
var MethodInsnNode = Java.type('org.objectweb.asm.tree.MethodInsnNode');

// Run after vanilla has prepared and drawn the hand, using that exact model-part pose.
// Forge's RenderArmEvent is before renderHand and may cancel it; injecting here respects
// that cancellation and avoids a second setupAnim call that can desynchronise the suit.
function initializeCoreMod() {
    return {
        'fiskheroes_first_person_suit': {
            'target': { 'type': 'CLASS', 'name': 'net.minecraft.client.renderer.entity.player.PlayerRenderer' },
            'transformer': function(classNode) {
                var target = null;
                var descriptor = '(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/player/AbstractClientPlayer;Lnet/minecraft/client/model/geom/ModelPart;Lnet/minecraft/client/model/geom/ModelPart;)V';
                for (var i = 0; i < classNode.methods.size(); ++i) {
                    var method = classNode.methods.get(i);
                    if (String(method.desc) === descriptor) {
                        if (target !== null) throw new Error('Multiple player hand render methods');
                        target = method;
                    }
                }
                if (target === null) throw new Error('Player hand render method not found');
                var returns = 0;
                var instructions = target.instructions.toArray();
                for (var j = 0; j < instructions.length; ++j) {
                    if (instructions[j].getOpcode() !== Opcodes.RETURN) continue;
                    var hook = new InsnList();
                    hook.add(new VarInsnNode(Opcodes.ALOAD, 1));
                    hook.add(new VarInsnNode(Opcodes.ALOAD, 2));
                    hook.add(new VarInsnNode(Opcodes.ILOAD, 3));
                    hook.add(new VarInsnNode(Opcodes.ALOAD, 4));
                    hook.add(new VarInsnNode(Opcodes.ALOAD, 5));
                    hook.add(new VarInsnNode(Opcodes.ALOAD, 6));
                    hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                        'com/fiskmods/heroes/client/render/FirstPersonSuitArms', 'render', descriptor, false));
                    target.instructions.insertBefore(instructions[j], hook);
                    ++returns;
                }
                if (returns === 0) throw new Error('Player hand render has no return');
                return classNode;
            }
        }
    };
}
