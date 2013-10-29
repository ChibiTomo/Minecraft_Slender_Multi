package org.chibitomo.interfaces;

import java.util.Map;

import org.bukkit.event.Event;

public interface IEventHandler {
	public Map<Class<? extends Event>, Integer> getManagedEvent();

	public void setManagedEvent();
}
