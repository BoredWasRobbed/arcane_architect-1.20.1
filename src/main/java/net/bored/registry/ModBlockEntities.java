package net.bored.registry;

import net.bored.ArcaneArchitect;
import net.bored.block.entity.RuneBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlockEntities {

    public static BlockEntityType<RuneBlockEntity> RUNE_BLOCK_ENTITY;

    public static void registerBlockEntities() {
        RUNE_BLOCK_ENTITY = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                new Identifier(ArcaneArchitect.MOD_ID, "rune_block_entity"),
                FabricBlockEntityTypeBuilder.create(RuneBlockEntity::new, ModBlocks.AETHER_RUNE).build()
        );
    }
}