package net.bored.recipe;

import com.google.gson.JsonObject;
import net.bored.block.enums.RuneType;
import net.minecraft.util.math.BlockPos;

public class RitualAugment {
    private final RuneType rune;
    private final BlockPos offset;
    private final JsonObject modify; // JSON object containing overrides

    public RitualAugment(RuneType rune, BlockPos offset, JsonObject modify) {
        this.rune = rune;
        this.offset = offset;
        this.modify = modify;
    }

    public RuneType getRune() { return rune; }
    public BlockPos getOffset() { return offset; }
    public JsonObject getModify() { return modify; }
}