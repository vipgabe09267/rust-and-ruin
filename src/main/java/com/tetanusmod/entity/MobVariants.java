package com.tetanusmod.entity;

import com.tetanusmod.TetanusMod;
import com.tetanusmod.effect.Infection;
import com.tetanusmod.registry.ModItems;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.EnchantedBookItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import java.util.UUID;

public final class MobVariants {
    private static final UUID HEALTH = UUID.fromString("a70b1111-6adf-4cca-8713-c481a7c5e011");
    private static final UUID DAMAGE = UUID.fromString("a70b2222-6adf-4cca-8713-c481a7c5e022");
    private static final UUID SPEED = UUID.fromString("a70b3333-6adf-4cca-8713-c481a7c5e033");

    /** Only the requested dragon mutation may exceed vanilla's1024health ceiling, and only up to1100. */
    public static double dragonHealthValue(net.minecraft.entity.attribute.EntityAttributeInstance attribute,double vanillaValue){
        if(attribute.getAttribute()!=EntityAttributes.GENERIC_MAX_HEALTH||Double.compare(vanillaValue,1024.0)!=0)return vanillaValue;
        var marker=attribute.getModifier(HEALTH);
        if(marker==null||marker.getOperation()!=EntityAttributeModifier.Operation.MULTIPLY_TOTAL||Double.compare(marker.getValue(),4.5)!=0)return vanillaValue;
        // Match native ADDITION -> MULTIPLY_BASE -> MULTIPLY_TOTAL ordering; preserve debuffs and the cached attribute value.
        double base=attribute.getBaseValue();
        for(var modifier:attribute.getModifiers(EntityAttributeModifier.Operation.ADDITION))base+=modifier.getValue();
        double value=base;
        for(var modifier:attribute.getModifiers(EntityAttributeModifier.Operation.MULTIPLY_BASE))value+=base*modifier.getValue();
        for(var modifier:attribute.getModifiers(EntityAttributeModifier.Operation.MULTIPLY_TOTAL))value*=1+modifier.getValue();
        return Double.isFinite(value)&&value>vanillaValue?Math.min(1100.0,value):vanillaValue;
    }

    public static boolean permanent(LivingEntity entity) { return ((TetanizedState) entity).rustruin$isPermanent(); }
    public static boolean isAnimal(LivingEntity entity) { return entity instanceof AnimalEntity || entity instanceof net.minecraft.entity.mob.WaterCreatureEntity || entity instanceof net.minecraft.entity.mob.AmbientEntity; }
    public static boolean infectedAnimal(LivingEntity entity){return isAnimal(entity)&&permanent(entity);}
    public static boolean susceptible(LivingEntity entity) {
        return entity instanceof MobEntity && !(entity instanceof WitherEntity)
                && !(entity instanceof net.minecraft.entity.boss.dragon.EnderDragonEntity) && !(entity instanceof WitherMinionEntity);
    }
    public static boolean eligibleRoll(LivingEntity target, float roll) {
        // A zero general mob chance also disables the dedicated golem contact roll.
        if(target instanceof net.minecraft.entity.passive.IronGolemEntity)
            return TetanusMod.CONFIG.mobConversionChance>0&&roll<TetanusMod.CONFIG.golemConversionChance;
        return isAnimal(target) ? roll < TetanusMod.CONFIG.animalConversionChance
                : susceptible(target) && roll < TetanusMod.CONFIG.mobConversionChance;
    }
    public static void projectileExposure(LivingEntity target) { projectileExposure(target,null); }
    public static void projectileExposure(LivingEntity target, net.minecraft.entity.Entity source) {
        if (!target.getWorld().isClient && target.isAlive() && !permanent(target) && !com.tetanusmod.expansion.Rabies.rabid(target) && eligibleRoll(target, target.getRandom().nextFloat())) {
            makePermanent(target);
            if(source instanceof net.minecraft.server.network.ServerPlayerEntity player)com.tetanusmod.registry.ModAdvancements.converted(player,target);
        }
    }
    public static void loaded(LivingEntity entity) {
        if (entity.getWorld().isClient) return;
        TetanizedState state = (TetanizedState) entity;
        if (entity instanceof WitherEntity && !state.rustruin$hasRolled()) {
            state.rustruin$setRolled(true);
        }
        if (permanent(entity)) applyAttributes(entity, false);
    }
    public static void rollSpecialWither(WitherEntity wither) { rollSpecialWither(wither,0); }
    public static void rollSpecialWither(WitherEntity wither,float bonus) {
        TetanizedState state=(TetanizedState)wither;
        if(state.rustruin$hasRolled())return;
        state.rustruin$setRolled(true);
        if(wither.getRandom().nextFloat()<Math.min(1,TetanusMod.CONFIG.witherConversionChance+bonus))makePermanent(wither);
    }
    public static void makePermanent(LivingEntity entity) {
        if (entity.getWorld().isClient || !entity.isAlive() || permanent(entity) || com.tetanusmod.expansion.Rabies.rabid(entity)) return;
        ((TetanizedState) entity).rustruin$setPermanent(true);
        applyAttributes(entity, true);
        if (entity instanceof WitherEntity wither) {
            wither.setCustomName(Text.translatable("entity.tetanusmod.tetanized_wither"));
            entity.getWorld().playSound(null,entity.getBlockPos(),net.minecraft.sound.SoundEvents.ENTITY_WITHER_AMBIENT,entity.getSoundCategory(),1.0F,0.65F);
        } else if (!(entity instanceof net.minecraft.entity.boss.dragon.EnderDragonEntity) && !(entity instanceof WitherMinionEntity)) {
            entity.addStatusEffect(new StatusEffectInstance(TetanusMod.TETANUS, -1));
        }
    }
    private static void modifier(LivingEntity entity, EntityAttribute attribute, UUID id, double value) {
        var instance = entity.getAttributeInstance(attribute);
        if(instance==null)return;
        var existing=instance.getModifier(id);
        if(existing!=null && Double.compare(existing.getValue(),value)!=0)instance.removeModifier(id);
        if (instance.getModifier(id) == null) instance.addPersistentModifier(
                new EntityAttributeModifier(id, "Permanent tetanized mutation", value, EntityAttributeModifier.Operation.MULTIPLY_TOTAL));
    }
    private static void applyAttributes(LivingEntity entity, boolean heal) {
        if(entity instanceof WitherMinionEntity)return;
        float previousMaximum=entity.getMaxHealth(),previousHealth=entity.getHealth();
        if (isAnimal(entity)) modifier(entity, EntityAttributes.GENERIC_MOVEMENT_SPEED, SPEED, -0.35);
        else {
            double healthBonus=entity instanceof WitherEntity ? 650.0/300.0-1.0
                    :entity instanceof net.minecraft.entity.boss.dragon.EnderDragonEntity?4.5
                    :entity instanceof net.minecraft.entity.passive.IronGolemEntity?1.0:0.5;
            modifier(entity, EntityAttributes.GENERIC_MAX_HEALTH, HEALTH, healthBonus);
            modifier(entity, EntityAttributes.GENERIC_ATTACK_DAMAGE, DAMAGE, 0.5);
            modifier(entity, EntityAttributes.GENERIC_MOVEMENT_SPEED, SPEED, 0.10);
            // Creepers have no melee damage attribute; their variant gets a larger explosion instead.
            if (entity instanceof CreeperEntity creeper) {
                var blast=(com.tetanusmod.mixin.CreeperAccessor)creeper;
                if(blast.rustruin$getExplosionRadius()<4)blast.rustruin$setExplosionRadius(4);
            }
        }
        if (heal) entity.setHealth(entity.getMaxHealth());
        else if((entity instanceof WitherEntity||entity instanceof net.minecraft.entity.boss.dragon.EnderDragonEntity)
                &&previousMaximum>0&&Float.compare(previousMaximum,entity.getMaxHealth())!=0)
            entity.setHealth(Math.min(entity.getMaxHealth(),Math.max(0,previousHealth/previousMaximum*entity.getMaxHealth())));
    }
    public static boolean cureAnimal(LivingEntity entity) {
        if (entity.getWorld().isClient || !entity.isAlive() || !isAnimal(entity) || !permanent(entity)) return false;
        ((TetanizedState) entity).rustruin$setPermanent(false);
        var speed = entity.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (speed != null) speed.removeModifier(SPEED);
        entity.removeStatusEffect(TetanusMod.TETANUS);
        entity.removeStatusEffect(TetanusMod.LOCKJAW);
        entity.removeStatusEffect(com.tetanusmod.expansion.ExpansionEffects.DISORIENTED);
        var state = (com.tetanusmod.effect.InfectionState) entity;
        state.tetanus$setLockjaw(0);
        state.tetanus$setFreeze(0);
        return true;
    }
    public static void tick(LivingEntity entity) {
        if (entity.getWorld().isClient || !permanent(entity) || entity.age % 40 != 0) return;
        if (!(entity instanceof WitherEntity) && !(entity instanceof net.minecraft.entity.boss.dragon.EnderDragonEntity)
                && !(entity instanceof WitherMinionEntity) && !entity.hasStatusEffect(TetanusMod.TETANUS)) {
            Infection.silently(()->entity.addStatusEffect(new StatusEffectInstance(TetanusMod.TETANUS, -1)));
        }
    }
    public static void drops(LivingEntity entity) {
        if (entity instanceof WitherMinionEntity || (!permanent(entity)&&!infectedAnimal(entity)) || entity.getWorld().isClient
                || !entity.getWorld().getGameRules().getBoolean(net.minecraft.world.GameRules.DO_MOB_LOOT)) return;
        if (isAnimal(entity)) entity.dropStack(new ItemStack(ModItems.INFECTED_MEAT, 1 + entity.getRandom().nextInt(3)));
        else if(susceptible(entity)) {
            net.minecraft.item.Item[] salvage={Items.IRON_NUGGET,Items.COPPER_INGOT,Items.FLINT,Items.STRING,Items.COAL,Items.BONE};
            var reward=salvage[entity.getRandom().nextInt(salvage.length)];
            entity.dropStack(new ItemStack(reward,reward==Items.IRON_NUGGET?1+entity.getRandom().nextInt(3):1));
            entity.dropStack(new ItemStack(ModItems.RUST));
        }
        if (entity instanceof WitherEntity) {
            // Exactly one book: the rare level replaces level I instead of adding a second book.
            entity.dropStack(com.tetanusmod.expansion.ExpansionItems.tetanusBook(entity.getRandom().nextFloat()<0.5F?2:1));
            ItemStack[] loot = {new ItemStack(Items.DIAMOND, 2 + entity.getRandom().nextInt(4)),
                    new ItemStack(Items.NETHERITE_SCRAP, 1 + entity.getRandom().nextInt(2)),
                    new ItemStack(Items.GOLDEN_APPLE, 2), new ItemStack(Items.EMERALD, 4 + entity.getRandom().nextInt(5))};
            entity.dropStack(loot[entity.getRandom().nextInt(loot.length)]);
            entity.dropStack(new ItemStack(Items.EXPERIENCE_BOTTLE, 5 + entity.getRandom().nextInt(6)));
        }
    }
}
