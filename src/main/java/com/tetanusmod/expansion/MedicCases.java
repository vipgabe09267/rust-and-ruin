package com.tetanusmod.expansion;

import com.tetanusmod.registry.ModAdvancements;
import com.tetanusmod.registry.ModItems;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.village.VillageGossipType;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static net.minecraft.server.command.CommandManager.literal;

/** Personal case files. Deliveries consume supplies and fieldwork advances on a successful action. */
public final class MedicCases {
    private static final String TEXT = "message.tetanusmod.medic_case.";
    private static final String CASE_TAG = "RustRuinMedicCase";
    private static final int STEPS = 2;

    public interface State {
        Progress rustruin$medicCase();
    }

    public enum Work {
        BRUSH, VETERINARY, CAVE_PATROL
    }

    public record Step(@Nullable Item delivery, @Nullable Work work, int amount) {
        Step(Item item, int amount) { this(item, null, amount); }
        Step(Work work, int amount) { this(null, work, amount); }
    }

    public enum CaseFile {
        CLINIC_RESTOCK(false, new Step(Items.GLASS_BOTTLE, 6), new Step(Items.HONEY_BOTTLE, 2),
                Items.HONEY_BOTTLE, 1, 3),
        WORKSHOP_INSPECTION(false, new Step(Items.LEATHER, 4), new Step(Work.BRUSH, 1),
                ModItems.PROTECTIVE_GLOVES, 1, 2),
        HERD_CHECKUP(false, new Step(Items.WHEAT, 8), new Step(Work.VETERINARY, 1),
                Items.GOLDEN_CARROT, 4, 5),
        CAMP_RATIONS(true, new Step(Items.GLOW_BERRIES, 6), new Step(Items.BAKED_POTATO, 6),
                Items.LANTERN, 2, 3),
        SALVAGE_INSPECTION(true, new Step(ModItems.RUST, 6), new Step(Work.BRUSH, 1),
                ExpansionItems.TRAP_RESET_TOOL, 1, 2),
        CAVE_PATROL(true, new Step(Items.TORCH, 12), new Step(Work.CAVE_PATROL, 2),
                Items.GOLDEN_CARROT, 3, 4);

        private final boolean field;
        private final List<Step> steps;
        private final Item reward;
        private final int rewardCount;
        private final int emeralds;

        CaseFile(boolean field, Step first, Step second, Item reward, int rewardCount, int emeralds) {
            this.field = field;
            this.steps = List.of(first, second);
            this.reward = reward;
            this.rewardCount = rewardCount;
            this.emeralds = emeralds;
        }

        private String id() { return name().toLowerCase(Locale.ROOT); }
        public Text title() { return Text.translatable(TEXT + id()); }
        public boolean isField() { return field; }
        public Text preview() {
            return Text.empty().append(describe(steps.get(0))).append("\n").append(describe(steps.get(1)));
        }

        private static Text describe(Step step) {
            return step.delivery() != null
                    ? Text.translatable(TEXT + "delivery_amount", step.amount(), step.delivery().getName())
                    : Text.translatable(TEXT + "work." + step.work().name().toLowerCase(Locale.ROOT), step.amount());
        }
    }

    private static final List<CaseFile> CLINIC_CASES =
            List.of(CaseFile.CLINIC_RESTOCK, CaseFile.WORKSHOP_INSPECTION, CaseFile.HERD_CHECKUP);
    private static final List<CaseFile> FIELD_CASES =
            List.of(CaseFile.CAMP_RATIONS, CaseFile.SALVAGE_INSPECTION, CaseFile.CAVE_PATROL);

    public static final class Progress {
        @Nullable private CaseFile active;
        private int step;
        private int count;
        private long completedDay = -1;

        @Nullable public CaseFile activeCase() { return active; }
        @Nullable public Step currentStep() { return active == null || step == STEPS ? null : active.steps.get(step); }
        public int remaining() {
            Step goal = currentStep();
            return goal == null ? 0 : goal.amount() - count;
        }

        private void clear() {
            active = null;
            step = 0;
            count = 0;
        }

        private void advance(int amount) {
            count += amount;
            if (remaining() == 0) {
                step++;
                count = 0;
            }
        }

        public void write(NbtCompound playerData) {
            if (active == null && completedDay < 0) return;
            var data = new NbtCompound();
            data.putLong("CompletedDay", completedDay);
            if (active != null) {
                data.putString("File", active.id());
                data.putInt("Step", step);
                data.putInt("Count", count);
            }
            playerData.put(CASE_TAG, data);
        }

        public void read(NbtCompound playerData) {
            clear();
            var data = playerData.getCompound(CASE_TAG);
            completedDay = data.contains("CompletedDay", NbtElement.LONG_TYPE) ? Math.max(-1, data.getLong("CompletedDay")) : -1;
            String id = data.getString("File");
            int savedStep = data.getInt("Step");
            int savedCount = data.getInt("Count");
            if (id.isEmpty() || savedStep < 0 || savedStep > STEPS || savedCount < 0) return;
            for (var file : CaseFile.values()) {
                if (!file.id().equals(id)) continue;
                if (savedStep == STEPS && (savedCount != 0 || file.steps.get(STEPS - 1).work() == null)) return;
                if (savedStep < STEPS && savedCount >= file.steps.get(savedStep).amount()) return;
                active = file;
                step = savedStep;
                count = savedCount;
                return;
            }
        }
    }

    private MedicCases() {}

    public static Progress progress(ServerPlayerEntity player) {
        return ((State) player).rustruin$medicCase();
    }

    public static boolean completedToday(ServerPlayerEntity player) {
        return progress(player).completedDay >= day(player);
    }

    public static boolean accept(ServerPlayerEntity player, CaseFile file) {
        var progress = progress(player);
        if (player.isSpectator() || !player.isAlive() || progress.active != null || completedToday(player)) return false;
        progress.active = file;
        player.sendMessage(instructions(progress), false);
        return true;
    }

    static long day(ServerPlayerEntity player) {
        return player.getServer().getOverworld().getTime() / 24_000L;
    }

    public static CaseFile offerFor(boolean field, UUID player, long day) {
        var pool = field ? FIELD_CASES : CLINIC_CASES;
        return pool.get(Math.floorMod(day + player.hashCode(), pool.size()));
    }

    private static Text medicRole(boolean field) {
        return Text.translatable(field ? "gui.tetanusmod.field_medic" : "gui.tetanusmod.medic_short");
    }

    private static Text instructions(Progress progress) {
        CaseFile file = progress.active;
        if (file == null) return Text.translatable(TEXT + "none");
        Step goal = progress.currentStep();
        if (goal == null) return Text.translatable(TEXT + "report", file.title(), medicRole(file.field));
        Text objective = goal.delivery() != null
                ? Text.translatable(TEXT + "deliver", progress.remaining(), goal.delivery().getName())
                : Text.translatable(TEXT + "work." + goal.work().name().toLowerCase(Locale.ROOT), progress.remaining());
        return Text.translatable(TEXT + "objective", file.title(), progress.step + 1, STEPS, objective);
    }

    private static void visit(VillagerEntity medic, ServerPlayerEntity player) {
        if (!medic.isAlive() || !player.isAlive() || player.getWorld() != medic.getWorld()
                || player.squaredDistanceTo(medic) > 36 || medic.getCustomer() != null) return;

        Progress progress = progress(player);
        boolean field = MedicTradeOffers.isFieldMedic(medic);
        long today = day(player);
        if (progress.active == null) {
            if (!accept(player, offerFor(field, player.getUuid(), today))) {
                player.sendMessage(Text.translatable(TEXT + "done"), false);
            }
            return;
        }
        if (progress.active.field != field) {
            player.sendMessage(Text.translatable(TEXT + "wrong_medic", progress.active.title(), medicRole(progress.active.field)), false);
            return;
        } else {
            Step goal = progress.currentStep();
            if (goal == null) {
                finish(medic, player, progress, today);
                return;
            }
            ItemStack held = player.getMainHandStack();
            if (goal.delivery() != null && held.isOf(goal.delivery())) {
                int delivered = Math.min(held.getCount(), progress.remaining());
                if (!player.getAbilities().creativeMode) {
                    held.decrement(delivered);
                    if (goal.delivery() == Items.HONEY_BOTTLE) give(player, new ItemStack(Items.GLASS_BOTTLE, delivered));
                }
                progress.advance(delivered);
                if (progress.currentStep() == null) {
                    finish(medic, player, progress, today);
                    return;
                }
            }
        }
        player.sendMessage(instructions(progress), false);
    }

    public static void recordWork(ServerPlayerEntity player, Work work) {
        if (player.isSpectator()) return;
        Progress progress = progress(player);
        Step goal = progress.currentStep();
        if (goal == null || goal.work() != work) return;
        progress.advance(1);
        player.sendMessage(instructions(progress), false);
    }

    private static void finish(VillagerEntity medic, ServerPlayerEntity player, Progress progress, long today) {
        CaseFile file = progress.active;
        progress.completedDay = today;
        progress.clear();
        give(player, new ItemStack(Items.EMERALD, file.emeralds));
        give(player, new ItemStack(file.reward, file.rewardCount));
        medic.getGossip().startGossip(player.getUuid(), VillageGossipType.MINOR_POSITIVE, 30);
        ModAdvancements.award(player, "supply_run", "deliver_supplies");
        player.sendMessage(Text.translatable(TEXT + "complete", file.title()), false);
    }

    private static void give(ServerPlayerEntity player, ItemStack stack) {
        player.getInventory().insertStack(stack);
        if (!stack.isEmpty()) player.dropItem(stack, false);
    }

    public static void initialize() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (!player.isSneaking() || player.isSpectator() || !(entity instanceof VillagerEntity medic)
                    || medic.isBaby() || medic.getVillagerData().getProfession() != MedicVillage.MEDIC) return ActionResult.PASS;
            if (hand != Hand.MAIN_HAND) return ActionResult.CONSUME;
            if (world.isClient) return ActionResult.SUCCESS;
            if (player instanceof ServerPlayerEntity serverPlayer) visit(medic, serverPlayer);
            return ActionResult.CONSUME;
        });
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, killer, killed) -> {
            if (killer instanceof ServerPlayerEntity player && killed instanceof Monster
                    && world.getRegistryKey() == World.OVERWORLD && killed.getY() < 32) recordWork(player, Work.CAVE_PATROL);
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) -> dispatcher.register(literal("mediccase")
                .executes(context -> {
                    var player = context.getSource().getPlayerOrThrow();
                    player.sendMessage(instructions(progress(player)), false);
                    return 1;
                })
                .then(literal("abandon").executes(context -> {
                    var player = context.getSource().getPlayerOrThrow();
                    Progress progress = progress(player);
                    boolean hadCase = progress.active != null;
                    progress.clear();
                    player.sendMessage(Text.translatable(TEXT + (hadCase ? "abandoned" : "none")), false);
                    return hadCase ? 1 : 0;
                }))));
    }
}
