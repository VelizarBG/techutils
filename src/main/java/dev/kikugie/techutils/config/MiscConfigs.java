package dev.kikugie.techutils.config;

import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigBooleanHotkeyed;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.config.options.ConfigInteger;

public class MiscConfigs extends Configs.BaseConfigs {
	public static final ConfigHotkey OPEN_CONFIG = new ConfigHotkey("openConfig", "U, C",
		"Opens this screen if none other is open");
	public static final ConfigBooleanHotkeyed COMPACT_SCOREBOARD = new ConfigBooleanHotkeyed("compactScoreboard", false, "F6", """
		Show scoreboard values in compact notation.
		For example: 123456 -> 123.4K""");
	public static final ConfigHotkey GIVE_FULL_INV = new ConfigHotkey("giveFullInv", "G", """
		Give full inventory of an item with following rules:
		- Main hand: item; Off hand: none; Result: shulker box of item.
		- Main hand: item; Off hand: container; Result: container of item.
		- Main hand: item; Off hand: bundle; Result: bundle of item * config.
		- Main hand: empty shulker box; Off hand: none: Result: chest of stacked boxes.
		- Main hand: empty shulker box; Off hand: container; Result: container of stacked boxes.
		- Main hand: shulker box; Off hand: none: Result: chest of full boxes.
		- Main hand: shulker box; Off hand: container; Result: container of full boxes.
		""");
	public static final ConfigInteger BUNDLE_FILL = new ConfigInteger("bundleFill", 1, 1, 100, true,
		"Amount of stacks to put in a bundle when using giveFullInv feature");
	public static final ConfigBoolean FILL_SAFETY = new ConfigBoolean("fillSafety", true, """
		Restrict nested containers to prevent crashing yourself.
		Here be cats and scratches!""");
	public static final ConfigInteger FILL_COOLDOWN = new ConfigInteger("fillCooldown", 1000, """
		Cooldown in milliseconds between filling operations
		""");
	public static final ConfigHotkey SCAN_INVENTORY = new ConfigHotkey("scanInventory", "I", "");
	public static final ConfigInteger REQUEST_TIMEOUT = new ConfigInteger("requestTimeout", 60, 1, 1000, false,
		"Time in game ticks before request is considered failed");

	public MiscConfigs() {
		super(ImmutableList.of(
			OPEN_CONFIG,
			COMPACT_SCOREBOARD,
			GIVE_FULL_INV,
			BUNDLE_FILL,
			FILL_SAFETY,
			FILL_COOLDOWN
		));
	}
}
