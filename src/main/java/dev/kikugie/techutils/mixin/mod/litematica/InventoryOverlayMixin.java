package dev.kikugie.techutils.mixin.mod.litematica;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static dev.kikugie.techutils.feature.containerscan.verifier.InventoryOverlay.delayRenderingHoveredStack;
import static dev.kikugie.techutils.feature.containerscan.verifier.InventoryOverlay.hoveredStackToRender;
import static dev.kikugie.techutils.feature.containerscan.verifier.InventoryOverlay.infoOverlayInstance;

@Mixin(value = fi.dy.masa.malilib.render.InventoryOverlay.class)
public class InventoryOverlayMixin {
	@WrapOperation(method = "renderInventoryStacks(Lnet/minecraft/client/gui/GuiGraphics;Lfi/dy/masa/malilib/render/InventoryOverlay$InventoryRenderType;Lnet/minecraft/world/Container;IIIIILjava/util/Set;Lnet/minecraft/client/Minecraft;DD)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/Container;getItem(I)Lnet/minecraft/world/item/ItemStack;"))
	private static ItemStack shareSlotIndex(Container instance, int i, Operation<ItemStack> original, @Share("slotIndex") LocalIntRef slotIndex) {
		slotIndex.set(i);
		return original.call(instance, i);
	}

	@WrapOperation(method = "renderInventoryStacks(Lnet/minecraft/client/gui/GuiGraphics;Lfi/dy/masa/malilib/render/InventoryOverlay$InventoryRenderType;Lnet/minecraft/world/Container;IIIIILjava/util/Set;Lnet/minecraft/client/Minecraft;DD)V", at = @At(value = "INVOKE", target = "Lfi/dy/masa/malilib/render/InventoryOverlay;renderStackAt(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/item/ItemStack;FFFLnet/minecraft/client/Minecraft;DD)V"))
	private static void drawOverlay(GuiGraphics graphics, ItemStack stack, float x, float y, float scale, Minecraft mc, double mouseX, double mouseY, Operation<Void> original, @Share("slotIndex") LocalIntRef slotIndex) {
		if (infoOverlayInstance != null) {
			stack = infoOverlayInstance.drawStackInternal(graphics, new Slot(null, slotIndex.get(), (int) x, (int) y), stack);

			original.call(graphics, stack, x, y, scale, mc, mouseX, mouseY);

			infoOverlayInstance.finalizeDrawStackInternal();
		} else {
			original.call(graphics, stack, x, y, scale, mc, mouseX, mouseY);
		}
	}

	@Redirect(method = "renderInventoryStacks(Lnet/minecraft/client/gui/GuiGraphics;Lfi/dy/masa/malilib/render/InventoryOverlay$InventoryRenderType;Lnet/minecraft/world/Container;IIIIILjava/util/Set;Lnet/minecraft/client/Minecraft;DD)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z"))
	private static boolean allowDrawingEmptySlots(ItemStack instance) {
		return false;
	}

	@WrapWithCondition(method = "renderInventoryStacks(Lnet/minecraft/client/gui/GuiGraphics;Lfi/dy/masa/malilib/render/InventoryOverlay$InventoryRenderType;Lnet/minecraft/world/Container;IIIIILjava/util/Set;Lnet/minecraft/client/Minecraft;DD)V", at = @At(value = "INVOKE", target = "Lfi/dy/masa/malilib/render/InventoryOverlay;renderStackToolTipStyled(Lnet/minecraft/client/gui/GuiGraphics;IILnet/minecraft/world/item/ItemStack;Lnet/minecraft/client/Minecraft;)V"))
	private static boolean delayRenderingHoveredStack(GuiGraphics graphics, int x, int y, ItemStack stack, Minecraft mc) {
		if (delayRenderingHoveredStack) {
			hoveredStackToRender = stack;
			return false;
		}
		return true;
	}

	@Inject(method = "renderInventoryStacks(Lnet/minecraft/client/gui/GuiGraphics;Lfi/dy/masa/malilib/render/InventoryOverlay$InventoryRenderType;Lnet/minecraft/world/Container;IIIIILjava/util/Set;Lnet/minecraft/client/Minecraft;DD)V", at = @At("RETURN"))
	private static void cleanUp(CallbackInfo ci) {
		infoOverlayInstance = null;
	}
}
