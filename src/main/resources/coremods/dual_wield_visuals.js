var ASMAPI = Java.type('net.minecraftforge.coremod.api.ASMAPI');
var Opcodes = Java.type('org.objectweb.asm.Opcodes');
var InsnList = Java.type('org.objectweb.asm.tree.InsnList');
var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode');
var MethodInsnNode = Java.type('org.objectweb.asm.tree.MethodInsnNode');

// Substitute a visual-only offhand stack at render call sites, never in the
// player's inventory or its interaction logic. Respect a real occupied offhand.
function initializeCoreMod() {
    return {
        'fiskheroes_dual_first_person': {
            'target': { 'type': 'CLASS', 'name': 'net.minecraft.client.renderer.ItemInHandRenderer' },
            'transformer': function(classNode) {
                var descriptor = '(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/player/LocalPlayer;I)V';
                var target = null;
                for (var i = 0; i < classNode.methods.size(); ++i) {
                    var method = classNode.methods.get(i);
                    if (String(method.desc) === descriptor) {
                        if (target !== null) throw new Error('Multiple first-person hand render methods');
                        target = method;
                    }
                }
                if (target === null) throw new Error('First-person hand render method not found');
                var count = 0;
                var fieldName = ASMAPI.mapField('f_109301_'); // offHandItem
                var instructions = target.instructions.toArray();
                for (var j = 0; j < instructions.length; ++j) {
                    var insn = instructions[j];
                    if (insn.getOpcode() !== Opcodes.GETFIELD ||
                        String(insn.owner) !== 'net/minecraft/client/renderer/ItemInHandRenderer' ||
                        String(insn.name) !== String(fieldName) ||
                        String(insn.desc) !== 'Lnet/minecraft/world/item/ItemStack;') continue;
                    var hook = new InsnList();
                    hook.add(new VarInsnNode(Opcodes.ALOAD, 4));
                    hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                        'com/fiskmods/heroes/client/render/DualWieldVisuals', 'firstPersonOffhand',
                        '(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/client/player/LocalPlayer;)Lnet/minecraft/world/item/ItemStack;', false));
                    target.instructions.insert(insn, hook);
                    ++count;
                }
                if (count < 1) throw new Error('First-person offhand render access not found');
                return classNode;
            }
        },
        'fiskheroes_dual_third_person': {
            'target': { 'type': 'CLASS', 'name': 'net.minecraft.client.renderer.entity.layers.ItemInHandLayer' },
            'transformer': function(classNode) {
                var descriptor = '(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V';
                var target = null;
                for (var i = 0; i < classNode.methods.size(); ++i) {
                    var method = classNode.methods.get(i);
                    if (String(method.desc) === descriptor) {
                        if (target !== null) throw new Error('Multiple third-person hand render methods');
                        target = method;
                    }
                }
                if (target === null) throw new Error('Third-person hand render method not found');
                var calls = [];
                var instructions = target.instructions.toArray();
                for (var j = 0; j < instructions.length; ++j) {
                    var insn = instructions[j];
                    if (insn.getOpcode() === Opcodes.INVOKEVIRTUAL &&
                        String(insn.owner) === 'net/minecraft/world/entity/LivingEntity' &&
                        String(insn.desc) === '()Lnet/minecraft/world/item/ItemStack;') calls.push(insn);
                }
                // Vanilla's two handedness ternaries read off, main, main, off.
                if (calls.length !== 4 || String(calls[0].name) !== String(calls[3].name) ||
                    String(calls[1].name) !== String(calls[2].name) || String(calls[0].name) === String(calls[1].name))
                    throw new Error('Unexpected third-person hand lookup layout');
                for (var k = 0; k < calls.length; k += 3) {
                    target.instructions.set(calls[k], new MethodInsnNode(Opcodes.INVOKESTATIC,
                        'com/fiskmods/heroes/client/render/DualWieldVisuals', 'thirdPersonOffhand',
                        '(Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/item/ItemStack;', false));
                }
                return classNode;
            }
        }
    };
}
