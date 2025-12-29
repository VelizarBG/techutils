package dev.kikugie.techutils.feature.containerscan.verifier;

import com.chocohead.mm.api.ClassTinkerers;
import com.mojang.serialization.Codec;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier.MismatchType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface SchematicVerifierExtension {
	String ERROR_LINES_ID = "techutils:error_lines";
	Codec<List<Component>> ERROR_LINES_CODEC = ComponentSerialization.CODEC.listOf();
	MismatchType WRONG_INVENTORIES = ClassTinkerers.getEnum(MismatchType.class, "WRONG_INVENTORIES");

	/**
	 * One must imagine Sisyphus happy
	 */
	static @NotNull ItemStack addErrorLines(ItemStack stack, List<Component> lines) {
		var data = stack.get(DataComponents.CUSTOM_DATA);
		CompoundTag nbt;
		if (data == null || !(nbt = data.copyTag()).contains(ERROR_LINES_ID)) {
			return stack;
		}

		stack = stack.copy();
		var errorLines = ERROR_LINES_CODEC.parse(NbtOps.INSTANCE, nbt.getList(ERROR_LINES_ID).orElseThrow()).getOrThrow();
		nbt.remove(ERROR_LINES_ID);
		if (nbt.isEmpty()) {
			stack.remove(DataComponents.CUSTOM_DATA);
			for (Component line : lines) {
				if (line.getContents() instanceof TranslatableContents contents
					&& contents.getKey().equals("item.components")
				) {
					Object[] args = contents.getArgs();
					args[0] = ((int) args[0]) - 1;
				}
			}
		} else {
			stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
		}
		lines.addAll(errorLines);

		return stack;
	}

	List<SchematicVerifier.BlockMismatch> getSelectedInventoryMismatches$techutils();

	int getWrongInventoriesCount$techutils();
}
