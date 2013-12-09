package org.chibitomo.slender.plugin;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

public class Misc {
	private Slender slender;

	private List<String> addPagePlayerList;

	public Misc(Slender slender) {
		this.slender = slender;
		addPagePlayerList = new ArrayList<String>();
	}

	public void addPage(Player player, Block block, BlockFace face) {
		if (!addPagePlayerList.contains(player.getName())) {
			return;
		}
		addPagePlayerList.remove(player.getName());

		if (block == null) {
			return;
		}

		Integer[] coord = slender.getFrameManager().placeFrame(block, face,
				false);
		if (coord == null) {
			return;
		}

		slender.getPageManager().addPageLocation(coord);

		player.sendMessage(ChatColor.GREEN + "New page placed at:"
				+ ChatColor.RESET + "x=" + coord[0] + " y=" + coord[1] + " z="
				+ coord[2]);
	}

	public void listenAddPage(Player player) {
		addPagePlayerList.add(player.getName());
	}
}
