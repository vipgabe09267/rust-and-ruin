package com.tetanusmod.expansion;

import com.tetanusmod.entity.MobVariants;
import com.tetanusmod.registry.ModAdvancements;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.*;
import net.minecraft.entity.effect.*;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import java.util.*;

public final class Rabies {
    private static final UUID HEALTH=UUID.fromString("7a8d9db2-a914-49b2-bf82-3e10615308e1");
    private static final UUID ARMOR=UUID.fromString("7a8d9db2-a914-49b2-bf82-3e10615308e2");
    private static final Set<LivingEntity> NAUSEA=Collections.newSetFromMap(new WeakHashMap<>());
    public static boolean rabid(LivingEntity entity){return ((ExpansionState)entity).rustruin$isRabid();}
    public static void queueNausea(LivingEntity entity){NAUSEA.add(entity);}
    public static void flush(LivingEntity entity){if(!entity.getWorld().isClient&&NAUSEA.remove(entity)&&(entity.hasStatusEffect(ExpansionEffects.RABIES)||entity.hasStatusEffect(ExpansionEffects.DISORIENTED)))entity.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA,200));}
    public static boolean expose(LivingEntity entity,Entity source) {
        if(entity.getWorld().isClient||!entity.isAlive()||entity.hasStatusEffect(ExpansionEffects.RABIES))return false;

        boolean applied=entity.addStatusEffect(new StatusEffectInstance(ExpansionEffects.RABIES,-1),source);
        if(applied&&entity instanceof ServerPlayerEntity player)ModAdvancements.award(player,"rabies","contract_rabies");
        return applied;
    }
    public static boolean eligible(MobEntity mob) {
        String id=net.minecraft.registry.Registries.ENTITY_TYPE.getId(mob.getType()).getPath();
        return Set.of("wolf","fox","cat","bat","rabbit","cow","pig","sheep","goat","horse","donkey","mule","llama","trader_llama","camel","panda","polar_bear","hoglin","zoglin","zombie","husk","drowned").contains(id);
    }
    public static void makeRabid(MobEntity mob) {
        if(mob.getWorld().isClient||!mob.isAlive()||rabid(mob)||MobVariants.permanent(mob))return;
        ((ExpansionState)mob).rustruin$setRabid(true);attributes(mob);mob.setHealth(mob.getMaxHealth());mob.setPersistent();installGoal(mob);
    }
    private static void add(LivingEntity entity,EntityAttribute attribute,UUID id,double value) {
        var instance=entity.getAttributeInstance(attribute);
        if(instance!=null&&instance.getModifier(id)==null)instance.addPersistentModifier(new EntityAttributeModifier(id,"Rabid mutation",value,EntityAttributeModifier.Operation.ADDITION));
    }
    private static void attributes(MobEntity mob) {
        add(mob,EntityAttributes.GENERIC_MAX_HEALTH,HEALTH,30);
        add(mob,EntityAttributes.GENERIC_ARMOR,ARMOR,7);
    }
    private static final Set<MobEntity> GOALS=Collections.newSetFromMap(new WeakHashMap<>());
    public static void loaded(LivingEntity entity) {
        if(entity.getWorld().isClient)return;
        if(entity.hasStatusEffect(ExpansionEffects.RABIES))RabiesEffect.restorePenalty(entity);
        if(entity instanceof MobEntity mob&&rabid(mob)){attributes(mob);installGoal(mob);}
    }
    private static void installGoal(MobEntity mob) {
        if(GOALS.add(mob))((MobGoals)mob).rustruin$goals().add(0,new RabidHuntGoal(mob));
    }
    public interface MobGoals {net.minecraft.entity.ai.goal.GoalSelector rustruin$goals();}
    public static final class RabidHuntGoal extends net.minecraft.entity.ai.goal.Goal {
        private final MobEntity mob;private PlayerEntity target;private int cooldown;
        public RabidHuntGoal(MobEntity mob){this.mob=mob;setControls(EnumSet.of(Control.MOVE,Control.LOOK,Control.TARGET));}
        @Override public boolean canStart(){target=mob.getWorld().getClosestPlayer(mob.getX(),mob.getY(),mob.getZ(),16,p->p instanceof PlayerEntity player&&player.isAlive()&&!player.isSpectator()&&!player.isCreative());return target!=null;}
        @Override public boolean shouldContinue(){return target!=null&&target.isAlive()&&!target.isSpectator()&&!target.isCreative()&&mob.squaredDistanceTo(target)<576;}
        @Override public void tick(){
            if(target==null)return;mob.getLookControl().lookAt(target,30,30);mob.getNavigation().startMovingTo(target,1.15);
            if(mob instanceof net.minecraft.entity.passive.BatEntity)mob.getMoveControl().moveTo(target.getX(),target.getEyeY(),target.getZ(),1);
            if(cooldown>0)cooldown--;
            if(cooldown==0&&mob.squaredDistanceTo(target)<4&&mob.canSee(target)) {
                if(!mob.hasStatusEffect(ExpansionEffects.DISORIENTED)||mob.getRandom().nextFloat()>=0.45F)target.damage(mob.getDamageSources().mobAttack(mob),5);
                cooldown=20;
            }
        }
        @Override public void stop(){target=null;mob.getNavigation().stop();}
    }
}