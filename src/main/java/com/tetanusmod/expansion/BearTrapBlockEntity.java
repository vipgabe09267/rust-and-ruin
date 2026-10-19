package com.tetanusmod.expansion;

import com.tetanusmod.TetanusMod;
import com.tetanusmod.effect.Infection;
import com.tetanusmod.rust.RustingSystem;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import java.util.UUID;

public final class BearTrapBlockEntity extends BlockEntity {
    private UUID captured;private int water,rain;
    public BearTrapBlockEntity(BlockPos pos,BlockState state){super(ExpansionBlocks.BEAR_TRAP_ENTITY,pos,state);}
    public boolean trigger(LivingEntity target){
        if(world==null||world.isClient||getCachedState().get(BearTrapBlock.CLOSED)||!target.isAlive()||target.isSpectator())return false;
        captured=target.getUuid();world.setBlockState(pos,getCachedState().with(BearTrapBlock.CLOSED,true),3);markDirty();
        target.damage(ModDamage.source(target,ModDamage.BEAR_TRAP),3);
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,100,9));
        if(((BearTrapBlock)getCachedState().getBlock()).rusty())Infection.at(world,Vec3d.ofCenter(pos),net.minecraft.sound.SoundCategory.BLOCKS,()->Infection.expose(target,null));
        world.playSound(null,pos,net.minecraft.sound.SoundEvents.BLOCK_CHAIN_PLACE,net.minecraft.sound.SoundCategory.BLOCKS,1,0.6F);return true;
    }
    public boolean reset(){
        if(world==null||world.isClient||!getCachedState().get(BearTrapBlock.CLOSED))return false;
        if(captured!=null&&world instanceof ServerWorld server&&server.getEntity(captured) instanceof LivingEntity target){var effect=target.getStatusEffect(StatusEffects.SLOWNESS);if(effect!=null&&effect.getAmplifier()==9)target.removeStatusEffect(StatusEffects.SLOWNESS);}
        captured=null;world.setBlockState(pos,getCachedState().with(BearTrapBlock.CLOSED,false),3);markDirty();return true;
    }
    public static void tick(World world,BlockPos pos,BlockState state,BearTrapBlockEntity trap){
        if(state.get(BearTrapBlock.CLOSED)&&trap.captured!=null&&world.getTime()%20==0&&world instanceof ServerWorld server&&server.getEntity(trap.captured) instanceof LivingEntity target&&target.isAlive()&&target.getBoundingBox().intersects(new Box(pos).expand(0.35)))target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,100,9));
        if(((BearTrapBlock)state.getBlock()).rusty())return;
        boolean wet=world.getFluidState(pos).isIn(net.minecraft.registry.tag.FluidTags.WATER)||world.getFluidState(pos.up()).isIn(net.minecraft.registry.tag.FluidTags.WATER);
        if(wet){trap.rain=0;trap.water++;}else{trap.water=0;trap.rain=TetanusMod.CONFIG.rainRusting&&world.hasRain(pos)?trap.rain+1:0;}
        if(trap.water>=TetanusMod.CONFIG.waterRustTicks||trap.rain>=TetanusMod.CONFIG.rainRustTicks){NbtCompound saved=trap.createNbt();world.setBlockState(pos,ExpansionBlocks.RUSTED_BEAR_TRAP.getDefaultState().with(BearTrapBlock.CLOSED,state.get(BearTrapBlock.CLOSED)),3);if(world.getBlockEntity(pos) instanceof BearTrapBlockEntity rusty){rusty.readNbt(saved);rusty.markDirty();}RustingSystem.rustFeedback(world,Vec3d.ofCenter(pos));}
        if(world.getTime()%20==0)trap.markDirty();
    }
    @Override protected void writeNbt(NbtCompound nbt){super.writeNbt(nbt);if(captured!=null)nbt.putUuid("Captured",captured);nbt.putInt("Water",water);nbt.putInt("Rain",rain);}
    @Override public void readNbt(NbtCompound nbt){super.readNbt(nbt);captured=nbt.containsUuid("Captured")?nbt.getUuid("Captured"):null;water=Math.max(0,Math.min(72000,nbt.getInt("Water")));rain=Math.max(0,Math.min(72000,nbt.getInt("Rain")));}
}