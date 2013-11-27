package org.chibitomo.slender;

import org.bukkit.entity.Player;

public class Dammager {
	private Player player;
	private int maxHealth;
	private Gameplay gameplay;

	public Dammager(Gameplay gameplay, Player player, int maxHealth) {
		this.gameplay = gameplay;
		this.player = player;
		this.maxHealth = maxHealth;
	}

	public void damage() {
		if (player.getMaxHealth() != maxHealth) {
			player.setMaxHealth(maxHealth);
		}
		gameplay.manageDead(player);
		int dist = gameplay.getDistance(player);

		double damage = gameplay.calculDamage(dist);
		gameplay.getPlugin().debug(
				"Damaging " + player.getName() + ": " + damage);
		if (damage < 1) {
			return;
		}
		if (player.getHealth() <= damage) {
			gameplay.addDeadPlayer(player);
		}
		player.damage(damage);
	}

}