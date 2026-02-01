package dev.kikugie.techutils.mixin.containerscan;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.kikugie.techutils.feature.containerscan.verifier.InventoryOverlay;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {
	// @WrapMethod ensures we can run after mods which return early via CallbackInfoReturnable
	@WrapMethod(method = "useItemOn")
	private InteractionResult recordContainer(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult, Operation<InteractionResult> original) {
		var result = original.call(player, hand, hitResult);
		if (result.consumesAction()) {
			InventoryOverlay.onContainerClick(hitResult);
		}
		return result;
	}
}
