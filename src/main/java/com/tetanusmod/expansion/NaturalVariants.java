package com.tetanusmod.expansion;

import com.tetanusmod.entity.MobVariants;
import net.minecraft.entity.*;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.world.Difficulty;

public final class NaturalVariants {
    public static float chance(Difficulty difficulty, boolean spawner) {
        return chance(difficulty, spawner, false);
    }
    public static float chance(Difficulty difficulty, boolean spawner, boolean animal) {
        var c = com.tetanusmod.TetanusMod.CONFIG;
        return switch (difficulty) {
            case PEACEFUL -> 0;
            case EASY -> spawner ? c.spawnerEasy : animal ? c.naturalAnimalEasy : c.naturalMobEasy;
            case NORMAL -> spawner ? c.spawnerNormal : animal ? c.naturalAnimalNormal : c.naturalMobNormal;
            case HARD -> spawner ? c.spawnerHard : animal ? c.naturalAnimalHard : c.naturalMobHard;
        };
    }
    public static void spawned(MobEntity mob,SpawnReason reason){
        if(mob.getWorld().isClient||mob instanceof net.minecraft.entity.boss.WitherEntity||mob instanceof net.minecraft.entity.boss.dragon.EnderDragonEntity)return;
        if(reason!=SpawnReason.NATURAL&&reason!=SpawnReason.CHUNK_GENERATION&&reason!=SpawnReason.SPAWNER)return;
        float rate=chance(mob.getWorld().getDifficulty(),reason==SpawnReason.SPAWNER,MobVariants.isAnimal(mob));
        if(mob.getRandom().nextFloat()<rate)MobVariants.makePermanent(mob);
        else if(Rabies.eligible(mob)&&mob.getRandom().nextFloat()<switch(mob.getWorld().getDifficulty()){case PEACEFUL->0F;case EASY->com.tetanusmod.TetanusMod.CONFIG.naturalRabiesEasy;case NORMAL->com.tetanusmod.TetanusMod.CONFIG.naturalRabiesNormal;case HARD->com.tetanusmod.TetanusMod.CONFIG.naturalRabiesHard;})Rabies.makeRabid(mob);
    }
}