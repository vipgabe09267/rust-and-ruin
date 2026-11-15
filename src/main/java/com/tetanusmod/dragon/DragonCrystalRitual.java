package com.tetanusmod.dragon;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.world.gen.feature.EndSpikeFeature;

/** Replaces only the ten native pillar-top crystals during a special respawn. */
public final class DragonCrystalRitual {
    private DragonCrystalRitual() {}

    public static int convertPillars(ServerWorld world) {
        int converted=0;
        for(var spike:EndSpikeFeature.getSpikes(world)) {
            var area=new Box(spike.getCenterX()-1,spike.getHeight(),spike.getCenterZ()-1,
                    spike.getCenterX()+2,spike.getHeight()+4,spike.getCenterZ()+2);
            for(var original:world.getEntitiesByClass(EndCrystalEntity.class,area,
                    crystal->crystal.getType()==EntityType.END_CRYSTAL&&!crystal.isRemoved())) {
                var replacement=new TetanizedEndCrystalEntity(DragonContent.CRYSTAL_ENTITY,world);
                replacement.refreshPositionAndAngles(original.getX(),original.getY(),original.getZ(),original.getYaw(),original.getPitch());
                replacement.setBeamTarget(original.getBeamTarget());
                replacement.setShowBottom(original.shouldShowBottom());
                replacement.setInvulnerable(original.isInvulnerable());
                // Spawn first so a failed placement never erases a native healing crystal.
                if(world.spawnEntity(replacement)) {original.discard();converted++;}
            }
        }
        return converted;
    }
}
