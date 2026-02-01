package dev.kikugie.techutils.mixin.containerscan;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.kikugie.techutils.feature.containerscan.verifier.InventoryOverlay;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {
	// @WrapMethod ensures we can run after mods which return early via CallbackInfoReturnable
	@WrapMethod(method = "interactBlock")
	private ActionResult recordContainer(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult, Operation<ActionResult> original) {
		var result = original.call(player, hand, hitResult);
		if (result.isAccepted()) {
			InventoryOverlay.onContainerClick(hitResult);
		}
		return result;
	}
}
