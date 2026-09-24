package dev.kikugie.techutils.mixin.mod.litematica;

import org.spongepowered.asm.mixin.Mixin;

@Mixin(targets = "fi/dy/masa/litematica/gui/GuiSchematicVerifier$ButtonListener$Type")
public enum GuiSchematicVerifier$ButtonListener$TypeMixin {
	TECHUTILS_SET_RESULT_MODE_WRONG_INVENTORIES
}
