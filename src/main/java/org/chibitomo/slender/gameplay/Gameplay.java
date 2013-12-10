package org.chibitomo.slender.gameplay;

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
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Team;
import org.chibitomo.misc.Utils;
import org.chibitomo.slender.page.PageManager;
import org.chibitomo.slender.plugin.Slender;

public class Gameplay {
	public static final String SLENDER_TEAM = "Slenderman";
	public static final String CHILDREN_TEAM = "Children";
	public static final String DEADS_TEAM = "Deads";
	public static final String PROXIES_TEAM = "Proxies"; // TODO: Proxy team.

	private static final String PAGE_LEFT_SCORE = "page_left";
	private static final String SOUL_CAPTURED_SCORE = "soul_captured";
	private static final String PAGE_FOUND_SCORE = "page_found";
	private static final String BUDIES_LEFT_SCORE = "budies_left";

	private static final float SAFE_DIST_RATIO = (float) 0.8;

	private Slender plugin;
	private Slenderman slenderman;

	private World world;
	private int viewDist;
	private int viewAngle;
	private int minDamageDist;
	private int maxHealth;
	private int maxRegainHealth;
	private int maxDamagePercent;
	private boolean slendermenHaveCompass;
	private boolean canRegainHealth;

	private HashMap<String, BukkitTask> damageShedulers;
	private HashMap<String, Integer> distanceMap;
	private TeamManager teamManager;

	private List<String> childrenSeen;
	private int totalChildren;

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

	public boolean isInTeam(String teamName, OfflinePlayer player) {
		for (OfflinePlayer p : getPlayers(teamName)) {
			if (p.getName().equals(player.getName())) {
				return true;
			}
		}
		return false;
	}

	public Set<OfflinePlayer> getPlayers(String teamName) {
		return getTeam(teamName).getPlayers();
	}

	public void addPlayer(String teamName, OfflinePlayer player) {
		String oldTeam = teamManager.getOldTeam(player);
		if (oldTeam != null) {
			removePlayer(oldTeam, player);
		}
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
			Utils.delay(getPlugin(), getPlugin(), "gameStop", 10);
			return;
		}

		Utils.delay(getPlugin(), this, "syncScores", 10);
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
		canRegainHealth = config.getBoolean(Slender.CAN_REGAIN_HEALTH);
		maxRegainHealth = config.getInt(Slender.MAX_REGAIN_HEALTH);

		slenderman = new Slenderman(getPlugin(), this);
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
		PageManager pageManager = getPlugin().getPageManager();

		int pageLeft = pageManager.getPageLeftAmount();
		int childrenNbr = getPlayers(CHILDREN_TEAM).size();
		int slendermenNbr = getPlayers(SLENDER_TEAM).size();

		teamManager
				.setScore(PAGE_FOUND_SCORE, pageManager.getPageTakenAmount());
		teamManager.setScore(PAGE_LEFT_SCORE, pageLeft);
		teamManager.setScore(BUDIES_LEFT_SCORE, childrenNbr - 1);
		teamManager.setScore(SOUL_CAPTURED_SCORE, totalChildren - childrenNbr);
		checkEndGame(pageLeft, childrenNbr, slendermenNbr);
	}

	private void checkEndGame(int pageLeft, int childrenNbr, int slendermenNbr) {
		plugin.debug("pageLeft=" + pageLeft);
		plugin.debug("childrenNbr=" + childrenNbr);
		plugin.debug("slendermenNbr=" + slendermenNbr);
		if (pageLeft < 1) {
			endGame(false);
		}
		if (slendermenNbr < 1) {
			endGame(false);
		}
		if (childrenNbr < 1) {
			endGame(true);
		}
	}

	private void populateTeams() {
		Player[] players = getPlugin().getServer().getOnlinePlayers();
		int playerId = (int) (Math.random() * players.length);
		slenderman.setSlenderman(players[playerId]);

		for (Player p : getPlugin().getServer().getOnlinePlayers()) {
			addToGame(p);
		}

		totalChildren = getPlayers(CHILDREN_TEAM).size();
	}

	private Team getTeam(String teamName) {
		return teamManager.getTeam(teamName);
	}

	public void takePage(Player player, ItemFrame frame) {
		if (!teamManager.isInTeam(CHILDREN_TEAM, player)) {
			return;
		}
		if (getPlugin().getPageManager().takePage(player, frame)) {
			informPageTaken(player);
		}
		syncScores();
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

	private void endGame(boolean slenderWon) {
		plugin.gameStop();
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
				slenderman.checkCanBeVisible(player, nearestPlayerDist);
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

	public void manageDead(OfflinePlayer offPlayer) {
		if (!offPlayer.isOnline() || isInTeam(Gameplay.DEADS_TEAM, offPlayer)) {
			return;
		}

		Player player = offPlayer.getPlayer();
		if (player.isDead() && !teamManager.isInTeam(DEADS_TEAM, player)) {
			addDeadPlayer(player);
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
		deleteDammager(player);
		distanceMap.remove(player.getName());

		teamManager.removePlayer(CHILDREN_TEAM, player);
		teamManager.removePlayer(SLENDER_TEAM, player);
		teamManager.addPlayer(DEADS_TEAM, player);
	}

	public Slender getPlugin() {
		return plugin;
	}

	public void addToGame(Player player) {
		player.setGameMode(GameMode.SURVIVAL);
		player.getInventory().clear();

		if (slenderman.isSlenderman(player)) {
			if (slendermenHaveCompass) {
				ItemStack compass = new ItemStack(Material.COMPASS);
				player.setItemInHand(compass);
			}
			return;
		}

		Dammager damager = new Dammager(this, player, maxHealth);
		BukkitTask task = Utils.time(plugin, damager, "damage", 5 * 20, 2 * 20);
		damageShedulers.put(player.getName(), task);

		teamManager.addPlayer(Gameplay.CHILDREN_TEAM, player);
		plugin.setPlayerVisibility(player, true);
		player.sendMessage(ChatColor.GREEN + "You are a Child");

		// TODO: Create a torch.
		// ItemStack torch = new ItemStack(Material.TORCH);
		// player.setItemInHand(torch);

		player.setMaxHealth(maxHealth);
		player.setHealth(maxHealth);
	}

	public void restoreOldTeam(Player player) {
		String oldTeam = teamManager.getOldTeam(player);
		if (oldTeam == null) {
			oldTeam = Gameplay.DEADS_TEAM;
		}
		if (oldTeam == Gameplay.DEADS_TEAM) {
			addDeadPlayer(player);
		} else {
			teamManager.addPlayer(oldTeam, player);
			addToGame(player);
		}
		Utils.delay(plugin, this, "restoreVanish");
	}

	private void restoreVanish() {
		Set<OfflinePlayer> deads = getPlayers(DEADS_TEAM);
		Set<OfflinePlayer> slendermen = getPlayers(SLENDER_TEAM);

		for (OfflinePlayer p : deads) {
			if (p.isOnline()) {
				plugin.setPlayerVisibility(p.getPlayer(), false);
			}
		}

		for (OfflinePlayer p : slendermen) {
			if (p.isOnline()) {
				boolean isVisible = slenderman.isVisible(p.getPlayer());
				plugin.setPlayerVisibility(p.getPlayer(), isVisible);
				plugin.debug("Slenderman: " + p.getName());
				plugin.debug("isVisible=" + isVisible);

			}
		}
	}

	public void deleteDammager(OfflinePlayer player) {
		BukkitTask task = damageShedulers.get(player.getName());
		if (task != null) {
			task.cancel();
		}
	}

	public TeamManager getTeamManager() {
		return teamManager;
	}

	public boolean canRegainHealth() {
		return canRegainHealth;
	}

	public float getMinDamageDist() {
		return minDamageDist;
	}

	public int getChildrenSafeDist() {
		PageManager pageManager = plugin.getPageManager();
		int pageLeft = pageManager.getPageLeftAmount();
		int pageTaken = pageManager.getPageTakenAmount();
		int totalPage = pageTaken + pageLeft;

		float percent = (pageLeft * 100) / totalPage;
		float maxDist = getMinDamageDist() * SAFE_DIST_RATIO;
		return (int) Math.ceil((maxDist * percent) / 100);
	}

	public void playerLeave(Player player) {
		deleteDammager(player);

		if (isInTeam(DEADS_TEAM, player)) {
			return;
		}

		Utils.delay(plugin, this, "playerQuit", player.getName(), 10 * 20);
	}

	private void playerQuit(String playerName) {
		Server server = plugin.getServer();
		OfflinePlayer player = server.getOfflinePlayer(playerName);
		if (player.isOnline()) {
			return;
		}
		plugin.debug(playerName + " has left the game...");

		String oldTeam = teamManager.getOldTeam(player);
		removePlayer(oldTeam, player);
		addPlayer(DEADS_TEAM, player);
		server.broadcastMessage(ChatColor.GRAY + "" + ChatColor.ITALIC
				+ player.getName() + " leaves the game...");
		syncScores();
	}

	public double getMaxRegainHealth() {
		return (maxRegainHealth * 100) / maxHealth;
	}

	public String getOldTeam(Player player) {
		return teamManager.getOldTeam(player);
	}
}
