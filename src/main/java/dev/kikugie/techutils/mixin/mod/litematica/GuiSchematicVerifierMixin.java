package dev.kikugie.techutils.mixin.mod.litematica;

import com.chocohead.mm.api.ClassTinkerers;
import com.google.common.collect.ObjectArrays;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import dev.kikugie.techutils.feature.containerscan.verifier.SchematicVerifierExtension;
import fi.dy.masa.litematica.gui.GuiSchematicVerifier;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * ALL THIS FOR A BUTTON!
 */
@Mixin(value = GuiSchematicVerifier.class, remap = false)
public abstract class GuiSchematicVerifierMixin {
	@Shadow private static SchematicVerifier.MismatchType resultMode;
	@Shadow @Final private SchematicVerifier verifier;

	@Unique
	private static final Enum<?> SET_RESULT_MODE_WRONG_INVENTORIES;
	@Unique
	private static final Method CREATE_BUTTON;

	static {
		try {
			Class<?> typeClass = Class.forName("fi.dy.masa.litematica.gui.GuiSchematicVerifier$ButtonListener$Type");
			//noinspection unchecked,rawtypes
			SET_RESULT_MODE_WRONG_INVENTORIES = ClassTinkerers.getEnum(
				(Class<? extends Enum>) typeClass,
				"SET_RESULT_MODE_WRONG_INVENTORIES"
			);
			CREATE_BUTTON = GuiSchematicVerifier.class.getDeclaredMethod("createButton", int.class, int.class, int.class, typeClass);
		} catch (ClassNotFoundException | NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	@WrapOperation(
		method = "initGui",
		slice = @Slice(
			from = @At(value = "CONSTANT", args = "stringValue=litematica.gui.label.schematic_verifier.status.done_errors.no_diff")
		),
		at = @At(
			value = "INVOKE",
			target = "Lfi/dy/masa/malilib/util/StringUtils;translate(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;",
			ordinal = 0
		)
	)
	private String provideEnglishTranslationForWrongInventoriesWithoutDiff(String key, Object[] args, Operation<String> original) {
		if (Minecraft.getInstance().getLanguageManager().getSelected().startsWith("en_")) {
			return "Wrong: §4Inventory: %s§r, §cBlock: %s§r, §6State: %s§r, §bMissing: %s§r, §dExtra: %s§r"
				.formatted(ObjectArrays.concat(((SchematicVerifierExtension) verifier).getWrongInventoriesCount$techutils(), args));
		}
		return original.call(key, args);
	}

	@WrapOperation(
		method = "initGui",
		slice = @Slice(
			from = @At(value = "CONSTANT", args = "stringValue=litematica.gui.label.schematic_verifier.status.done_errors")
		),
		at = @At(
			value = "INVOKE",
			target = "Lfi/dy/masa/malilib/util/StringUtils;translate(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;",
			ordinal = 0
		)
	)
	private String provideEnglishTranslationForWrongInventoriesWithDiff(String key, Object[] args, Operation<String> original) {
		if (Minecraft.getInstance().getLanguageManager().getSelected().startsWith("en_")) {
			return "Wrong: §4Inventory: %s§r, §cBlock: %s§r, §6State: %s§r, §bMissing: %s§r, §dExtra: %s§r, §eDifferent: %s§r"
				.formatted(ObjectArrays.concat(((SchematicVerifierExtension) verifier).getWrongInventoriesCount$techutils(), args));
		}
		return original.call(key, args);
	}

	@Inject(
		method = "initGui",
		slice = @Slice(
			from = @At(
				value = "FIELD",
				target = "Lfi/dy/masa/litematica/gui/GuiSchematicVerifier$ButtonListener$Type;SET_RESULT_MODE_ALL:Lfi/dy/masa/litematica/gui/GuiSchematicVerifier$ButtonListener$Type;"
			)
		),
		at = @At(
			value = "INVOKE",
			target = "Lfi/dy/masa/litematica/gui/GuiSchematicVerifier;createButton(IIILfi/dy/masa/litematica/gui/GuiSchematicVerifier$ButtonListener$Type;)I",
			ordinal = 0,
			shift = At.Shift.BY,
			by = 5
		)
	)
	private void addButtons(CallbackInfo ci, @Local(ordinal = 0) LocalIntRef x, @Local(ordinal = 1) int y) throws InvocationTargetException, IllegalAccessException {
		var res = (Integer) CREATE_BUTTON.invoke(this, x.get(), y, -1, SET_RESULT_MODE_WRONG_INVENTORIES);
		x.set(x.get() + res + 4);
	}

	@ModifyExpressionValue(method = "createButton", at = @At(value = "INVOKE", target = "Lfi/dy/masa/litematica/gui/GuiSchematicVerifier$ButtonListener$Type;ordinal()I", ordinal = 0))
	private int addWrongInventoriesMap(int ordinal, @Local LocalBooleanRef enabled, @Local LocalRef<String> label) {
		if (ordinal == SET_RESULT_MODE_WRONG_INVENTORIES.ordinal()) {
			label.set(SchematicVerifierExtension.WRONG_INVENTORIES.getDisplayname());
			enabled.set(resultMode != SchematicVerifierExtension.WRONG_INVENTORIES);
		}
		return ordinal;
	}

	@Mixin(targets = "fi.dy.masa.litematica.gui.GuiSchematicVerifier$ButtonListener", remap = false)
	public static class ButtonListenerMixin {
		@Unique
		private static final Enum<?> SET_RESULT_MODE_WRONG_INVENTORIES;
		@Unique
		private static final Method SET_RESULT_MODE;

		static {
			try {
				Class<?> typeClass = Class.forName("fi.dy.masa.litematica.gui.GuiSchematicVerifier$ButtonListener$Type");
				//noinspection unchecked,rawtypes
				SET_RESULT_MODE_WRONG_INVENTORIES = ClassTinkerers.getEnum(
					(Class<? extends Enum>) typeClass,
					"SET_RESULT_MODE_WRONG_INVENTORIES"
				);
				SET_RESULT_MODE = GuiSchematicVerifier.class.getDeclaredMethod("setResultMode", SchematicVerifier.MismatchType.class);
			} catch (ClassNotFoundException | NoSuchMethodException e) {
				throw new RuntimeException(e);
			}
		}

		@Shadow @Final private GuiSchematicVerifier parent;

		@ModifyExpressionValue(method = "actionPerformedWithButton", at = @At(value = "INVOKE", target = "Lfi/dy/masa/litematica/gui/GuiSchematicVerifier$ButtonListener$Type;ordinal()I", ordinal = 0))
		private int addWrongInventoriesMap(int ordinal) throws InvocationTargetException, IllegalAccessException {
			if (ordinal == SET_RESULT_MODE_WRONG_INVENTORIES.ordinal()) {
				SET_RESULT_MODE.invoke(parent, SchematicVerifierExtension.WRONG_INVENTORIES);
			}
			return ordinal;
		}
	}
}
