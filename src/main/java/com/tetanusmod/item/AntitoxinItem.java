package com.tetanusmod.item;

import com.tetanusmod.TetanusMod;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import java.util.List;

public final class AntitoxinItem extends Item {
    public AntitoxinItem(Settings settings) { super(settings); }
    @Override public int getMaxUseTime(ItemStack stack) { return 24; }
    @Override public UseAction getUseAction(ItemStack stack) { return UseAction.DRINK; }
    @Override public SoundEvent getDrinkSound() { return SoundEvents.ENTITY_GENERIC_DRINK; }
    @Override public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if(user.isSpectator())return TypedActionResult.pass(user.getStackInHand(hand));
        return ItemUsage.consumeHeldItem(world, user, hand);
    }
    @Override public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if(world.isClient || stack.isEmpty() || !stack.isOf(this) || !user.isAlive() || user.isSpectator())return stack;
        MedicineConsumption.record(user,stack);
        {
            var state=(com.tetanusmod.effect.InfectionState)user;
            boolean ill=user.hasStatusEffect(TetanusMod.TETANUS)||user.hasStatusEffect(TetanusMod.LOCKJAW)||state.tetanus$getLockjaw()>0;
            user.removeStatusEffect(TetanusMod.TETANUS);
            user.removeStatusEffect(TetanusMod.LOCKJAW);
            user.removeStatusEffect(com.tetanusmod.expansion.ExpansionEffects.SPLINTED);
            if(ill)user.removeStatusEffect(com.tetanusmod.expansion.ExpansionEffects.DISORIENTED);
            state.tetanus$setInfection(0,0);state.tetanus$setLockjaw(0);state.tetanus$setLockjawWindow(0);state.tetanus$setFreeze(0);
            // Award the completed cure, including eating-blocked Lockjaw before its visible effect refresh.
            if(ill&&!user.hasStatusEffect(TetanusMod.TETANUS)&&!user.hasStatusEffect(TetanusMod.LOCKJAW)
                    &&state.tetanus$getLockjaw()==0&&user instanceof net.minecraft.server.network.ServerPlayerEntity player)
                com.tetanusmod.registry.ModAdvancements.award(player,"antitoxin","cure_tetanus");
            ((ServerWorld) world).spawnParticles(ParticleTypes.HAPPY_VILLAGER, user.getX(), user.getBodyY(0.6),
                    user.getZ(), 10, 0.3, 0.4, 0.3, 0.05);
        }
        if (user instanceof PlayerEntity player && player.getAbilities().creativeMode) return stack;
        stack.decrement(1);
        ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
        if (stack.isEmpty()) return bottle;
        if (user instanceof PlayerEntity player) {
            if(!player.getInventory().insertStack(bottle))player.dropItem(bottle,false);
        } else user.dropStack(bottle);
        return stack;
    }
    @Override public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("tooltip.tetanusmod.antitoxin").formatted(Formatting.GREEN));
    }
}
