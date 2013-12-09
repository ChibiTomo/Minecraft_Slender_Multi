package org.chibitomo.slender.page;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.chibitomo.misc.Utils;
import org.chibitomo.slender.plugin.Slender;

public class PageManager {

	private Slender plugin;

	private ArrayList<Page> pages;

	private List<String> messages;
	private int totalPages;

	private List<String> pagesLocations;

	public PageManager(Slender plugin) {
		this.plugin = plugin;

		FileConfiguration config = plugin.getConfig();
		pagesLocations = config.getStringList(Slender.PAGE_LOCATION_PATH);
		messages = config.getStringList(Slender.PAGE_MESSAGES_PATH);

		placePageDummies();
	}

	public boolean remove(ItemFrame frame) {
		if (plugin.getFrameManager().remove(frame)) {
			String coord = Utils.loc2Coord(frame.getLocation(),
					frame.getFacing());
			pagesLocations.remove(coord);
			plugin.getConfig().set(Slender.PAGE_LOCATION_PATH, pagesLocations);
			plugin.saveConfig();
			return true;
		}
		return false;
	}

	public void start() {
		FileConfiguration config = plugin.getConfig();
		totalPages = config.getInt(Slender.PAGE_QUANTITY_PATH);
		messages = config.getStringList(Slender.PAGE_MESSAGES_PATH);
		pagesLocations = config.getStringList(Slender.PAGE_LOCATION_PATH);

		totalPages = Math.min(totalPages, pagesLocations.size());

		pages = new ArrayList<Page>();

		Utils.delay(plugin, this, "placePages");
	}

	public String addMessage(String msg) {
		if (messages.contains(msg)) {
			return null;
		}
		messages.add(msg);
		plugin.getConfig().set(Slender.PAGE_MESSAGES_PATH, messages);
		plugin.saveConfig();
		return msg;
	}

	public void addPageLocation(Integer[] coord) {
		String str = StringUtils.join(coord, ',');
		pagesLocations.add(str);
		plugin.getConfig().set(Slender.PAGE_LOCATION_PATH, pagesLocations);
		plugin.saveConfig();
	}

	public void placePageDummies() {
		Utils.delay(plugin, this, "placePageDummiesCallback");
	}

	public void placePageDummiesCallback() {
		World world = plugin.getGameplay().getWorld();

		for (String coord : pagesLocations) {
			Location loc = Utils.coord2Loc(world, coord);
			if (Utils.getEntityAt(loc) == null) {
				BlockFace face = Utils.coord2BlockFace(coord);
				Location blockLoc = Utils.fLoc2BLoc(loc, face);

				plugin.getFrameManager().placeFrame(world.getBlockAt(blockLoc),
						face, false);
			}
		}
	}

	public int getPageLeftAmount() {
		return pages.size();
	}

	public int getPageTakenAmount() {
		return totalPages - getPageLeftAmount();
	}

	public void placePages() {
		World world = plugin.getGameplay().getWorld();

		List<Location> success = new ArrayList<Location>();
		List<Location> fail = new ArrayList<Location>();

		int nbMessage = messages.size();
		int i = 0;
		while (i < totalPages) {
			if ((fail.size() + success.size()) >= pagesLocations.size()) {
				totalPages = i;
			}
			String message = "";

			if (nbMessage > 0) {
				message = messages.get(i % nbMessage);
			}

			Location loc = null;
			String coord = "";
			int j = 0;
			while ((loc == null) && (j < totalPages)) {
				int pageId = (int) (Math.random() * pagesLocations.size());
				coord = pagesLocations.get(pageId);
				plugin.debug("coord=" + coord);
				loc = Utils.coord2Loc(world, coord);
				for (Page p : pages) {
					if (p.getLoc().equals(loc)) {
						loc = null;
					}
				}

				if ((loc != null) && (Utils.getEntityAt(loc) != null)) {
					if (!fail.contains(loc)) {
						fail.add(loc);
						plugin.debug("loc fail");
					}
					loc = null;
				}
				j++;
			}

			if (loc == null) {
				continue;
			}

			BlockFace face = Utils.coord2BlockFace(coord);
			Location frameLoc = Utils.fLoc2BLoc(loc, face);

			Integer[] frameCoord = plugin.getFrameManager().placeFrame(
					world.getBlockAt(frameLoc), face, true);
			if (frameCoord == null) {
				if (!fail.contains(loc)) {
					fail.add(loc);
				}
				continue;
			}

			Page page = new Page(i, loc, message);
			pages.add(page);
			success.add(loc);
			i++;
		}

		totalPages = pages.size();

		plugin.getGameplay().syncScores();
	}

	public void stop() {
		if (!plugin.isClosing()) {
			placePageDummies();
		}
	}

	public boolean takePage(Player player, ItemFrame frame) {
		try {
			ItemStack item = frame.getItem();
			if (item.getTypeId() != 339) {
				return false;
			}
			Location location = frame.getLocation();
			for (Page page : pages) {
				if (page.isLocatedAt(location) && !page.isTaken()) {
					page.setToken(true);
					frame.remove();
					player.sendMessage(page.getMessage());
					pages.remove(page);
					return true;
				}
			}
			player.sendMessage("Sorry... This is not a real page...");
		} catch (Exception e) {
			plugin.error(e);
		}
		return false;
	}
}
