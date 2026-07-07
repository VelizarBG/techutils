package dev.kikugie.techutils.mixin.mod.litematica;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.kikugie.techutils.config.LitematicConfigs;
import dev.kikugie.techutils.feature.preview.gui.PreviewRenderManager;
import fi.dy.masa.litematica.gui.widgets.WidgetSchematicBrowser;
import fi.dy.masa.litematica.util.FileType;
import fi.dy.masa.malilib.gui.widgets.WidgetFileBrowserBase;
import net.minecraft.client.gui.DrawContext;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = WidgetSchematicBrowser.class, remap = false)
public abstract class WidgetSchematicBrowserMixin {
	@Definition(id = "cachedPreviewImages", field = "Lfi/dy/masa/litematica/gui/widgets/WidgetSchematicBrowser;cachedPreviewImages:Ljava/util/Map;")
	@Definition(id = "get", method = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;")
	@Expression("this.cachedPreviewImages.get(?)")
	@ModifyExpressionValue(method = "drawSelectedSchematicInfo", at = @At("MIXINEXTRAS:EXPRESSION"))
	private Object drawPreview(
		Object original,
		@Local(argsOnly = true) @Nullable WidgetFileBrowserBase.DirectoryEntry entry,
		@Local(argsOnly = true) DrawContext drawContext,
		@Local(name = "x") int x,
		@Local(name = "y") int y,
		@Local(name = "height") int height
	) {
		if (!LitematicConfigs.RENDER_PREVIEW.getBooleanValue()
			|| (!LitematicConfigs.OVERRIDE_PREVIEW.getBooleanValue() && original != null)
			|| !entry.getFullPath().exists()
			|| FileType.fromFile(entry.getFullPath()) != FileType.LITEMATICA_SCHEMATIC
		) {
			return original;
		}
		PreviewRenderManager.getInstance().ifPresent(manager -> manager.getOrCreateRenderer(entry).render(drawContext, x + 4, y + 14, height - y - 2));
		return null;
	}
}
