package org.chibitomo.slender.gameplay;

import org.bukkit.entity.Player;

public class Dammager {
	private String playerName;
	private int maxHealth;
	private Gameplay gameplay;

	public Dammager(Gameplay gameplay, Player player, int maxHealth) {
		this.gameplay = gameplay;
		playerName = player.getName();
		this.maxHealth = maxHealth;
	}

	private void damage() {
		Player player = gameplay.getPlugin().getServer().getPlayer(playerName);
		if (player == null) {
			return;
		}
		if (player.getMaxHealth() != maxHealth) {
			player.setMaxHealth(maxHealth);
		}
		gameplay.manageDead(player);
		int dist = gameplay.getDistance(player);

		double damage = gameplay.calculDamage(dist);
		// gameplay.getPlugin().debug(
		// "Damaging " + player.getName() + ": " + damage);
		if (damage < 1) {
			return;
		}
		if (player.getHealth() <= damage) {
			gameplay.addDeadPlayer(player);
		}
		player.damage(damage);
	}

}