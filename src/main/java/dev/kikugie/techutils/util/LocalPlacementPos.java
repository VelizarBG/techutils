package dev.kikugie.techutils.util;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.litematica.util.SchematicUtils;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents world position inside a schematic placement.
 *
 * @param pos       Block position in subregion's block state array.
 * @param region    Name of the targeted subregion
 * @param placement Targeted schematic placement instance
 */
public record LocalPlacementPos(BlockPos pos, String region, SchematicPlacement placement) {
	public static BlockPos getWorldPos(BlockPos pos, String region, SchematicPlacement placement) {
		return placement.getOrigin().offset(PositionUtils.getTransformedPlacementPosition(pos, placement, Objects.requireNonNull(placement.getRelativeSubRegionPlacement(region))));
	}

	public static Optional<LocalPlacementPos> get(BlockPos worldPos) {
		List<SchematicPlacementManager.PlacementPart> parts = DataManager
			.getSchematicPlacementManager()
			.getAllPlacementsTouchingChunk(worldPos);

		for (SchematicPlacementManager.PlacementPart part : parts) {
			if (!part.getBox().containsPos(worldPos))
				continue;

			SchematicPlacement placement = part.placement;
			String region = part.subRegionName;
			LitematicaBlockStateContainer container = placement.getSchematic().getSubRegionContainer(region);
			BlockPos schematicPos = SchematicUtils.getSchematicContainerPositionFromWorldPosition(
				worldPos,
				placement.getSchematic(),
				region,
				placement,
				Objects.requireNonNull(
					placement.getRelativeSubRegionPlacement(region),
					"Somehow subregion is null"),
				container);
			return Optional.of(new LocalPlacementPos(schematicPos, region, placement));
		}
		return Optional.empty();
	}

	public BlockPos getWorldPos() {
		return getWorldPos(this.pos, this.region, this.placement);
	}

	public BlockState blockState() {
		return this.placement.getSchematic().getSubRegionContainer(this.region).get(this.pos.getX(), this.pos.getY(), this.pos.getZ());
	}
}
