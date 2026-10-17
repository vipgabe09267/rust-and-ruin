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

    public static boolean permanent(LivingEntity entity) { return ((TetanizedState) entity).rustruin$isPermanent(); }
    public static boolean susceptible(LivingEntity entity) {
        return entity instanceof ZombieEntity || entity instanceof CreeperEntity || entity instanceof EndermanEntity
                || entity instanceof SkeletonEntity || entity instanceof VillagerEntity;
    }
    public static boolean eligibleRoll(LivingEntity target, float roll) {
        return target instanceof AnimalEntity ? roll < TetanusMod.CONFIG.animalConversionChance
                : susceptible(target) && roll < TetanusMod.CONFIG.mobConversionChance;
    }
    public static void projectileExposure(LivingEntity target) {
        if (!target.getWorld().isClient && target.isAlive() && !permanent(target) && eligibleRoll(target, target.getRandom().nextFloat())) makePermanent(target);
    }
    public static void loaded(LivingEntity entity) {
        if (entity.getWorld().isClient) return;
        TetanizedState state = (TetanizedState) entity;
        if (entity instanceof WitherEntity && !state.rustruin$hasRolled()) {
            state.rustruin$setRolled(true);
        }
        if (permanent(entity)) applyAttributes(entity, false);
    }
    public static void rollSpecialWither(WitherEntity wither) {
        TetanizedState state=(TetanizedState)wither;
        if(state.rustruin$hasRolled())return;
        state.rustruin$setRolled(true);
        if(wither.getRandom().nextFloat()<TetanusMod.CONFIG.witherConversionChance)makePermanent(wither);
    }
    public static void makePermanent(LivingEntity entity) {
        if (entity.getWorld().isClient || !entity.isAlive() || permanent(entity)) return;
        ((TetanizedState) entity).rustruin$setPermanent(true);
        applyAttributes(entity, true);
        if (entity instanceof MobEntity mob) mob.setPersistent();
        if (entity instanceof WitherEntity wither) {
            wither.setCustomName(Text.translatable("entity.tetanusmod.tetanized_wither"));
            Infection.playSound(entity);
        } else {
            entity.addStatusEffect(new StatusEffectInstance(TetanusMod.TETANUS, -1));
        }
    }
    private static void modifier(LivingEntity entity, EntityAttribute attribute, UUID id, double value) {
        var instance = entity.getAttributeInstance(attribute);
        if (instance != null && instance.getModifier(id) == null) instance.addPersistentModifier(
                new EntityAttributeModifier(id, "Permanent tetanized mutation", value, EntityAttributeModifier.Operation.MULTIPLY_TOTAL));
    }
    private static void applyAttributes(LivingEntity entity, boolean heal) {
        if (entity instanceof AnimalEntity) modifier(entity, EntityAttributes.GENERIC_MOVEMENT_SPEED, SPEED, -0.35);
        else {
            modifier(entity, EntityAttributes.GENERIC_MAX_HEALTH, HEALTH, entity instanceof WitherEntity ? 1.0 : 0.5);
            modifier(entity, EntityAttributes.GENERIC_ATTACK_DAMAGE, DAMAGE, 0.5);
            modifier(entity, EntityAttributes.GENERIC_MOVEMENT_SPEED, SPEED, 0.10);
            // Creepers have no melee damage attribute; their variant gets a larger explosion instead.
            if (entity instanceof CreeperEntity creeper) ((com.tetanusmod.mixin.CreeperAccessor) creeper).rustruin$setExplosionRadius(4);
        }
        if (heal) entity.setHealth(entity.getMaxHealth());
    }
    public static void tick(LivingEntity entity) {
        if (entity.getWorld().isClient || !permanent(entity) || entity.age % 40 != 0) return;
        if (!(entity instanceof WitherEntity) && !entity.hasStatusEffect(TetanusMod.TETANUS)) {
            entity.addStatusEffect(new StatusEffectInstance(TetanusMod.TETANUS, -1));
        }
    }
    public static void drops(LivingEntity entity) {
        if (!permanent(entity) || entity.getWorld().isClient
                || !entity.getWorld().getGameRules().getBoolean(net.minecraft.world.GameRules.DO_MOB_LOOT)) return;
        if (entity instanceof AnimalEntity) entity.dropStack(new ItemStack(ModItems.INFECTED_MEAT, 1 + entity.getRandom().nextInt(3)));
        else if(susceptible(entity)) {
            entity.dropStack(new ItemStack(Items.IRON_NUGGET,1+entity.getRandom().nextInt(3)));
            entity.dropStack(new ItemStack(ModItems.RUST));
        }
        if (entity instanceof WitherEntity) {
            ItemStack book=EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(ModItems.TETANUS_ENCHANTMENT, 1));
            book.setCustomName(Text.translatable("item.tetanusmod.tetanus_book"));
            entity.dropStack(book);
            ItemStack[] loot = {new ItemStack(Items.DIAMOND, 2 + entity.getRandom().nextInt(4)),
                    new ItemStack(Items.NETHERITE_SCRAP, 1 + entity.getRandom().nextInt(2)),
                    new ItemStack(Items.GOLDEN_APPLE, 2), new ItemStack(Items.EMERALD, 4 + entity.getRandom().nextInt(5))};
            entity.dropStack(loot[entity.getRandom().nextInt(loot.length)]);
            entity.dropStack(new ItemStack(Items.EXPERIENCE_BOTTLE, 5 + entity.getRandom().nextInt(6)));
        }
    }
}
