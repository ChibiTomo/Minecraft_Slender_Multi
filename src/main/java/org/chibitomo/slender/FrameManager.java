package org.chibitomo.slender;

import java.util.ArrayList;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.ItemStack;

public class FrameManager {

	private Slender plugin;

	private ArrayList<Entity> frames;

	public FrameManager(Slender slender) {
		plugin = slender;

		frames = new ArrayList<Entity>();
	}

	public void start() {
		removeFrames();
	}

	public void stop() {
		removeFrames();
	}

	public Integer[] placeFrame(Block block, BlockFace face, boolean withPaper) {
		if (face.equals(BlockFace.DOWN) || face.equals(BlockFace.UP)) {
			return null;
		}
		World world = block.getWorld();

		Location[] locs = Utils.block2Locs(block, face);

		Block b1 = world.getBlockAt(locs[0]);
		Block b2 = world.getBlockAt(locs[1]);
		Block b3 = world.getBlockAt(locs[2]);

		int t1 = b1.getTypeId();
		int t2 = b2.getTypeId();
		int t3 = b3.getTypeId();

		b1.setTypeId(2);
		b2.setTypeId(2);
		b3.setTypeId(2);

		if (Utils.getEntityAt(locs[3]) != null) {
			return null;
		}

		try {
			ItemFrame i = block.getWorld().spawn(block.getLocation(),
					ItemFrame.class);
			if (withPaper) {
				i.setItem(new ItemStack(339));
			}

			frames.add(i);
		} catch (IllegalArgumentException e) {
			locs[3] = null;
		} finally {
			b1.setTypeId(t1);
			b2.setTypeId(t2);
			b3.setTypeId(t3);
		}

		if (locs[3] == null) {
			return null;
		}

		return Utils.coord2inta(Utils.loc2Coord(locs[3], face));
	}

	public boolean remove(Entity frame) {
		if (frames.contains(frame)) {
			frames.remove(frame);
			frame.remove();
			return true;
		}
		return false;
	}

	public void removeFrames() {
		for (Entity frame : frames) {
			frame.remove();
		}
		frames = new ArrayList<Entity>();
	}
}
