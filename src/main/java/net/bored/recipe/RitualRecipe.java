package net.bored.recipe;

import com.google.gson.JsonObject;
import net.bored.block.enums.RuneType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.Map;

public class RitualRecipe {
    private final Identifier id;
    private final Map<BlockPos, RuneType> pattern;
    private final float minPressure;
    private final float maxPressure;
    private final String effectId;
    private final JsonObject effectData;
    private final boolean continuous;
    private final String requiredItem;
    private final boolean consumeItem;

    // New Option
    private final boolean requireAllItems;

    public RitualRecipe(Identifier id, Map<BlockPos, RuneType> pattern, float minPressure, float maxPressure, String effectId, JsonObject effectData, boolean continuous, String requiredItem, boolean consumeItem, boolean requireAllItems) {
        this.id = id;
        this.pattern = pattern;
        this.minPressure = minPressure;
        this.maxPressure = maxPressure;
        this.effectId = effectId;
        this.effectData = effectData;
        this.continuous = continuous;
        this.requiredItem = requiredItem;
        this.consumeItem = consumeItem;
        this.requireAllItems = requireAllItems;
    }

    public Identifier getId() { return id; }
    public Map<BlockPos, RuneType> getPattern() { return pattern; }
    public float getMinPressure() { return minPressure; }
    public float getMaxPressure() { return maxPressure; }
    public String getEffectId() { return effectId; }
    public JsonObject getEffectData() { return effectData; }
    public boolean isContinuous() { return continuous; }
    public String getRequiredItem() { return requiredItem; }
    public boolean consumesItem() { return consumeItem; }
    public boolean requiresAllItems() { return requireAllItems; }
}