package dev.kikugie.techutils.mixin.compactscoreboard;

import dev.kikugie.techutils.config.MiscConfigs;
import net.minecraft.client.gui.Gui;
import net.minecraft.network.chat.numbers.NumberFormatType;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.text.NumberFormat;
import java.util.Locale;

@Mixin(Gui.class)
public class GuiMixin {
	@Unique
	private static final NumberFormat FORMATTER = NumberFormat.getCompactNumberInstance(Locale.US, NumberFormat.Style.SHORT);

	static {
		FORMATTER.setMaximumFractionDigits(1);
	}

	@SuppressWarnings("unchecked")
	@ModifyArg(method = "displayScoreboardSidebar(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/scores/Objective;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/scores/Objective;numberFormatOrDefault(Lnet/minecraft/network/chat/numbers/NumberFormat;)Lnet/minecraft/network/chat/numbers/NumberFormat;"))
	private <T extends net.minecraft.network.chat.numbers.NumberFormat> T replaceWithCompactFormat(T format) {
		if (!MiscConfigs.COMPACT_SCOREBOARD.getBooleanValue())
			return format;

		return (T) new net.minecraft.network.chat.numbers.NumberFormat() {
			@Override
			public MutableComponent format(int number) {
				return Component.literal(FORMATTER.format(number)).withStyle(ChatFormatting.RED);
			}

			@Override
			public NumberFormatType<T> type() {
				return null;
			}
		};
	}
}
