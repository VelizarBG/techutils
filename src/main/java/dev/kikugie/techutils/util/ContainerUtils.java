package dev.kikugie.techutils.util;

import fi.dy.masa.litematica.util.InventoryUtils;
import fi.dy.masa.malilib.render.InventoryOverlayContext;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TextComponentTagVisitor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.Container;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ContainerUtils {
	public static Optional<Container> validateContainer(Level level, BlockPos pos, BlockState state) {
		if (state.getBlock() instanceof EntityBlock provider) {
			BlockEntity blockEntity;
			if (level != null && InventoryUtils.getTargetInventory(level, pos) instanceof InventoryOverlayContext ctx && ctx.be() instanceof BlockEntity be) {
				blockEntity = be;
			} else {
				blockEntity = provider.newBlockEntity(pos, state);
			}
			if (blockEntity instanceof Container inventory)
				return Optional.of(inventory);
		}
		return Optional.empty();
	}

	public static Optional<Container> validateContainer(BlockPos pos, BlockState state) {
		return validateContainer(null, pos, state);
	}

	public static boolean isChestAccessible(LevelAccessor level, BlockPos pos, BlockState state) {
		assert state.getBlock() instanceof ChestBlock;
		if (ChestBlock.isChestBlockedAt(level, pos))
			return false;

		ChestType type = state.getValue(ChestBlock.TYPE);
		if (type == ChestType.SINGLE)
			return true;

		BlockPos adjacent = pos.offset(ChestBlock.getConnectedDirection(state).getUnitVec3i());
		return level.getBlockState(adjacent).getBlock() != state.getBlock()
			|| !ChestBlock.isChestBlockedAt(level, adjacent);
	}

	public static boolean isShulkerBoxAccessible(LevelAccessor level, BlockPos pos, BlockState state) {
		assert state.getBlock() instanceof ShulkerBoxBlock;
		ShulkerBoxBlockEntity box = (ShulkerBoxBlockEntity) level.getBlockEntity(pos);
		if (box == null || box.getAnimationStatus() != ShulkerBoxBlockEntity.AnimationStatus.CLOSED)
			return true;

		return level.noCollision(Shulker
			.getProgressAabb(1.0F, state.getValue(ShulkerBoxBlock.FACING), 0.0F, new Vec3(0.5, 0.0, 0.5))
			.move(pos).deflate(1.0E-6D));
	}

	public static List<Component> getFormattedComponents(ItemStack stack) {
		var ops = RegistryOps.create(NbtOps.INSTANCE, Minecraft.getInstance().level.registryAccess());
		var lines = new ArrayList<Component>();
		for (TypedDataComponent<?> component : stack.getComponents()) {
			component.encodeValue(ops).mapOrElse(
				nbt -> {
					var text = Component.empty();
					text.withStyle(style -> style.withColor(ChatFormatting.WHITE).withItalic(false));
					text.append(Component.literal(String.valueOf(BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(component.type())))
						.withStyle(style -> style.withColor(ChatFormatting.GRAY))
					);
					text.append(" => ");

					var prettyLines = prettifyNbt(nbt);
					text.append(prettyLines.getFirst());

					lines.add(text);
					lines.addAll(prettyLines.subList(1, prettyLines.size()));

					return null;
				},
				e -> {
					var error = Component.literal(
						"Failed to encode component '%s' - %s"
							.formatted(BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(component.type()), e.message())
					).withStyle(style -> style.withColor(ChatFormatting.RED).withItalic(false));
					lines.add(error);
					return null;
				}
			);
		}
		return lines;
	}

	public static List<Component> prettifyNbt(Tag nbt) {
		var style = Style.EMPTY.withColor(ChatFormatting.WHITE).withItalic(false);
		var lines = new ArrayList<Component>();

		var currentLine = Component.empty().setStyle(style);
		lines.add(currentLine);
		Component prettyPrintedText = new TextComponentTagVisitor("    ").visit(nbt);
		for (Component sibling : prettyPrintedText.getSiblings()) {
			String string = sibling.getString();
			if (string.contains("\n")) {
				var parts = string.split("\n", 2);

				if (!parts[0].isEmpty())
					currentLine.append(Component.literal(parts[0]).setStyle(sibling.getStyle()));

				currentLine = Component.empty().setStyle(style);
				lines.add(currentLine);

				if (!parts[1].isEmpty())
					currentLine.append(Component.literal(parts[1]).setStyle(sibling.getStyle()));
			} else {
				currentLine.append(sibling);
			}
		}

		return lines;
	}
}
