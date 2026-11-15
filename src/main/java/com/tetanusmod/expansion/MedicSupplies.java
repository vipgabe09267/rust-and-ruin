package com.tetanusmod.expansion;

import com.tetanusmod.registry.ModAdvancements;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.village.VillageGossipType;

/** A medic accepts one delivery per world day. Reputation uses vanilla price calculation. */
public final class MedicSupplies {
    public interface State {
        long rustruin$lastSupplyDay();
        void rustruin$lastSupplyDay(long day);
    }
    public static long day(VillagerEntity medic) { return Math.max(0, medic.getWorld().getTime() / 24000); }
    public static ItemStack request(VillagerEntity medic) {
        return requestForDay(medic.getUuid(),day(medic));
    }
    public static ItemStack requestForDay(java.util.UUID medic,long day) {
        int index = (int)Math.floorMod(Math.floorMod(day,4L) + medic.hashCode(), 4L);
        return switch (index) {
            case 0 -> new ItemStack(Items.GLASS_BOTTLE, 12);
            case 1 -> new ItemStack(Items.HONEY_BOTTLE, 3);
            case 2 -> new ItemStack(Items.GLOW_BERRIES, 8);
            default -> new ItemStack(Items.WHITE_WOOL, 8);
        };
    }
    public static boolean deliver(VillagerEntity medic, ServerPlayerEntity player, Hand hand) {
        if (!medic.isAlive() || medic.isBaby() || medic.getVillagerData().getProfession() != MedicVillage.MEDIC
                || player.isSpectator() || player.squaredDistanceTo(medic) > 36 || medic.getCustomer() != null) return false;
        State state = (State)medic;
        long today = day(medic);
        ItemStack wanted = request(medic), held = player.getStackInHand(hand);
        if (state.rustruin$lastSupplyDay() >= today || !held.isOf(wanted.getItem()) || held.getCount() < wanted.getCount()) return false;
        // Mark before inventory/reward changes; all interactions run on the server thread.
        state.rustruin$lastSupplyDay(today);
        if (!player.getAbilities().creativeMode) {
            held.decrement(wanted.getCount());
            if (wanted.isOf(Items.HONEY_BOTTLE)) {
                ItemStack bottles = new ItemStack(Items.GLASS_BOTTLE, wanted.getCount());
                if (!player.getInventory().insertStack(bottles)) player.dropItem(bottles, false);
            }
        }
        medic.getGossip().startGossip(player.getUuid(), VillageGossipType.MINOR_POSITIVE, 30);
        ModAdvancements.award(player, "supply_run", "deliver_supplies");
        player.sendMessage(Text.translatable("message.tetanusmod.supplies_delivered"), false);
        return true;
    }
    public static Text requestInstructions(VillagerEntity medic,ItemStack held){
        ItemStack wanted=request(medic);
        String key=((State)medic).rustruin$lastSupplyDay()>=day(medic)?"message.tetanusmod.supplies_done":"message.tetanusmod.supply_request";
        return Text.translatable(key,wanted.getCount(),wanted.getName(),held.isOf(wanted.getItem())?held.getCount():0);
    }
    public static void initialize() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (!player.isSneaking() || player.isSpectator() || !(entity instanceof VillagerEntity medic)
                    || medic.isBaby() || medic.getVillagerData().getProfession() != MedicVillage.MEDIC) return ActionResult.PASS;
            if (world.isClient) return ActionResult.SUCCESS;
            if (player instanceof ServerPlayerEntity serverPlayer && !deliver(medic, serverPlayer, hand)) {
                player.sendMessage(requestInstructions(medic,player.getStackInHand(hand)), false);
            }
            return ActionResult.CONSUME;
        });
    }
}