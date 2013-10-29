package org.chibitomo.plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.TreeMap;

import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.chibitomo.interfaces.IEventHandler;

public class EventListener implements Listener {
	private Map<Class<? extends Event>, TreeMap<Integer, IEventHandler>> listeners;
	private Plugin plugin;

	public EventListener(
			Plugin plugin,
			Map<Class<? extends Event>, TreeMap<Integer, IEventHandler>> listeners) {
		super();
		this.plugin = plugin;
		this.listeners = listeners;
	}

	private void send(Event event) {
		try {
			TreeMap<Integer, IEventHandler> map = listeners.get(event
					.getClass());
			if (map == null) {
				return;
			}
			for (int i : map.keySet()) {
				IEventHandler handler = map.get(i);
				Method method = handler.getClass().getDeclaredMethod(
						"on" + event.getEventName(), Event.class);
				method.invoke(handler, event);
			}
		} catch (InvocationTargetException e) {
			plugin.error((Exception) e.getCause());
		} catch (Exception e) {
			plugin.error(e);
		}
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onHangingBreakByEntityEvent(HangingBreakByEntityEvent event) {
		send(event);
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onSignChange(SignChangeEvent event) {
		send(event);
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onItemSpawn(ItemSpawnEvent event) {
		send(event);
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onPlayerJoin(PlayerJoinEvent event) {
		send(event);
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onPlayerDeath(PlayerDeathEvent event) {
		send(event);
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onItemHeld(PlayerItemHeldEvent event) {
		send(event);
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onBlockBreak(BlockBreakEvent event) {
		send(event);
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onBlockPlace(BlockPlaceEvent event) {
		send(event);
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onCraftPrepare(CraftItemEvent event) {
		send(event);
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onInteract(PlayerInteractEvent event) {
		send(event);
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onInventoryClick(InventoryClickEvent event) {
		send(event);
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onPlayerMoveEvent(PlayerMoveEvent event) {
		send(event);
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onHangingBreakEvent(HangingBreakEvent event) {
		send(event);
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onPlayerInteractEntityEvent(PlayerInteractEntityEvent event) {
		send(event);
	}
}
