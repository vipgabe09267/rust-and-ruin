package com.tetanusmod.expansion;

import com.tetanusmod.effect.Infection;
import com.tetanusmod.registry.*;
import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.*;
import net.minecraft.util.*;
import net.minecraft.world.World;

public final class SpearItem extends SwordItem {
    private final boolean rusty;
    public SpearItem(boolean rusty,Settings settings){super(ToolMaterials.IRON,4,-2.9F,settings);this.rusty=rusty;}
    public boolean rusty(){return rusty;}
    @Override public TypedActionResult<ItemStack> use(World world,PlayerEntity player,Hand hand){
        var stack=player.getStackInHand(hand);if(player.isSpectator()||!player.isAlive()||player.getItemCooldownManager().isCoolingDown(this))return TypedActionResult.pass(stack);
        var start=player.getEyePos();var direction=player.getRotationVec(1);var end=start.add(direction.multiply(4.5));
        var block=world.raycast(new net.minecraft.world.RaycastContext(start,end,net.minecraft.world.RaycastContext.ShapeType.COLLIDER,net.minecraft.world.RaycastContext.FluidHandling.NONE,player));
        double limit=block.getType()==net.minecraft.util.hit.HitResult.Type.MISS?20.25:start.squaredDistanceTo(block.getPos());
        var hit=ProjectileUtil.raycast(player,start,end,player.getBoundingBox().stretch(direction.multiply(4.5)).expand(1),e->e instanceof LivingEntity&&e.isAlive()&&!e.isSpectator(),limit);
        if(hit==null)return TypedActionResult.pass(stack);
        if(!world.isClient&&hit.getEntity() instanceof LivingEntity target){
            if(Infection.withoutMeleeExposure(()->target.damage(world.getDamageSources().playerAttack(player),7))){
                int enchant=EnchantmentHelper.getLevel(ModItems.TETANUS_ENCHANTMENT,stack);
                if(rusty||enchant>0)CombatInfection.expose(target,player,enchant,false);
                stack.damage(1,player,p->p.sendToolBreakStatus(hand));
                if(player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer)ModAdvancements.award(serverPlayer,"spear","use_spear");
            }
            player.getItemCooldownManager().set(this,22);
        }
        return TypedActionResult.success(stack,world.isClient);
    }
}