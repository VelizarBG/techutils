package dev.kikugie.techutils.mixin.mod.litematica;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import fi.dy.masa.malilib.gui.Message;
import fi.dy.masa.malilib.util.InfoUtils;
import net.minecraft.client.Minecraft;
import org.lwjgl.sdl.SDLDialog;
import org.lwjgl.sdl.SDLEvents;
import org.lwjgl.sdl.SDLTimer;
import org.lwjgl.sdl.SDL_DialogFileFilter;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Replaces Litematica's default method of selecting custom preview image with a file selection menu.
 */
@Mixin(targets = "fi/dy/masa/litematica/gui/GuiSchematicManager$ButtonListener", remap = false)
public class GuiSchematicManagerMixin {
	@Unique
	private static final String[] fileFormats = {"png"/*, "jpg", "bmp"*/};

	/**
	 * Dark pointer magic taken from LWJGUI.
	 * Ported to SDL3 thanks to CustomPlayerModels.
	 *
	 * @see <a href="https://github.com/orange451/LWJGUI/blob/bdc10971be84157e05aa0dbc1eccb6e51c5b04ca/src/main/java/lwjgui/LWJGUIDialog.java#L85">LWJGUI</a>
	 * @see <a href="https://github.com/tom5454/CustomPlayerModels/blob/5d35bbc1d5d02e842fd20f9378f98b668b291134/CustomPlayerModels-26.3/src/platform-shared/java/com/tom/cpm/client/SDLChooser.java#L58">CustomPlayerModels</a>
	 */
	@Definition(id = "getDirectory", method = "Lfi/dy/masa/malilib/gui/widgets/WidgetFileBrowserBase$DirectoryEntry;getDirectory()Ljava/nio/file/Path;")
	@Definition(id = "resolve", method = "Ljava/nio/file/Path;resolve(Ljava/lang/String;)Ljava/nio/file/Path;")
	@Expression("?.getDirectory().resolve('thumb.png')")
	@ModifyExpressionValue(method = "actionPerformedWithButton", at = @At("MIXINEXTRAS:EXPRESSION"))
	private Path pickCustomImage(Path original, @Share("pickingCustomImage") LocalBooleanRef pickingCustomImage) {
		pickingCustomImage.set(true);

		try (MemoryStack stack = MemoryStack.stackPush()) {
			long window = Minecraft.getInstance().getWindow().handle();
			var buffer = SDL_DialogFileFilter.create(1);
			buffer.name(stack.UTF8("Image files")).pattern(stack.UTF8(String.join(";", fileFormats)));
			String path = original.toAbsolutePath().getParent().toUri().toString();
			CompletableFuture<String> selectedFileFuture = new CompletableFuture<>();
			SDLDialog.SDL_ShowOpenFileDialog((_, filelist, _) -> {
				if (filelist == MemoryUtil.NULL) {
					selectedFileFuture.complete(null);
					return;
				}
				long pathPtr = MemoryUtil.memGetAddress(filelist);
				if (pathPtr == MemoryUtil.NULL) {
					selectedFileFuture.complete(null);
					return;
				}
				selectedFileFuture.complete(MemoryUtil.memUTF8(pathPtr));
			}, 0, window, buffer, path, false);

			while (!selectedFileFuture.isDone()) {
				SDLEvents.SDL_PumpEvents();
				SDLTimer.SDL_Delay(16);
			}

			try {
				String selectedFile = selectedFileFuture.get();
				if (selectedFile == null) {
					InfoUtils.showGuiAndInGameMessage(Message.MessageType.ERROR, "Image not selected");
					return original;
				}
				return Path.of(selectedFile);
			} catch (InterruptedException | ExecutionException e) {
				throw new RuntimeException(e);
			}
		}
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
