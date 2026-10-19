package com.tetanusmod.expansion;

import com.tetanusmod.entity.MobVariants;
import net.minecraft.entity.*;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.world.Difficulty;

public final class NaturalVariants {
    public static float chance(Difficulty difficulty,boolean spawner){return switch(difficulty){case PEACEFUL->0;case EASY->spawner?0.25F:0.10F;case NORMAL->spawner?0.35F:0.15F;case HARD->spawner?0.45F:0.20F;};}
    public static void spawned(MobEntity mob,SpawnReason reason){
        if(mob.getWorld().isClient||mob instanceof net.minecraft.entity.boss.WitherEntity||mob instanceof net.minecraft.entity.boss.dragon.EnderDragonEntity)return;
        if(reason!=SpawnReason.NATURAL&&reason!=SpawnReason.CHUNK_GENERATION&&reason!=SpawnReason.SPAWNER)return;
        float rate=chance(mob.getWorld().getDifficulty(),reason==SpawnReason.SPAWNER);
        if(mob.getRandom().nextFloat()<rate)MobVariants.makePermanent(mob);
        else if(Rabies.eligible(mob)&&mob.getRandom().nextFloat()<switch(mob.getWorld().getDifficulty()){case PEACEFUL->0F;case EASY->0.01F;case NORMAL->0.02F;case HARD->0.03F;})Rabies.makeRabid(mob);
    }
}