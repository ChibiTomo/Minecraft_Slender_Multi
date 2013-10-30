package org.chibitomo.slender;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;
import org.chibitomo.plugin.Plugin;

public class Slender extends Plugin {
	private static final String PAGE_LOCATION_PATH = "page_location";
	private static final String PAGE_MESSAGES_PATH = "page_messages";
	private static final String PAGE_QUANTITY_PATH = "page_qty";
	private static final String MAX_TP_DIST_PATH = "max_tp_distance";
	private static final String VIEW_DIST_PATH = "view_dist";
	private static final String WORLD_NAME_PATH = "world";

	private Slenderman slenderman;
	private Map<Integer, Integer[]> pagesLocations;
	private List<String> messages;
	private List<Page> pages;

	private int totalPages;
	private int takenPages;
	private boolean gameIsStarted;
	private int etenal_night_runnable_id;
	private int viewDist;

	private Map<Player, Integer> damageShedulers;
	public Map<Player, Integer> damageMap;

	public Team slenderTeam;
	public Team childrenTeam;
	public Team deadsTeam;

	public boolean addPage = false;
	private Scoreboard scoreboard;
	private List<Entity> frames;
	private World world;
	private boolean isClosing = false;

	@Override
	protected void init() {
		NAME = "Slender Multi";
		EVENTPRIORITY = 1;

		gameIsStarted = false;
		etenal_night_runnable_id = -1;

		damageShedulers = new HashMap<Player, Integer>();
		damageMap = new HashMap<Player, Integer>();
		messages = new ArrayList<String>();
		pagesLocations = new HashMap<Integer, Integer[]>();
		frames = new ArrayList<Entity>();

		String worldName = getConfig().getString(WORLD_NAME_PATH);
		if (worldName != null) {
			world = server.getWorld(worldName);
		} else {
			world = server.getWorlds().get(0);
		}

		loadPageLocations();
		placePageDummies();

		ScoreboardManager manager = Bukkit.getScoreboardManager();
		scoreboard = manager.getNewScoreboard();

		slenderTeam = scoreboard.registerNewTeam("Slenderman");
		slenderTeam.setDisplayName(ChatColor.RED + "Slenderman");
		slenderTeam.setPrefix(ChatColor.RED + "");
		slenderTeam.setSuffix(" Slenderman" + ChatColor.RESET);

		childrenTeam = scoreboard.registerNewTeam("Children");
		childrenTeam.setDisplayName(ChatColor.GREEN + "Children");
		childrenTeam.setAllowFriendlyFire(false);
		childrenTeam.setPrefix(ChatColor.GREEN + "");
		childrenTeam.setSuffix(" Children" + ChatColor.RESET);

		deadsTeam = scoreboard.registerNewTeam("Deads");
		deadsTeam.setDisplayName(ChatColor.AQUA + "Deads");
		deadsTeam.setCanSeeFriendlyInvisibles(true);
		deadsTeam.setPrefix(ChatColor.AQUA + "");
		deadsTeam.setSuffix(" Dead" + ChatColor.RESET);

		slenderman = new Slenderman(this);
	}

	private void placePageDummies() {
		server.getScheduler().runTaskLater(this, new Runnable() {
			@Override
			public void run() {
				Location loc = null;

				for (Integer[] coord : pagesLocations.values()) {
					loc = new Location(world, coord[0], coord[1], coord[2]);
					if (getEntityAt(loc) == null) {
						BlockFace face = int2BlockFace(coord[3]);
						Location blockLoc = fLoc2BLoc(loc, face);

						placeFrame(world.getBlockAt(blockLoc), face, false);
					}
				}
			}
		}, 10);
	}

	@Override
	protected void close() {
		isClosing = true;
		if (gameIsStarted) {
			gameStop();
		}

		removeFrames();
	}

	@Override
	protected void registerEventHandlers() {
		addPluginsEventHandler(new SlenderEventHandler(this, EVENTPRIORITY));
	}

	@Override
	protected void registerCommandHandler(String cmd) {
		setCommandExecutor(cmd, new SlenderCommandHandler(this));
	}

	public Slenderman getSlenderman() {
		return slenderman;
	}

	public void takePage(Player player, ItemFrame frame) {
		try {
			ItemStack item = frame.getItem();
			if (item.getTypeId() != 339) {
				return;
			}
			Location location = frame.getLocation();
			for (Page page : pages) {
				if (page.isLocatedAt(location) && !page.isTaken()) {
					page.setToken(true);
					frame.remove();
					player.sendMessage(page.getMessage());
					takenPages++;

					checkPages();
					return;
				}
			}
			player.sendMessage("Sorry... This is not a real page...");
		} catch (Exception e) {
			error(e);
		}
	}

	private void checkPages() {
		if (takenPages != totalPages) {
			getServer().broadcastMessage(
					takenPages + "/" + totalPages + " pages");
			return;
		}
		endGame(false);
	}

	private void endGame(Boolean slenderWon) {
		gameStop();
		String message = "Game is finished. Children won.";
		if (slenderWon) {
			message = "Game is finished. Slenderman won.";
		}
		getServer().broadcastMessage(message);
	}

	public void gameStart(Player player) {
		removeFrames();
		pages = new ArrayList<Page>();
		List<Player> players = player.getWorld().getPlayers();
		int playerId = (int) (Math.random() * players.size());
		slenderman.setSlenderman(players.get(playerId));

		FileConfiguration config = getConfig();
		slenderman.setMaxTpDistance(config.getInt(MAX_TP_DIST_PATH));
		totalPages = config.getInt(PAGE_QUANTITY_PATH);
		viewDist = config.getInt(VIEW_DIST_PATH);

		messages = config.getStringList(PAGE_MESSAGES_PATH);

		loadPageLocations();

		damageShedulers = new HashMap<Player, Integer>();

		totalPages = Math.min(totalPages, pagesLocations.size());

		gameIsStarted = true;
		eternal_night_on();

		for (Player p : server.getOnlinePlayers()) {
			if (!slenderman.isSlenderman(p)) {
				childrenTeam.addPlayer(p);
			}
			p.setScoreboard(scoreboard);
		}

		server.broadcastMessage("Start a new game.");

		server.getScheduler().runTaskLater(this, new Runnable() {
			@Override
			public void run() {
				placePages();
			}
		}, 10);
	}

	private void loadPageLocations() {
		FileConfiguration config = getConfig();

		int i = 0;
		List<Integer> loc = config.getIntegerList(PAGE_LOCATION_PATH + "." + i);

		while (!loc.isEmpty()) {
			Integer[] array = new Integer[4];
			loc.toArray(array);
			pagesLocations.put(i, array);
			i++;
			loc = config.getIntegerList(PAGE_LOCATION_PATH + "." + i);
		}
	}

	private void removeFrames() {
		for (Entity e : frames) {
			e.remove();
		}
		frames = new ArrayList<Entity>();
	}

	private void placePages() {
		World world = slenderman.getPlayer().getWorld();

		for (int i = 0; i < totalPages; i++) {
			int nbMessage = messages.size();
			String message = "";

			if (nbMessage > 0) {
				message = messages.get(i % nbMessage);
			}

			Location loc = null;

			Integer[] array = new Integer[4];

			while (loc == null) {
				int pageId = (int) (Math.random() * pagesLocations.size());
				array = pagesLocations.get(pageId);
				loc = new Location(world, array[0], array[1], array[2]);
				for (Page p : pages) {
					if (p.getLoc().equals(loc)) {
						loc = null;
					}
				}
				// info("no page placed here!");
				if ((loc != null) && (getEntityAt(loc) != null)) {
					// info("there is an entity here...");
					loc = null;
					totalPages--;
					continue;
				}
			}

			BlockFace face = int2BlockFace(array[3]);
			Location frameLoc = fLoc2BLoc(loc, face);

			Page page = new Page(i, loc, message);
			pages.add(page);

			placeFrame(world.getBlockAt(frameLoc), face, true);
		}
	}

	private Location fLoc2BLoc(Location loc, BlockFace face) {
		double x = loc.getBlockX();
		double z = loc.getBlockZ();
		if (face.equals(BlockFace.NORTH)) {
			z++;
		} else if (face.equals(BlockFace.SOUTH)) {
			z--;
		} else if (face.equals(BlockFace.EAST)) {
			x--;
		} else if (face.equals(BlockFace.WEST)) {
			x++;
		}
		return new Location(loc.getWorld(), x, loc.getBlockY(), z);
	}

	private BlockFace int2BlockFace(Integer i) {
		BlockFace result = BlockFace.NORTH;
		if (i == 1) {
			result = BlockFace.EAST;
		} else if (i == 2) {
			result = BlockFace.SOUTH;
		} else if (i == 3) {
			result = BlockFace.WEST;
		}
		return result;
	}

	private int blockFace2int(BlockFace face) {
		int result = 0;
		if (face == BlockFace.EAST) {
			result = 1;
		} else if (face == BlockFace.SOUTH) {
			result = 2;
		} else if (face == BlockFace.WEST) {
			result = 3;
		}
		return result;
	}

	public void gameStop() {
		removeFrames();
		slenderman.setSlenderman(null);
		gameIsStarted = false;
		totalPages = 0;
		takenPages = 0;
		eternal_night_off();
		for (int id : damageShedulers.values()) {
			server.getScheduler().cancelTask(id);
		}

		for (Player p : server.getOnlinePlayers()) {
			setPlayerVisibility(p, true);

			p.setScoreboard(server.getScoreboardManager().getNewScoreboard());
		}

		server.broadcastMessage("Stop current game.");

		if (!isClosing) {
			placePageDummies();
		}
	}

	private void eternal_night_on() {
		if (etenal_night_runnable_id != -1) {
			return;
		}
		final World world = slenderman.getPlayer().getWorld();
		etenal_night_runnable_id = server.getScheduler()
				.scheduleSyncRepeatingTask(this, new Runnable() {
					@Override
					public void run() {
						world.setTime(15000);
					}
				}, 0, 7000);
	}

	private void eternal_night_off() {
		if (etenal_night_runnable_id == -1) {
			return;
		}
		server.getScheduler().cancelTask(etenal_night_runnable_id);
	}

	public boolean gameisStarted() {
		return gameIsStarted;
	}

	public int getViewDist() {
		return viewDist;
	}

	public double getDist(Location loc1, Location loc2) {
		double x = Math.pow(loc1.getX() - loc2.getX(), 2);
		double y = Math.pow(loc1.getY() - loc2.getY(), 2);
		double z = Math.pow(loc1.getZ() - loc2.getZ(), 2);
		return Math.sqrt(x + y + z);
	}

	public List<Player> getNearbyPlayers(Player player, int dist) {
		List<Player> result = new ArrayList<Player>();
		for (Player p : server.getOnlinePlayers()) {
			double playerDist = getDist(player.getLocation(), p.getLocation());
			if (!player.equals(p) && (playerDist <= dist)) {
				result.add(p);
			}
		}
		return result;
	}

	public boolean canSeeSlenderman(Player player) {
		if (!slenderman.isVisible()) {
			return false;
		}

		Location eyeLoc = player.getEyeLocation();
		Location slenderLoc = slenderman.getPlayer().getEyeLocation();

		double toSlendermanDist = getDist(eyeLoc, slenderLoc);

		float x = (float) (eyeLoc.getX() - slenderLoc.getX());
		float y = (float) (eyeLoc.getY() - slenderLoc.getY());
		float z = (float) (eyeLoc.getZ() - slenderLoc.getZ());
		Vector toSlendermanVect = new Vector(x, y, z);

		Vector eyeDir = eyeLoc.getDirection();
		double angle = (eyeDir.angle(toSlendermanVect) / (2 * Math.PI)) * 360;

		World world = player.getWorld();
		boolean nothingBetween = isSomethingBetween(
				toSlendermanVect.toLocation(world), (int) toSlendermanDist);

		// TODO: Configurable vision angle.
		if ((toSlendermanDist <= viewDist) && nothingBetween && (angle > 105)) {
			return true;
		}
		return false;
	}

	private boolean isSomethingBetween(Location start, int maxDist) {
		// BlockIterator blockIt = new BlockIterator(start, maxDist);
		// while (blockIt.hasNext()) {
		// Block block = blockIt.next();
		// info("blockType=" + block.getType());
		// }
		return true;
	}

	public void checkDamages() {
		Player slendermanPlayer = slenderman.getPlayer();
		for (Player player : server.getOnlinePlayers()) {
			double dist = getDist(player.getLocation(),
					slendermanPlayer.getLocation());

			int id = -1;
			Integer o = damageShedulers.get(player);
			if (o != null) {
				id = o;
			}
			// TODO: Configurable player maxHealth.
			int maxHealth = 100;
			// TODO: Configurable min damage dist.
			int minDamageDist = 30;
			// TODO: Configurable maxDamage (%).
			int maxDamagePercent = 30;

			double damagePercent = ((minDamageDist - dist) * 100)
					/ minDamageDist;
			double realDamagePercent = (damagePercent * maxDamagePercent) / 100;
			Integer damage = new Integer(
					(int) ((realDamagePercent * maxHealth) / 100));

			if (slenderman.isSlenderman(player)) {
				damage = new Integer(-1);
			}
			damageMap.put(player, damage);
			if (dist < minDamageDist) {
				if (id == -1) {
					id = server.getScheduler().scheduleSyncRepeatingTask(this,
							new Dammager(player, maxHealth), 20, 20);

					damageShedulers.put(player, new Integer(id));
				}
			}
		}
	}

	public class Dammager implements Runnable {
		private Player player;
		private int maxHealth;

		public Dammager(Player player, int maxHealth) {
			this.player = player;
			this.maxHealth = maxHealth;
		}

		@Override
		public void run() {
			if (player.getMaxHealth() != maxHealth) {
				player.setMaxHealth(maxHealth);
			}
			if (isDead(player)) {
				damageMap.put(player, -1);
			}
			int amount = damageMap.get(player);
			if (getSlenderman().isVisible() && (amount > 0)) {
				double playerMaxHealth = player.getMaxHealth();
				double damage = (amount * playerMaxHealth) / maxHealth;
				if (player.getHealth() <= damage) {
					addDeadPlayer(player);
				}
				player.damage(damage);
			}
		}

	}

	public void addPage(PlayerInteractEvent event) throws Exception {
		if (!addPage) {
			return;
		}
		addPage = false;

		Block block = event.getClickedBlock();

		if (block == null) {
			return;
		}

		BlockFace face = event.getBlockFace();
		Integer[] coord = placeFrame(block, face, false);
		if (coord == null) {
			throw new Exception("Error while placing frame: coord are null");
		}

		pagesLocations.put(pagesLocations.size(), coord);
		getConfig().set(PAGE_LOCATION_PATH, pagesLocations);
		saveConfig();
	}

	private boolean isDead(Player player) {
		return player.isDead() || isInDeadsTeam(player);
	}

	public boolean isInDeadsTeam(Player player) {
		Set<OfflinePlayer> players = deadsTeam.getPlayers();
		OfflinePlayer ofp = server.getOfflinePlayer(player.getName());
		return players.contains(ofp);
	}

	private void addDeadPlayer(Player player) {
		deadsTeam.addPlayer(player);

		setPlayerVisibility(player, false);

		Set<OfflinePlayer> players = deadsTeam.getPlayers();
		for (OfflinePlayer p1 : players) {
			if (!p1.isOnline()) {
				continue;
			}
			Player player1 = server.getPlayer(p1.getName());
			for (OfflinePlayer p2 : players) {
				if (!p2.isOnline()) {
					continue;
				}
				Player player2 = server.getPlayer(p2.getName());
				if (!player2.equals(player1)) {
					player1.showPlayer(player2);
				}
			}
		}

		damageMap.put(player, -1);
	}

	private Integer[] placeFrame(Block block, BlockFace face, boolean withPaper) {
		if (face.equals(BlockFace.DOWN) || face.equals(BlockFace.UP)) {
			return null;
		}

		Integer[] coord = new Integer[4];

		coord[0] = block.getX();
		coord[1] = block.getY();
		coord[2] = block.getZ();
		coord[3] = 0;
		World world = block.getWorld();

		Location l1 = new Location(world, coord[0], coord[1], coord[2]);
		Location l2 = new Location(world, coord[0], coord[1], coord[2]);
		Location l3 = new Location(world, coord[0], coord[1], coord[2]);

		if (face.equals(BlockFace.NORTH)) {
			l1.setX(coord[0] + 1);
			l2.setX(coord[0] - 1);
			l3.setZ(coord[2] + 1);
			coord[3] = 0;
			coord[2]--;
		} else if (face.equals(BlockFace.SOUTH)) {
			l1.setX(coord[0] + 1);
			l2.setX(coord[0] - 1);
			l3.setZ(coord[2] - 1);
			coord[3] = 2;
			coord[2]++;
		} else if (face.equals(BlockFace.EAST)) {
			l1.setX(coord[0] - 1);
			l2.setZ(coord[2] - 1);
			l3.setZ(coord[2] + 1);
			coord[3] = 1;
			coord[0]++;
		} else if (face.equals(BlockFace.WEST)) {
			l1.setX(coord[0] + 1);
			l2.setZ(coord[2] - 1);
			l3.setZ(coord[2] + 1);
			coord[3] = 3;
			coord[0]--;
		}

		Block b1 = world.getBlockAt(l1);
		Block b2 = world.getBlockAt(l2);
		Block b3 = world.getBlockAt(l3);

		int t1 = b1.getTypeId();
		int t2 = b2.getTypeId();
		int t3 = b3.getTypeId();

		b1.setTypeId(2);
		b2.setTypeId(2);
		b3.setTypeId(2);

		Location loc = new Location(world, coord[0], coord[1], coord[2]);
		if (getEntityAt(loc) != null) {
			return null;
		}

		ItemFrame i = block.getWorld().spawn(block.getLocation(),
				ItemFrame.class);

		b1.setTypeId(t1);
		b2.setTypeId(t2);
		b3.setTypeId(t3);

		if (withPaper) {
			i.setItem(new ItemStack(339));
		}

		frames.add(i);

		return coord;
	}

	private Entity getEntityAt(Location loc) {
		World world = loc.getWorld();
		for (Entity e : world.getEntities()) {
			if (e instanceof LivingEntity) {
				continue;
			}
			Location eLoc = e.getLocation();
			if ((loc.getBlockX() == eLoc.getBlockX())
					&& (loc.getBlockY() == eLoc.getBlockY())
					&& (loc.getBlockZ() == eLoc.getBlockZ())) {
				return e;
			}
		}
		return null;
	}

	public void addMessage(String msg) {
		messages.add(msg);
		getConfig().set(PAGE_MESSAGES_PATH, messages);
		saveConfig();
	}

	public void setPlayerVisibility(Player player, Boolean isVisible) {
		World world = player.getWorld();

		for (Player p : world.getPlayers()) {
			if (isVisible) {
				p.showPlayer(player);
			} else {
				p.hidePlayer(player);
			}
		}
	}

	public void checkDead() {
		Player[] players = server.getOnlinePlayers();
		if (deadsTeam.getPlayers().size() == (players.length - 1)) {
			server.getScheduler().runTaskLater(this, new Runnable() {
				@Override
				public void run() {
					endGame(true);
				}
			}, 20);
		}
	}

	public void removePage(ItemFrame frame) {
		frames.remove(frame);
		frame.remove();
		Integer[] coord = loc2Coord(frame.getLocation(), frame.getFacing());
		info("coord: x=" + coord[0] + " y=" + coord[1] + " z=" + coord[2]
				+ " face=" + coord[3]);
		for (Integer i : pagesLocations.keySet()) {
			Integer[] c = pagesLocations.get(i);
			info("id=" + i + "c: x=" + c[0] + " y=" + c[1] + " z=" + c[2]
					+ " face=" + c[3]);
			if ((c[0].compareTo(coord[0]) == 0)
					&& (c[1].compareTo(coord[1]) == 0)
					&& (c[2].compareTo(coord[2]) == 0)
					&& (c[3].compareTo(coord[3]) == 0)) {
				pagesLocations.remove(i);

				break;
			}
		}

		HashMap<Integer, Integer[]> newLocations = new HashMap<Integer, Integer[]>();
		for (Integer[] a : pagesLocations.values()) {
			newLocations.put(newLocations.size(), a);
		}
		pagesLocations = newLocations;
		getConfig().set(PAGE_LOCATION_PATH, pagesLocations);
		saveConfig();
	}

	private Integer[] loc2Coord(Location loc, BlockFace face) {
		Integer[] coord = { loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(),
				blockFace2int(face) };
		return coord;
	}
}
