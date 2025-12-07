package net.bored.registry;

import net.bored.ArcaneArchitect;
import net.bored.block.AbstractRuneBlock;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlocks {

    // Define specific rune types as anonymous classes for now (or distinct classes later)
    public static final AbstractRuneBlock AETHER_RUNE = new AbstractRuneBlock(FabricBlockSettings.copy(Blocks.GLASS).noCollision().breakInstantly()) {};

    public static void registerBlocks() {
        Registry.register(Registries.BLOCK, new Identifier(ArcaneArchitect.MOD_ID, "aether_rune"), AETHER_RUNE);
        Registry.register(Registries.ITEM, new Identifier(ArcaneArchitect.MOD_ID, "aether_rune"), new BlockItem(AETHER_RUNE, new FabricItemSettings()));
    }
}