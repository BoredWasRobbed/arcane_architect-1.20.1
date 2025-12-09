package net.bored.ritual;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.bored.block.enums.RuneType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RitualRecipe {
    private final Identifier id;
    private final Map<BlockPos, RuneType> pattern;
    private final Map<BlockPos, String> itemRequirements;
    private final float minPressure;
    private final float maxPressure;
    private final String effectId;
    private final JsonObject effectData;
    private final boolean continuous;
    private final String requiredItem;
    private final boolean consumeItem;

    // Options
    private final boolean requireAllItems;
    private final boolean clearItems;
    private final boolean shapeless;
    private final boolean affectsOwner;

    // Timing
    private final int startupTime;
    private final int interval;

    // Thermodynamics
    private final float pressureCost;
    private final float instabilityCost;

    // Augments
    private final List<RitualAugment> augments;

    public RitualRecipe(Identifier id, Map<BlockPos, RuneType> pattern, Map<BlockPos, String> itemRequirements, float minPressure, float maxPressure, String effectId, JsonObject effectData, boolean continuous, String requiredItem, boolean consumeItem, boolean requireAllItems, boolean clearItems, boolean shapeless, int startupTime, int interval, boolean affectsOwner, float pressureCost, float instabilityCost, List<RitualAugment> augments) {
        this.id = id;
        this.pattern = pattern;
        this.itemRequirements = itemRequirements;
        this.minPressure = minPressure;
        this.maxPressure = maxPressure;
        this.effectId = effectId;
        this.effectData = effectData;
        this.continuous = continuous;
        this.requiredItem = requiredItem;
        this.consumeItem = consumeItem;
        this.requireAllItems = requireAllItems;
        this.clearItems = clearItems;
        this.shapeless = shapeless;
        this.startupTime = startupTime;
        this.interval = interval;
        this.affectsOwner = affectsOwner;
        this.pressureCost = pressureCost;
        this.instabilityCost = instabilityCost;
        this.augments = augments != null ? augments : new ArrayList<>();
    }

    // Copy Constructor for modifications
    private RitualRecipe(RitualRecipe original, JsonObject newEffectData, boolean newAffectsOwner) {
        this.id = original.id;
        this.pattern = original.pattern;
        this.itemRequirements = original.itemRequirements;
        this.minPressure = original.minPressure;
        this.maxPressure = original.maxPressure;
        this.effectId = original.effectId;
        this.effectData = newEffectData; // Modified
        this.continuous = original.continuous;
        this.requiredItem = original.requiredItem;
        this.consumeItem = original.consumeItem;
        this.requireAllItems = original.requireAllItems;
        this.clearItems = original.clearItems;
        this.shapeless = original.shapeless;
        this.startupTime = original.startupTime;
        this.interval = original.interval;
        this.affectsOwner = newAffectsOwner; // Modified
        this.pressureCost = original.pressureCost;
        this.instabilityCost = original.instabilityCost;
        this.augments = original.augments;
    }

    public RitualRecipe applyAugments(List<RitualAugment> activeAugments) {
        if (activeAugments.isEmpty()) return this;

        JsonObject mergedData = this.effectData.deepCopy();
        boolean modAffectsOwner = this.affectsOwner;

        for (RitualAugment aug : activeAugments) {
            JsonObject mod = aug.getModify();

            // Override Booleans
            if (mod.has("affects_owner")) {
                modAffectsOwner = mod.get("affects_owner").getAsBoolean();
            }

            // Merge Effect Data (e.g. increase power)
            if (mod.has("effect_data")) {
                JsonObject modData = mod.getAsJsonObject("effect_data");
                for (Map.Entry<String, JsonElement> entry : modData.entrySet()) {
                    // Simple replacement/addition of keys
                    mergedData.add(entry.getKey(), entry.getValue());
                }
            }
        }

        return new RitualRecipe(this, mergedData, modAffectsOwner);
    }

    public Identifier getId() { return id; }
    public Map<BlockPos, RuneType> getPattern() { return pattern; }
    public Map<BlockPos, String> getItemRequirements() { return itemRequirements; }
    public float getMinPressure() { return minPressure; }
    public float getMaxPressure() { return maxPressure; }
    public String getEffectId() { return effectId; }
    public JsonObject getEffectData() { return effectData; }
    public boolean isContinuous() { return continuous; }
    public String getRequiredItem() { return requiredItem; }
    public boolean consumesItem() { return consumeItem; }
    public boolean requiresAllItems() { return requireAllItems; }
    public boolean clearsItems() { return clearItems; }
    public boolean isShapeless() { return shapeless; }
    public int getStartupTime() { return startupTime; }
    public int getInterval() { return interval; }
    public boolean affectsOwner() { return affectsOwner; }
    public float getPressureCost() { return pressureCost; }
    public float getInstabilityCost() { return instabilityCost; }
    public List<RitualAugment> getAugments() { return augments; }
}