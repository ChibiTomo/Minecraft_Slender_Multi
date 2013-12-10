package org.chibitomo.slender.event;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.chibitomo.interfaces.IPlugin;
import org.chibitomo.misc.Utils;
import org.chibitomo.plugin.EventHandler;
import org.chibitomo.slender.gameplay.Gameplay;
import org.chibitomo.slender.gameplay.Slenderman;
import org.chibitomo.slender.plugin.Slender;

public class GameplayEventHandler extends EventHandler {

	private Slender slender;
	private boolean canHangingBreakByEntityEvent = true;

	public GameplayEventHandler(IPlugin plugin, int priority) {
		super(plugin, priority);

		slender = (Slender) plugin;
	}

	@Override
	public void setManagedEvent() {
		addEvent(BlockBreakEvent.class);
		addEvent(BlockPlaceEvent.class);
		addEvent(PlayerInteractEvent.class);
		addEvent(HangingBreakByEntityEvent.class);
		addEvent(PlayerInteractEntityEvent.class);
		addEvent(PlayerMoveEvent.class);
		addEvent(PlayerJoinEvent.class);
		addEvent(PlayerQuitEvent.class);
		addEvent(PlayerDeathEvent.class);
		addEvent(EntityRegainHealthEvent.class);
	}

	public void onEntityRegainHealthEvent(Event givenEvent) {
		if (!slender.gameisStarted()) {
			return;
		}
		EntityRegainHealthEvent event = (EntityRegainHealthEvent) givenEvent;
		Entity entity = event.getEntity();

		if (!(entity instanceof Player)) {
			return;
		}

		double currentHealth = ((Player) entity).getHealth();
		if (slender.canRegainHealth()
				&& (currentHealth <= slender.getMaxRegainHealth())) {
			return;
		}
		event.setCancelled(true);
	}

	public void onPlayerJoinEvent(Event givenEvent) {
		if (!slender.gameisStarted()) {
			return;
		}
		PlayerJoinEvent event = (PlayerJoinEvent) givenEvent;
		Player player = event.getPlayer();
		Gameplay gameplay = slender.getGameplay();
		gameplay.restoreOldTeam(player);

		String message = "Just a spectator who's joining...";
		if (gameplay.isInTeam(Gameplay.CHILDREN_TEAM, player)) {
			message = "A Children just came back!";
		}
		if (gameplay.isInTeam(Gameplay.SLENDER_TEAM, player)) {
			message = "Hide your soul... A Slenderman came back...";
		}
		// if (gameplay.isInTeam(Gameplay.PROXIES_TEAM, player)) {
		// message = "Fear the Proxy...";
		// }
		event.setJoinMessage(ChatColor.GRAY + "" + ChatColor.ITALIC + message);
	}

	public void onPlayerQuitEvent(Event givenEvent) {
		if (!slender.gameisStarted()) {
			return;
		}
		PlayerQuitEvent event = (PlayerQuitEvent) givenEvent;
		Player player = event.getPlayer();
		event.setQuitMessage(ChatColor.GRAY + "" + ChatColor.ITALIC
				+ player.getName()
				+ " just disconnect. Maybe will he come back later...");
		slender.getGameplay().playerLeave(player);
	}

	public void onBlockPlaceEvent(Event givenEvent) throws Exception {
		if (!slender.gameisStarted()) {
			return;
		}
		BlockPlaceEvent event = (BlockPlaceEvent) givenEvent;
		event.setCancelled(true);
	}

	public void onBlockBreakEvent(Event givenEvent) {
		if (!slender.gameisStarted()) {
			return;
		}
		BlockBreakEvent event = (BlockBreakEvent) givenEvent;
		event.setCancelled(true);
	}

	public void onPlayerInteractEvent(Event givenEvent) throws Exception {
		if (!slender.gameisStarted()) {
			return;
		}
		PlayerInteractEvent event = (PlayerInteractEvent) givenEvent;
		if (slender.getGameplay().getTeamManager()
				.isInTeam(Gameplay.DEADS_TEAM, event.getPlayer())) {
			event.setCancelled(true);
		}

		vanish(event);
	}

	public void onHangingBreakByEntityEvent(Event givenEvent) {
		HangingBreakByEntityEvent event = (HangingBreakByEntityEvent) givenEvent;
		Entity entity = event.getEntity();

		if (!slender.gameisStarted()) {
			return;
		}

		event.setCancelled(true);

		if (!canHangingBreakByEntityEvent) {
			return;
		}
		avoidHangingBreakByEntityEventDoubleCall();

		boolean isFrame = entity instanceof ItemFrame;
		Entity remover = event.getRemover();

		if (!(remover instanceof Player)) {
			return;
		}

		Player player = (Player) remover;

		if (slender.getSlenderman().isSlenderman(player)) {
			return;
		}

		if (isFrame) {
			slender.takePage(player, (ItemFrame) entity);
		}
	}

	public void onPlayerInteractEntityEvent(Event givenEvent) {
		if (!slender.gameisStarted()) {
			return;
		}

		PlayerInteractEntityEvent event = (PlayerInteractEntityEvent) givenEvent;
		Slenderman slenderman = slender.getSlenderman();
		Player player = event.getPlayer();
		Entity entity = event.getRightClicked();

		if (!(entity instanceof ItemFrame)) {
			return;
		}

		if (slender.getGameplay().getTeamManager()
				.isInTeam(Gameplay.DEADS_TEAM, player)) {
			event.setCancelled(true);
		}

		if (!slenderman.isSlenderman(player)) {
			slender.takePage(player, (ItemFrame) entity);
		}
	}

	private void vanish(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		Slenderman slenderman = slender.getSlenderman();
		if (!slenderman.isSlenderman(player)) {
			return;
		}
		slenderman.toogleVisibility(player);
	}

	public void onPlayerMoveEvent(Event givenEvent) {
		if (!slender.gameisStarted()) {
			return;
		}
		PlayerMoveEvent event = (PlayerMoveEvent) givenEvent;

		Player player = event.getPlayer();
		Slenderman slenderman = slender.getSlenderman();

		slender.getGameplay().checkWhoSeeWho();

		if (slenderman.isSlenderman(player) && slenderman.isSeen(player)) {
			Location newLoc = event.getFrom();
			newLoc.setPitch(event.getTo().getPitch());
			newLoc.setYaw(event.getTo().getYaw());
			event.setTo(newLoc);
		}
	}

	public void onPlayerDeathEvent(Event givenEvent) {
		if (!slender.gameisStarted()) {
			return;
		}
		PlayerDeathEvent event = (PlayerDeathEvent) givenEvent;
		Player player = event.getEntity();
		player.getInventory().clear();

		Gameplay gameplay = slender.getGameplay();
		gameplay.deleteDammager(player);

		if (gameplay.isInTeam(Gameplay.DEADS_TEAM, player)) {
			event.setDeathMessage(ChatColor.RED + "Slenderman cought "
					+ player.getName() + "...");
		} else {
			gameplay.addPlayer(Gameplay.DEADS_TEAM, player);
		}
		Utils.delay(slender, gameplay, "syncScores");
	}

	private void avoidHangingBreakByEntityEventDoubleCall() {
		canHangingBreakByEntityEvent = false;

		Utils.delay(slender, this, "resetHangingBreakByEntityEvent", 20);
	}

	public void resetHangingBreakByEntityEvent() {
		canHangingBreakByEntityEvent = true;
	}

}
