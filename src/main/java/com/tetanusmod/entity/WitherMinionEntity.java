package com.tetanusmod.entity;

import net.minecraft.entity.*;
import net.minecraft.entity.attribute.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.WitherSkeletonEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import java.util.UUID;

/** A small, temporary boss summon; it cannot be farmed for swords or generic mutation salvage. */
public final class WitherMinionEntity extends WitherSkeletonEntity {
    private UUID boss;
    private long expiresAt;
    public WitherMinionEntity(EntityType<? extends WitherSkeletonEntity> type,World world){
        super(type,world);experiencePoints=0;setCanPickUpLoot(false);
        ((TetanizedState)(Object)this).rustruin$setPermanent(true);
        equipStack(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_SWORD));
        setHealth(getMaxHealth());
        expiresAt=world.getTime()+WitherBossEncounter.MINION_LIFETIME;
    }
    public static DefaultAttributeContainer.Builder attributes(){
        return createAbstractSkeletonAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH,20)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE,4).add(EntityAttributes.GENERIC_MOVEMENT_SPEED,0.28);
    }
    @Override public EntityData initialize(net.minecraft.world.ServerWorldAccess world,net.minecraft.world.LocalDifficulty difficulty,SpawnReason reason,EntityData data,NbtCompound nbt){
        var initialized=super.initialize(world,difficulty,reason,data,nbt);
        // Native skeleton initialization randomizes looting; temporary summons must never destroy player gear.
        setCanPickUpLoot(false);equipStack(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_SWORD));setHealth(getMaxHealth());return initialized;
    }
    public void assignBoss(UUID id,long expiry){boss=id;expiresAt=expiry;}
    public UUID bossUuid(){return boss;}
    public long expiresAt(){return expiresAt;}
    @Override protected float getActiveEyeHeight(EntityPose pose,EntityDimensions dimensions){return dimensions.height*0.85F;}
    @Override public void tick(){
        if(!getWorld().isClient){
            if(getWorld().getTime()>=expiresAt){discard();return;}
            if(boss!=null&&getWorld() instanceof ServerWorld world){
                Entity owner=world.getEntity(boss);
                if(owner!=null&&(!owner.isAlive()||owner.isRemoved())){discard();return;}
            }
        }
        super.tick();
    }
    @Override public void writeCustomDataToNbt(NbtCompound nbt){
        super.writeCustomDataToNbt(nbt);if(boss!=null)nbt.putUuid("RustRuinMinionBoss",boss);nbt.putLong("RustRuinMinionExpires",expiresAt);
    }
    @Override public void readCustomDataFromNbt(NbtCompound nbt){
        super.readCustomDataFromNbt(nbt);setCanPickUpLoot(false);boss=nbt.containsUuid("RustRuinMinionBoss")?nbt.getUuid("RustRuinMinionBoss"):null;
        long now=getWorld().getTime();expiresAt=nbt.contains("RustRuinMinionExpires")?Math.min(now+WitherBossEncounter.MINION_LIFETIME,nbt.getLong("RustRuinMinionExpires")):now+WitherBossEncounter.MINION_LIFETIME;
        // A saved0.1minion retains its native base attribute; migrate that old default without healing its damage.
        var maximum=getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if(maximum!=null&&Double.compare(maximum.getBaseValue(),10.0)==0){float oldMaximum=getMaxHealth(),oldHealth=getHealth();maximum.setBaseValue(20);if(oldMaximum>0)setHealth(Math.min(getMaxHealth(),oldHealth/oldMaximum*getMaxHealth()));}
        ((TetanizedState)(Object)this).rustruin$setPermanent(true);
    }
    @Override protected void dropLoot(DamageSource source,boolean causedByPlayer){}
    @Override protected void dropEquipment(DamageSource source,int looting,boolean causedByPlayer){}
}
