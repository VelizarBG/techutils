package dev.kikugie.techutils.mixin.mod.litematica;

import com.llamalad7.mixinextras.sugar.Local;
import fi.dy.masa.litematica.render.OverlayRenderer;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.util.WorldUtils;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(OverlayRenderer.class)
public class OverlayRendererMixin {
	@ModifyArg(method = "renderVerifierOverlay", at = @At(value = "INVOKE", target = "Lfi/dy/masa/litematica/render/RenderUtils;renderInventoryOverlays(Lfi/dy/masa/malilib/render/GuiContext;Lfi/dy/masa/litematica/util/BlockInfoAlignment;ILnet/minecraft/world/level/Level;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)I"), index = 3)
	private Level provideBestWorld(Level level, @Local(argsOnly = true) GuiContext ctx) {
		return WorldUtils.getBestWorld(ctx.mc());
	}
}
