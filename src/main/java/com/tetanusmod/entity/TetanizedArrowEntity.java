package com.tetanusmod.entity;
import com.tetanusmod.TetanusMod;
import com.tetanusmod.effect.Infection;
import com.tetanusmod.registry.ModItems;
import net.minecraft.entity.*;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;
public final class TetanizedArrowEntity extends PersistentProjectileEntity {
    public TetanizedArrowEntity(EntityType<? extends TetanizedArrowEntity> type, World world) { super(type,world); }
    public TetanizedArrowEntity(World world, LivingEntity owner) { super(TetanusMod.TETANIZED_ARROW_ENTITY,owner,world); }
    @Override protected ItemStack asItemStack() { return new ItemStack(ModItems.TETANIZED_ARROW); }
    @Override protected void onHit(LivingEntity target) {
        super.onHit(target);

    }
    @Override protected void onEntityHit(EntityHitResult hit) {
        if(!getWorld().isClient) {
            Infection.silently(()->hitTarget(hit));
            Infection.playAt(getWorld(),hit.getPos(),net.minecraft.sound.SoundCategory.PLAYERS);
        }else hitTarget(hit);
    }
    private void hitTarget(EntityHitResult hit) {
        // Vanilla endermen reject projectile damage before the infection callback can run.
        if (!getWorld().isClient && hit.getEntity() instanceof EndermanEntity enderman) {
            if (enderman.damage(new net.minecraft.entity.damage.DamageSource(getWorld().getRegistryManager().get(net.minecraft.registry.RegistryKeys.DAMAGE_TYPE).entryOf(net.minecraft.entity.damage.DamageTypes.MAGIC),this,getOwner()), (float)Math.ceil(getVelocity().length()*getDamage()))) onHit(enderman);
            discard();
        } else super.onEntityHit(hit);
    }
    @Override protected void onBlockHit(net.minecraft.util.hit.BlockHitResult hit) {
        super.onBlockHit(hit);
        if(!getWorld().isClient)Infection.playAt(getWorld(),hit.getPos(),net.minecraft.sound.SoundCategory.PLAYERS);
    }
}
