package com.tetanusmod.item;

import com.tetanusmod.effect.Infection;
import com.tetanusmod.entity.MobVariants;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.*;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.*;
import net.minecraft.util.*;
import net.minecraft.world.World;

public final class HooklineItem extends DescribedItem {
    public HooklineItem(Settings settings) { super(settings,"hookline"); }
    @Override public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack=player.getStackInHand(hand);
        var start=player.getEyePos();var direction=player.getRotationVec(1);
        var end=start.add(direction.multiply(12));
        var block=world.raycast(new net.minecraft.world.RaycastContext(start,end,net.minecraft.world.RaycastContext.ShapeType.COLLIDER,net.minecraft.world.RaycastContext.FluidHandling.NONE,player));
        double distance=block.getType()==net.minecraft.util.hit.HitResult.Type.MISS?144:start.squaredDistanceTo(block.getPos());
        var hit=ProjectileUtil.raycast(player,start,end,player.getBoundingBox().stretch(direction.multiply(12)).expand(1),e->e instanceof LivingEntity && e.isAlive() && !e.isSpectator(),distance);
        if(hit==null)return TypedActionResult.pass(stack);
        if(world instanceof ServerWorld server && hit.getEntity() instanceof LivingEntity target) {
            if(target.damage(world.getDamageSources().playerAttack(player),1)) {
                var pull=player.getPos().subtract(target.getPos()).normalize().multiply(0.8);
                target.addVelocity(pull.x,0.25,pull.z);target.velocityModified=true;
                Infection.expose(target,player);
                for(int i=0;i<24;i++){var point=start.lerp(hit.getPos(),i/24.0);server.spawnParticles(ParticleTypes.CRIT,point.x,point.y,point.z,1,0,0,0,0);}
                world.playSound(null,target.getBlockPos(),SoundEvents.BLOCK_CHAIN_HIT,SoundCategory.PLAYERS,0.8F,0.7F);
                stack.damage(1,player,p->p.sendToolBreakStatus(hand));
            }
            player.getItemCooldownManager().set(this,40);
        }
        return TypedActionResult.success(stack,world.isClient);
    }
}