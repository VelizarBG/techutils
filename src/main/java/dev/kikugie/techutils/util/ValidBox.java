package dev.kikugie.techutils.util;

import fi.dy.masa.litematica.selection.Box;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * An extension of {@link Box} that ensures corners are not null (to shut up the IDE). As well as provides a few utility methods I need.
 */
@SuppressWarnings("NullableProblems")
public class ValidBox extends Box {
	private BlockPos minPos;
	private BlockPos maxPos;

	public ValidBox(BlockPos pos1, BlockPos pos2) {
		super(pos1, pos2, "");
		Objects.requireNonNull(pos1);
		Objects.requireNonNull(pos2);
	}

	public ValidBox(BlockPos pos1, BlockPos pos2, String name) {
		super(pos1, pos2, name);
		Objects.requireNonNull(pos1);
		Objects.requireNonNull(pos2);
	}

	public ValidBox(int[] corners) {
		super(new BlockPos(corners[0], corners[1], corners[2]), new BlockPos(corners[3], corners[4], corners[5]), "");
	}

	public static ValidBox of(Box box) {
		return new ValidBox(box.getPos1(), box.getPos2(), box.getName());
	}

	@NotNull
	@Override
	public BlockPos getPos1() {
		return Objects.requireNonNull(super.getPos1());
	}

	@Override
	public void setPos1(@NotNull BlockPos pos) {
		super.setPos1(pos);
		updateCorners();
	}

	@NotNull
	@Override
	public BlockPos getPos2() {
		return Objects.requireNonNull(super.getPos2());
	}

	@Override
	public void setPos2(@NotNull BlockPos pos) {
		super.setPos2(pos);
		updateCorners();
	}

	public boolean contains(BlockPos pos) {
		BlockPos min = getMin();
		BlockPos max = getMax();
		return pos.getX() >= min.getX() && pos.getX() <= max.getX() &&
			pos.getY() >= min.getY() && pos.getY() <= max.getY() &&
			pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ();
	}

	public boolean equals(Box other) {
		if (other == this)
			return true;
		if (other == null)
			return false;
		if (other.getPos1() == null || other.getPos2() == null)
			return false;
		return getPos1().equals(other.getPos1()) && getPos2().equals(other.getPos2());
	}

	public BlockPos getPos1(BlockPos fallback) {
		BlockPos pos = super.getPos1();
		return pos != null ? pos : fallback;
	}

	public BlockPos getPos2(BlockPos fallback) {
		BlockPos pos = super.getPos2();
		return pos != null ? pos : fallback;
	}

	public BlockPos getMin() {
		if (this.minPos == null)
			updateCorners();
		return this.minPos;
	}

	public BlockPos getMax() {
		if (this.maxPos == null)
			updateCorners();
		return this.maxPos;
	}

	private void updateCorners() {
		BlockPos pos1 = getPos1(BlockPos.ZERO);
		BlockPos pos2 = getPos2(BlockPos.ZERO);

		int x1 = pos1.getX();
		int y1 = pos1.getY();
		int z1 = pos1.getZ();
		int x2 = pos2.getX();
		int y2 = pos2.getY();
		int z2 = pos2.getZ();

		int minX = Math.min(x1, x2);
		int minY = Math.min(y1, y2);
		int minZ = Math.min(z1, z2);
		int maxX = Math.max(x1, x2);
		int maxY = Math.max(y1, y2);
		int maxZ = Math.max(z1, z2);

		this.minPos = new BlockPos(minX, minY, minZ);
		this.maxPos = new BlockPos(maxX, maxY, maxZ);
	}
}
