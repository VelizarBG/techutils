package dev.kikugie.techutils.mixin.compactscoreboard;

import dev.kikugie.techutils.config.MiscConfigs;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Gui;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.chat.numbers.NumberFormatType;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.Locale;

@NullMarked
@Mixin(Gui.class)
public class GuiMixin {
	@Unique
	private static final java.text.NumberFormat FORMATTER = java.text.NumberFormat.getCompactNumberInstance(Locale.US, java.text.NumberFormat.Style.SHORT);
	@Unique
	private static final NumberFormat COMPACT = new NumberFormat() {
		@Override
		public MutableComponent format(int number) {
			return Component.literal(FORMATTER.format(number)).withStyle(ChatFormatting.RED);
		}

		@Override
		public NumberFormatType<? extends NumberFormat> type() {
			return null;
		}
	};

	static {
		FORMATTER.setMaximumFractionDigits(1);
	}

	@ModifyArg(method = "displayScoreboardSidebar", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/scores/Objective;numberFormatOrDefault(Lnet/minecraft/network/chat/numbers/NumberFormat;)Lnet/minecraft/network/chat/numbers/NumberFormat;"))
	private NumberFormat replaceWithCompactFormat(NumberFormat format) {
		return MiscConfigs.COMPACT_SCOREBOARD.getBooleanValue() ? COMPACT : format;
	}
}
