package org.chibitomo.slender;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.chibitomo.interfaces.IPlugin;
import org.chibitomo.plugin.EventHandler;

public class CommandEventHandler extends EventHandler {

	private boolean canHangingBreakByEntityEvent = true;
	private Slender slender;

	public CommandEventHandler(IPlugin plugin, int priority) {
		super(plugin, priority);

		slender = (Slender) plugin;
	}

	@Override
	public void setManagedEvent() {
		addEvent(PlayerInteractEvent.class);
		addEvent(HangingBreakByEntityEvent.class);
	}

	public void onPlayerInteractEvent(Event givenEvent) throws Exception {
		PlayerInteractEvent event = (PlayerInteractEvent) givenEvent;
		if (slender.gameisStarted()) {
			return;
		}
		Action action = event.getAction();

		if (action.equals(Action.LEFT_CLICK_BLOCK)
				|| action.equals(Action.RIGHT_CLICK_BLOCK)) {
			slender.addPage(event);
		}
	}

	public void onHangingBreakByEntityEvent(Event givenEvent) {
		HangingBreakByEntityEvent event = (HangingBreakByEntityEvent) givenEvent;
		Entity entity = event.getEntity();

		if (!canHangingBreakByEntityEvent) {
			plugin.debug("return: cant hanging break");
			return;
		}
		avoidHangingBreakByEntityEventDoubleCall();

		boolean isFrame = entity instanceof ItemFrame;
		Entity remover = event.getRemover();

		if (!(remover instanceof Player)) {
			plugin.debug("return: not a player");
			return;
		}

		Player player = (Player) remover;

		if (slender.gameisStarted() || !isFrame) {
			plugin.debug("return: game started || not frame");
			return;
		}
		if (slender.removePage((ItemFrame) entity)) {
			Location loc = entity.getLocation();
			player.sendMessage(ChatColor.RED + "Page removed:"
					+ ChatColor.RESET + " x=" + loc.getBlockX() + " y="
					+ loc.getBlockY() + " z=" + loc.getBlockZ());
		}
	}

	private void avoidHangingBreakByEntityEventDoubleCall() {
		canHangingBreakByEntityEvent = false;

		Utils.delay(slender, this, "resetHangingBreakByEntityEvent", 10);
	}

	public void resetHangingBreakByEntityEvent() {
		canHangingBreakByEntityEvent = true;
	}

}
