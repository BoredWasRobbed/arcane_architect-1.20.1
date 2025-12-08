package net.bored.networking;

import net.bored.ArcaneArchitect;
import net.bored.networking.packet.ChangeRuneTypeC2SPacket;
import net.bored.networking.packet.CreateRuneC2SPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.util.Identifier;

public class ModMessages {
    public static final Identifier CREATE_RUNE_ID = new Identifier(ArcaneArchitect.MOD_ID, "create_rune");
    public static final Identifier CHANGE_RUNE_TYPE_ID = new Identifier(ArcaneArchitect.MOD_ID, "change_rune_type");

    public static void registerC2SPackets() {
        ServerPlayNetworking.registerGlobalReceiver(CREATE_RUNE_ID, CreateRuneC2SPacket::receive);
        ServerPlayNetworking.registerGlobalReceiver(CHANGE_RUNE_TYPE_ID, ChangeRuneTypeC2SPacket::receive);
    }

    public static void registerS2CPackets() {
        // Register server-to-client packets here if needed later
    }
}