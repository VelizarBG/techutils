package dev.kikugie.techutils.feature.containerscan.handlers;

import dev.kikugie.techutils.config.MiscConfigs;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;
import java.util.function.Supplier;

/**
 * Allows intercepting a screen with previously registered handler.
 */
public abstract class InteractionHandler {
	/**
	 * Queue for the handlers. Given that screen packets arrive in the same order as the requests, screen listener may only access the head of the queue.
	 */
	private static final Queue<InteractionHandler> queue = new ArrayDeque<>();
	/**
	 * Timeout duration in game ticks.
	 */
	private static final Supplier<Integer> timeout = MiscConfigs.REQUEST_TIMEOUT::getIntegerValue;
	/**
	 * Time at the moment of registering the handler. Used to calculate timeout.
	 */
	private final long tick;
	/**
	 * An in-world container position awaited screen is bound to. Also used to avoid requesting multiple screens for the same position.
	 */
	private final BlockPos pos;

	public InteractionHandler(BlockPos pos, long tick) {
		this.tick = tick;
		this.pos = pos;
	}

	public static void add(InteractionHandler handler) {
		queue.offer(handler);
	}

	public static boolean contains(BlockPos pos) {
		for (InteractionHandler handler : queue)
			if (handler.pos.equals(pos))
				return true;
		return false;
	}

	public static void tick(long current) {
		if (queue.isEmpty())
			return;

		Iterator<InteractionHandler> itr = queue.iterator();
		int timeout = InteractionHandler.timeout.get();
		while (itr.hasNext())
			if (current - itr.next().tick > timeout)
				itr.remove();
			else break;
	}

	public static boolean onScreen(Screen screen) {
		if (queue.isEmpty())
			return true;
		return queue.poll().accept(screen);
	}

	/**
	 * @return true if screen should be opened
	 */
	public abstract boolean accept(Screen screen);
}
