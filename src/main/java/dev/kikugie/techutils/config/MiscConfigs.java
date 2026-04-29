package dev.kikugie.techutils.config;

import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigBooleanHotkeyed;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.config.options.ConfigInteger;

public class MiscConfigs extends Configs.BaseConfigs {
	public static final ConfigHotkey OPEN_CONFIG = new ConfigHotkey("openConfig", "U, C")
		.apply(Configs.CONFIG_KEY);
	public static final ConfigBooleanHotkeyed COMPACT_SCOREBOARD = new ConfigBooleanHotkeyed("compactScoreboard", false, "F6")
		.apply(Configs.CONFIG_KEY);
	public static final ConfigHotkey GIVE_FULL_INV = new ConfigHotkey("giveFullInv", "G")
		.apply(Configs.CONFIG_KEY);
	public static final ConfigInteger BUNDLE_FILL = new ConfigInteger("bundleFill", 1, 1, 100, true)
		.apply(Configs.CONFIG_KEY);
	public static final ConfigBoolean FILL_SAFETY = new ConfigBoolean("fillSafety", true)
		.apply(Configs.CONFIG_KEY);
	public static final ConfigHotkey SCAN_INVENTORY = new ConfigHotkey("scanInventory", "I")
		.apply(Configs.CONFIG_KEY);
	public static final ConfigInteger REQUEST_TIMEOUT = new ConfigInteger("requestTimeout", 60, 1, 1000, false)
		.apply(Configs.CONFIG_KEY);

	public MiscConfigs() {
		super(ImmutableList.of(
			OPEN_CONFIG,
			COMPACT_SCOREBOARD,
			GIVE_FULL_INV,
			BUNDLE_FILL,
			FILL_SAFETY
		));
	}
}
