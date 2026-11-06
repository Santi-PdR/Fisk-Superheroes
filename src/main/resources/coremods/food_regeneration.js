var Opcodes = Java.type('org.objectweb.asm.Opcodes');
var InsnList = Java.type('org.objectweb.asm.tree.InsnList');
var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode');
var MethodInsnNode = Java.type('org.objectweb.asm.tree.MethodInsnNode');

// Change only the two healing thresholds in FoodData.tick. The later 80-tick starvation
// comparison must stay vanilla. FoodData continues to own the healing, exhaustion and timer.
function initializeCoreMod() {
    return {
        'fiskheroes_food_regeneration': {
            'target': { 'type': 'CLASS', 'name': 'net.minecraft.world.food.FoodData' },
            'transformer': function(classNode) {
                var target = null;
                for (var i = 0; i < classNode.methods.size(); ++i) {
                    var method = classNode.methods.get(i);
                    if (String(method.desc) === '(Lnet/minecraft/world/entity/player/Player;)V') {
                        if (target !== null) throw new Error('Multiple FoodData player tick methods');
                        target = method;
                    }
                }
                if (target === null) throw new Error('FoodData player tick method not found');
                var ten = [], eighty = [];
                var instructions = target.instructions.toArray();
                for (var j = 0; j < instructions.length; ++j) {
                    var node = instructions[j];
                    if (node.getOpcode() !== Opcodes.BIPUSH) continue;
                    var next = node.getNext();
                    while (next !== null && next.getOpcode() < 0) next = next.getNext();
                    if (next === null || next.getOpcode() !== Opcodes.IF_ICMPLT) continue;
                    if (node.operand === 10) ten.push(node);
                    if (node.operand === 80) eighty.push(node);
                }
                if (ten.length !== 1 || eighty.length !== 2) {
                    throw new Error('Unexpected FoodData regeneration comparisons: ' + ten.length + ', ' + eighty.length);
                }
                var thresholds = [ten[0], eighty[0]];
                for (var k = 0; k < thresholds.length; ++k) {
                    var threshold = thresholds[k];
                    target.instructions.insertBefore(threshold, new VarInsnNode(Opcodes.ALOAD, 1));
                    target.instructions.insert(threshold, new MethodInsnNode(Opcodes.INVOKESTATIC,
                        'com/fiskmods/heroes/common/hero/modifier/ModifierRegeneration', 'healInterval',
                        '(Lnet/minecraft/world/entity/player/Player;I)I', false));
                }
                return classNode;
            }
        }
    };
}
