package dev.kikugie.techutils.mixin.worldeditsync;

import dev.kikugie.techutils.feature.worldedit.WorldEditSync;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import org.enginehub.worldeditcui.fabric.network.FabricCUIPacketHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(value = FabricCUIPacketHandler.class, remap = false)
public class FabricCUIPacketHandlerMixin {
	@Inject(method = "registerClient", at = @At("RETURN"))
	private static void initWESync(CallbackInfo ci) {
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> WorldEditSync.init());
	}
}
