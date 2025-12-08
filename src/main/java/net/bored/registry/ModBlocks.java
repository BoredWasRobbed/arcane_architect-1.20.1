package net.bored.registry;

import net.bored.ArcaneArchitect;
import net.bored.block.RuneBlock;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlocks {

    // Now using the concrete RuneBlock class instead of anonymous AbstractRuneBlock
    public static final RuneBlock AETHER_RUNE = new RuneBlock(FabricBlockSettings.copy(Blocks.GLASS).noCollision().breakInstantly());

    public static void registerBlocks() {
        Registry.register(Registries.BLOCK, new Identifier(ArcaneArchitect.MOD_ID, "aether_rune"), AETHER_RUNE);
        Registry.register(Registries.ITEM, new Identifier(ArcaneArchitect.MOD_ID, "aether_rune"), new BlockItem(AETHER_RUNE, new FabricItemSettings()));
    }
}