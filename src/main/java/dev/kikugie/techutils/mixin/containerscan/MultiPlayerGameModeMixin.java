package dev.kikugie.techutils.mixin.containerscan;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.kikugie.techutils.feature.containerscan.verifier.InventoryOverlay;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {
	@Definition(id = "InteractionResult", type = InteractionResult.class)
	@Definition(id = "getValue", method = "Lorg/apache/commons/lang3/mutable/MutableObject;getValue()Ljava/lang/Object;")
	@Expression("return @((InteractionResult) ?.getValue())")
	@ModifyExpressionValue(method = "useItemOn", at = @At("MIXINEXTRAS:EXPRESSION"))
	private InteractionResult recordContainer(InteractionResult original, @Local(argsOnly = true) BlockHitResult hitResult) {
		if (original.consumesAction()) {
			InventoryOverlay.onContainerClick(hitResult);
		}
		return original;
	}
}
