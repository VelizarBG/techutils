package dev.kikugie.techutils.mixin.mod.litematica;

import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(SchematicVerifier.MismatchType.class)
public enum SchematicVerifier$MismatchTypeMixin {
	TECHUTILS_WRONG_INVENTORIES(0xFF0000, "litematica.gui.label.schematic_verifier_display_type.wrong_inventories", "§4");

	@Shadow
	SchematicVerifier$MismatchTypeMixin(int color, String unlocName, String colorCode) {
	}
}
