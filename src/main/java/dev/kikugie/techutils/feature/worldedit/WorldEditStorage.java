package dev.kikugie.techutils.feature.worldedit;

import dev.kikugie.techutils.util.ValidBox;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class WorldEditStorage {
	@Nullable
	private static WorldEditStorage instance = null;
	private boolean cuboid = false;
	private BlockPos pos1;
	private BlockPos pos2;

	public static Optional<WorldEditStorage> getInstance() {
		return Optional.ofNullable(instance);
	}

	public static WorldEditStorage init() {
		instance = new WorldEditStorage();
		return instance;
	}

	public Optional<ValidBox> getBox() {
		return this.pos1 != null && this.pos2 != null ? Optional.of(new ValidBox(this.pos1, this.pos2, "region")) : Optional.empty();
	}

	public void setBox(@Nullable ValidBox box) {
		if (box == null) {
			this.pos1 = null;
			this.pos2 = null;
		} else {
			this.pos1 = box.getPos1();
			this.pos2 = box.getPos2();
		}
	}

	public void setPos(boolean p, BlockPos pos) {
		if (p)
			this.pos1 = pos;
		else
			this.pos2 = pos;
	}

	public boolean isCuboid() {
		return this.cuboid;
	}

	public void setCuboid(boolean cuboid) {
		this.cuboid = cuboid;
		if (!cuboid) {
			this.pos1 = null;
			this.pos2 = null;
		}
	}
}
