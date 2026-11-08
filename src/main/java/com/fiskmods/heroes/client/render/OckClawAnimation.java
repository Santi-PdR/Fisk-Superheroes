package com.fiskmods.heroes.client.render;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/** The bundled ock_claw.fsk transforms, applied after the cached Tabula pose is reset. */
final class OckClawAnimation
{
    private static final float DEG = (float) Math.PI / 180.0F;
    private static boolean loggedGrab;
    private static boolean loggedStrike;
    private OckClawAnimation() { }

    static void apply(ModelPart model, int index, float extension, float grab, float strike)
    {
        extension = Mth.clamp(extension, 0.0F, 1.0F);
        grab = Mth.clamp(grab, 0.0F, 1.0F);
        strike = Mth.clamp(strike, 0.0F, 1.0F);
        float folded = 1.0F - Math.min(extension * 2.0F, 1.0F);
        try
        {
            ModelPart root = model.getChild("root_root_0");
            root.y += 3.0F * folded;
            ModelPart eye = root.getChild("eye_root_3");
            eye.y -= 0.1F * strike;
            ModelPart blade1 = root.getChild("blade1_root_4");
            ModelPart blade2 = blade1.getChild("blade2_blade1_0");
            blade1.y += 3.0F * strike;
            blade2.y += 3.0F * strike;
            for (int i = 1; i <= 3; ++i)
            {
                String n = "claw" + i;
                ModelPart base = root.getChild(n + "_1_root_" + (i - 1));
                ModelPart joint = base.getChild(n + "_2_" + n + "_1_0");
                ModelPart fingerA = joint.getChild(n + "_3_" + n + "_2_0");
                ModelPart fingerB = joint.getChild(n + "_4_" + n + "_2_1");
                ModelPart finger = joint.getChild(n + "_5_" + n + "_2_2");
                ModelPart tipA = finger.getChild(n + "_6_" + n + "_5_0");
                ModelPart tipB = finger.getChild(n + "_7_" + n + "_5_1");
                base.yRot += 45.0F * index * DEG;
                joint.y -= 2.0F * folded;
                joint.x += 0.2F * grab;
                joint.zRot += (-80.0F * grab + 5.0F * strike) * DEG;
                fingerA.yRot += 3.0F * grab * DEG;
                fingerB.yRot += 3.0F * grab * DEG;
                finger.x -= 2.0F * folded;
                finger.zRot = finger.zRot * (1.0F - folded) + 27.0F * strike * DEG;
                tipA.yRot = tipA.yRot * (1.0F - folded) - 5.0F * grab * DEG;
                tipB.yRot = tipB.yRot * (1.0F - folded) + 5.0F * grab * DEG;
            }
            if (grab > 0.05F && !loggedGrab)
            {
                loggedGrab = true;
                com.fiskmods.heroes.FiskHeroes.LOGGER.info("Octopus claw grab pose applied");
            }
            if (strike > 0.05F && !loggedStrike)
            {
                loggedStrike = true;
                com.fiskmods.heroes.FiskHeroes.LOGGER.info("Octopus claw strike pose applied");
            }
        }
        catch (java.util.NoSuchElementException ignored)
        {
            // External packs can replace this model with a different hierarchy.
        }
    }
}
