package dev.kikugie.techutils.feature.containerscan;

import dev.kikugie.techutils.TechUtilsMod;
import dev.kikugie.techutils.util.ContainerUtils;
import dev.kikugie.techutils.util.LocalPlacementPos;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;

/**
 * Used to access container data inside a placement.
 * <p>
 * Is this overcomplicated? Definitely.
 */
public final class PlacementContainerAccess {
	public static LinkedStorageEntry getEntry(BlockPos worldPos, BlockState worldState) {
		return new LinkedStorageEntry(worldPos, null, getSchematicInventory(worldPos, worldState).orElse(null));
	}

	public static LinkedStorageEntry getEntry(BlockPos worldPos, BlockState worldState, SimpleContainer worldInventory) {
		return new LinkedStorageEntry(worldPos, worldInventory, getSchematicInventory(worldPos, worldState).orElse(null));
	}

	/**
	 * Gets placement container data at a position, if there's a placement there. For double chests returns their combined inventory.
	 *
	 * @param worldPos   position in the world
	 * @param worldState block state of the given position
	 * @return {@link SimpleContainer} with schematic items if its present
	 * @see LocalPlacementPos
	 */
	public static Optional<SimpleContainer> getSchematicInventory(BlockPos worldPos, BlockState worldState) {
		ChestType type = getChestType(worldState);
		if (type == ChestType.SINGLE)
			return getSchematicInventoryInternal(worldPos, worldState);

		// Double chest handling
		BlockPos adjacentChest = worldPos.offset(ChestBlock.getConnectedDirection(worldState).getUnitVec3i());
		assert Minecraft.getInstance().level != null;
		BlockState adjacentState = Minecraft.getInstance().level.getBlockState(adjacentChest);

		Optional<SimpleContainer> opt1 = getSchematicInventoryInternal(worldPos, worldState);
		Optional<SimpleContainer> opt2 = getSchematicInventoryInternal(adjacentChest, adjacentState);
		if (opt1.isEmpty() && opt2.isEmpty())
			return Optional.empty();
		SimpleContainer chest1 = opt1.orElse(new SimpleContainer(27));
		SimpleContainer chest2 = opt2.orElse(new SimpleContainer(27));

		return type == ChestType.RIGHT ? Optional.of(merge(chest1, chest2)) : Optional.of(merge(chest2, chest1));
	}

	public static Optional<SimpleContainer> getSchematicInventoryInternal(BlockPos worldPos, BlockState worldState) {
		Optional<Container> dummyInv = ContainerUtils.validateContainer(worldPos, worldState);
		// World block is not a container
		if (dummyInv.isEmpty())
			return Optional.empty();

		Optional<LocalPlacementPos> optionalPos = LocalPlacementPos.get(worldPos);
		// Block is not in the schematic
		if (optionalPos.isEmpty())
			return Optional.empty();

		LocalPlacementPos placementPos = optionalPos.get();
		Optional<Container> schemInv = ContainerUtils.validateContainer(worldPos, placementPos.blockState());
		// Schematic and world blocks don't match
		if (schemInv.isEmpty()
			|| dummyInv.get().getContainerSize() != schemInv.get().getContainerSize()
			|| !(schemInv.get() instanceof BlockEntity schemBE)
			|| !(dummyInv.get() instanceof BlockEntity dummyBE)
			|| schemBE.getType() != dummyBE.getType()
		) {
			return Optional.empty();
		}

		return Optional.ofNullable(getItems(placementPos));
	}

	private static SimpleContainer merge(Container first, Container second) {
		CompoundContainer combined = new CompoundContainer(first, second);
		SimpleContainer inventory = new SimpleContainer(combined.getContainerSize());
		for (int i = 0; i < combined.getContainerSize(); i++) {
			inventory.setItem(i, combined.getItem(i));
		}
		return inventory;
	}

	private static void sendError(String message) {
		TechUtilsMod.LOGGER.warn(message);
	}

	/**
	 * @return {@link ChestType} of a block state. Returns {@link ChestType#SINGLE} for any other state, as all that matters is if it's a single-block storage.
	 */
	private static ChestType getChestType(BlockState state) {
		if (!(state.getBlock() instanceof ChestBlock))
			return ChestType.SINGLE;
		return state.getValue(ChestBlock.TYPE);
	}

	@Nullable
	private static SimpleContainer getItems(LocalPlacementPos placementPos) {
		Map<BlockPos, CompoundTag> blockEntities = placementPos.placement().getSchematic()
			.getBlockEntityMapForRegion(placementPos.region());
		// No block entity map for the region. Shouldn't be possible unless it was manually modified
		if (blockEntities == null)
			return null;

		CompoundTag nbt = blockEntities.get(placementPos.pos());
		// No such entry in the map
		if (nbt == null)
			return null;

		var registryAccess = Minecraft.getInstance().level.registryAccess();
		var blockEntity = BlockEntity.loadStatic(
			placementPos.pos(),
			placementPos.blockState(),
			nbt,
			registryAccess
		);

		if (!(blockEntity instanceof Container schematicInventory)) {
			return null;
		}

		var inventory = new SimpleContainer(schematicInventory.getContainerSize());

		for (int i = 0; i < inventory.getContainerSize(); i++) {
			var stack = schematicInventory.getItem(i);
			inventory.setItem(i, stack);
		}

		return inventory;
	}
}
