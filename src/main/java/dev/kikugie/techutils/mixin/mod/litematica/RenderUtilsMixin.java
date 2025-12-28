package dev.kikugie.techutils.mixin.mod.litematica;

import com.llamalad7.mixinextras.sugar.Local;
import dev.kikugie.techutils.feature.containerscan.verifier.InventoryOverlay;
import fi.dy.masa.litematica.render.RenderUtils;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = RenderUtils.class)
public class RenderUtilsMixin {
	@Inject(
		method = "renderInventoryOverlays",
		slice = @Slice(
			from = @At(
				value = "FIELD",
				target = "Lfi/dy/masa/malilib/gui/LeftRight;RIGHT:Lfi/dy/masa/malilib/gui/LeftRight;",
				remap = false
			)
		),
		at = @At(
			value = "INVOKE",
			target = "Lfi/dy/masa/litematica/render/RenderUtils;renderInventoryOverlay(Lnet/minecraft/client/gui/GuiGraphics;Lfi/dy/masa/litematica/util/BlockInfoAlignment;Lfi/dy/masa/malilib/gui/LeftRight;ILnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/client/Minecraft;)I",
			ordinal = 0
		)
	)
	private static void createOverlay(CallbackInfoReturnable<Integer> cir, @Local(argsOnly = true) BlockPos pos) {
		InventoryOverlay.infoOverlayInstance = InventoryOverlay.get(pos, false).orElse(null);
	}
}
