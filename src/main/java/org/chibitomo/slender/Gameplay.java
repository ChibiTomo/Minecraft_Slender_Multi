package org.chibitomo.slender;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Team;

public class Gameplay {
	public static final String SLENDER_TEAM = "Slenderman";
	public static final String CHILDREN_TEAM = "Children";
	public static final String DEADS_TEAM = "Deads";

	private static final String PAGE_LEFT_SCORE = "page_left";
	private static final String SOUL_CAPTURED_SCORE = "soul_captured";
	private static final String PAGE_FOUND_SCORE = "page_found";
	private static final String BUDIES_LEFT_SCORE = "budies_left";

	private Slender plugin;
	private Slenderman slenderman;

	private World world;
	private int viewDist;
	private int viewAngle;
	private int minDamageDist;
	private int maxHealth;
	private int maxDamagePercent;
	private boolean slendermenHaveCompass;

	private HashMap<String, BukkitTask> damageShedulers;
	private HashMap<String, Integer> distanceMap;
	private List<String> addPagePlayerList;
	private TeamManager teamManager;

	private List<String> childrenSeen;

	public Gameplay(Slender plugin) {
		this.plugin = plugin;

		init();
	}

	public Slenderman getSlenderman() {
		return slenderman;
	}

	public World getWorld() {
		return world;
	}

	public boolean isInTeam(String teamNo, Player player) {
		for (OfflinePlayer p : getTeam(teamNo).getPlayers()) {
			if (p.getName().equals(player.getName())) {
				return true;
			}
		}
		return false;
	}

	public Set<OfflinePlayer> getPlayers(String childrenTeam2) {
		return getTeam(childrenTeam2).getPlayers();
	}

	public void addPlayer(String teamName, OfflinePlayer player) {
		teamManager.addPlayer(teamName, player);
	}

	public void removePlayer(String teamName, OfflinePlayer player) {
		teamManager.removePlayer(teamName, player);
	}

	public void start() {
		plugin.getServer().setDefaultGameMode(GameMode.SURVIVAL);

		damageShedulers = new HashMap<String, BukkitTask>();
		distanceMap = new HashMap<String, Integer>();

		resetScores();
		populateTeams();

		int childrenNbr = teamManager.getTeam(CHILDREN_TEAM).getPlayers()
				.size();

		boolean stopGame = false;
		if (getPlugin().getPageManager().getPageLeftAmount() < 1) {
			getPlugin()
					.getServer()
					.broadcastMessage(
							ChatColor.RED
									+ "No page can be placed: please check locations...");
			stopGame = true;
		}

		if (childrenNbr < 1) {
			getPlugin().getServer().broadcastMessage(
					ChatColor.RED + "Not enought players to start a game...");
			stopGame = true;
		}

		if (stopGame) {
			Utils.delay(getPlugin(), getPlugin(), "gameStop");
			return;
		}

		Utils.delay(getPlugin(), this, "syncScores");
	}

	public void stop() {
		slenderman.setSlenderman(null);

		for (BukkitTask task : damageShedulers.values()) {
			task.cancel();
		}

		Server server = getPlugin().getServer();
		for (Player p : server.getOnlinePlayers()) {
			getPlugin().setPlayerVisibility(p, true);

			p.setScoreboard(server.getScoreboardManager().getNewScoreboard());
			p.getInventory().clear();
		}
	}

	private void init() {
		FileConfiguration config = getPlugin().getConfig();
		viewDist = config.getInt(Slender.VIEW_DIST_PATH);
		viewAngle = config.getInt(Slender.VIEW_ANGLE_PATH);
		minDamageDist = config.getInt(Slender.MIN_DAMAGE_DIST);
		maxHealth = config.getInt(Slender.MAX_HEATH);
		maxDamagePercent = config.getInt(Slender.MAX_DAMAGE_PERCENT);
		slendermenHaveCompass = config
				.getBoolean(Slender.SLENDERMEN_HAVE_COMPASS);

		slenderman = new Slenderman(getPlugin(), this);
		addPagePlayerList = new ArrayList<String>();
		childrenSeen = new ArrayList<String>();

		teamManager = new TeamManager(getPlugin());

		initWorld();
		initTeams();
		initScores();
	}

	private void initWorld() {
		String worldName = getPlugin().getConfig().getString(
				Slender.WORLD_NAME_PATH);
		if (worldName != null) {
			world = getPlugin().getServer().getWorld(worldName);
		} else {
			world = getPlugin().getServer().getWorlds().get(0);
		}
	}

	private void initTeams() {
		teamManager.addTeam(SLENDER_TEAM, ChatColor.RED);
		teamManager.addTeam(CHILDREN_TEAM, ChatColor.GREEN);
		teamManager.addTeam(DEADS_TEAM, ChatColor.GRAY);
	}

	private void initScores() {
		teamManager.addScore(PAGE_LEFT_SCORE, "Page left:", ChatColor.RED,
				new String[] { DEADS_TEAM, SLENDER_TEAM });
		teamManager.addScore(SOUL_CAPTURED_SCORE, "Soul captured:",
				ChatColor.BLUE, new String[] { DEADS_TEAM, SLENDER_TEAM });

		teamManager.addScore(PAGE_FOUND_SCORE, "Page found:", ChatColor.GREEN,
				new String[] { DEADS_TEAM, CHILDREN_TEAM });
		teamManager.addScore(BUDIES_LEFT_SCORE, "Budies left:", ChatColor.AQUA,
				new String[] { DEADS_TEAM, CHILDREN_TEAM });
	}

	private void resetScores() {
		teamManager.setScore(PAGE_FOUND_SCORE, 0);
		teamManager.setScore(PAGE_LEFT_SCORE, 0);
		teamManager.setScore(BUDIES_LEFT_SCORE, 0);
		teamManager.setScore(SOUL_CAPTURED_SCORE, 0);
	}

	public void syncScores() {
		int childrenNbr = teamManager.getTeam(CHILDREN_TEAM).getPlayers()
				.size();
		int deadsNbr = teamManager.getTeam(DEADS_TEAM).getPlayers().size();
		PageManager pageManager = getPlugin().getPageManager();

		teamManager
				.setScore(PAGE_FOUND_SCORE, pageManager.getPageTakenAmount());
		teamManager.setScore(PAGE_LEFT_SCORE, pageManager.getPageLeftAmount());
		teamManager.setScore(BUDIES_LEFT_SCORE, childrenNbr - 1);
		teamManager.setScore(SOUL_CAPTURED_SCORE, deadsNbr);
	}

	private void populateTeams() {
		Player[] players = getPlugin().getServer().getOnlinePlayers();
		int playerId = (int) (Math.random() * players.length);
		slenderman.setSlenderman(players[playerId]);

		for (Player p : getPlugin().getServer().getOnlinePlayers()) {
			addToGame(p);
		}
	}

	private Team getTeam(String teamName) {
		return teamManager.getTeam(teamName);
	}

	public void addPage(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		if (!addPagePlayerList.contains(player.getName())) {
			return;
		}
		addPagePlayerList.remove(player.getName());

		Block block = event.getClickedBlock();

		if (block == null) {
			return;
		}

		BlockFace face = event.getBlockFace();
		Integer[] coord = getPlugin().getFrameManager().placeFrame(block, face,
				false);
		if (coord == null) {
			return;
		}

		getPlugin().getPageManager().addPageLocation(coord);

		event.getPlayer()
				.sendMessage(
						ChatColor.GREEN + "New page placed at:"
								+ ChatColor.RESET + "x=" + coord[0] + " y="
								+ coord[1] + " z=" + coord[2]);
	}

	public void playerAddPage(Player player) {
		addPagePlayerList.add(player.getName());
	}

	public void takePage(Player player, ItemFrame frame) {
		if (!teamManager.isInTeam(CHILDREN_TEAM, player)) {
			return;
		}
		if (getPlugin().getPageManager().takePage(player, frame)) {
			informPageTaken(player);
			checkPages();
		}
	}

	private void informPageTaken(Player player) {
		String message = player.getName() + " found a page!";

		for (OfflinePlayer p : getPlayers(CHILDREN_TEAM)) {
			if (p.isOnline()) {
				p.getPlayer().sendMessage(ChatColor.GREEN + message);
			}
		}
		for (OfflinePlayer p : getPlayers(DEADS_TEAM)) {
			if (p.isOnline()) {
				p.getPlayer().sendMessage(ChatColor.GREEN + message);
			}
		}
		for (OfflinePlayer p : getPlayers(SLENDER_TEAM)) {
			if (p.isOnline()) {
				p.getPlayer().sendMessage(ChatColor.RED + message);
			}
		}
	}

	private void checkPages() {
		if (getPlugin().getPageManager().getPageLeftAmount() < 1) {
			endGame(false);
		}
		syncScores();
	}

	private void endGame(boolean slenderWon) {
		getPlugin().gameStop();
		String message = "Game is finished. Children won.";
		if (slenderWon) {
			message = "Game is finished. Slenderman won.";
		}
		getPlugin().getServer().broadcastMessage(message);
	}

	public List<String> getSlendermenSeenBy(Player player) {
		return slenderman.getSeenBy(player);
	}

	public double getViewDist() {
		return viewDist;
	}

	public int getViewAngle() {
		return viewAngle;
	}

	public void checkWhoSeeWho() {
		List<String> seenSlendermen = new ArrayList<String>();
		List<String> seenChildren = new ArrayList<String>();

		for (Player player : getPlugin().getServer().getOnlinePlayers()) {
			if (teamManager.isInTeam(Gameplay.DEADS_TEAM, player)) {
				continue;
			}

			distanceMap.put(player.getName(), Integer.MAX_VALUE);

			int nearestPlayerDist = Integer.MAX_VALUE;
			Location nearestPlayerLoc = null;
			for (Player otherPlayer : plugin.getServer().getOnlinePlayers()) {
				if (otherPlayer.equals(player)) {
					continue;
				}

				// Check if otherPlayer is dead or spectator
				if (teamManager.isInTeam(Gameplay.DEADS_TEAM, otherPlayer)) {
					continue;
				}

				boolean playerIsChild = teamManager.isInTeam(
						Gameplay.CHILDREN_TEAM, player);
				boolean playerIsSlenderman = teamManager.isInTeam(
						Gameplay.SLENDER_TEAM, player);

				boolean otherPlayerIsChild = teamManager.isInTeam(
						Gameplay.CHILDREN_TEAM, otherPlayer);
				boolean otherPlayerIsSlenderman = teamManager.isInTeam(
						Gameplay.SLENDER_TEAM, otherPlayer);

				// Check distance for damage
				if (playerIsChild && otherPlayerIsSlenderman
						&& slenderman.isVisible(otherPlayer)) {
					int dist = (int) Utils.getDist(otherPlayer.getLocation(),
							player.getLocation());
					int oldDist = getDistance(player);
					if (!Utils.isSomethingBeetween(player, otherPlayer)) {
						distanceMap.put(player.getName(),
								Math.min(oldDist, dist));
					}
				}

				// Check if player can see otherPlayer
				if (Utils.canSee(player, otherPlayer, viewDist, viewAngle)) {
					// player is a child, otherPlayer is a Slenderman
					if (playerIsChild && otherPlayerIsSlenderman
							&& slenderman.isVisible(otherPlayer)) {

						String slenderName = otherPlayer.getName();
						if (!seenSlendermen.contains(slenderName)) {
							seenSlendermen.add(slenderName);
						}
					}

					// player is a Slenderman, otherPlayer is a child
					if (playerIsSlenderman && otherPlayerIsChild) {
						String childName = otherPlayer.getName();
						if (!seenChildren.contains(childName)) {
							seenChildren.add(childName);
						}
					}
				}

				if (playerIsSlenderman && otherPlayerIsChild) {
					int dist = (int) Utils.getDist(player.getLocation(),
							otherPlayer.getLocation());
					nearestPlayerDist = Math.min(nearestPlayerDist, dist);
					if (nearestPlayerDist == dist) {
						nearestPlayerLoc = otherPlayer.getLocation();
					}
				}
			}

			if (nearestPlayerLoc != null) {
				player.setCompassTarget(nearestPlayerLoc);
			}
		}

		for (OfflinePlayer p : teamManager.getTeam(SLENDER_TEAM).getPlayers()) {
			if (!p.isOnline()) {
				continue;
			}

			String playerName = p.getName();
			if (seenSlendermen.contains(playerName)) {
				slenderman.addSeenSlender((Player) p);
			} else {
				slenderman.removeSeenSlender((Player) p);
			}
		}

		for (OfflinePlayer p : teamManager.getTeam(CHILDREN_TEAM).getPlayers()) {
			if (!p.isOnline()) {
				continue;
			}

			String playerName = p.getName();
			if (seenChildren.contains(playerName)) {
				addSeenChildren((Player) p);
			} else {
				removeSeenChildren((Player) p);
			}
		}
	}

	public int calculDamage(int dist) {
		if (dist > minDamageDist) {
			return -1;
		}
		double damagePercent = ((minDamageDist - dist) * 100) / minDamageDist;
		double realDamagePercent = (damagePercent * maxDamagePercent) / 100;
		int damage = (int) ((realDamagePercent * maxHealth) / 100);
		return damage;
	}

	private void addSeenChildren(Player player) {
		if (!childrenSeen.contains(player.getName())) {
			slenderman.tellTo(player, "I can see you...");
			setChildrenAlreadySeen(player, true);
		}
	}

	private void removeSeenChildren(Player player) {
		if (childrenSeen.contains(player.getName())) {
			setChildrenAlreadySeen(player, false);
		}
	}

	private void setChildrenAlreadySeen(Player player, boolean isSeen) {
		if (isSeen) {
			childrenSeen.add(player.getName());
		} else {
			childrenSeen.remove(player.getName());
		}
	}

	public void checkDead() {
		Set<OfflinePlayer> players = teamManager.getTeam(CHILDREN_TEAM)
				.getPlayers();
		if (players.size() < 1) {
			endGame(true);
		}
	}

	public void manageDead(Player player) {
		if (player.isDead() && !teamManager.isInTeam(DEADS_TEAM, player)) {
			teamManager.removePlayer(CHILDREN_TEAM, player);
			teamManager.removePlayer(SLENDER_TEAM, player);

			teamManager.addPlayer(DEADS_TEAM, player);
		}
		if (teamManager.isInTeam(DEADS_TEAM, player)) {
			distanceMap.put(player.getName(), -1);
		}
	}

	public int getDistance(Player player) {
		if (player == null) {
			Utils.throwNullException();
			return 0;
		}
		String playerName = player.getName();
		if (!distanceMap.containsKey(playerName)) {
			return Integer.MAX_VALUE;
		}
		return distanceMap.get(playerName);
	}

	public void addDeadPlayer(Player player) {
		distanceMap.put(player.getName(), -1);
		teamManager.addPlayer(DEADS_TEAM, player);
	}

	public Slender getPlugin() {
		return plugin;
	}

	public void addToGame(Player player) {
		player.setGameMode(GameMode.SURVIVAL);

		player.getInventory().clear();

		if (plugin.gameisStarted()) {
			String oldTeam = teamManager.getOldTeam(player);
			if (oldTeam == null) {
				oldTeam = Gameplay.DEADS_TEAM;
			}
			teamManager.addPlayer(oldTeam, player);
			return;
		}
		Dammager damager = new Dammager(this, player, maxHealth);
		BukkitTask task = Utils.time(getPlugin(), damager, "damage", 5 * 20,
				2 * 20);
		damageShedulers.put(player.getName(), task);

		if (!slenderman.isSlenderman(player)) {
			teamManager.addPlayer(Gameplay.CHILDREN_TEAM, player);
			plugin.setPlayerVisibility(player, true);
			player.sendMessage(ChatColor.GREEN + "You are a Child");
		} else if (slendermenHaveCompass) {
			ItemStack compass = new ItemStack(Material.COMPASS);
			player.setItemInHand(compass);
		}
		player.setMaxHealth(maxHealth);
		player.setHealth(maxHealth);
	}

	public void deleteDammager(Player player) {
		if (damageShedulers.containsKey(player.getName())) {
			BukkitTask task = damageShedulers.get(player.getName());
			task.cancel();
		}
	}

	public TeamManager getTeamManager() {
		return teamManager;
	}
}
