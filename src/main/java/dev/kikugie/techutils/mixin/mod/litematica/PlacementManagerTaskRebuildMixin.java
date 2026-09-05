package dev.kikugie.techutils.mixin.mod.litematica;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.placement.PlacementManagerTaskRebuild;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Comparator;
import java.util.List;

@Mixin(PlacementManagerTaskRebuild.class)
public class PlacementManagerTaskRebuildMixin {
	@ModifyExpressionValue(method = "lambda$buildTask$0", at = @At(value = "INVOKE", target = "Lfi/dy/masa/litematica/schematic/placement/SchematicPlacementManager;getAllSchematicsTouchingChunk(Lnet/minecraft/world/level/ChunkPos;)Ljava/util/List;"))
	private List<SchematicPlacement> sortPlacements(List<SchematicPlacement> original) {
		SchematicPlacementManager schematicPlacementManager = DataManager.getSchematicPlacementManager();
		SchematicPlacement selectedPlacement = schematicPlacementManager.getSelectedSchematicPlacement();
		// Make sure the selected placement is last so **its** inventories are placed in the schematic world
		original.sort(Comparator.comparing(placement -> placement == selectedPlacement));
		return original;
	}
}
