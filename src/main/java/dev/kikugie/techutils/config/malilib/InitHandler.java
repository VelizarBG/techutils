package dev.kikugie.techutils.config.malilib;

import dev.kikugie.techutils.Reference;
import dev.kikugie.techutils.config.ConfigGui;
import dev.kikugie.techutils.config.Configs;
import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.interfaces.IInitializationHandler;
import fi.dy.masa.malilib.registry.Registry;
import fi.dy.masa.malilib.util.data.ModInfo;

public class InitHandler implements IInitializationHandler {
	@Override
	public void registerModHandlers() {
		ConfigManager.getInstance().registerConfigHandler(Reference.MOD_ID, new Configs());
		Registry.CONFIG_SCREEN.registerConfigScreenFactory(
				new ModInfo(Reference.MOD_ID, Reference.MOD_NAME, ConfigGui::new)
		);

		InputEventHandler.getKeybindManager().registerKeybindProvider(InputHandler.getInstance());
		KeyCallbacks.init();
	}
}
