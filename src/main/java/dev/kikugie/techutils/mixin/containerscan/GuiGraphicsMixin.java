package dev.kikugie.techutils.mixin.containerscan;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.kikugie.techutils.feature.containerscan.verifier.InventoryOverlay;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiItemRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Partially taken from <a href="https://modrinth.com/mod/autohud">Auto HUD</a> by Crendgrim.
 *
 * @see <a href="https://github.com/Crendgrim/AutoHUD/blob/fd7cecaad0094b52314e458ec7ad45f6bd3ac733/src/main/java/mod/crend/autohud/mixin/DrawContextMixin.java">DrawContextMixin.java</a>
 */
@Mixin(GuiGraphics.class)
public class GuiGraphicsMixin {
	@ModifyArg(
		method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;III)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/render/state/GuiRenderState;submitItem(Lnet/minecraft/client/gui/render/state/GuiItemRenderState;)V"
		),
		index = 0
	)
	private GuiItemRenderState saveItemGuiState(GuiItemRenderState original) {
		if (InventoryOverlay.isRenderingTransparentItem) {
			InventoryOverlay.transparentItemStates.add(original);
		}
		return original;
	}

	@WrapMethod(method = "submitColoredRectangle(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/client/gui/render/TextureSetup;IIIIILjava/lang/Integer;)V")
	private void fillWithTransparency(
		RenderPipeline pipeline, TextureSetup textureSetup, int x1, int y1, int x2, int y2, int color, Integer color2, Operation<Void> original
	) {
		if (InventoryOverlay.isRenderingTransparentItem) {
			color = ARGB.color(Math.round(ARGB.alpha(color) * InventoryOverlay.MISSING_ITEM_ALPHA), color);
			if (color2 != null) {
				color2 = ARGB.color(Math.round(ARGB.alpha(color2) * InventoryOverlay.MISSING_ITEM_ALPHA), color2);
			}
		}

		original.call(pipeline, textureSetup, x1, y1, x2, y2, color, color2);
	}

	@WrapMethod(method = "submitBlit")
	private void quadWithTransparency(
		RenderPipeline pipeline, GpuTextureView atlasTexture, GpuSampler sampler, int x0, int y0, int x1, int y1, float u0, float u1, float v0, float v1, int color, Operation<Void> original
	) {
		if (InventoryOverlay.isRenderingTransparentItem) {
			if (pipeline.getBlendFunction().isPresent() && pipeline.getBlendFunction().get().destAlpha() == DestFactor.ZERO) {
				color = ARGB.scaleRGB(color, InventoryOverlay.MISSING_ITEM_ALPHA);
			} else {
				color = ARGB.color(Math.round(ARGB.alpha(color) * InventoryOverlay.MISSING_ITEM_ALPHA), color);
			}
		}

		original.call(pipeline, atlasTexture, sampler, x0, y0, x1, y1, u0, u1, v0, v1, color);
	}

	@WrapMethod(method = "drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)V")
	private void textWithTransparency(Font font, FormattedCharSequence text, int x, int y, int color, boolean shadow, Operation<Void> original) {
		if (InventoryOverlay.isRenderingTransparentItem) {
			color = ARGB.color(Math.round(ARGB.alpha(color) * InventoryOverlay.MISSING_ITEM_ALPHA), color);
		}

		original.call(font, text, x, y, color, shadow);
	}
}
