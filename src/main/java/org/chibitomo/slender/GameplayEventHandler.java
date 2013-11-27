package org.chibitomo.slender;

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
import org.chibitomo.plugin.EventHandler;

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
		event.setCancelled(true);
	}

	public void onPlayerJoinEvent(Event givenEvent) {
		if (!slender.gameisStarted()) {
			return;
		}
		PlayerJoinEvent event = (PlayerJoinEvent) givenEvent;
		Player player = event.getPlayer();
		slender.getGameplay().addToGame(player);
	}

	public void onPlayerQuitEvent(Event givenEvent) {
		if (!slender.gameisStarted()) {
			return;
		}
		PlayerQuitEvent event = (PlayerQuitEvent) givenEvent;
		Player player = event.getPlayer();
		slender.getGameplay().deleteDammager(player);
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
		slender.getGameplay().deleteDammager(player);
		if (slender.getGameplay().isInTeam(Gameplay.DEADS_TEAM, player)) {
			event.setDeathMessage("Slenderman cought " + player.getName()
					+ "...");
		}
		slender.getGameplay().checkDead();
	}

	private void avoidHangingBreakByEntityEventDoubleCall() {
		canHangingBreakByEntityEvent = false;

		Utils.delay(slender, this, "resetHangingBreakByEntityEvent", 20);
	}

	public void resetHangingBreakByEntityEvent() {
		canHangingBreakByEntityEvent = true;
	}

}
