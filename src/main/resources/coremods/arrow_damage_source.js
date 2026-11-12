var Opcodes = Java.type('org.objectweb.asm.Opcodes');

// Preserve vanilla arrow hit handling (piercing, enchantments and rejected-hit bounce), while
// allowing the fireball trick arrow to supply the fire damage source used by the original mod.
function initializeCoreMod() {
    return {
        'fiskheroes_arrow_damage_source': {
            'target': { 'type': 'CLASS', 'name': 'net.minecraft.world.entity.projectile.AbstractArrow' },
            'transformer': function(classNode) {
                var hitDesc = '(Lnet/minecraft/world/phys/EntityHitResult;)V';
                var sourceDesc = '(Lnet/minecraft/world/entity/projectile/AbstractArrow;Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/world/damagesource/DamageSource;';
                var count = 0;
                for (var i = 0; i < classNode.methods.size(); ++i) {
                    var method = classNode.methods.get(i);
                    if (String(method.desc) !== hitDesc) continue;
                    for (var insn = method.instructions.getFirst(); insn !== null; insn = insn.getNext()) {
                        if (insn.getOpcode() === Opcodes.INVOKEVIRTUAL &&
                                String(insn.owner) === 'net/minecraft/world/damagesource/DamageSources' &&
                                String(insn.desc) === sourceDesc) {
                            insn.setOpcode(Opcodes.INVOKESTATIC);
                            insn.owner = 'com/fiskmods/heroes/common/entity/arrow/ArrowDamageSources';
                            insn.name = 'directHit';
                            insn.desc = '(Lnet/minecraft/world/damagesource/DamageSources;Lnet/minecraft/world/entity/projectile/AbstractArrow;Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/world/damagesource/DamageSource;';
                            insn.itf = false;
                            ++count;
                        }
                    }
                }
                if (count !== 2) throw new Error('Expected two vanilla arrow damage source calls, found ' + count);
                return classNode;
            }
        }
    };
}
