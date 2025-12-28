package dev.kikugie.techutils.mixin.mod.litematica;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.kikugie.techutils.config.LitematicConfigs;
import dev.kikugie.techutils.feature.containerscan.verifier.BlockMismatchExtension;
import dev.kikugie.techutils.feature.containerscan.verifier.SchematicVerifierExtension;
import dev.kikugie.techutils.util.ItemPredicateUtils;
import fi.dy.masa.litematica.data.EntitiesDataStorage;
import fi.dy.masa.litematica.scheduler.tasks.TaskBase;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier;
import fi.dy.masa.litematica.util.ItemUtils;
import fi.dy.masa.malilib.util.IntBoundingBox;
import fi.dy.masa.malilib.util.WorldUtils;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static fi.dy.masa.litematica.schematic.verifier.SchematicVerifier.BlockMismatch;
import static fi.dy.masa.litematica.schematic.verifier.SchematicVerifier.MismatchRenderPos;
import static fi.dy.masa.litematica.schematic.verifier.SchematicVerifier.MismatchType;

@Mixin(value = SchematicVerifier.class, remap = false)
public abstract class SchematicVerifierMixin<InventoryBE extends BlockEntity & Container> extends TaskBase implements SchematicVerifierExtension {
	@Shadow @Final private static BlockPos.MutableBlockPos MUTABLE_POS;
	@Shadow private SchematicPlacement schematicPlacement;
	@Shadow private ClientLevel worldClient;

	@Shadow protected abstract void addAndSortPositions(MismatchType type, ArrayListMultimap<Pair<BlockState, BlockState>, BlockPos> sourceMap, List<BlockPos> listOut, int maxEntries);

	@Unique
	private final Set<Pair<InventoryBE, InventoryBE>> wrongInventories = new ReferenceOpenHashSet<>();
	@Unique
	private final ArrayListMultimap<Pair<BlockState, BlockState>, BlockPos> wrongInventoriesPositions = ArrayListMultimap.create();
	@Unique
	private final List<BlockPos> wrongInventoriesPositionsClosest = new ArrayList<>();
	@Unique
	private final List<BlockMismatch> selectedInventoryMismatches = new ArrayList<>();

	@Override
	public List<BlockMismatch> getSelectedInventoryMismatches$techutils() {
		return Collections.unmodifiableList(selectedInventoryMismatches);
	}

	@Override
	public int getWrongInventoriesCount$techutils() {
		return wrongInventories.size();
	}

	@ModifyExpressionValue(method = "verifyChunks", at = @At(value = "INVOKE", target = "Lfi/dy/masa/litematica/world/ChunkManagerSchematic;hasChunk(II)Z", remap = true))
	private boolean ensureInventoriesAreLoaded(boolean isLoaded, @Local ChunkPos pos) {
		return isLoaded && canProcessChunk(pos);
	}

	@Redirect(
		method = "verifyChunks",
		slice = @Slice(
			from = @At(value = "INVOKE", target = "Lfi/dy/masa/litematica/world/ChunkManagerSchematic;hasChunk(II)Z")
		),
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/multiplayer/ClientLevel;getChunk(II)Lnet/minecraft/world/level/chunk/LevelChunk;",
			ordinal = 0,
			remap = true
		)
	)
	private LevelChunk pickBestWorld(ClientLevel clientLevel, int x, int z) {
		return (WorldUtils.getBestWorld(mc) instanceof Level level ? level : clientLevel).getChunk(x, z);
	}

	/**
	 * Basically a clone of {@link fi.dy.masa.litematica.scheduler.tasks.TaskSaveSchematic#canProcessChunk}
	 */
	@Unique
	private boolean canProcessChunk(ChunkPos pos)
	{
		// Request entity data from Servux, if the ClientWorld matches, and treat it as not yet loaded
		EntitiesDataStorage eds = EntitiesDataStorage.getInstance();
		if ((eds.hasServuxServer() || eds.getIfReceivedBackupPackets())
			&& Objects.equals(eds.getWorld(), this.worldClient)
			&& !eds.hasCompletedChunk(pos))
		{
			if (eds.hasPendingChunk(pos))
				return false;

			ImmutableMap<String, IntBoundingBox> volumes = schematicPlacement.getBoxesWithinChunk(pos.x, pos.z);
			int minY = 319;         // Invert Values
			int maxY = -64;

			for (Map.Entry<String, IntBoundingBox> volumeEntry : volumes.entrySet())
			{
				IntBoundingBox bb = volumeEntry.getValue();

				minY = Math.min(bb.minY, minY);
				maxY = Math.max(bb.maxY, maxY);
			}

			if (eds.hasServuxServer())
			{
				eds.requestServuxBulkEntityData(pos, minY, maxY);
			}
			else if (eds.getIfReceivedBackupPackets())
			{
				eds.requestBackupBulkEntityData(pos, minY, maxY);
			}

			return false;
		}

		return this.areSurroundingChunksLoaded(pos, this.worldClient, 0);
	}

	@Inject(method = "verifyChunk", at = @At(value = "INVOKE", target = "Lfi/dy/masa/litematica/schematic/verifier/SchematicVerifier;checkBlockStates(IIILnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;)V", remap = true))
	private void checkInventories(ChunkAccess chunkClient, ChunkAccess chunkSchematic, IntBoundingBox box, CallbackInfoReturnable<Boolean> cir) {
		var expectedBE = chunkSchematic.getBlockEntity(MUTABLE_POS);
		var foundBE = chunkClient.getBlockEntity(MUTABLE_POS);
		if (!(expectedBE instanceof Container expected && foundBE instanceof Container found)
			|| expectedBE.getType() != foundBE.getType()) {
			return;
		}

		int size = expected.getContainerSize();
		if (size != found.getContainerSize()) {
			return;
		}

		var itemsForStates = ItemUtilsAccessor.getItemsForStates();
		boolean verifyItemComponents = LitematicConfigs.VERIFY_ITEM_COMPONENTS.getBooleanValue();
		for (int i = size - 1; i >= 0; i--) {
			var expectedStack = expected.getItem(i);
			var foundStack = found.getItem(i);

			Boolean predFailed = null;
			if (ItemPredicateUtils.getPredicate(expectedStack) instanceof ItemPredicate predicate) {
				predFailed = !predicate.test(foundStack);
			}

			if (predFailed != null
				? predFailed
				: expectedStack.getItem() != foundStack.getItem()
					|| expectedStack.getCount() != foundStack.getCount()
					|| verifyItemComponents
					&& !Objects.equals(expectedStack.getComponents(), foundStack.getComponents())
			) {
				var pos = MUTABLE_POS.immutable();
				//noinspection unchecked
				var pair = populateTooltipsIfNecessary((InventoryBE) expected, (InventoryBE) found, verifyItemComponents);
				wrongInventories.add(pair);
				warCrime(pair.getLeft(), pair.getRight(), itemsForStates, pos);
				break;
			}
		}
	}

	/**
	 * I ask for your forgiveness, future viewer (this makes differentiating inventories with the same block state possible)
	 */
	@Unique
	private void warCrime(InventoryBE expected, InventoryBE found, IdentityHashMap<BlockState, ItemStack> itemsForStates, BlockPos pos) {
		BlockState foundState = found.getBlockState();
		HashMap<Property<?>, Comparable<?>> propertyMap = new HashMap<>(foundState.getValues());

		propertyMap.put(BooleanProperty.create("war_crime"), true);
		BlockState newState = new BlockState(foundState.getBlock(), new Reference2ObjectArrayMap<>(propertyMap), null);

		itemsForStates.put(newState, ItemUtils.getItemForBlock(worldClient, pos, foundState, true));
		wrongInventoriesPositions.put(Pair.of(expected.getBlockState(), newState), pos);
		found.setBlockState(newState);
	}

	@SuppressWarnings("unchecked")
	@Unique
	private Pair<InventoryBE, InventoryBE> populateTooltipsIfNecessary(InventoryBE expected, InventoryBE found, boolean verifyItemComponents) {
		RegistryAccess accessClient = worldClient.registryAccess();
		RegistryAccess accessExpected = expected.getLevel().registryAccess();
		final var expectedNew = (InventoryBE) BlockEntity.loadStatic(expected.getBlockPos(), expected.getBlockState(), expected.saveWithFullMetadata(accessExpected), accessClient);
		RegistryAccess accessFound = found.getLevel().registryAccess();
		final var foundNew = (InventoryBE) BlockEntity.loadStatic(found.getBlockPos(), found.getBlockState(), found.saveWithFullMetadata(accessFound), accessClient);
		int size = expected.getContainerSize();
		for (int i = size - 1; i >= 0; i--) {
			var expectedStack = expectedNew.getItem(i);
			var foundStack = foundNew.getItem(i);

			if (ItemPredicateUtils.getPredicate(expectedStack) instanceof ItemPredicate predicate) {
				expectedStack.set(DataComponents.CUSTOM_NAME, Component.literal("Item Predicate")
					.withStyle(style -> style.withColor(ChatFormatting.WHITE).withItalic(false))
				);
				foundStack.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, nbtComponent ->
					nbtComponent.update(nbt ->
						nbt.put(ERROR_LINES_ID, ERROR_LINES_CODEC
							.encodeStart(NbtOps.INSTANCE, ItemPredicateUtils.getErrorLines(foundStack, predicate)).getOrThrow())
					)
				);
				continue;
			}

			if (verifyItemComponents && !Objects.equals(expectedStack.getComponents(), foundStack.getComponents())) {
				foundStack.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, nbtComponent ->
					nbtComponent.update(nbt ->
						nbt.put(ERROR_LINES_ID, ERROR_LINES_CODEC
							.encodeStart(NbtOps.INSTANCE, List.of(Component.literal("Item components don't match!")
								.withStyle(style -> style.withColor(ChatFormatting.RED).withItalic(false)))).getOrThrow())
					)
				);
			}
		}
		return Pair.of(expectedNew, foundNew);
	}

	@Inject(method = "addCountFor", at = @At("HEAD"), cancellable = true)
	private void addCountForWrongInventories(MismatchType mismatchType, ArrayListMultimap<Pair<BlockState, BlockState>, BlockPos> map, List<BlockMismatch> list, CallbackInfo ci) {
		if (mismatchType != WRONG_INVENTORIES) {
			return;
		}

		for (var pair : wrongInventories) {
			BlockState leftState = pair.getLeft().getBlockState();
			BlockState rightState = pair.getRight().getBlockState();
			BlockMismatch blockMismatch = new BlockMismatch(WRONG_INVENTORIES, leftState, rightState, 1);
			//noinspection unchecked
			((BlockMismatchExtension<InventoryBE>) blockMismatch).setInventories$techutils(pair);
			list.add(blockMismatch);
		}
		ci.cancel();
	}

	@Inject(method = "toggleMismatchEntrySelected", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/HashMultimap;remove(Ljava/lang/Object;Ljava/lang/Object;)Z"))
	private void tryRemoveSelectedInventoryMismatch(BlockMismatch mismatch, CallbackInfo ci, @Local MismatchType type) {
		if (type == WRONG_INVENTORIES) {
			selectedInventoryMismatches.remove(mismatch);
		}
	}

	@Inject(method = "toggleMismatchEntrySelected", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/HashMultimap;put(Ljava/lang/Object;Ljava/lang/Object;)Z"))
	private void tryAddSelectedInventoryMismatch(BlockMismatch mismatch, CallbackInfo ci, @Local MismatchType type) {
		if (type == WRONG_INVENTORIES) {
			selectedInventoryMismatches.add(mismatch);
		}
	}

	@Inject(method = "removeSelectedEntriesOfType", at = @At("HEAD"))
	private void tryRemoveSelectedInventoryMismatches(MismatchType type, CallbackInfo ci) {
		if (type == WRONG_INVENTORIES) {
			selectedInventoryMismatches.clear();
		}
	}

	@WrapOperation(method = "getMismatchOverviewCombined", at = @At(value = "INVOKE", target = "Lfi/dy/masa/litematica/schematic/verifier/SchematicVerifier;addCountFor(Lfi/dy/masa/litematica/schematic/verifier/SchematicVerifier$MismatchType;Lcom/google/common/collect/ArrayListMultimap;Ljava/util/List;)V", ordinal = 0))
	private void updateClosestWrongInventoriesPositions(SchematicVerifier instance, MismatchType type, ArrayListMultimap<Pair<BlockState, BlockState>, BlockPos> positions, List<BlockMismatch> list, Operation<Void> original) {
		original.call(instance, WRONG_INVENTORIES, wrongInventoriesPositions, list);
		original.call(instance, type, positions, list);
	}

	@Inject(method = "updateClosestPositions", at = @At("TAIL"))
	private void updateClosestWrongInventoriesPositions(BlockPos centerPos, int maxEntries, CallbackInfo ci) {
		addAndSortPositions(WRONG_INVENTORIES, wrongInventoriesPositions, wrongInventoriesPositionsClosest, maxEntries);
	}

	@WrapOperation(method = "combineClosestPositions", at = @At(value = "INVOKE", target = "Lfi/dy/masa/litematica/schematic/verifier/SchematicVerifier;getMismatchRenderPositionFor(Lfi/dy/masa/litematica/schematic/verifier/SchematicVerifier$MismatchType;Ljava/util/List;)V", ordinal = 0))
	private void updateClosestWrongInventoriesPositions(SchematicVerifier instance, MismatchType type, List<MismatchRenderPos> tempList, Operation<Void> original) {
		original.call(instance, WRONG_INVENTORIES, tempList);
		original.call(instance, type, tempList);
	}

	@Inject(method = "getMapForMismatchType", at = @At("HEAD"), cancellable = true)
	private void addWrongInventoriesMap(MismatchType mismatchType, CallbackInfoReturnable<ArrayListMultimap<Pair<BlockState, BlockState>, BlockPos>> cir) {
		if (mismatchType == WRONG_INVENTORIES) {
			cir.setReturnValue(wrongInventoriesPositions);
		}
	}

	@Inject(method = "getClosestMismatchedPositionsFor", at = @At("HEAD"), cancellable = true)
	private void addWrongInventoriesMismatchedPositions(MismatchType type, CallbackInfoReturnable<List<BlockPos>> cir) {
		if (type == WRONG_INVENTORIES) {
			cir.setReturnValue(wrongInventoriesPositionsClosest);
		}
	}

	@ModifyExpressionValue(method = "ignoreStateMismatch(Lfi/dy/masa/litematica/schematic/verifier/SchematicVerifier$BlockMismatch;Z)V", at = @At(value = "INVOKE", target = "Lfi/dy/masa/litematica/schematic/verifier/SchematicVerifier;getMapForMismatchType(Lfi/dy/masa/litematica/schematic/verifier/SchematicVerifier$MismatchType;)Lcom/google/common/collect/ArrayListMultimap;"))
	private ArrayListMultimap<Pair<BlockState, BlockState>, BlockPos> removeInventoryIfNecessary(ArrayListMultimap<Pair<BlockState, BlockState>, BlockPos> positions, @Local(argsOnly = true) BlockMismatch mismatch) {
		if (positions == wrongInventoriesPositions) {
			wrongInventories.remove(((BlockMismatchExtension<?>) mismatch).getInventories$techutils());
			selectedInventoryMismatches.remove(mismatch);
		}
		return positions;
	}

	@Inject(method = "clearData", at = @At("HEAD"))
	private void clearAdditionalData(CallbackInfo ci) {
		var itemsForStates = ItemUtilsAccessor.getItemsForStates();
		for (Pair<BlockState, BlockState> pair : wrongInventoriesPositions.keySet()) {
			itemsForStates.remove(pair.getRight());
		}
		wrongInventories.clear();
		wrongInventoriesPositions.clear();
		selectedInventoryMismatches.clear();
		EntitiesDataStorage.getInstance().reset(false);
	}
}
