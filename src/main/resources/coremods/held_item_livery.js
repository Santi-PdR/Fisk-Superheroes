var Opcodes = Java.type('org.objectweb.asm.Opcodes');
var InsnList = Java.type('org.objectweb.asm.tree.InsnList');
var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode');
var MethodInsnNode = Java.type('org.objectweb.asm.tree.MethodInsnNode');
var LabelNode = Java.type('org.objectweb.asm.tree.LabelNode');
var FrameNode = Java.type('org.objectweb.asm.tree.FrameNode');
var InsnNode = Java.type('org.objectweb.asm.tree.InsnNode');
var TryCatchBlockNode = Java.type('org.objectweb.asm.tree.TryCatchBlockNode');

// Keep the original ItemInHandRenderer -> ItemRenderer.renderStatic call intact:
// Player Animator injects immediately before that exact invocation. Scope the
// wearer and livery inside ItemRenderer instead, with cleanup even on exceptions.
function initializeCoreMod() {
    return {
        'fiskheroes_held_item_livery': {
            'target': { 'type': 'CLASS', 'name': 'net.minecraft.client.renderer.entity.ItemRenderer' },
            'transformer': function(classNode) {
                var desc = '(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/level/Level;III)V';
                var target = null;
                for (var i = 0; i < classNode.methods.size(); ++i) {
                    var method = classNode.methods.get(i);
                    if (String(method.desc) !== desc) continue;
                    if (target !== null) throw new Error('Multiple scoped item render methods');
                    target = method;
                }
                if (target === null) throw new Error('Scoped item render method not found');
                var start = new LabelNode();
                var end = new LabelNode();
                var handler = new LabelNode();
                var enter = new InsnList();
                enter.add(new VarInsnNode(Opcodes.ALOAD, 1));
                enter.add(new VarInsnNode(Opcodes.ALOAD, 2));
                enter.add(new VarInsnNode(Opcodes.ALOAD, 3));
                enter.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                    'com/fiskmods/heroes/client/render/SuitItemRenderContext', 'push',
                    '(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;)V', false));
                enter.add(start);
                target.instructions.insert(enter);
                var returns = 0;
                var instructions = target.instructions.toArray();
                for (var j = 0; j < instructions.length; ++j) {
                    if (instructions[j].getOpcode() !== Opcodes.RETURN) continue;
                    target.instructions.insertBefore(instructions[j], new MethodInsnNode(Opcodes.INVOKESTATIC,
                        'com/fiskmods/heroes/client/render/SuitItemRenderContext', 'pop', '()V', false));
                    ++returns;
                }
                if (returns === 0) throw new Error('Scoped item render has no return');
                target.instructions.add(end);
                target.instructions.add(handler);
                target.instructions.add(new FrameNode(Opcodes.F_FULL, 11, Java.to([
                    'net/minecraft/client/renderer/entity/ItemRenderer',
                    'net/minecraft/world/entity/LivingEntity',
                    'net/minecraft/world/item/ItemStack',
                    'net/minecraft/world/item/ItemDisplayContext', Opcodes.INTEGER,
                    'com/mojang/blaze3d/vertex/PoseStack',
                    'net/minecraft/client/renderer/MultiBufferSource',
                    'net/minecraft/world/level/Level', Opcodes.INTEGER, Opcodes.INTEGER, Opcodes.INTEGER
                ], 'java.lang.Object[]'), 1, Java.to(['java/lang/Throwable'], 'java.lang.Object[]')));
                target.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                    'com/fiskmods/heroes/client/render/SuitItemRenderContext', 'pop', '()V', false));
                target.instructions.add(new InsnNode(Opcodes.ATHROW));
                target.tryCatchBlocks.add(new TryCatchBlockNode(start, end, handler, null));
                target.maxStack = Math.max(target.maxStack, 3);
                return classNode;
            }
        }
    };
}
