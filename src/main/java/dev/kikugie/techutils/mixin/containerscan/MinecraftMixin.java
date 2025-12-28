package dev.kikugie.techutils.mixin.containerscan;

import dev.kikugie.techutils.feature.containerscan.handlers.InteractionHandler;
import dev.kikugie.techutils.feature.containerscan.verifier.InventoryOverlay;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(Minecraft.class)
public class MinecraftMixin {
	@Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
	private void onScreen(Screen screen, CallbackInfo ci) {
		if (screen == null) {
			InventoryOverlay.clearOverlay();
			return;
		}

		if (!(screen instanceof MenuAccess<?>))
			return;

		InventoryOverlay.onScreenPostContainerClick();
		if (!InteractionHandler.onScreen(screen))
			ci.cancel();
	}
}
