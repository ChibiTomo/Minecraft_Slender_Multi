package org.chibitomo.slender.gameplay;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import org.chibitomo.slender.plugin.Slender;

public class TeamManager {
	private ScoreboardManager scoreboardManager;
	private Scoreboard mainScoreboard;
	private Map<String, Scoreboard> scoreboards;
	private Map<String, Objective> teamObjectives;
	private Map<String, List<String>> teamScores;
	private Map<String, OfflinePlayer> scores;
	private Slender plugin;
	private Map<String, ChatColor> colors;
	private Map<String, String> playersTeam;

	public TeamManager(Slender plugin) {
		this.plugin = plugin;

		scoreboardManager = Bukkit.getScoreboardManager();
		mainScoreboard = scoreboardManager.getNewScoreboard();

		scoreboards = new HashMap<String, Scoreboard>();
		teamObjectives = new HashMap<String, Objective>();
		teamScores = new HashMap<String, List<String>>();
		scores = new HashMap<String, OfflinePlayer>();
		colors = new HashMap<String, ChatColor>();
		playersTeam = new HashMap<String, String>();
	}

	public void addTeam(String name, ChatColor color) {
		addTeam(name, color, false);
	}

	public void addTeam(String name, ChatColor color, boolean friendlyFire) {
		Scoreboard scoreboard = scoreboardManager.getNewScoreboard();

		registerTeam(mainScoreboard, name, color, friendlyFire);
		scoreboards.put(name, scoreboard);
		colors.put(name, color);

		for (String scoreboardName : scoreboards.keySet()) {
			Scoreboard s = scoreboards.get(scoreboardName);
			for (Team t : mainScoreboard.getTeams()) {
				String teamName = t.getName();
				if (s.getTeam(teamName) == null) {
					registerTeam(s, teamName, colors.get(teamName),
							t.allowFriendlyFire());
				}
			}
		}
	}

	private void registerTeam(Scoreboard scoreboard, String name,
			ChatColor color, boolean friendlyFire) {
		Team team = scoreboard.registerNewTeam(name);
		team.setDisplayName(color + name);
		team.setPrefix(color + "");
		team.setSuffix(" " + name + ChatColor.RESET);
		team.setAllowFriendlyFire(friendlyFire);
		team.setCanSeeFriendlyInvisibles(true);

		Objective objective = scoreboard.registerNewObjective(name, "dummy");
		objective.setDisplaySlot(DisplaySlot.SIDEBAR);
		objective.setDisplayName(color + name);

		teamObjectives.put(name, objective);
	}

	public Team getTeam(String teamName) {
		return mainScoreboard.getTeam(teamName);
	}

	public void addPlayer(String teamName, OfflinePlayer player) {
		Team team = mainScoreboard.getTeam(teamName);
		team.addPlayer(player);
		playersTeam.put(player.getName(), teamName);

		for (String name : scoreboards.keySet()) {
			Scoreboard s = scoreboards.get(name);

			Team t = s.getTeam(teamName);
			if (t == null) {
				plugin.error(new Exception("Cannot find the given team: "
						+ teamName));
				continue;
			}
			t.addPlayer(player);
		}

		if (player.isOnline()) {
			Scoreboard scoreboard = scoreboards.get(teamName);
			Player p = player.getPlayer();
			p.setScoreboard(scoreboard);

			plugin.debug(scoreboard.getPlayers().toString());

			if (teamName == Gameplay.DEADS_TEAM) {
				plugin.setPlayerVisibility(p, false);
				// for (OfflinePlayer offP : getTeam(Gameplay.DEADS_TEAM)
				// .getPlayers()) {
				// if (!offP.isOnline()) {
				// continue;
				// }
				// offP.getPlayer().showPlayer(p);
				// p.showPlayer(offP.getPlayer());
				// }
			}
		}
	}

	public void removePlayer(String teamName, OfflinePlayer player) {
		Team team = mainScoreboard.getTeam(teamName);
		team.removePlayer(player);

		if (player.isOnline()) {
			player.getPlayer().setScoreboard(
					scoreboardManager.getNewScoreboard());
		}

		for (Scoreboard s : scoreboards.values()) {
			Team t = s.getTeam(teamName);
			t.removePlayer(player);
		}
	}

	public void addScore(String scoreId, String scoreName, ChatColor color,
			String[] teams) {
		addScore(scoreId, scoreName, color, teams, 0);
	}

	public void addScore(String scoreId, String scoreName, ChatColor color,
			String[] teams, int defaultValue) {

		OfflinePlayer offPlayer = Bukkit.getOfflinePlayer(color + scoreName);

		for (String name : teams) {
			Objective objective = teamObjectives.get(name);
			Score score = objective.getScore(offPlayer);
			score.setScore(defaultValue);
		}

		teamScores.put(scoreId, Arrays.asList(teams));
		scores.put(scoreId, offPlayer);
	}

	public void setScore(String scoreId, int value) {
		List<String> teams = teamScores.get(scoreId);
		OfflinePlayer scorePlayer = scores.get(scoreId);

		for (String teamName : teams) {
			Scoreboard s = scoreboards.get(teamName);
			Team team = s.getTeam(teamName);
			Objective objective = team.getScoreboard().getObjective(teamName);
			objective.setDisplaySlot(DisplaySlot.SIDEBAR);
			Score score = objective.getScore(scorePlayer);
			score.setScore(value);
		}
	}

	public boolean isInTeam(String teamName, OfflinePlayer player) {
		return getTeam(teamName).hasPlayer(player);
	}

	public boolean inSameTeam(Player player, Player otherPlayer) {
		for (Team team : mainScoreboard.getTeams()) {
			if (team.hasPlayer(player) && team.hasPlayer(otherPlayer)) {
				return true;
			}
		}
		return false;
	}

	public Scoreboard getScoreboard(String teamName) {
		return scoreboards.get(teamName);
	}

	public String getOldTeam(OfflinePlayer offlinePlayer) {
		return playersTeam.get(offlinePlayer.getName());
	}
}
