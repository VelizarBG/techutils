package dev.kikugie.techutils.mixin.containerscan;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import dev.kikugie.techutils.config.LitematicConfigs;
import dev.kikugie.techutils.feature.containerscan.verifier.InventoryOverlay;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AbstractContainerScreen.class)
public class AbstractContainerScreenMixin {

	@Shadow @Nullable protected Slot hoveredSlot;

	@ModifyExpressionValue(method = "renderSlot", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/Slot;getItem()Lnet/minecraft/world/item/ItemStack;", ordinal = 0))
	private ItemStack injectTransparency(ItemStack stack, @Local(argsOnly = true) GuiGraphics graphics, @Local(argsOnly = true) Slot slot) {
		return InventoryOverlay.drawStack(graphics, slot, stack);
	}

	@Inject(method = "renderSlot", at = @At("RETURN"))
	private void finalizeDraw(CallbackInfo ci) {
		InventoryOverlay.finalizeDrawStack();
	}

	@ModifyExpressionValue(method = "renderTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/Slot;hasItem()Z"))
	private boolean tryDrawTooltipOfSchematicItem(boolean hasStack, @Share("prevItem") LocalRef<ItemStack> prevItemRef) {
		var prevItem = hoveredSlot.getItem();
		if ((!hasStack || LitematicConfigs.FORCE_SCHEMATIC_ITEM_OVERLAY.getBooleanValue())
			&& InventoryOverlay.setSlotToSchematicItem(hoveredSlot)
		) {
			hasStack = hoveredSlot.hasItem();
			if (hasStack) {
				prevItemRef.set(prevItem);
			} else {
				hoveredSlot.setByPlayer(prevItem);
			}
		}
		return hasStack;
	}

	@Inject(method = "renderTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/Slot;getItem()Lnet/minecraft/world/item/ItemStack;", shift = At.Shift.AFTER))
	private void trySetFocusedSlotBackToPrev(CallbackInfo ci, @Share("prevItem") LocalRef<ItemStack> prevItemRef) {
		var prevItem = prevItemRef.get();
		if (prevItem != null) {
			hoveredSlot.setByPlayer(prevItem);
		}
	}
}
