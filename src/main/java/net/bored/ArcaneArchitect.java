package net.bored;

import net.bored.command.AetherDebugCommand;
import net.bored.registry.ModBlockEntities;
import net.bored.registry.ModBlocks;
import net.bored.mixin.ServerWorldMixin;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArcaneArchitect implements ModInitializer {
	public static final String MOD_ID = "arcanearchitect";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Arcane Architect: Initializing Aether Systems...");

		// Register Blocks and Entities
		ModBlocks.registerBlocks();
		ModBlockEntities.registerBlockEntities();

		CommandRegistrationCallback.EVENT.register(AetherDebugCommand::register);

		// Note: Mixins are loaded automatically by Fabric via json
	}
}