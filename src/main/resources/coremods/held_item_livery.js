var Opcodes = Java.type('org.objectweb.asm.Opcodes');
var MethodInsnNode = Java.type('org.objectweb.asm.tree.MethodInsnNode');

// The vanilla hand renderer knows which living entity owns the item. Replace just
// its ItemRenderer call so the custom item renderer can use that entity's livery.
function initializeCoreMod() {
    return {
        'fiskheroes_held_item_livery': {
            'target': { 'type': 'CLASS', 'name': 'net.minecraft.client.renderer.ItemInHandRenderer' },
            'transformer': function(classNode) {
                var handDesc = '(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V';
                var renderDesc = '(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/level/Level;III)V';
                var targets = 0;
                for (var i = 0; i < classNode.methods.size(); ++i) {
                    var method = classNode.methods.get(i);
                    if (String(method.desc) !== handDesc) continue;
                    var instructions = method.instructions.toArray();
                    for (var j = 0; j < instructions.length; ++j) {
                        var insn = instructions[j];
                        if (insn.getOpcode() !== Opcodes.INVOKEVIRTUAL ||
                            String(insn.owner) !== 'net/minecraft/client/renderer/entity/ItemRenderer' ||
                            String(insn.desc) !== renderDesc) continue;
                        var wrapperDesc = '(Lnet/minecraft/client/renderer/entity/ItemRenderer;' + renderDesc.substring(1);
                        method.instructions.set(insn, new MethodInsnNode(Opcodes.INVOKESTATIC,
                            'com/fiskmods/heroes/client/render/SuitItemRenderContext', 'renderHeld', wrapperDesc, false));
                        ++targets;
                    }
                }
                if (targets !== 1) throw new Error('Expected one held item render call, found ' + targets);
                return classNode;
            }
        }
    };
}
