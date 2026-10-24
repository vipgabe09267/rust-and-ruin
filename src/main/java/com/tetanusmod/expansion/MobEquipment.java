package com.tetanusmod.expansion;

import com.tetanusmod.entity.NailEntity;
import com.tetanusmod.registry.ModItems;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.mob.*;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.world.Difficulty;
import java.util.EnumSet;

public final class MobEquipment {
    public static float chance(Difficulty difficulty){return switch(difficulty){case PEACEFUL->0;case EASY->0.02F;case NORMAL->0.04F;case HARD->0.06F;};}
    public static void spawned(MobEntity mob,SpawnReason reason){
        if(mob.getWorld().isClient||reason!=SpawnReason.NATURAL&&reason!=SpawnReason.CHUNK_GENERATION&&reason!=SpawnReason.SPAWNER)return;
        if(mob.getRandom().nextFloat()>=chance(mob.getWorld().getDifficulty()))return;
        if(mob instanceof SkeletonEntity||mob instanceof StrayEntity){
            mob.equipStack(EquipmentSlot.MAINHAND,new ItemStack(ModItems.RUSTED_NAIL));
            ((AbstractSkeletonEntity)mob).updateAttackType();
        }else if(mob instanceof ZombieEntity){
            var weapons=new net.minecraft.item.Item[]{ModItems.REBAR,ModItems.RUSTED_METAL_PIPE,ExpansionItems.RUSTED_SPEAR};
            mob.equipStack(EquipmentSlot.MAINHAND,new ItemStack(weapons[mob.getRandom().nextInt(weapons.length)]));
        }
    }
    public static void refreshSkeleton(AbstractSkeletonEntity skeleton){
        if(skeleton.getWorld().isClient)return;
        var selector=((Rabies.MobGoals)skeleton).rustruin$goals();
        selector.getGoals().stream().map(PrioritizedGoal::getGoal).filter(goal->goal instanceof NailThrowGoal).toList().forEach(selector::remove);
        if(!skeleton.getMainHandStack().isOf(ModItems.RUSTED_NAIL))return;
        selector.getGoals().stream().map(PrioritizedGoal::getGoal).filter(goal->goal instanceof MeleeAttackGoal||goal instanceof BowAttackGoal).toList().forEach(selector::remove);
        selector.add(3,new NailThrowGoal(skeleton));
    }
    public static final class NailThrowGoal extends Goal {
        private final AbstractSkeletonEntity skeleton;
        private int cooldown;
        public NailThrowGoal(AbstractSkeletonEntity skeleton){this.skeleton=skeleton;setControls(EnumSet.of(Control.MOVE,Control.LOOK));}
        @Override public boolean canStart(){var target=skeleton.getTarget();return skeleton.isAlive()&&skeleton.getMainHandStack().isOf(ModItems.RUSTED_NAIL)&&target!=null&&target.isAlive()&&target.getWorld()==skeleton.getWorld()&&!(target instanceof net.minecraft.entity.player.PlayerEntity player&&(player.isCreative()||player.isSpectator()));}
        @Override public boolean shouldContinue(){return canStart();}
        @Override public void tick(){
            if(!canStart())return;var target=skeleton.getTarget();
            skeleton.getLookControl().lookAt(target,30,30);
            double distance=skeleton.squaredDistanceTo(target);
            if(distance>64||!skeleton.canSee(target))skeleton.getNavigation().startMovingTo(target,1);else skeleton.getNavigation().stop();
            if(cooldown>0)cooldown--;
            if(cooldown==0&&distance<=144&&skeleton.canSee(target)){
                var nail=new NailEntity(skeleton.getWorld(),skeleton);nail.setItem(new ItemStack(ModItems.RUSTED_NAIL));
                // Mob ammunition cannot be farmed by collecting missed projectiles.
                nail.setRecoverable(false);
                double dx=target.getX()-nail.getX(),dz=target.getZ()-nail.getZ();
                double dy=target.getBodyY(0.5)-nail.getY()+Math.sqrt(dx*dx+dz*dz)*0.12;
                nail.setVelocity(dx,dy,dz,1.35F,5);
                skeleton.getWorld().spawnEntity(nail);skeleton.swingHand(Hand.MAIN_HAND);
                skeleton.playSound(net.minecraft.sound.SoundEvents.ENTITY_SNOWBALL_THROW,0.8F,0.9F);
                cooldown=40;
            }
        }
        @Override public void stop(){skeleton.getNavigation().stop();}
    }
}
