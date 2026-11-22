package com.tetanusmod.entity;

import com.tetanusmod.effect.Infection;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.UUID;

public final class TetanusTntEntity extends TntEntity {
    private static final DustParticleEffect RUST_DUST = new DustParticleEffect(new Vector3f(0.72F, 0.27F, 0.09F), 1.15F);
    private static final int MIN_CLOUD_TICKS = 5 * 20;
    private static final int MAX_CLOUD_TICKS = 15 * 20;
    private static final float CLOUD_RADIUS = 3.5F;

    @Nullable private UUID igniterUuid;

    public TetanusTntEntity(EntityType<? extends TntEntity> type, World world) {
        super(type, world);
    }

    public void setIgniter(@Nullable LivingEntity igniter) {
        igniterUuid = igniter == null ? null : igniter.getUuid();
    }

    @Override
    @Nullable
    public LivingEntity getOwner() {
        if (igniterUuid != null && getWorld() instanceof ServerWorld world
                && world.getEntity(igniterUuid) instanceof LivingEntity igniter) return igniter;
        return null;
    }

    public void leaveRustCloud() {
        if (!(getWorld() instanceof ServerWorld world)) return;
        double y = getBodyY(0.0625);
        Infection.playAt(world, new Vec3d(getX(), y, getZ()), SoundCategory.BLOCKS);
        world.spawnParticles(RUST_DUST, getX(), y + 0.6, getZ(), 64, 1.5, 0.8, 1.5, 0.03);

        var cloud = new AreaEffectCloudEntity(world, getX(), y, getZ());
        cloud.setParticleType(RUST_DUST);
        cloud.setRadius(CLOUD_RADIUS);
        cloud.setWaitTime(0);
        cloud.setDuration(MIN_CLOUD_TICKS + random.nextInt(MAX_CLOUD_TICKS - MIN_CLOUD_TICKS + 1));
        // Shrink gradually while retaining a visible radius until the duration expires.
        cloud.setRadiusGrowth(-2.5F / cloud.getDuration());
        world.spawnEntity(cloud);
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (igniterUuid != null) nbt.putUuid("RustRuinTntIgniter", igniterUuid);
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        igniterUuid = nbt.containsUuid("RustRuinTntIgniter") ? nbt.getUuid("RustRuinTntIgniter") : null;
    }
}
