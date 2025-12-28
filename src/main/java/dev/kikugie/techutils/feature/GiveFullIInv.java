package dev.kikugie.techutils.feature;

import dev.kikugie.techutils.TechUtilsMod;
import dev.kikugie.techutils.config.MiscConfigs;
import fi.dy.masa.malilib.util.game.BlockUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Inserts a full container of given item into player's hand. Works <b>only</b> in creative mode.
 */
public class GiveFullIInv {
	private static final GiveFullIInv INSTANCE = new GiveFullIInv();
	private static final Supplier<Boolean> SAFETY = MiscConfigs.FILL_SAFETY::getBooleanValue;

	public static boolean onKeybind() {
		LocalPlayer player = Minecraft.getInstance().player;
		assert player != null;
		if (!player.isCreative()) {
			INSTANCE.sendError("not_creative_enough");
			return false;
		}

		ItemStack mainHand = player.getMainHandItem();
		ItemStack offHand = player.getOffhandItem();

		Optional<ItemStack> result = get(mainHand, offHand);
		if (result.isEmpty())
			return false;
		int selectedSlot = player.getInventory().getSelectedSlot();
		player.getInventory().setItem(selectedSlot, result.get());
		Objects.requireNonNull(Minecraft.getInstance().gameMode).handleCreativeModeItemAdd(result.get(), 36 + selectedSlot);
		player.inventoryMenu.broadcastChanges();
		return true;
	}

	public static Optional<ItemStack> get(ItemStack mainHand, ItemStack offHand) {
		return INSTANCE.getItem(mainHand, offHand);
	}

	public static ItemStack fillShulker(ItemStack stack, @Nullable DyeColor color) {
		Block shulker = ShulkerBoxBlock.getBlockByColor(color);
		ShulkerBoxBlockEntity box = new ShulkerBoxBlockEntity(BlockPos.ZERO, shulker.defaultBlockState());
		return fillLootable(stack, shulker.asItem(), box);
	}

	public static ItemStack fillChest(ItemStack stack) {
		Block chest = Blocks.CHEST;
		ChestBlockEntity box = new ChestBlockEntity(BlockPos.ZERO, chest.defaultBlockState());
		return fillLootable(stack, chest.asItem(), box);
	}

	public static ItemStack fillLootable(ItemStack stack, Item item, RandomizableContainerBlockEntity lootable) {
		for (int i = 0; i < lootable.getContainerSize(); i++) {
			lootable.setItem(i, stack);
		}
		ItemStack container = item.getDefaultInstance();
		BlockUtils.setStackNbt(container, lootable, Minecraft.getInstance().level.registryAccess());
		return container;
	}

	public static ItemStack fillBundle(ItemStack stack) {
		List<ItemStack> stacks = new ArrayList<>();
		for (int i = 0; i < MiscConfigs.BUNDLE_FILL.getIntegerValue(); i++) {
			stacks.add(stack.copy());
		}
		ItemStack bundle = Items.BUNDLE.getDefaultInstance();
		BundleContents.Mutable builder = new BundleContents.Mutable(new BundleContents(stacks));
		bundle.set(DataComponents.BUNDLE_CONTENTS, builder.toImmutable());
		return bundle;
	}

	@SuppressWarnings("DataFlowIssue")
	public static boolean containerHasItems(ItemStack container) {
		if (container.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof EntityBlock provider) {
			var containerData = container.get(DataComponents.CONTAINER);
			if (containerData == null)
				return false;

			BlockEntity blockEntity = provider.newBlockEntity(BlockPos.ZERO, blockItem.getBlock().defaultBlockState());
			blockEntity.applyComponentsFromItemStack(container);
			if (blockEntity instanceof Container inventory)
				return !inventory.isEmpty();
		}
		return false;
	}

	public static boolean bundleHasItems(ItemStack bundle) {
		return BundleItem.getFullnessDisplay(bundle) > 0;
	}

	private static String generateCommand(ItemStack stack, int slot) {
		String template = "item replace entity @s container.%s with %s 1";
		return "";
	}

	private Optional<ItemStack> getItem(ItemStack mainHand, ItemStack offHand) {
		if (mainHand.isEmpty()) {
			sendError("no_item");
			return Optional.empty();
		}

		return isShulkerBox(mainHand) ? handleBox(mainHand, offHand) : handleItem(mainHand, offHand);
	}

	private Optional<ItemStack> handleItem(ItemStack mainHand, ItemStack offHand) {
		if (!SAFETY.get() && !recursionCheck(mainHand)) {
			sendError("nested_stack");
			return Optional.empty();
		}
		ItemStack fullStack = mainHand.copyWithCount(mainHand.getMaxStackSize());
		return Optional.of(handleOffHand(offHand, stack -> fillShulker(stack, null)).apply(fullStack));
	}

	private Optional<ItemStack> handleBox(ItemStack mainHand, ItemStack offHand) {
		if (!SAFETY.get() && isShulkerBox(offHand)) {
			sendError("nested_box");
			return Optional.empty();
		}
		ItemStack fullStack = containerHasItems(mainHand) ? mainHand.copy() : mainHand.copyWithCount(64);
		return Optional.of(handleOffHand(offHand, GiveFullIInv::fillChest).apply(fullStack));
	}

	private boolean recursionCheck(ItemStack mainHand) {
		if (mainHand.getItem() instanceof BundleItem)
			return !bundleHasItems(mainHand);
		return !containerHasItems(mainHand);
	}

	private Function<ItemStack, ItemStack> handleOffHand(ItemStack offHand, @Nullable Function<ItemStack, ItemStack> fallback) {
		if (offHand.isEmpty())
			return fallback;
		// Item has a corresponding block entity
		if (offHand.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof EntityBlock provider) {
			BlockEntity blockEntity = provider.newBlockEntity(BlockPos.ZERO, blockItem.getBlock().defaultBlockState());
			// Block entity is a container
			if (blockEntity instanceof RandomizableContainerBlockEntity lootable)
				return stack -> fillLootable(stack, blockItem, lootable);
		}
		if (offHand.getItem() instanceof BundleItem) {
			return GiveFullIInv::fillBundle;
		}
		// Some other item
		return fallback;
	}

	private void sendError(String key) {
		Component message = Component.translatable("techutils.feature.givefullinv." + key).withStyle(ChatFormatting.DARK_RED);
		if (Minecraft.getInstance() != null && Minecraft.getInstance().player != null)
			Minecraft.getInstance().player.displayClientMessage(message, true);
		else
			TechUtilsMod.LOGGER.warn(message.getString());
	}

	private boolean isShulkerBox(ItemStack stack) {
		return stack.getItem().toString().contains("shulker_box");
	}
}
