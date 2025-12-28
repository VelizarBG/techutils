package dev.kikugie.techutils.feature.containerscan.verifier;

import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

public interface BlockMismatchExtension<InventoryBE extends BlockEntity & Container> {
	void setInventories$techutils(Pair<InventoryBE, InventoryBE> inventories);

	@Nullable
	Pair<InventoryBE, InventoryBE> getInventories$techutils();
}
