package org.chibitomo.slender;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.chibitomo.interfaces.IPlugin;
import org.chibitomo.plugin.EventHandler;
import org.chibitomo.plugin.Plugin;

public class SlenderEventHandler extends EventHandler {

	private boolean canHangingBreakByEntityEvent = true;

	public SlenderEventHandler(IPlugin plugin, int priority) {
		super(plugin, priority);
	}

	@Override
	public void setManagedEvent() {
		addEvent(BlockBreakEvent.class);
		addEvent(PlayerInteractEvent.class);
		addEvent(PlayerInteractEntityEvent.class);
		addEvent(PlayerMoveEvent.class);
		addEvent(HangingBreakByEntityEvent.class);
		addEvent(PlayerDeathEvent.class);
		addEvent(BlockPlaceEvent.class);
	}

	public void onPlayerInteractEvent(Event givenEvent) throws Exception {
		PlayerInteractEvent event = (PlayerInteractEvent) givenEvent;
		((Slender) plugin).addPage(event);
		if (!((Slender) plugin).gameisStarted()) {
			return;
		}
		Action action = event.getAction();

		if (action.equals(Action.LEFT_CLICK_AIR)
				|| action.equals(Action.LEFT_CLICK_BLOCK)
				|| action.equals(Action.RIGHT_CLICK_AIR)
				|| action.equals(Action.RIGHT_CLICK_BLOCK)) {
			vanish(event);
		}
	}

	public void onBlockPlaceEvent(Event givenEvent) throws Exception {
		BlockPlaceEvent event = (BlockPlaceEvent) givenEvent;
		if (!((Slender) plugin).gameisStarted()) {
			return;
		}
		event.setCancelled(true);
	}

	private void vanish(PlayerInteractEvent event) {
		if (!((Slender) plugin).gameisStarted()) {
			return;
		}
		Player player = event.getPlayer();
		Slenderman slenderman = ((Slender) plugin).getSlenderman();
		if (!slenderman.isSlenderman(player)) {
			return;
		}
		slenderman.toogleVisibility();
	}

	public void onHangingBreakByEntityEvent(Event givenEvent) {
		HangingBreakByEntityEvent event = (HangingBreakByEntityEvent) givenEvent;
		Entity entity = event.getEntity();

		if (!canHangingBreakByEntityEvent) {
			return;
		}
		canHangingBreakByEntityEvent = false;

		plugin.getServer().getScheduler()
				.runTaskLater((Plugin) plugin, new Runnable() {
					@Override
					public void run() {
						canHangingBreakByEntityEvent = true;
					}
				}, 20);

		boolean isFrame = entity instanceof ItemFrame;

		Entity remover = event.getRemover();

		if (!(remover instanceof Player)) {
			return;
		}

		Player player = (Player) remover;

		if (!((Slender) plugin).gameisStarted()) {
			if (isFrame) {
				((Slender) plugin).removePage((ItemFrame) entity);
				Location loc = entity.getLocation();
				player.sendMessage("Page removed: x=" + loc.getBlockX() + " y="
						+ loc.getBlockY() + " z=" + loc.getBlockZ());
				return;
			}
		}

		event.setCancelled(true);
		if (((Slender) plugin).getSlenderman().isSlenderman(player)) {
			return;
		}

		if (isFrame) {
			((Slender) plugin).takePage(player, (ItemFrame) entity);
		}
	}

	public void onBlockBreakEvent(Event givenEvent) {
		if (!((Slender) plugin).gameisStarted()) {
			return;
		}
		BlockBreakEvent event = (BlockBreakEvent) givenEvent;
		event.setCancelled(true);
	}

	public void onPlayerInteractEntityEvent(Event givenEvent) {
		if (!((Slender) plugin).gameisStarted()) {
			return;
		}
		PlayerInteractEntityEvent event = (PlayerInteractEntityEvent) givenEvent;

		Slenderman slenderman = ((Slender) plugin).getSlenderman();
		Player player = event.getPlayer();

		if (slenderman.isSlenderman(player)) {
			return;
		}

		Entity entity = event.getRightClicked();

		if (entity instanceof ItemFrame) {
			((Slender) plugin).takePage(player, (ItemFrame) entity);
		}
	}

	public void onPlayerMoveEvent(Event givenEvent) {
		if (!((Slender) plugin).gameisStarted()) {
			return;
		}
		PlayerMoveEvent event = (PlayerMoveEvent) givenEvent;

		Player player = event.getPlayer();
		Slenderman slenderman = ((Slender) plugin).getSlenderman();

		boolean slendermanIsSeen = false;
		if (!slenderman.isSlenderman(player)) {
			slendermanIsSeen |= ((Slender) plugin).canSeeSlenderman(player);
		} else {
			if (slenderman.isAlreadySeen()) {
				Location newLoc = event.getFrom();
				newLoc.setPitch(event.getTo().getPitch());
				newLoc.setYaw(event.getTo().getYaw());
				event.setTo(newLoc);
			}
			slendermanIsSeen |= slenderman.isSeen();
		}

		if (slendermanIsSeen) {
			slenderman.setAlreadySeen(true);
		} else {
			slenderman.setAlreadySeen(false);
		}

		((Slender) plugin).checkDamages();
	}

	public void onPlayerDeathEvent(Event givenEvent) {
		if (!((Slender) plugin).gameisStarted()) {
			return;
		}
		PlayerDeathEvent event = (PlayerDeathEvent) givenEvent;
		Player player = event.getEntity();
		((Slender) plugin).info(player.toString());
		if (((Slender) plugin).isInDeadsTeam(player)) {
			event.setDeathMessage("Slenderman cought " + player.getName()
					+ "...");
		}
		((Slender) plugin).checkDead();
	}
}
