package dev.kikugie.techutils.mixin.mod.litematica;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import fi.dy.masa.malilib.gui.Message;
import fi.dy.masa.malilib.util.InfoUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;

import java.nio.file.Path;

/**
 * Replaces Litematica's default method of selecting custom preview image with a file selection menu.
 */
@Mixin(targets = "fi/dy/masa/litematica/gui/GuiSchematicManager$ButtonListener", remap = false)
public class GuiSchematicManagerMixin {
	@Unique
	private static final String[] fileFormats = {"jpg", "png", "bmp"};

	/**
	 * Dark pointer magic taken from LWJGUI.
	 *
	 * @see <a href="https://github.com/orange451/LWJGUI/blob/bdc10971be84157e05aa0dbc1eccb6e51c5b04ca/src/main/java/lwjgui/LWJGUIDialog.java#L85">Source</a>
	 */
	@Definition(id = "getDirectory", method = "Lfi/dy/masa/malilib/gui/widgets/WidgetFileBrowserBase$DirectoryEntry;getDirectory()Ljava/io/File;")
	@Definition(id = "toPath", method = "Ljava/io/File;toPath()Ljava/nio/file/Path;")
	@Definition(id = "resolve", method = "Ljava/nio/file/Path;resolve(Ljava/lang/String;)Ljava/nio/file/Path;")
	@Expression("?.getDirectory().toPath().resolve('thumb.png')")
	@ModifyExpressionValue(method = "actionPerformedWithButton", at = @At("MIXINEXTRAS:EXPRESSION"))
	private Path pickCustomImage(Path original, @Share("pickingCustomImage") LocalBooleanRef pickingCustomImage) {
		pickingCustomImage.set(true);
		PointerBuffer filters;
		String selectedFile;
		try (MemoryStack stack = MemoryStack.stackPush()) {
			filters = stack.mallocPointer(fileFormats.length);
			for (String format : fileFormats) {
				filters.put(stack.UTF8("*." + format));
			}
			filters.flip();
			selectedFile = TinyFileDialogs.tinyfd_openFileDialog(
				"Select a preview image",
				original.toAbsolutePath().getParent().toUri().toString(),
				filters,
				"Image files",
				false
			);
		}

		if (selectedFile == null) {
			InfoUtils.showGuiAndInGameMessage(Message.MessageType.ERROR, "Image not selected");
			return original;
		}
		return Path.of(selectedFile);
	}

	@Redirect(method = "actionPerformedWithButton",
		at = @At(value = "INVOKE", target = "Lfi/dy/masa/malilib/gui/GuiBase;isShiftDown()Z"))
	private boolean dontRequireShift() {
		return true;
	}

	@Redirect(method = "actionPerformedWithButton",
		at = @At(value = "INVOKE", target = "Lfi/dy/masa/malilib/gui/GuiBase;isAltDown()Z"))
	private boolean dontRequireAlt() {
		return true;
	}

	@WrapWithCondition(method = "actionPerformedWithButton",
		slice = @Slice(from = @At(value = "CONSTANT", args = "stringValue=Image 'thumb.png' not found")),
		at = @At(value = "INVOKE", target = "Lfi/dy/masa/malilib/util/InfoUtils;showGuiAndInGameMessage(Lfi/dy/masa/malilib/gui/Message$MessageType;Ljava/lang/String;[Ljava/lang/Object;)V", ordinal = 0)
	)
	private boolean muteOriginalError(Message.MessageType type, String translationKey, Object[] args, @Share("pickingCustomImage") LocalBooleanRef pickingCustomImage) {
		if (pickingCustomImage.get()) {
			pickingCustomImage.set(false);
			return false;
		}
		return true;
	}
}
