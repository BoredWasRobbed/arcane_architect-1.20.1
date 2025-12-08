package net.bored.client.gui;

import net.bored.block.enums.RuneType;
import net.bored.networking.ModMessages;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public class RuneSelectionScreen extends Screen {
    private final BlockPos runePos;

    public RuneSelectionScreen(BlockPos pos) {
        super(Text.literal("Select Rune Type"));
        this.runePos = pos;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int buttonWidth = 80;
        int buttonHeight = 20;
        int padding = 5;

        int yOffset = -50;

        // Add a button for each rune type
        for (RuneType type : RuneType.values()) {
            this.addDrawableChild(ButtonWidget.builder(Text.literal(type.name()), button -> {
                        sendChangePacket(type);
                        this.close(); // Close screen after selection
                    })
                    .dimensions(centerX - (buttonWidth / 2), centerY + yOffset, buttonWidth, buttonHeight)
                    .build());

            yOffset += buttonHeight + padding;
        }
    }

    private void sendChangePacket(RuneType type) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(this.runePos);
        buf.writeEnumConstant(type);
        ClientPlayNetworking.send(ModMessages.CHANGE_RUNE_TYPE_ID, buf);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, this.height / 2 - 70, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }
}