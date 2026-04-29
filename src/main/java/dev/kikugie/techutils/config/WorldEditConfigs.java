package dev.kikugie.techutils.config;

import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigBooleanHotkeyed;
import fi.dy.masa.malilib.config.options.ConfigInteger;

public class WorldEditConfigs extends Configs.BaseConfigs {
	public static final ConfigBooleanHotkeyed WE_SYNC = new ConfigBooleanHotkeyed("autoWeSync", true, "")
		.apply(Configs.CONFIG_KEY);
	public static final ConfigInteger WE_SYNC_TICKS = new ConfigInteger("autoWeSyncTicks", 10, 1, 1000, false)
		.apply(Configs.CONFIG_KEY);
	public static final ConfigBoolean WE_SYNC_FEEDBACK = new ConfigBoolean("autoWeSyncFeedback", true)
		.apply(Configs.CONFIG_KEY);
	public static final ConfigBoolean DISABLE_UPDATES = new ConfigBoolean("autoDisableUpdates", true)
		.apply(Configs.CONFIG_KEY);

	public WorldEditConfigs() {
		super(ImmutableList.of(
			WE_SYNC,
			WE_SYNC_TICKS,
			WE_SYNC_FEEDBACK,
			DISABLE_UPDATES
		));
	}
}
