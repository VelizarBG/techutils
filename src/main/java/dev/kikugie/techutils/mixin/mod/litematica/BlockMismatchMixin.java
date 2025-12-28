package dev.kikugie.techutils.mixin.mod.litematica;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.kikugie.techutils.feature.containerscan.verifier.BlockMismatchExtension;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier.BlockMismatch;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = BlockMismatch.class, remap = false)
public class BlockMismatchMixin<InventoryBE extends BlockEntity & Container> implements BlockMismatchExtension<InventoryBE> {
	@Unique @Nullable
	private Pair<InventoryBE, InventoryBE> inventories;

	@Override
	public void setInventories$techutils(Pair<InventoryBE, InventoryBE> inventories) {
		this.inventories = inventories;
	}

	@Override @Nullable
	public Pair<InventoryBE, InventoryBE> getInventories$techutils() {
		return inventories;
	}

	@ModifyReturnValue(method = "hashCode", at = @At("RETURN"))
	private int hashInventories(int result, @Local(ordinal = 0) int prime) {
		return prime * result + ((inventories == null) ? 0 : inventories.hashCode());
	}
}
