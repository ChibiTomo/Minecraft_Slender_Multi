package org.chibitomo.slender;

import org.bukkit.event.Event;
import org.bukkit.event.server.ServerListPingEvent;
import org.chibitomo.interfaces.IPlugin;
import org.chibitomo.plugin.EventHandler;

public class SlenderEventHandler extends EventHandler {

	private Slender slender;

	public SlenderEventHandler(IPlugin plugin, int priority) {
		super(plugin, priority);

		slender = (Slender) plugin;
	}

	@Override
	public void setManagedEvent() {
		addEvent(ServerListPingEvent.class);
	}

	public void onServerListPingEvent(Event givenEvent) {
		ServerListPingEvent event = (ServerListPingEvent) givenEvent;
		event.setMotd("Bonjour a tous!!!");
	}

}
