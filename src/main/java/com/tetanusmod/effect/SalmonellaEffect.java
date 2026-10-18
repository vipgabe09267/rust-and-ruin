package com.tetanusmod.effect;
import com.tetanusmod.TetanusMod;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.particle.DustParticleEffect;
import org.joml.Vector3f;
public final class SalmonellaEffect extends StatusEffect {
    private static final java.util.Set<LivingEntity> NAUSEA=java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());
    public static void flush(LivingEntity entity) {
        if(!entity.getWorld().isClient && NAUSEA.remove(entity) && entity.hasStatusEffect(TetanusMod.SALMONELLA))
            entity.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA,160));
    }
    public SalmonellaEffect(){super(StatusEffectCategory.HARMFUL,0x79833A);}
    @Override public boolean canApplyUpdateEffect(int duration,int amplifier){return duration>0&&duration<1200&&duration%200==0;}
    @Override public void applyUpdateEffect(LivingEntity entity,int amplifier) {
        if(!(entity.getWorld() instanceof ServerWorld world))return;
        if(entity instanceof PlayerEntity player) {
            player.getHungerManager().setFoodLevel(Math.max(0,player.getHungerManager().getFoodLevel()-2));
            player.getHungerManager().setSaturationLevel(Math.max(0,player.getHungerManager().getSaturationLevel()-2));
        }
        float damage=Math.min(0.5F,Math.max(0,entity.getHealth()-1));
        if(damage>0)entity.damage(entity.getDamageSources().magic(),damage);
        NAUSEA.add(entity);
        var direction=entity.getRotationVec(1).multiply(0.18);
        var mouth=entity.getEyePos().add(direction);
        for(int i=0;i<10;i++)world.spawnParticles(new DustParticleEffect(new Vector3f(0.47F,0.45F,0.16F),0.85F),
                mouth.x,mouth.y-0.15,mouth.z,0,direction.x+(world.random.nextDouble()-0.5)*0.08,
                -0.04-world.random.nextDouble()*0.08,direction.z+(world.random.nextDouble()-0.5)*0.08,1);
        world.playSound(null,entity.getBlockPos(),TetanusMod.VOMIT_SOUND,entity.getSoundCategory(),0.7F,0.75F);
    }
}
