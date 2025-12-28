package dev.kikugie.techutils.util;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.TagParser;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.core.component.predicates.DataComponentPredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ProblemReporter;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public final class ItemPredicateUtils {
	public static final String PREDICATE_ID = "techutils:item_predicate";
	private static final Map<String, ItemPredicate> PREDICATE_CACHE = new HashMap<>();
	private static final Reference2ReferenceOpenHashMap<ItemPredicate, List<Component>> PRETTIFIED_PREDICATES = new Reference2ReferenceOpenHashMap<>();

	private ItemPredicateUtils() {}

	public static ItemStack createPredicateStack(String rawPredicate, ItemStack placeholder) {
		TagValueOutput nbtOutput = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
		ItemStack stack = Items.COMMAND_BLOCK.getDefaultInstance();

		nbtOutput.putString("Command", rawPredicate);
		BlockItem.setBlockEntityData(stack, BlockEntityType.COMMAND_BLOCK, nbtOutput);

		setPlaceholder(stack, placeholder);

		stack.update(
			DataComponents.CUSTOM_DATA,
			CustomData.EMPTY,
			customData -> customData.update(custom -> custom.put(PREDICATE_ID, new CompoundTag()))
		);

		stack.set(DataComponents.CUSTOM_NAME, Component.literal("Item Predicate")
			.withStyle(style -> style.withColor(ChatFormatting.WHITE).withItalic(false))
		);

		return stack;
	}

	public static boolean isPredicate(ItemStack stack) {
		return stack.getItem() == Items.COMMAND_BLOCK
			&& stack.get(DataComponents.CUSTOM_DATA) instanceof CustomData customData
			&& customData.copyTag().contains(PREDICATE_ID);
	}

	public static String getRawPredicate(ItemStack stack) {
		return stack.get(DataComponents.BLOCK_ENTITY_DATA) instanceof TypedEntityData<BlockEntityType<?>> data
			? data.copyTagWithoutId().getString("Command").orElse("")
			: "";
	}

	public static @Nullable ItemPredicate getPredicate(ItemStack stack) {
		if (!isPredicate(stack)) {
			return null;
		}

		var rawPredicate = getRawPredicate(stack);
		return getPredicate(rawPredicate);
	}

	public static ItemPredicate getPredicate(String rawPredicate) {
		if (PREDICATE_CACHE.containsKey(rawPredicate)) {
			return PREDICATE_CACHE.get(rawPredicate);
		}

		int startingTokenIndex = rawPredicate.indexOf('{');
		if (startingTokenIndex == -1)
			return saveFailedPredicate(rawPredicate, "No item predicate is present!");

		rawPredicate = rawPredicate.substring(startingTokenIndex);

		CompoundTag nbt;
		try {
			nbt = TagParser.parseCompoundFully(rawPredicate).getCompound("predicate").orElseGet(CompoundTag::new);
			if (nbt.isEmpty()) {
				throw new IllegalArgumentException("No item predicate is present!");
			}
		} catch (Throwable throwable) {
			return saveFailedPredicate(rawPredicate, throwable.getMessage());
		}
		var result = ItemPredicate.CODEC.parse(RegistryOps.create(NbtOps.INSTANCE, Minecraft.getInstance().level.registryAccess()), nbt);
		if (result.isSuccess()) {
			var predicate = result.getOrThrow();
			PREDICATE_CACHE.put(rawPredicate, predicate);
			PRETTIFIED_PREDICATES.put(predicate, ContainerUtils.prettifyNbt(nbt));

			return predicate;
		} else {
			return saveFailedPredicate(rawPredicate, result.error().get().message());
		}
	}

	public static List<Component> getPrettyPredicate(ItemStack predicateStack) {
		var predicate = ItemPredicateUtils.getPredicate(predicateStack);
		if (predicate == null) {
			return List.of();
		}

		if (ItemPredicateUtils.getPlaceholder(predicateStack) instanceof ItemStack placeholder) {
			var nbt = new CompoundTag();
			var registryAccess = Minecraft.getInstance().level.registryAccess();
			nbt.put("placeholder", toNbtAllowEmpty(placeholder, registryAccess));
			var lines = new ArrayList<>(PRETTIFIED_PREDICATES.get(predicate));
			lines.addAll(ContainerUtils.prettifyNbt(nbt));
			return lines;
		} else {
			return Collections.unmodifiableList(PRETTIFIED_PREDICATES.get(predicate));
		}
	}

	public static List<Component> getErrorLines(ItemStack stack, ItemPredicate predicate) {
		var lines = new ArrayList<Component>();
		var items = predicate.items();
		var count = predicate.count();
		var components = predicate.components();

		if (items.isPresent() && !stack.is(items.get())) {
			var msg = Component.literal("Incorrect item type. Expected: ")
				.withStyle(style -> style.withColor(ChatFormatting.RED).withItalic(false));
			items.get().stream()
				.flatMap(i -> Stream.of(Component.literal(", "), Component.literal(i.getRegisteredName())))
				.skip(1)
				.forEach(msg::append);
			lines.add(msg);
		}

		if (!count.matches(stack.getCount())) {
			var min = count.bounds().min();
			var max = count.bounds().max();
			var msg = Component.literal("Incorrect count. Expected: ")
				.withStyle(style -> style.withColor(ChatFormatting.RED).withItalic(false));
			if (min.isPresent() && max.isPresent() && min.get().equals(max.get())) {
				msg.append(Component.literal(min.get().toString()));
			} else {
				if (min.isPresent()) {
					msg.append("at least " + min.get());
					if (max.isPresent()) {
						msg.append(" and ");
					}
				}
				max.ifPresent(i -> msg.append("at most " + i));
			}
			lines.add(msg);
		}

		var wrongComponents = new ArrayList<DataComponentType<?>>();
		for (Map.Entry<DataComponentType<?>, Optional<?>> entry : components.exact().asPatch().entrySet()) {
			DataComponentType<?> type = entry.getKey();
			if (!Objects.equals(entry.getValue().orElse(null), stack.get(type))) {
				wrongComponents.add(type);
			}
		}
		if (!wrongComponents.isEmpty()) {
			var msg = Component.literal("Wrong/missing components: ")
				.withStyle(style -> style.withColor(ChatFormatting.RED).withItalic(false));
			wrongComponents.stream()
				.flatMap(t -> Stream.of(Component.literal(", "), Component.literal(Util.getRegisteredName(BuiltInRegistries.DATA_COMPONENT_TYPE, t))))
				.skip(1)
				.forEach(msg::append);
			lines.add(msg);
		}

		var wrongSubPredicates = new ArrayList<DataComponentPredicate.Type<?>>();
		for (Map.Entry<DataComponentPredicate.Type<?>, DataComponentPredicate> entry : components.partial().entrySet()) {
			if(!entry.getValue().matches(stack)) {
				wrongSubPredicates.add(entry.getKey());
			}
		}
		if (!wrongSubPredicates.isEmpty()) {
			var msg = Component.literal("Failed sub-predicates: ")
				.withStyle(style -> style.withColor(ChatFormatting.RED).withItalic(false));
			wrongSubPredicates.stream()
				.flatMap(t -> Stream.of(Component.literal(", "), Component.literal(Util.getRegisteredName(BuiltInRegistries.DATA_COMPONENT_PREDICATE_TYPE, t))))
				.skip(1)
				.forEach(msg::append);
			lines.add(msg);
		}

		return lines;
	}

	@Nullable
	public static ItemStack getPlaceholder(ItemStack stack) {
		return isPredicate(stack) && stack.get(DataComponents.CONTAINER) instanceof ItemContainerContents contents
			? contents.copyOne()
			: null;
	}

	public static void setPlaceholder(ItemStack predicateStack, ItemStack placeholder) {
		if (placeholder == null || placeholder.isEmpty()) {
			predicateStack.remove(DataComponents.CONTAINER);
		} else {
			predicateStack.set(
				DataComponents.CONTAINER,
				ItemContainerContents.fromItems(List.of(placeholder))
			);
		}
	}

	private static ItemPredicate saveFailedPredicate(String rawPredicate, String message) {
		var markerPredicate = ItemPredicate.Builder.item().withCount(MinMaxBounds.Ints.exactly(-1)).build();
		PREDICATE_CACHE.put(rawPredicate, markerPredicate);

		var title = Component.literal("Could not parse item predicate!")
			.withStyle(style -> style.withColor(ChatFormatting.RED).withItalic(false));
		var lines = new ArrayList<Component>();
		lines.add(title);
		for (String line : message.split("\n")) {
			lines.add(Component.literal(line)
				.withStyle(style -> style.withColor(ChatFormatting.RED).withItalic(false)));
		}
		PRETTIFIED_PREDICATES.put(markerPredicate, lines);
		return markerPredicate;
	}

	private static Tag toNbtAllowEmpty(ItemStack stack, HolderLookup.Provider registries) {
		return stack.isEmpty() ? new CompoundTag() : ItemStack.CODEC.encode(stack, registries.createSerializationContext(NbtOps.INSTANCE), new CompoundTag()).getOrThrow();
	}
}
