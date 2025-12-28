package dev.kikugie.techutils.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import dev.kikugie.techutils.TechUtilsMod;
import dev.kikugie.techutils.feature.containerscan.verifier.ItemPredicateEntryScreen;
import dev.kikugie.techutils.util.ItemPredicateUtils;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.MixinEnvironment;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class ItemPredicateCommand {
	private static final SimpleCommandExceptionType WRONG_MAIN_HAND_EXCEPTION = new SimpleCommandExceptionType(Component.translatable("commands.itempredicate.placeholder.wrong_main_hand"));
	private static final SimpleCommandExceptionType NO_PLACEHOLDER_FOUND_EXCEPTION = new SimpleCommandExceptionType(Component.translatable("commands.itempredicate.placeholder.get.not_found"));
	private static final SimpleCommandExceptionType NOT_IN_CREATIVE_MODE_EXCEPTION = new SimpleCommandExceptionType(Component.translatable("commands.itempredicate.not_in_creative_mode"));

	public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandBuildContext ignoredContext) {
		dispatcher.register(literal("itempredicate")
			.then(literal("audit")
				.executes(ctx -> {
					MixinEnvironment.getCurrentEnvironment().audit();
					return 1;
				})
			)
			.then(literal("give")
				.executes(context -> {
					var player = context.getSource().getPlayer();

					enforceCreativeMode(player);

					var offHandStack = player.getOffhandItem().copy();
					TechUtilsMod.QUEUED_END_CLIENT_TICK_TASKS.add(client -> client.setScreen(new ItemPredicateEntryScreen(player, offHandStack)));
					return 1;
				})
			)
			.then(literal("edit")
				.executes(context -> {
					var source = context.getSource();
					var player = source.getPlayer();

					enforceCreativeMode(player);

					var mainHandStack = player.getMainHandItem().copy();

					if (!ItemPredicateUtils.isPredicate(mainHandStack)) {
						throw WRONG_MAIN_HAND_EXCEPTION.create();
					}

					var rawPredicate = ItemPredicateUtils.getRawPredicate(mainHandStack);
					var placeholder = ItemPredicateUtils.getPlaceholder(mainHandStack);

					TechUtilsMod.QUEUED_END_CLIENT_TICK_TASKS.add(client -> client.setScreen(new ItemPredicateEntryScreen(context.getSource().getPlayer(), rawPredicate, placeholder)));
					return 1;
				})
			)
			.then(literal("placeholder")
				.then(literal("set")
					.executes(context -> {
						var source = context.getSource();
						var player = source.getPlayer();

						enforceCreativeMode(player);

						var mainHandStack = player.getMainHandItem().copy();
						var offHandStack = player.getOffhandItem().copy();

						if (!ItemPredicateUtils.isPredicate(mainHandStack)) {
							throw WRONG_MAIN_HAND_EXCEPTION.create();
						}

						ItemPredicateUtils.setPlaceholder(mainHandStack, offHandStack);
						int selectedSlot = player.getInventory().getSelectedSlot();
						player.getInventory().setItem(selectedSlot, mainHandStack);
						source.getClient().gameMode.handleCreativeModeItemAdd(mainHandStack, 36 + selectedSlot);
						player.inventoryMenu.broadcastChanges();

						source.sendFeedback(Component.translatable("commands.itempredicate.placeholder.set.success"));

						return 1;
					})
				)
				.then(literal("get")
					.executes(context -> {
						var source = context.getSource();
						var player = source.getPlayer();

						enforceCreativeMode(player);

						var mainHandStack = player.getMainHandItem().copy();

						if (!ItemPredicateUtils.isPredicate(mainHandStack)) {
							throw WRONG_MAIN_HAND_EXCEPTION.create();
						}

						if (!(ItemPredicateUtils.getPlaceholder(mainHandStack) instanceof ItemStack placeholder)) {
							throw NO_PLACEHOLDER_FOUND_EXCEPTION.create();
						}

						player.getInventory().setItem(Inventory.SLOT_OFFHAND, placeholder);
						source.getClient().gameMode.handleCreativeModeItemAdd(placeholder, 45);
						player.inventoryMenu.broadcastChanges();

						source.sendFeedback(Component.translatable("commands.itempredicate.placeholder.get.success"));

						return 1;
					})
				)
			)
		);
	}

	private static void enforceCreativeMode(LocalPlayer player) throws CommandSyntaxException {
		if (!player.isCreative())
			throw NOT_IN_CREATIVE_MODE_EXCEPTION.create();
	}
}
