package org.chibitomo.slender.gameplay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.chibitomo.misc.Utils;
import org.chibitomo.slender.plugin.Slender;

public class Slenderman {

	private static final float SPEED_INVISIBLE = (float) 0.5;
	private static final float SPEED_VISIBLE = (float) 0.1;
	private Gameplay gameplay;
	private Slender plugin;

	private Map<String, Boolean> slendermansVisibility;
	private Map<String, Boolean> canBecomeVisible;

	private List<String> slendermenSeen;
	private BukkitTask taskCoolDown;
	private Map<String, Integer> coolDown;
	private Map<String, Float> oldWalkSpeed;

	public Slenderman(Slender plugin, Gameplay gameplay) {
		this.plugin = plugin;
		this.gameplay = gameplay;

		slendermansVisibility = new HashMap<String, Boolean>();
		canBecomeVisible = new HashMap<String, Boolean>();
		slendermenSeen = new ArrayList<String>();
		coolDown = new HashMap<String, Integer>();
		oldWalkSpeed = new HashMap<String, Float>();
	}

	public boolean isSlenderman(Player player) {
		if (player == null) {
			return false;
		}
		String oldTeam = gameplay.getOldTeam(player);
		return gameplay.isInTeam(Gameplay.SLENDER_TEAM, player)
				|| (oldTeam == Gameplay.SLENDER_TEAM);
	}

	public void setSlenderman(Player player) {
		if (!isSlenderman(player)) {
			emptySlenderTeam();
		}
		if (player != null) {
			gameplay.addPlayer(Gameplay.SLENDER_TEAM, player);
			oldWalkSpeed.put(player.getName(), player.getWalkSpeed());

			setVisibility(player, false);
			coolDown.put(player.getName(), 10);
			if (taskCoolDown == null) {
				taskCoolDown = Utils.time(plugin, this, "coolDown", 20, 20);
			}

			player.sendMessage(ChatColor.RED + "You are the Slenderman");
		}
	}

	public void tellTo(Player p, String msg) {
		String newMsg = transformMsg(msg);
		p.sendMessage(ChatColor.RED + newMsg);
	}

	private String transformMsg(String msg) {
		String result = "";
		for (int i = 0; i < msg.length();) {
			int x = (int) (Math.random() * 5);

			String suffix = "";
			if (x < 3) {
				result += ChatColor.MAGIC;
				suffix = "" + ChatColor.RESET + ChatColor.RED;
			}

			result += msg.substring(i, Math.min(i + x, msg.length())) + suffix;
			i += x;
		}
		return result;
	}

	private void emptySlenderTeam() {
		Set<OfflinePlayer> players = gameplay.getPlayers(Gameplay.SLENDER_TEAM);
		for (OfflinePlayer p : players) {
			gameplay.removePlayer(Gameplay.SLENDER_TEAM, p);
			gameplay.addPlayer(Gameplay.CHILDREN_TEAM, p);
			slendermansVisibility.remove(p.getName());

			if (p.isOnline()) {
				setVisibility(p.getPlayer(), true);
				p.getPlayer().setWalkSpeed(oldWalkSpeed.get(p.getName()));
			}
		}
	}

	private boolean setVisibility(Player player, boolean visible) {
		String playerName = player.getName();
		Boolean isVisible = slendermansVisibility.get(playerName);
		if (isVisible == null) {
			isVisible = true;
		}
		if (!isVisible && !canBecomeVisible.get(playerName)) {
			player.sendMessage(ChatColor.RED + "Children are not far enough...");
			return false;
		}
		slendermansVisibility.put(playerName, visible);
		plugin.setPlayerVisibility(player, visible);
		player.setWalkSpeed(SPEED_VISIBLE);
		if (!visible) {
			player.setWalkSpeed(SPEED_INVISIBLE);
			coolDown.put(playerName, 5);
		}
		return true;
	}

	public void toogleVisibility(Player player) {
		if (coolDown.get(player.getName()) > 0) {
			return;
		}
		boolean visibility = isVisible(player);

		boolean visibilityChanged = setVisibility(player, !visibility);

		if (!visibilityChanged) {
			return;
		}

		String message = ChatColor.RED + "You are now visible";
		if (!slendermansVisibility.get(player.getName())) {
			setAlreadySeen(player, false);
			message = ChatColor.GREEN + "You are now invisible";
		}
		player.sendMessage(message);
	}

	public boolean isVisible(Player player) {
		if (player == null) {
			Utils.throwNullException();
		}
		return slendermansVisibility.get(player.getName());
	}

	public void coolDown() {
		for (OfflinePlayer p : gameplay.getPlayers(Gameplay.SLENDER_TEAM)) {
			if (!p.isOnline()) {
				continue;
			}

			Player player = p.getPlayer();

			Integer c = coolDown.get(player.getName());
			c--;
			if (c > 0) {
				coolDown.put(player.getName(), c);
				player.sendMessage(ChatColor.RED + c.toString()
						+ "s to wait...");
			} else if (c == 0) {
				coolDown.put(player.getName(), -1);
				player.sendMessage(ChatColor.GREEN + "You can become visible!");
			}
		}
	}

	public List<String> getSeenBy(Player player) {
		Set<OfflinePlayer> players = gameplay.getPlayers(Gameplay.SLENDER_TEAM);

		List<String> slenders = new ArrayList<String>();

		for (OfflinePlayer p : players) {
			if (!p.isOnline()) {
				continue;
			}

			if (!slendermansVisibility.get(p.getName())) {
				continue;
			}

			Player slenderman = p.getPlayer();

			boolean canSee = Utils.canSee(player, p.getPlayer(),
					gameplay.getViewDist(), gameplay.getViewAngle());
			if (canSee) {
				slenders.add(slenderman.getName());
				addSeenSlender(slenderman);
			}
		}

		return slenders;
	}

	public void addSeenSlender(Player player) {
		if (!slendermenSeen.contains(player.getName())) {
			player.sendMessage(ChatColor.RED + "Someone sees you...");
			setAlreadySeen(player, true);
		}
	}

	public void removeSeenSlender(Player player) {
		if (slendermenSeen.contains(player.getName())) {
			player.sendMessage(ChatColor.GREEN + "No one sees you...");
			setAlreadySeen(player, false);
		}
	}

	public List<String> getSeen() {
		return slendermenSeen;
	}

	public boolean isSeen(Player player) {
		return slendermenSeen.contains(player.getName());
	}

	private void setAlreadySeen(Player player, boolean isSeen) {
		if (isSeen) {
			slendermenSeen.add(player.getName());
		} else {
			slendermenSeen.remove(player.getName());
		}
	}

	public void checkCanBeVisible(Player player, int nearestPlayerDist) {
		int dist = gameplay.getChildrenSafeDist();

		boolean canBecomeVisible = true;
		if (dist > nearestPlayerDist) {
			canBecomeVisible = false;
		}

		this.canBecomeVisible.put(player.getName(), canBecomeVisible);
	}
}
