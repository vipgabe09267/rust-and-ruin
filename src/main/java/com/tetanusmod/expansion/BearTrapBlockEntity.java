package com.tetanusmod.expansion;

import com.tetanusmod.TetanusMod;
import com.tetanusmod.effect.Infection;
import com.tetanusmod.rust.RustingSystem;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import java.util.UUID;

public final class BearTrapBlockEntity extends BlockEntity {
    public static final int RESET_GRACE_TICKS = 60;
    private UUID captured;
    private double anchorX, anchorY, anchorZ;
    private int water, rain, rearmTicks;

    public BearTrapBlockEntity(BlockPos pos, BlockState state) { super(ExpansionBlocks.BEAR_TRAP_ENTITY, pos, state); }
    public boolean holds(Entity target) { return captured != null && captured.equals(target.getUuid()) && getCachedState().get(BearTrapBlock.CLOSED); }
    public boolean armed() { return !getCachedState().get(BearTrapBlock.CLOSED) && rearmTicks == 0; }

    public boolean trigger(LivingEntity target) {
        if (world == null || world.isClient || !armed() || !target.isAlive() || target.isSpectator() || target.hasVehicle()
                || target instanceof PlayerEntity player && player.getAbilities().creativeMode) return false;
        captured = target.getUuid();
        anchorX = target.getX(); anchorY = target.getY(); anchorZ = target.getZ();
        world.setBlockState(pos, getCachedState().with(BearTrapBlock.CLOSED, true), Block.NOTIFY_ALL);
        sync();
        target.damage(ModDamage.source(target, ModDamage.BEAR_TRAP), 3);
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 9));
        if (((BearTrapBlock) getCachedState().getBlock()).rusty())
            Infection.at(world, Vec3d.ofCenter(pos), SoundCategory.BLOCKS, () -> Infection.expose(target, null));
        world.playSound(null, pos, SoundEvents.BLOCK_CHAIN_PLACE, SoundCategory.BLOCKS, 1, 0.6F);
        return true;
    }

    public boolean reset() {
        if (world == null || world.isClient || !getCachedState().get(BearTrapBlock.CLOSED)) return false;
        release();
        // Immediate rearming recaptured the victim in the very same tick as resetting.
        rearmTicks = RESET_GRACE_TICKS;
        world.setBlockState(pos, getCachedState().with(BearTrapBlock.CLOSED, false), Block.NOTIFY_ALL);
        sync();
        world.playSound(null, pos, SoundEvents.BLOCK_IRON_DOOR_OPEN, SoundCategory.BLOCKS, 0.7F, 1.3F);
        return true;
    }

    public void release() {
        if (world != null && !world.isClient && captured != null && world instanceof ServerWorld server
                && server.getEntity(captured) instanceof LivingEntity target) {
            var effect = target.getStatusEffect(StatusEffects.SLOWNESS);
            if (effect != null && effect.getAmplifier() == 9 && !effect.isInfinite() && effect.getDuration() <= 100) target.removeStatusEffect(StatusEffects.SLOWNESS);
        }
        captured = null;
        sync();
    }

    private void restrain(ServerWorld server) {
        if (captured == null || !getCachedState().get(BearTrapBlock.CLOSED)) return;
        Entity found = server.getEntity(captured);
        if (!(found instanceof LivingEntity target) || !target.isAlive() || target.isSpectator()
                || target instanceof PlayerEntity player && player.getAbilities().creativeMode) {
            release(); return;
        }
        // An external teleport or a dimension change must never pull a player back.
        if (target.squaredDistanceTo(anchorX, anchorY, anchorZ) > 9 || target.hasVehicle()) { release(); return; }
        if (target instanceof MobEntity mob) mob.getNavigation().stop();
        target.setVelocity(Vec3d.ZERO);
        if (target.squaredDistanceTo(anchorX, anchorY, anchorZ) > 0.0004) {
            if (target instanceof ServerPlayerEntity player)
                player.networkHandler.requestTeleport(anchorX, anchorY, anchorZ, player.getYaw(), player.getPitch());
            else target.setPosition(anchorX, anchorY, anchorZ);
        }
        if (world.getTime() % 20 == 0) target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 9));
    }

    private void sync() {
        markDirty();
        if (world != null && !world.isClient) world.updateListeners(pos, getCachedState(), getCachedState(), Block.NOTIFY_LISTENERS);
    }

    public static void tick(World world, BlockPos pos, BlockState state, BearTrapBlockEntity trap) {
        if (world instanceof ServerWorld server) trap.restrain(server);
        if (trap.rearmTicks > 0 && (--trap.rearmTicks % 20 == 0)) trap.markDirty();
        if (((BearTrapBlock) state.getBlock()).rusty()) return;
        int oldWater = trap.water, oldRain = trap.rain;
        boolean wet = world.getFluidState(pos).isIn(net.minecraft.registry.tag.FluidTags.WATER)
                || world.getFluidState(pos.up()).isIn(net.minecraft.registry.tag.FluidTags.WATER);
        if (wet) { trap.rain = 0; trap.water++; }
        else { trap.water = 0; trap.rain = TetanusMod.CONFIG.rainRusting && world.hasRain(pos) ? trap.rain + 1 : 0; }
        if (trap.water >= TetanusMod.CONFIG.waterRustTicks || trap.rain >= TetanusMod.CONFIG.rainRustTicks) {
            NbtCompound saved = trap.createNbt();
            world.setBlockState(pos, ExpansionBlocks.RUSTED_BEAR_TRAP.getDefaultState().with(BearTrapBlock.CLOSED, state.get(BearTrapBlock.CLOSED)), Block.NOTIFY_ALL);
            if (world.getBlockEntity(pos) instanceof BearTrapBlockEntity rusty) { rusty.readNbt(saved); rusty.sync(); }
            RustingSystem.rustFeedback(world, Vec3d.ofCenter(pos));
        }
        if ((oldWater != trap.water || oldRain != trap.rain) && (world.getTime() % 20 == 0 || trap.water == 0 && trap.rain == 0)) trap.markDirty();
    }

    @Override public BlockEntityUpdateS2CPacket toUpdatePacket() { return BlockEntityUpdateS2CPacket.create(this); }
    @Override public NbtCompound toInitialChunkDataNbt() { return createNbt(); }
    @Override protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        if (captured != null) { nbt.putUuid("Captured", captured); nbt.putDouble("AnchorX", anchorX); nbt.putDouble("AnchorY", anchorY); nbt.putDouble("AnchorZ", anchorZ); }
        nbt.putInt("Water", water); nbt.putInt("Rain", rain); nbt.putInt("RearmTicks", rearmTicks);
    }
    @Override public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        captured = nbt.containsUuid("Captured") ? nbt.getUuid("Captured") : null;
        anchorX = finiteAnchor(nbt, "AnchorX", pos.getX() + 0.5);
        anchorY = finiteAnchor(nbt, "AnchorY", pos.getY());
        anchorZ = finiteAnchor(nbt, "AnchorZ", pos.getZ() + 0.5);
        water = Math.max(0, Math.min(72000, nbt.getInt("Water")));
        rain = Math.max(0, Math.min(72000, nbt.getInt("Rain")));
        rearmTicks = Math.max(0, Math.min(RESET_GRACE_TICKS, nbt.getInt("RearmTicks")));
    }
    private static double finiteAnchor(NbtCompound nbt, String key, double fallback) {
        double value = nbt.contains(key) ? nbt.getDouble(key) : fallback;
        return Double.isFinite(value) && Math.abs(value - fallback) <= 2 ? value : fallback;
    }
}
