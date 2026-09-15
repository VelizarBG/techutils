package dev.kikugie.techutils.util;

import com.google.common.collect.MapMaker;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JavaOps;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.criterion.ItemPredicate;
import net.minecraft.advancements.criterion.MinMaxBounds;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.predicates.DataComponentPredicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

public final class ItemPredicateUtils {
	public static final int VERSION = 1;
	public static final String VERSION_ID = "version";
	public static final String ROOT_PREDICATE_ID = "techutils:item_predicate";
	public static final String RAW_PREDICATE_ID = "predicate";
	public static final String PLACEHOLDER_ID = "placeholder";
	public static final String ITEM_PREDICATE_WITHIN_PREDICATE = "predicate";
	public static final String STYLE_MARKER = ROOT_PREDICATE_ID;
	private static final ConcurrentMap<ItemStack, Predicate> PREDICATE_PER_STACK = new MapMaker().weakKeys().makeMap();
	private static final ConcurrentMap<CompoundTag, Predicate> PREDICATE_PER_ROOT_PREDICATE = new MapMaker().weakValues().makeMap();

	private record Predicate(ItemPredicate predicate, List<Component> prettyPredicate) {}

	private ItemPredicateUtils() {}

	public static ItemStack makePredicateStack(String rawPredicateString, ItemStack stack, ItemStack placeholder) {
		if (stack.isEmpty())
			return ItemStack.EMPTY;

		setPlaceholder(stack, placeholder);

		DataResult<CompoundTag> rawPredicate = TagParser.FLATTENED_CODEC.parse(JavaOps.INSTANCE, rawPredicateString);
		if (rawPredicate.isSuccess()) {
			stack.update(
				DataComponents.CUSTOM_DATA,
				CustomData.EMPTY,
				customData -> customData.update(data -> {
					var root = data.getCompoundOrEmpty(ROOT_PREDICATE_ID);
					root.put(RAW_PREDICATE_ID, rawPredicate.getOrThrow());
					root.putInt(VERSION_ID, VERSION);
					data.put(ROOT_PREDICATE_ID, root);
				})
			);
		} else {
			throw new RuntimeException("Failed to parse predicate NBT: " + rawPredicate.error().get().message());
		}

		var style = Style.EMPTY.withColor(ChatFormatting.WHITE).withItalic(false).withInsertion(STYLE_MARKER);
		stack.set(DataComponents.CUSTOM_NAME, Component.literal("Item Predicate").setStyle(style));
		stack.set(DataComponents.LORE, ItemLore.EMPTY.withLineAdded(
			Component.literal("Install the techutils mod for Item Predicate support.").setStyle(style)));

		return stack;
	}

	public static boolean isPredicate(ItemStack stack) {
		return stack.get(DataComponents.CUSTOM_DATA) instanceof CustomData customData
			&& customData.copyTag().contains(ROOT_PREDICATE_ID);
	}

	public static void modifyTooltip(ItemStack stack, List<Component> lines) {
		if (!isPredicate(stack))
			return;

		lines.removeIf(text -> Objects.equals(text.getStyle().getInsertion(), STYLE_MARKER)
			|| text.getContents() instanceof TranslatableContents contents && contents.getKey().contains("op_warning"));
		lines.addAll(getPrettyPredicate(stack));
	}

	public static @Nullable CompoundTag getRawPredicate(ItemStack stack) {
		return getRootPredicate(stack) instanceof CompoundTag rootPredicate
			? rootPredicate.getCompound(RAW_PREDICATE_ID).orElse(null)
			: null;
	}

	public static @Nullable ItemPredicate getItemPredicate(ItemStack stack) {
		return getPredicate(stack) instanceof Predicate predicate ? predicate.predicate() : null;
	}

	public static List<Component> getPrettyPredicate(ItemStack predicateStack) {
		var predicate = getPredicate(predicateStack);
		if (predicate == null) {
			return List.of();
		}

		return Collections.unmodifiableList(predicate.prettyPredicate());
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

	public static @Nullable ItemStack getPlaceholder(ItemStack stack) {
		CompoundTag root;
		if (!(stack.get(DataComponents.CUSTOM_DATA) instanceof CustomData customData)
			|| (root = customData.copyTag().getCompound(ROOT_PREDICATE_ID).orElse(null)) == null
		) {
			return null;
		}
		var ops = Minecraft.getInstance().level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
		return root.read(PLACEHOLDER_ID, ItemStack.CODEC, ops).orElse(null);
	}

	public static void setPlaceholder(ItemStack predicateStack, ItemStack placeholder) {
		if (placeholder == null || placeholder.isEmpty()) {
			if (predicateStack.get(DataComponents.CUSTOM_DATA) instanceof CustomData customData) {
				customData.update(data -> data.getCompoundOrEmpty(ROOT_PREDICATE_ID).remove(PLACEHOLDER_ID));
			}
		} else {
			var ops = Minecraft.getInstance().level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
			predicateStack.update(DataComponents.CUSTOM_DATA, CustomData.EMPTY, customData ->
				customData.update(data -> {
					var root = data.getCompoundOrEmpty(ROOT_PREDICATE_ID);
					root.store(PLACEHOLDER_ID, ItemStack.CODEC, ops, placeholder);
					data.put(ROOT_PREDICATE_ID, root);
				})
			);
		}
	}

	private static @Nullable CompoundTag getRootPredicate(ItemStack stack) {
		return stack.get(DataComponents.CUSTOM_DATA) instanceof CustomData data
			? data.copyTag().getCompound(ROOT_PREDICATE_ID).orElse(null)
			: null;
	}

	private static @Nullable Predicate getPredicate(ItemStack stack) {
		if (PREDICATE_PER_STACK.get(stack) instanceof Predicate predicate) {
			return predicate;
		}

		if (getRootPredicate(stack) instanceof CompoundTag rootPredicate) {
			Predicate predicate = getPredicate(rootPredicate);
			PREDICATE_PER_STACK.put(stack, predicate);
			return predicate;
		}
		return null;
	}

	private static Predicate getPredicate(CompoundTag rootPredicate) {
		if (PREDICATE_PER_ROOT_PREDICATE.get(rootPredicate) instanceof Predicate predicate) {
			return predicate;
		}

		rootPredicate = rootPredicate.copy();
		DataResult<ItemPredicate> itemPredicate;
		if (!rootPredicate.contains(RAW_PREDICATE_ID)) {
			itemPredicate = DataResult.error(() -> "Missing raw predicate (key '" + RAW_PREDICATE_ID + "' in '" + ROOT_PREDICATE_ID + "')");
		} else {
			Optional<CompoundTag> rawPredicate = rootPredicate.getCompound(RAW_PREDICATE_ID);
			if (rawPredicate.isEmpty() || !rawPredicate.get().contains(ITEM_PREDICATE_WITHIN_PREDICATE)) {
				itemPredicate = DataResult.error(() -> "No item predicate found");
			} else {
				var ops = Minecraft.getInstance().level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
				itemPredicate = ItemPredicate.CODEC.parse(ops, rawPredicate.get().get(ITEM_PREDICATE_WITHIN_PREDICATE));
			}
		}
		Predicate predicate;
		if (itemPredicate.isSuccess()) {
			var cleanRoot = rootPredicate.copy();
			cleanRoot.put(RAW_PREDICATE_ID, cleanRoot.getCompoundOrEmpty(RAW_PREDICATE_ID).getCompoundOrEmpty(ITEM_PREDICATE_WITHIN_PREDICATE));
			predicate = new Predicate(itemPredicate.getOrThrow(), ContainerUtils.prettifyNbt(cleanRoot));
		} else {
			predicate = getFailedPredicate(itemPredicate.error().get().message());
		}
		PREDICATE_PER_ROOT_PREDICATE.put(rootPredicate, predicate);
		return predicate;
	}

	private static Predicate getFailedPredicate(String message) {
		var title = Component.literal("Could not parse item predicate!")
			.withStyle(style -> style.withColor(ChatFormatting.RED).withItalic(false));
		var lines = new ArrayList<Component>();
		lines.add(title);
		for (String line : message.split("\n")) {
			lines.add(Component.literal(line)
				.withStyle(style -> style.withColor(ChatFormatting.RED).withItalic(false)));
		}
		var markerPredicate = ItemPredicate.Builder.item().withCount(MinMaxBounds.Ints.exactly(-1)).build();
		return new Predicate(markerPredicate, lines);
	}
}
