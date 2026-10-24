package com.tetanusmod.block;

import com.tetanusmod.TetanusMod;
import com.tetanusmod.effect.Infection;
import com.tetanusmod.registry.ModBlocks;
import com.tetanusmod.rust.RustingSystem;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CaltropsBlockEntity extends BlockEntity {
    private int waterTicks;
    private int rainTicks;
    private final Map<UUID, Long> nextHit = new HashMap<>();

    public CaltropsBlockEntity(BlockPos pos, BlockState state) { super(ModBlocks.CALTROPS_ENTITY, pos, state); }

    public void stepOn(LivingEntity entity, boolean rusty) {
        if(world==null||world.isClient||!entity.isAlive()||entity.isSpectator())return;
        long now = world.getTime();
        if (now < nextHit.getOrDefault(entity.getUuid(), 0L)) return;
        nextHit.put(entity.getUuid(), now + 20);
        boolean wire=getCachedState().isOf(ModBlocks.BARBED_WIRE)||getCachedState().isOf(ModBlocks.RUSTED_BARBED_WIRE);
        if(wire)entity.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.SLOWNESS,60,1));
        if (entity.damage(wire?com.tetanusmod.expansion.ModDamage.source(entity,com.tetanusmod.expansion.ModDamage.BARBED_WIRE):world.getDamageSources().cactus(), wire?2.0F:1.0F) && rusty) {
            Infection.at(world,Vec3d.ofCenter(pos),net.minecraft.sound.SoundCategory.BLOCKS,()->Infection.expose(entity,null));
            RustingSystem.particles(world, entity.getPos());
        }
    }

    public static void tick(World world, BlockPos pos, BlockState state, CaltropsBlockEntity trap) {
        if (world.getTime() % 100 == 0) trap.nextHit.entrySet().removeIf(entry -> entry.getValue() <= world.getTime());
        if (((CaltropsBlock) state.getBlock()).isRusty()) return;
        int oldWater=trap.waterTicks,oldRain=trap.rainTicks;
        boolean convert;
        if (state.get(CaltropsBlock.WATERLOGGED)) {
            trap.rainTicks = 0;
            convert = ++trap.waterTicks >= TetanusMod.CONFIG.waterRustTicks;
        } else {
            trap.waterTicks = 0;
            if (TetanusMod.CONFIG.rainRusting && world.hasRain(pos)) convert = ++trap.rainTicks >= TetanusMod.CONFIG.rainRustTicks;
            else { trap.rainTicks = 0; convert = false; }
        }
        if ((oldWater!=trap.waterTicks||oldRain!=trap.rainTicks)
                && (world.getTime()%20==0||trap.waterTicks==0&&trap.rainTicks==0)) trap.markDirty();
        if (convert) {
            world.setBlockState(pos, (state.isOf(ModBlocks.BARBED_WIRE)?ModBlocks.RUSTED_BARBED_WIRE:ModBlocks.RUSTED_CALTROPS).getDefaultState()
                    .with(CaltropsBlock.WATERLOGGED, state.get(CaltropsBlock.WATERLOGGED)), 3);
            RustingSystem.rustFeedback(world, Vec3d.ofCenter(pos));
        }
    }

    @Override protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putInt("WaterTicks", waterTicks);
        nbt.putInt("RainTicks", rainTicks);
    }
    @Override public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        waterTicks = Math.max(0, Math.min(TetanusMod.CONFIG.waterRustTicks - 1, nbt.getInt("WaterTicks")));
        rainTicks = Math.max(0, Math.min(TetanusMod.CONFIG.rainRustTicks - 1, nbt.getInt("RainTicks")));
    }
}
