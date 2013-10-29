package org.chibitomo.interfaces;

import org.bukkit.Server;

public interface IPlugin {
	public void info(String msg);

	public void onEnable();

	public void registerEvents();

	public Server getServer();
}
