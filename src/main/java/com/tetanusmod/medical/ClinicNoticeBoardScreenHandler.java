package com.tetanusmod.medical;

import com.tetanusmod.expansion.MedicCases;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.Property;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public final class ClinicNoticeBoardScreenHandler extends ScreenHandler {
    public static final List<MedicCases.CaseFile> CASES = List.of(MedicCases.CaseFile.values());
    public static final int AVAILABLE = -1;
    public static final int COMPLETED_TODAY = -2;
    private final PlayerEntity owner;
    private final ScreenHandlerContext location;
    private final Property selection;
    private int clientSelection = AVAILABLE;

    public ClinicNoticeBoardScreenHandler(int syncId, PlayerInventory inventory) {
        this(syncId, inventory, ScreenHandlerContext.EMPTY);
    }

    ClinicNoticeBoardScreenHandler(int syncId, PlayerInventory inventory, ScreenHandlerContext location) {
        super(MedicalWorkstations.BOARD_SCREEN, syncId);
        owner = inventory.player;
        this.location = location;
        selection = addProperty(new Property() {
            @Override
            public int get() {
                if (!(owner instanceof ServerPlayerEntity player)) return clientSelection;
                var active = MedicCases.progress(player).activeCase();
                if (active != null) return CASES.indexOf(active);
                return MedicCases.completedToday(player) ? COMPLETED_TODAY : AVAILABLE;
            }

            @Override
            public void set(int value) {
                clientSelection = value >= COMPLETED_TODAY && value < CASES.size() ? value : COMPLETED_TODAY;
            }
        });
    }

    public int selection() { return selection.get(); }

    @Override
    public boolean canUse(PlayerEntity player) {
        return player == owner && player.isAlive() && !player.isSpectator()
                && location.get((world, pos) -> player.getWorld() == world
                        && world.getBlockState(pos).isOf(MedicalWorkstations.BOARD)
                        && player.squaredDistanceTo(Vec3d.ofCenter(pos)) <= 64, player.getWorld().isClient);
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        return player instanceof ServerPlayerEntity server && player.currentScreenHandler == this
                && id >= 0 && id < CASES.size() && canUse(player) && MedicCases.accept(server, CASES.get(id));
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) { return ItemStack.EMPTY; }
}
