package com.tetanusmod.client;

import com.tetanusmod.medical.ClinicNoticeBoardScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public final class ClinicNoticeBoardScreen extends HandledScreen<ClinicNoticeBoardScreenHandler> {
    private static final String KEY = "gui.tetanusmod.notice_board.";
    private static final int INK = 0xFF35281D;
    private final List<ButtonWidget> choices = new ArrayList<>();

    public ClinicNoticeBoardScreen(ClinicNoticeBoardScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        backgroundWidth = 280;
        backgroundHeight = 136;
    }

    @Override
    protected void init() {
        super.init();
        choices.clear();
        int villageRow = 0;
        int fieldRow = 0;
        for (int index = 0; index < ClinicNoticeBoardScreenHandler.CASES.size(); index++) {
            var file = ClinicNoticeBoardScreenHandler.CASES.get(index);
            int id = index;
            int row = file.isField() ? fieldRow++ : villageRow++;
            int left = file.isField() ? 146 : 12;
            var button = ButtonWidget.builder(file.title(), clicked -> {
                if (client != null && client.interactionManager != null) client.interactionManager.clickButton(handler.syncId, id);
            }).dimensions(x + left, y + 41 + row * 23, 122, 20).tooltip(Tooltip.of(file.preview())).build();
            choices.add(addDrawableChild(button));
        }
        updateChoices();
    }

    @Override
    protected void handledScreenTick() {
        super.handledScreenTick();
        updateChoices();
    }

    private void updateChoices() {
        boolean available = handler.selection() == ClinicNoticeBoardScreenHandler.AVAILABLE;
        choices.forEach(button -> button.active = available);
    }

    @Override
    protected void drawBackground(DrawContext draw, float delta, int mouseX, int mouseY) {
        draw.fill(x, y, x + backgroundWidth, y + backgroundHeight, 0xFFDFCCAA);
        draw.drawBorder(x, y, backgroundWidth, backgroundHeight, 0xFF5C3C2D);
        draw.fill(x + 1, y + 1, x + backgroundWidth - 1, y + 24, 0xFF334B43);
        draw.fill(x + 138, y + 28, x + 142, y + 108, 0xFFB99771);
        draw.fill(x + 1, y + 113, x + backgroundWidth - 1, y + backgroundHeight - 1, 0xFFEFE4D0);
    }

    @Override
    protected void drawForeground(DrawContext draw, int mouseX, int mouseY) {
        draw.drawText(textRenderer, title, 12, 8, 0xFFFAF3E3, false);
        draw.drawText(textRenderer, Text.translatable(KEY + "village"), 12, 29, INK, false);
        draw.drawText(textRenderer, Text.translatable(KEY + "field"), 146, 29, INK, false);
        int selection = handler.selection();
        Text status = switch (selection) {
            case ClinicNoticeBoardScreenHandler.AVAILABLE -> Text.translatable(KEY + "select");
            case ClinicNoticeBoardScreenHandler.COMPLETED_TODAY -> Text.translatable(KEY + "completed");
            default -> Text.translatable(KEY + "active", ClinicNoticeBoardScreenHandler.CASES.get(selection).title());
        };
        draw.drawText(textRenderer, status, 12, 121, INK, false);
    }

    @Override
    public void render(DrawContext draw, int mouseX, int mouseY, float delta) {
        renderBackground(draw);
        super.render(draw, mouseX, mouseY, delta);
        drawMouseoverTooltip(draw, mouseX, mouseY);
    }
}
