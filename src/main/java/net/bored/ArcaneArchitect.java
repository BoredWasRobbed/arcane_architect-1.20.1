package net.bored;

import net.bored.command.AetherDebugCommand;
import net.bored.networking.ModMessages;
import net.bored.recipe.RitualManager;
import net.bored.registry.ModBlockEntities;
import net.bored.registry.ModBlocks;
import net.bored.registry.RitualEffects;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resource.ResourceType;
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

		// Register Ritual Effects
		RitualEffects.registerEffects();

		// Register Networking
		ModMessages.registerC2SPackets();

		// Register Ritual Data Loader
		ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(RitualManager.INSTANCE);

		CommandRegistrationCallback.EVENT.register(AetherDebugCommand::register);
	}
}