package dev.kikugie.techutils.feature.containerscan.verifier;

import dev.kikugie.techutils.config.LitematicConfigs;
import dev.kikugie.techutils.feature.containerscan.LinkedStorageEntry;
import dev.kikugie.techutils.feature.containerscan.PlacementContainerAccess;
import dev.kikugie.techutils.feature.containerscan.handlers.InteractionHandler;
import dev.kikugie.techutils.util.ContainerUtils;
import dev.kikugie.techutils.util.ItemPredicateUtils;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.malilib.util.WorldUtils;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.state.GuiItemRenderState;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.world.inventory.MenuConstructor;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class InventoryOverlay {
	public static final float MISSING_ITEM_ALPHA = 0.5F;
	@Nullable
	public static InventoryOverlay infoOverlayInstance = null;
	@Nullable
	public static ItemStack hoveredStackToRender;
	public static boolean delayRenderingHoveredStack = false;
	public static boolean isRenderingTransparentItem = false;
	public static Set<GuiItemRenderState> transparentItemStates = new ReferenceOpenHashSet<>();
	@Nullable
	private static InventoryOverlay instance = null;
	@Nullable
	private static BlockPos lastClickedPos = null;
	private final int MISSING_COLOR = Configs.Colors.SCHEMATIC_OVERLAY_COLOR_MISSING.getIntegerValue();
	private final int WRONG_COLOR = Configs.Colors.SCHEMATIC_OVERLAY_COLOR_WRONG_BLOCK.getIntegerValue();
	private final int MISMATCHED_COLOR = Configs.Colors.SCHEMATIC_OVERLAY_COLOR_WRONG_STATE.getIntegerValue();
	private final int EXTRA_COLOR = Configs.Colors.SCHEMATIC_OVERLAY_COLOR_EXTRA.getIntegerValue();
	private final LinkedStorageEntry entry;

	public InventoryOverlay(LinkedStorageEntry entry) {
		this.entry = entry;
	}

	public static void clearOverlay() {
		instance = null;
		lastClickedPos = null;
	}

	public static void onContainerClick(BlockHitResult hitResult) {
		lastClickedPos = hitResult.getBlockPos().immutable();
	}

	public static void onScreenPostContainerClick() {
		if (lastClickedPos == null)
			return;
		BlockPos pos = lastClickedPos;
		Level level = WorldUtils.getBestWorld(Minecraft.getInstance());
		if (!(ContainerUtils.validateContainer(level, pos, level.getBlockState(pos)).orElse(null) instanceof MenuConstructor))
			return;
		if (InteractionHandler.contains(pos))
			return;
		instance = get(pos, true).orElse(null);
	}

	public static Optional<InventoryOverlay> get(BlockPos pos, boolean queueInteraction) {
		Level level = Objects.requireNonNull(WorldUtils.getBestWorld(Minecraft.getInstance()));

		long tick = level.getGameTime();
		BlockState state = level.getBlockState(pos);
		Optional<Container> inventory = ContainerUtils.validateContainer(level, pos, state);
		if (inventory.isEmpty())
			return Optional.empty();

		LinkedStorageEntry entry = PlacementContainerAccess.getEntry(pos, state, null);
		entry.setWorldInventory(inventory.get());
		if (entry.getPlacementInventory().isEmpty())
			return Optional.empty();
		if (queueInteraction) InteractionHandler.add(new InteractionHandler(pos, tick) {
			@Override
			public boolean accept(Screen screen) {
				Slot validSlot = null;
				for (Slot slot : ((MenuAccess<?>) screen).getMenu().slots) {
					if (!(slot.container instanceof Inventory)) {
						validSlot = slot;
						break;
					}
				}
				if (validSlot != null)
					entry.setWorldInventory(validSlot.container);
				return true;
			}
		});
		return Optional.of(new InventoryOverlay(entry));
	}

	public static ItemStack drawStack(GuiGraphics graphics, Slot slot, ItemStack stack) {
		return instance == null ? stack : instance.drawStackInternal(graphics, slot, stack);
	}

	public static void finalizeDrawStack() {
		if (instance != null)
			instance.finalizeDrawStackInternal();
	}

	public static boolean setSlotToSchematicItem(Slot slot) {
		if (!LitematicConfigs.INVENTORY_SCREEN_OVERLAY.getBooleanValue()
			|| instance == null || slot.container instanceof Inventory
		)
			return false;

		var schematicItem = instance.entry.getPlacementInventory().get().getItem(slot.getContainerSlot());
		slot.setByPlayer(ItemPredicateUtils.getPlaceholder(schematicItem) instanceof ItemStack placeholder
			? placeholder
			: schematicItem
		);
		return true;
	}

	public ItemStack drawStackInternal(GuiGraphics graphics, Slot slot, ItemStack stack) {
		if (!LitematicConfigs.INVENTORY_SCREEN_OVERLAY.getBooleanValue()
			|| slot.container instanceof Inventory
			|| slot.container instanceof ResultContainer
			|| this.entry.getWorldInventory().isEmpty()
		)
			return stack;

		if (LitematicConfigs.FORCE_SCHEMATIC_ITEM_OVERLAY.getBooleanValue())
			stack = ItemStack.EMPTY;

		ItemStack schematicStack = this.entry.getPlacementInventory().get().getItem(slot.getContainerSlot());
		if (schematicStack == null) {
			schematicStack = ItemStack.EMPTY;
		}

		int color = 0;
		boolean shouldRenderItemAsTransparent = false;
		if (ItemPredicateUtils.getPredicate(schematicStack) instanceof ItemPredicate predicate) {
			if (stack.isEmpty()) {
				color = this.MISSING_COLOR;
				stack = ItemPredicateUtils.getPlaceholder(schematicStack) instanceof ItemStack placeholder
					? placeholder
					: schematicStack;
				shouldRenderItemAsTransparent = true;
			} else if (!predicate.test(stack)) {
				color = this.WRONG_COLOR;
			}
		} else if (stack.isEmpty() && !schematicStack.isEmpty()) {
			color = this.MISSING_COLOR;
			stack = schematicStack;
			shouldRenderItemAsTransparent = true;
		} else if (!stack.isEmpty() && schematicStack.isEmpty()) {
			color = this.EXTRA_COLOR;
		} else if (!stack.getItem().equals(schematicStack.getItem())) {
			color = this.WRONG_COLOR;
		} else if (stack.getCount() != schematicStack.getCount()) {
			color = this.MISMATCHED_COLOR;
		} else if (LitematicConfigs.VERIFY_ITEM_COMPONENTS.getBooleanValue()
			&& !Objects.equals(schematicStack.getComponents(), stack.getComponents())) {
			color = WRONG_COLOR;
		}

		if (color != 0)
			drawBackground(graphics, slot, color);

		isRenderingTransparentItem = shouldRenderItemAsTransparent;

		return stack;
	}

	public void drawBackground(GuiGraphics graphics, Slot slot, int color) {
		int x = slot.x;
		int y = slot.y;
		graphics.fill(x, y, x + 16, y + 16, color);
	}

	public void finalizeDrawStackInternal() {
		if (LitematicConfigs.INVENTORY_SCREEN_OVERLAY.getBooleanValue() && isRenderingTransparentItem) {
			isRenderingTransparentItem = false;
		}
	}
}
