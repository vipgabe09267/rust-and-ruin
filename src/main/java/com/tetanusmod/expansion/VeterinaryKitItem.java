package com.tetanusmod.expansion;

import com.tetanusmod.entity.MobVariants;
import com.tetanusmod.registry.ModAdvancements;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

/** A single-use treatment, deliberately limited to mutated animals. */
public final class VeterinaryKitItem extends Item {
    public VeterinaryKitItem(Settings settings) { super(settings); }

    @Override
    public void appendTooltip(ItemStack stack, net.minecraft.world.World world, java.util.List<Text> tooltip, net.minecraft.client.item.TooltipContext context) {
        tooltip.add(Text.translatable("tooltip.tetanusmod.veterinary_kit").formatted(net.minecraft.util.Formatting.GRAY));
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity target, Hand hand) {
        if (stack.isEmpty() || !stack.isOf(this)) return ActionResult.PASS;
        if (!(target instanceof MobEntity mob) || !target.isAlive() || !MobVariants.isAnimal(target)
                || (!MobVariants.permanent(target) && !Rabies.rabid(target))) return ActionResult.PASS;
        if (user.isSpectator() || user.squaredDistanceTo(target) > 36) return ActionResult.FAIL;
        if (!user.getWorld().isClient) {
            boolean cured = MobVariants.cureAnimal(target) || Rabies.cureAnimal(mob);
            if (!cured) return ActionResult.FAIL;
            if (target.getWorld() instanceof net.minecraft.server.world.ServerWorld world) {
                world.spawnParticles(net.minecraft.particle.ParticleTypes.HAPPY_VILLAGER,target.getX(),target.getBodyY(0.6),target.getZ(),14,0.35,0.35,0.35,0.02);
                world.playSound(null,target.getBlockPos(),net.minecraft.sound.SoundEvents.ENTITY_ZOMBIE_VILLAGER_CURE,net.minecraft.sound.SoundCategory.NEUTRAL,0.7F,1.25F);
            }
            if (!user.getAbilities().creativeMode) stack.decrement(1);
            if (user instanceof ServerPlayerEntity player) {
                ModAdvancements.award(player,"clean_bill_of_health","treat_animal");
                MedicCases.recordWork(player, MedicCases.Work.VETERINARY);
            }
            user.sendMessage(Text.translatable("message.tetanusmod.animal_treated"), true);
        }
        return ActionResult.success(user.getWorld().isClient);
    }
}
