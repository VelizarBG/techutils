package dev.kikugie.techutils.mixin.mod.litematica;

import dev.kikugie.techutils.feature.containerscan.verifier.SchematicVerifierExtension;
import fi.dy.masa.litematica.gui.GuiSchematicVerifier;
import fi.dy.masa.litematica.gui.GuiSchematicVerifier.BlockMismatchEntry;
import fi.dy.masa.litematica.gui.widgets.WidgetListSchematicVerificationResults;
import fi.dy.masa.litematica.gui.widgets.WidgetSchematicVerificationResult;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier;
import fi.dy.masa.malilib.gui.interfaces.ISelectionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Comparator;
import java.util.List;

import static dev.kikugie.techutils.feature.containerscan.verifier.SchematicVerifierExtension.WRONG_INVENTORIES;

@Mixin(value = WidgetListSchematicVerificationResults.class, remap = false)
public abstract class WidgetListSchematicVerificationResultsMixin extends WidgetListBase<BlockMismatchEntry, WidgetSchematicVerificationResult> {
	@Shadow @Final private GuiSchematicVerifier guiSchematicVerifier;

	@Shadow protected abstract void addEntriesForType(SchematicVerifier.MismatchType type);

	public WidgetListSchematicVerificationResultsMixin(int x, int y, int width, int height, @Nullable ISelectionListener<BlockMismatchEntry> selectionListener) {
		super(x, y, width, height, selectionListener);
	}

	@Override
	protected boolean shouldRenderHoverStuff() {
		return getSelectedInventoryMismatches().isEmpty();
	}

	@Inject(method = "drawContents", at = @At("RETURN"))
	private void tryDrawSelectedWrongInventory(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
		var selectedInventoryMismatches = getSelectedInventoryMismatches();
		if (selectedInventoryMismatches.isEmpty()) {
			return;
		}

		listWidgets.stream()
			.filter(w -> w.getEntry() instanceof BlockMismatchEntry bme
				&& bme.blockMismatch != null && bme.blockMismatch.mismatchType == WRONG_INVENTORIES
			)
			.max(Comparator.comparingInt(w -> selectedInventoryMismatches.indexOf(w.getEntry().blockMismatch)))
			.ifPresent(w -> w.postRenderHovered(graphics, mouseX, mouseY, true));
	}

	@Unique
	private List<SchematicVerifier.BlockMismatch> getSelectedInventoryMismatches() {
		return ((SchematicVerifierExtension) ((GuiSchematicVerifierAccessor) guiSchematicVerifier).getVerifier())
			.getSelectedInventoryMismatches$techutils();
	}

	@Inject(
		method = "refreshBrowserEntries",
		at = @At(value = "INVOKE", target = "Lfi/dy/masa/litematica/gui/widgets/WidgetListSchematicVerificationResults;addEntriesForType(Lfi/dy/masa/litematica/schematic/verifier/SchematicVerifier$MismatchType;)V", ordinal = 0)
	)
	private void addEntriesForWrongInventories(CallbackInfo ci) {
		addEntriesForType(WRONG_INVENTORIES);
	}
}
