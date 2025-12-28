package dev.kikugie.techutils.mixin.containerscan;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import dev.kikugie.techutils.feature.containerscan.verifier.InventoryOverlay;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CrafterScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.CrafterMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CrafterScreen.class)
public abstract class CrafterScreenMixin extends AbstractContainerScreen<CrafterMenu> {
	public CrafterScreenMixin(CrafterMenu handler, Inventory inventory, Component title) {
		super(handler, inventory, title);
	}

	/**
	 * This method complements {@link AbstractContainerScreenMixin#tryDrawTooltipOfSchematicItem(boolean, LocalRef)}
	 * because without it the "Click to disable slot" tooltip will clash with the forced schematic item tooltip.
	 */
	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/CrafterScreen;renderTooltip(Lnet/minecraft/client/gui/GuiGraphics;II)V"))
	private void tryDrawTooltipOfMissingItem(CallbackInfo ci, @Share("didSetItem") LocalBooleanRef didSetItem) {
		if (hoveredSlot != null && hoveredSlot.getItem().isEmpty()
			&& InventoryOverlay.setSlotToSchematicItem(hoveredSlot)
		) {
			didSetItem.set(true);
		}
	}

	@Inject(method = "render", at = @At("TAIL"))
	private void trySetFocusedSlotBackToEmpty(CallbackInfo ci, @Share("didSetItem") LocalBooleanRef didSetItem) {
		if (didSetItem.get()) {
			hoveredSlot.setByPlayer(ItemStack.EMPTY);
		}
	}
}
