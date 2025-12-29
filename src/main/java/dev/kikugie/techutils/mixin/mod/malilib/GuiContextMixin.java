package dev.kikugie.techutils.mixin.mod.malilib;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.kikugie.techutils.feature.containerscan.verifier.InventoryOverlay;
import fi.dy.masa.malilib.render.GuiContext;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(GuiContext.class)
public class GuiContextMixin {
	@ModifyReturnValue(method = "itemTooltips", at = @At("RETURN"))
	private static List<Component> addSpecialTooltipModifications(List<Component> original, @Local(argsOnly = true) ItemStack stack) {
		InventoryOverlay.addSpecialTooltipModifications(stack, original);

		return original;
	}
}
