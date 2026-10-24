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
    public static boolean infectious(DamageSource source){
        if(level(source)>0||source.getSource() instanceof TetanizedArrowEntity)return true;
        if(source.getSource() instanceof NailEntity nail&&nail.getStack().getItem() instanceof com.tetanusmod.item.NailItem item)return item.isRusty();
        if(source.getSource()==source.getAttacker()&&source.getAttacker() instanceof LivingEntity attacker){
            var item=attacker.getMainHandStack().getItem();
            return item instanceof com.tetanusmod.item.RebarItem rebar&&rebar.isRusty()
                    ||item instanceof SpearItem spear&&spear.rusty()||com.tetanusmod.registry.ModIronTools.isRusted(item);
        }
        return false;
    }
    public static int level(DamageSource source){
        if(source.getSource() instanceof RangedCarrier carrier)return carrier.rustruin$weaponLevel();
        return source.getAttacker() instanceof LivingEntity attacker?EnchantmentHelper.getLevel(ModItems.TETANUS_ENCHANTMENT,attacker.getMainHandStack()):0;
    }
    public static boolean expose(LivingEntity target,Entity source,int level,boolean weakArrow){
        return expose(target,source,level,weakArrow,false);
    }
    private static boolean expose(LivingEntity target,Entity source,int level,boolean weakArrow,boolean combined){
        if(target.getWorld().isClient||!target.isAlive()||!InfectionProtection.allows(target,true,target.getRandom().nextFloat(),level))return false;
        int ticks=(TetanusMod.CONFIG.minDurationSeconds+target.getRandom().nextInt(TetanusMod.CONFIG.maxDurationSeconds-TetanusMod.CONFIG.minDurationSeconds+1))*20;
        if(weakArrow)ticks=1200;
        // Extend this hit's rolled duration, never repeatedly multiply the existing infection.
        if(combined)ticks+=ticks/4;
        boolean applied=Infection.applyDurationChecked(target,source,ticks,level>=2?1:0);
        var infection=target.getStatusEffect(TetanusMod.TETANUS);
        if(applied&&infection!=null&&infection.getAmplifier()>=1){
            target.addStatusEffect(new StatusEffectInstance(ExpansionEffects.DISORIENTED,infection.getDuration()),source);
        }
        if(applied)MobVariants.projectileExposure(target,source);
        return applied;
    }
    public static void rangedHit(LivingEntity target,DamageSource source){
        if(!(source.getSource() instanceof ProjectileEntity projectile))return;
        int level=projectile instanceof RangedCarrier carrier?carrier.rustruin$weaponLevel():0;
        boolean rusty=projectile instanceof TetanizedArrowEntity;
        if(level==0&&!rusty)return;
        expose(target,projectile.getOwner(),level,level==0,rusty&&level>0);
    }
}
