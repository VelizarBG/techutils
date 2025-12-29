package dev.kikugie.techutils;

import dev.kikugie.techutils.command.IsorenderSelectionCommand;
import dev.kikugie.techutils.command.ItemPredicateCommand;
import dev.kikugie.techutils.config.malilib.InitHandler;
import dev.kikugie.techutils.feature.containerscan.handlers.InteractionHandler;
import dev.kikugie.techutils.feature.containerscan.verifier.InventoryOverlay;
import dev.kikugie.techutils.feature.worldedit.WorldEditSync;
import dev.kikugie.techutils.util.ResponseMuffler;
import fi.dy.masa.malilib.event.InitializationHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class TechUtilsMod implements ClientModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger(Reference.MOD_ID);
	public static final List<Consumer<Minecraft>> QUEUED_END_CLIENT_TICK_TASKS = new ArrayList<>();

	@Override
	public void onInitializeClient() {
		InitializationHandler.getInstance().registerInitializationHandler(new InitHandler());

		registerCommands();
		registerWorldEditSync();

		ClientTickEvents.START_WORLD_TICK.register(world -> InteractionHandler.tick(world.getGameTime()));
//        WorldRenderEvents.END.register(Remderer::onRender);
		ItemTooltipCallback.EVENT.register((stack, tooltipContext, tooltipType, lines) -> InventoryOverlay.addSpecialTooltipModifications(stack, lines));
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			for (var task : QUEUED_END_CLIENT_TICK_TASKS) {
				task.accept(client);
			}
			QUEUED_END_CLIENT_TICK_TASKS.clear();
		});
	}

	private void registerCommands() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			IsorenderSelectionCommand.register(dispatcher, registryAccess);
			ItemPredicateCommand.register(dispatcher, registryAccess);
		});
	}

	private void registerWorldEditSync() {
		ClientTickEvents.START_WORLD_TICK.register(tick -> WorldEditSync.getInstance().ifPresent(WorldEditSync::onTick));
		ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> !ResponseMuffler.test(message.getString()));
	}

}
