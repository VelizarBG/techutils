package dev.kikugie.techutils.feature.worldedit;

import dev.kikugie.techutils.TechUtilsMod;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import org.enginehub.worldeditcui.protocol.CUIPacket;

import java.util.Optional;

/**
 * Opens a WorldEdit CUI channel and passes packets to {@link WorldEditSync} instance.
 * <p>
 * If channel has already been registered by another mod, it leeches packets from it instead.
 */
public class WorldEditNetworkHandler {
	public static final ResourceLocation CHANNEL = ResourceLocation.fromNamespaceAndPath("worldedit", "cui");
	private static final int PROTOCOL = 4;
	private static WorldEditNetworkHandler instance;
	public final WorldEditStorage storage;
	private boolean worldEditConnected = false;
	private boolean yoinkPackets = false;

	private WorldEditNetworkHandler() {
		this.storage = WorldEditStorage.init();
		var receivers = ClientPlayNetworking.getGlobalReceivers();
		if (receivers.contains(CHANNEL)) {
			this.yoinkPackets = true;
			handshake();
			return;
		}
		ClientPlayNetworking.registerReceiver(CUIPacket.TYPE, (payload, context) -> this.onPacket(payload));
		handshake();
	}

	public static WorldEditNetworkHandler initHandler() {
		if (Minecraft.getInstance().level == null) {
			throw new RuntimeException("Registering WorldEdit handler in non-world context!");
		}
		instance = new WorldEditNetworkHandler();
		return instance;
	}

	public static Optional<WorldEditNetworkHandler> getInstance() {
		return Optional.ofNullable(instance);
	}

	public void onYoinkedPacket(CustomPacketPayload payload) {
		if (!this.yoinkPackets) return;
		onPacket((CUIPacket) payload);
	}

	private void onPacket(CUIPacket packet) {
		if (!this.worldEditConnected) {
			this.worldEditConnected = true;
			TechUtilsMod.LOGGER.info("WorldEdit connected");
			WorldEditSync.getInstance().ifPresent(WorldEditSync::onWorldEditConnected);
		}
		if (packet.args().isEmpty()) {
			TechUtilsMod.LOGGER.warn("WorldEditNetworkHandler#onPacket(): Received CUI packet of length zero");
			return;
		}
		handlePacket(packet.eventType(), packet.args().toArray(String[]::new));
	}

	private void handlePacket(String type, String[] args) {
		if (type.equals("s")) {
			this.storage.setCuboid(args[0].equals("cuboid"));
			return;
		}
		if (!type.equals("p") || !this.storage.isCuboid())
			return;
		try {
			int p = Integer.parseInt(args[0]);
			BlockPos pos = new BlockPos(Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]));
			this.storage.setPos(p == 0, pos);
		} catch (NumberFormatException e) {
			TechUtilsMod.LOGGER.warn("WorldEditCUINetworkHandler#handlePacket(): Failed int parsing of position", e);
		}
	}

	private void handshake() {
		ClientPlayNetworking.send(new CUIPacket("v", String.valueOf(PROTOCOL)));
	}
}
