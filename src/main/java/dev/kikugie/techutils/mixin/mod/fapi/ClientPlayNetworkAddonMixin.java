package dev.kikugie.techutils.mixin.mod.fapi;

import dev.kikugie.techutils.feature.worldedit.WorldEditNetworkHandler;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.impl.networking.client.ClientPlayNetworkAddon;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("UnstableApiUsage")
@Mixin(ClientPlayNetworkAddon.class)
public class ClientPlayNetworkAddonMixin {
	/**
	 * Prevents packet receiver collision with other WorldEdit addons, such as WorldEditCUI.
	 */
	@Inject(method = "receive(Lnet/fabricmc/fabric/api/client/networking/v1/ClientPlayNetworking$PlayPayloadHandler;Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload;)V", at = @At(value = "INVOKE", target = "Lnet/fabricmc/fabric/api/client/networking/v1/ClientPlayNetworking$PlayPayloadHandler;receive(Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload;Lnet/fabricmc/fabric/api/client/networking/v1/ClientPlayNetworking$Context;)V"))
	private void yoinkWorldEditPacket(ClientPlayNetworking.PlayPayloadHandler<?> handler, CustomPacketPayload payload, CallbackInfo ci) {
		if (payload.type().id().equals(WorldEditNetworkHandler.CHANNEL))
			WorldEditNetworkHandler.getInstance().ifPresent(handlerInstance -> handlerInstance.onYoinkedPacket(payload));
	}
}
