package dev.kikugie.techutils.mixin.containerscan;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.kikugie.techutils.feature.containerscan.verifier.InventoryOverlay;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.state.GuiItemRenderState;
import net.minecraft.client.gui.render.state.BlitRenderState;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.util.ARGB;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Partially taken from <a href="https://modrinth.com/mod/autohud">Auto HUD</a> by Crendgrim.
 *
 * @see <a href="https://github.com/Crendgrim/AutoHUD/blob/fd7cecaad0094b52314e458ec7ad45f6bd3ac733/src/main/java/mod/crend/autohud/mixin/GuiRendererMixin.java">GuiRendererMixin.java</a>
 */
@Mixin(GuiRenderer.class)
public class GuiRendererMixin {
	@Inject(method = "render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V", at = @At("RETURN"))
	private void clearTransparentItemStates(CallbackInfo ci) {
		InventoryOverlay.transparentItemStates.clear();
	}

	@WrapOperation(method = "submitBlitFromItemAtlas", at = @At(value = "NEW", target = "Lnet/minecraft/client/gui/render/state/BlitRenderState;"))
	BlitRenderState processTransparentItemState(
		RenderPipeline pipeline,
		TextureSetup textureSetup,
		Matrix3x2f pose,
		int x1,
		int y1,
		int x2,
		int y2,
		float u1,
		float u2,
		float v1,
		float v2,
		int color,
		@Nullable ScreenRectangle scissorArea,
		@Nullable ScreenRectangle bounds,
		Operation<BlitRenderState> original,
		@Local(argsOnly = true) GuiItemRenderState state
	) {
		if (InventoryOverlay.transparentItemStates.contains(state)) {
			color = ARGB.color(Math.round(ARGB.alpha(color) * InventoryOverlay.MISSING_ITEM_ALPHA), color);
			pipeline = RenderPipelines.GUI_TEXTURED;
		}

		return original.call(pipeline, textureSetup, pose, x1, y1, x2, y2, u1, u2, v1, v2, color, scissorArea, bounds);
	}
}
