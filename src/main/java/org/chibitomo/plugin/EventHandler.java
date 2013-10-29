package org.chibitomo.plugin;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.event.Event;
import org.chibitomo.interfaces.IEventHandler;
import org.chibitomo.interfaces.IPlugin;

public abstract class EventHandler implements IEventHandler {

	protected Map<Class<? extends Event>, Integer> map = new HashMap<Class<? extends Event>, Integer>();
	protected int PRIORITY = 0;
	protected IPlugin plugin;

	public EventHandler(IPlugin plugin, int priority) {
		this.plugin = plugin;
		PRIORITY = priority;
	}

	public final Map<Class<? extends Event>, Integer> getManagedEvent() {
		return map;
	}

	protected void addEvent(Class<? extends Event> eventClass) {
		map.put(eventClass, PRIORITY);
	}
}
