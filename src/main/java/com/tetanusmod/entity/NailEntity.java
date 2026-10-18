package com.tetanusmod.entity;

import com.tetanusmod.TetanusMod;
import com.tetanusmod.effect.Infection;
import com.tetanusmod.item.NailItem;
import com.tetanusmod.rust.RustingSystem;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;

public final class NailEntity extends ThrownItemEntity {
    private boolean recoverable;
    public NailEntity(EntityType<? extends NailEntity> type, World world) { super(type, world); }
    public NailEntity(World world, LivingEntity owner) { super(TetanusMod.NAIL_ENTITY, owner, world); }
    public void setRecoverable(boolean value) { recoverable = value; }

    @Override
    protected Item getDefaultItem() { return TetanusMod.NAIL; }

    @Override
    protected void onEntityHit(EntityHitResult hit) {
        super.onEntityHit(hit);
        if (!getWorld().isClient && getStack().getItem() instanceof NailItem nail
                && hit.getEntity().damage(nail.isRusty() && hit.getEntity() instanceof net.minecraft.entity.mob.EndermanEntity
                        ? getDamageSources().magic() : getDamageSources().thrown(this, getOwner()), nail.isBent() ? 1 : TetanusMod.CONFIG.nailDamage)
                && nail.isRusty() && hit.getEntity() instanceof LivingEntity living) {
            Infection.apply(living, getOwner());
            MobVariants.projectileExposure(living,getOwner());
        }
    }

    @Override
    protected void onCollision(HitResult hit) {
        super.onCollision(hit);
        if (!getWorld().isClient) {
            getWorld().playSound(null, getBlockPos(), SoundEvents.BLOCK_CHAIN_HIT, SoundCategory.PLAYERS, 0.6F, 1.4F);
            if (getStack().getItem() instanceof NailItem nail && nail.isRusty()) RustingSystem.particles(getWorld(), getPos());
            // Only survival-thrown nails hitting blocks can be recovered. Entity hits consume the nail.
            if (recoverable && hit.getType() == HitResult.Type.BLOCK) {
                recoverable = false;
                ItemStack dropped = getStack().copyWithCount(1);
                var pos = getPos().subtract(getVelocity().normalize().multiply(0.15));
                ItemEntity item = new ItemEntity(getWorld(), pos.x, pos.y, pos.z, dropped);
                item.setToDefaultPickupDelay();
                getWorld().spawnEntity(item);
            }
            getWorld().sendEntityStatus(this, (byte) 3);
            discard();
        }
    }

    @Override public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putBoolean("Recoverable", recoverable);
    }
    @Override public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        recoverable = nbt.getBoolean("Recoverable");
    }

    @Override
    public void tick() {
        super.tick();
        if (!getWorld().isClient && age > 200) discard();
    }

    @Override
    public void handleStatus(byte status) {
        if (status == 3) {
            for (int i = 0; i < 6; i++) {
                getWorld().addParticle(new ItemStackParticleEffect(ParticleTypes.ITEM, getStack()),
                        getX(), getY(), getZ(), 0, 0, 0);
            }
        } else super.handleStatus(status);
    }
}
