package net.bored.recipe;

import com.google.gson.*;
import net.bored.ArcaneArchitect;
import net.bored.block.enums.RuneType;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.math.BlockPos;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class RitualManager implements SimpleSynchronousResourceReloadListener {
    public static final RitualManager INSTANCE = new RitualManager();
    public static final Identifier ID = new Identifier(ArcaneArchitect.MOD_ID, "rituals");

    private final Map<Identifier, RitualRecipe> recipes = new HashMap<>();

    @Override
    public Identifier getFabricId() {
        return ID;
    }

    @Override
    public void reload(ResourceManager manager) {
        recipes.clear();
        ArcaneArchitect.LOGGER.info("Loading Arcane Architect Rituals...");

        for (Identifier id : manager.findResources("rituals", path -> path.getPath().endsWith(".json")).keySet()) {
            try (InputStream stream = manager.getResource(id).get().getInputStream()) {
                JsonObject json = JsonHelper.deserialize(new InputStreamReader(stream, StandardCharsets.UTF_8));
                RitualRecipe recipe = parse(id, json);
                recipes.put(recipe.getId(), recipe);
            } catch (Exception e) {
                ArcaneArchitect.LOGGER.error("Error loading ritual: " + id, e);
            }
        }
        ArcaneArchitect.LOGGER.info("Loaded " + recipes.size() + " rituals.");
    }

    public Map<Identifier, RitualRecipe> getAll() {
        return recipes;
    }

    private RitualRecipe parse(Identifier fileId, JsonObject json) {
        JsonObject keysJson = JsonHelper.getObject(json, "keys");
        Map<Character, RuneType> keyMap = new HashMap<>();
        for (String key : keysJson.keySet()) {
            String runeName = keysJson.get(key).getAsString();
            RuneType type = RuneType.valueOf(runeName.toUpperCase());
            keyMap.put(key.charAt(0), type);
        }

        JsonArray patternJson = JsonHelper.getArray(json, "pattern");
        Map<BlockPos, RuneType> patternMap = new HashMap<>();

        int height = patternJson.size();
        int centerX = 0;
        int centerZ = 0;
        boolean centerFound = false;

        for (int z = 0; z < height; z++) {
            String row = patternJson.get(z).getAsString();
            for (int x = 0; x < row.length(); x++) {
                if (row.charAt(x) == '@') {
                    centerX = x;
                    centerZ = z;
                    centerFound = true;
                }
            }
        }

        if (!centerFound) {
            throw new IllegalStateException("Ritual pattern missing center marker '@'");
        }

        for (int z = 0; z < height; z++) {
            String row = patternJson.get(z).getAsString();
            for (int x = 0; x < row.length(); x++) {
                char c = row.charAt(x);
                if (c == ' ') continue;

                if (keyMap.containsKey(c)) {
                    BlockPos relPos = new BlockPos(x - centerX, 0, z - centerZ);
                    patternMap.put(relPos, keyMap.get(c));
                }
            }
        }

        float minP = JsonHelper.getFloat(json, "pressure_min", 0.0f);
        float maxP = JsonHelper.getFloat(json, "pressure_max", 1.0f);
        String effect = JsonHelper.getString(json, "effect");
        JsonObject data = json.has("effect_data") ? JsonHelper.getObject(json, "effect_data") : new JsonObject();
        boolean continuous = JsonHelper.getBoolean(json, "continuous", false);
        String requiredItem = JsonHelper.getString(json, "required_item", "");
        boolean consumeItem = JsonHelper.getBoolean(json, "consume_item", false);

        // New Field
        boolean requireAllItems = JsonHelper.getBoolean(json, "require_all_items", false);

        return new RitualRecipe(fileId, patternMap, minP, maxP, effect, data, continuous, requiredItem, consumeItem, requireAllItems);
    }
}