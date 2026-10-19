package com.tetanusmod.expansion;

import com.tetanusmod.TetanusMod;
import com.tetanusmod.effect.*;
import com.tetanusmod.entity.*;
import com.tetanusmod.registry.ModItems;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;

public final class CombatInfection {
    public interface RangedCarrier {int rustruin$weaponLevel();void rustruin$weaponLevel(int level);}
    public static void prepare(Entity projectile,ItemStack weapon){if(projectile instanceof RangedCarrier carrier)carrier.rustruin$weaponLevel(EnchantmentHelper.getLevel(ModItems.TETANUS_ENCHANTMENT,weapon));}
    public static int level(DamageSource source){
        if(source.getSource() instanceof RangedCarrier carrier)return carrier.rustruin$weaponLevel();
        return source.getAttacker() instanceof LivingEntity attacker?EnchantmentHelper.getLevel(ModItems.TETANUS_ENCHANTMENT,attacker.getMainHandStack()):0;
    }
    public static boolean expose(LivingEntity target,Entity source,int level,boolean weakArrow){
        if(target.getWorld().isClient||!target.isAlive()||!InfectionProtection.allows(target,true,target.getRandom().nextFloat(),level))return false;
        int ticks=(TetanusMod.CONFIG.minDurationSeconds+target.getRandom().nextInt(TetanusMod.CONFIG.maxDurationSeconds-TetanusMod.CONFIG.minDurationSeconds+1))*20;
        if(weakArrow)ticks=1200;
        boolean applied=Infection.applyDurationChecked(target,source,ticks,level>=2?1:0);
        if(applied&&level>=2){target.removeStatusEffect(ExpansionEffects.DISORIENTED);target.addStatusEffect(new StatusEffectInstance(ExpansionEffects.DISORIENTED,ticks),source);}
        MobVariants.projectileExposure(target,source);
        return applied;
    }
    public static void rangedHit(LivingEntity target,DamageSource source){
        if(!(source.getSource() instanceof ProjectileEntity projectile))return;
        int level=projectile instanceof RangedCarrier carrier?carrier.rustruin$weaponLevel():0;
        boolean rusty=projectile instanceof TetanizedArrowEntity;
        if(level==0&&!rusty)return;
        boolean applied=expose(target,projectile.getOwner(),level,level==0);
        if(applied&&rusty&&level>0&&target.hasStatusEffect(TetanusMod.TETANUS)&&!MobVariants.permanent(target)){
            var effect=target.getStatusEffect(TetanusMod.TETANUS);
            int extended=effect.getDuration()+effect.getDuration()/4;
            InfectionProtection.checked(target,()->target.addStatusEffect(new StatusEffectInstance(TetanusMod.TETANUS,extended,effect.getAmplifier()),projectile.getOwner()));
            if(level>=2)target.addStatusEffect(new StatusEffectInstance(ExpansionEffects.DISORIENTED,extended),projectile.getOwner());
        }
    }
}