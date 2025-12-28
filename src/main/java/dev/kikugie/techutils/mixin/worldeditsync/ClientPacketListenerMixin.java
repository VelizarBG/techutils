package dev.kikugie.techutils.mixin.worldeditsync;

import com.mojang.brigadier.CommandDispatcher;
import dev.kikugie.techutils.feature.worldedit.WorldEditSync;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
	@Shadow
	private CommandDispatcher<SharedSuggestionProvider> commands;

	@Inject(method = "handleCommands", at = @At("RETURN"))
	private void registerWorldEdit(ClientboundCommandsPacket packet, CallbackInfo ci) {
		WorldEditSync.getInstance().ifPresent(instance -> instance.onCommandTreePacket(this.commands));
	}
}
