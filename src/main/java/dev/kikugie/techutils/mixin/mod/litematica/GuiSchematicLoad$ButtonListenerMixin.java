package dev.kikugie.techutils.mixin.mod.litematica;

import com.llamalad7.mixinextras.sugar.Local;
import dev.kikugie.techutils.config.LitematicConfigs;
import dev.kikugie.techutils.util.ItemPredicateUtils;
import fi.dy.masa.litematica.gui.GuiSchematicLoad;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

@Mixin(targets = "fi.dy.masa.litematica.gui.GuiSchematicLoad$ButtonListener", remap = false)
public class GuiSchematicLoad$ButtonListenerMixin {
	@Shadow @Final private GuiSchematicLoad gui;

	@Inject(method = "actionPerformedWithButton", at = @At(value = "FIELD", target = "Lfi/dy/masa/litematica/gui/GuiSchematicLoad$ButtonListener$Type;LOAD_SCHEMATIC:Lfi/dy/masa/litematica/gui/GuiSchematicLoad$ButtonListener$Type;"))
	private void replaceItemPredicatesWithPlaceholders(CallbackInfo ci, @Local LitematicaSchematic schematic) {
		if (!LitematicConfigs.REPLACE_ITEM_PREDICATES_WITH_PLACEHOLDERS.getBooleanValue()) {
			return;
		}

		var accessedSchematic = ((LitematicaSchematicAccessor) schematic);
		var containers = accessedSchematic.getBlockContainers();
		var blockEntities = accessedSchematic.getTileEntities();
		Map<String, Map<BlockPos, CompoundTag>> processedBlockEntities = new HashMap<>();
		var registryAccess = gui.mc.level.registryAccess();

		for (var regionEntry : blockEntities.entrySet()) {
			String region = regionEntry.getKey();
			var regionBlockEntities = regionEntry.getValue();
			var regionContainer = containers.get(region);
			Map<BlockPos, CompoundTag> processedRegionBlockEntities = new HashMap<>();

			for (var entry : regionBlockEntities.entrySet()) {
				BlockPos blockEntityPos = entry.getKey();
				var blockEntityNbt = entry.getValue();
				var blockEntity = BlockEntity.loadStatic(
					blockEntityPos,
					regionContainer.get(blockEntityPos.getX(), blockEntityPos.getY(), blockEntityPos.getZ()),
					blockEntityNbt,
					registryAccess
				);

				if (blockEntity instanceof Container inventory) {
					for (int i = 0; i < inventory.getContainerSize(); i++) {
						var stack = inventory.getItem(i);
						if (ItemPredicateUtils.getPlaceholder(stack) instanceof ItemStack placeholder) {
							inventory.setItem(i, placeholder);
						}
					}
					processedRegionBlockEntities.put(blockEntityPos, blockEntity.saveWithFullMetadata(registryAccess));
				} else {
					processedRegionBlockEntities.put(blockEntityPos, blockEntityNbt);
				}
			}

			processedBlockEntities.put(region, processedRegionBlockEntities);
		}

		accessedSchematic.setTileEntities(processedBlockEntities);
	}
}
