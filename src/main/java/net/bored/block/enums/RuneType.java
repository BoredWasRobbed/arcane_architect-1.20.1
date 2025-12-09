package net.bored.block.enums;

import net.minecraft.util.StringIdentifiable;

public enum RuneType implements StringIdentifiable {
    AETHER("aether", 0.0f, 1.0f, 1.0f),  // Cyan
    AIR("air", 0.8f, 1.0f, 1.0f),        // Pale Blue/White
    FIRE("fire", 1.0f, 0.3f, 0.0f),      // Orange/Red
    WATER("water", 0.2f, 0.4f, 1.0f),    // Deep Blue
    EARTH("earth", 0.4f, 1.0f, 0.4f),    // Green
    AMPLIFY("amplify", 0.8f, 0.0f, 1.0f),// Purple
    CHANNEL("channel", 1.0f, 1.0f, 0.0f),// Yellow
    WALL("wall", 0.5f, 0.5f, 0.5f),      // Gray
    ITEM("item", 1.0f, 0.84f, 0.0f),     // Gold
    VENT("vent", 0.8f, 0.9f, 0.9f);      // Steam/Light Gray

    private final String name;
    public final float r;
    public final float g;
    public final float b;

    RuneType(String name, float r, float g, float b) {
        this.name = name;
        this.r = r;
        this.g = g;
        this.b = b;
    }

    @Override
    public String asString() {
        return this.name;
    }
}