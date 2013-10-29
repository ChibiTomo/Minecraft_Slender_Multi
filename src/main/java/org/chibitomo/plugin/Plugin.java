package org.chibitomo.plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.java.JavaPlugin;
import org.chibitomo.interfaces.IEventHandler;
import org.chibitomo.interfaces.IPlugin;
import org.chibitomo.logger.Logger;

public abstract class Plugin extends JavaPlugin implements IPlugin {

	protected static String NAME = "A_ChibiTomos_Plugin";
	protected static String ROOT = "plugins/" + NAME;
	protected static String CONFIG_FILE = ROOT + "/config.yml";
	public static Integer EVENTPRIORITY;

	protected Server server;
	protected Logger logger;
	protected Map<Class<? extends Event>, TreeMap<Integer, IEventHandler>> eventHandlers = new HashMap<Class<? extends Event>, TreeMap<Integer, IEventHandler>>();

	/**
	 * Call when plugin is initialized. Should set {@link NAME} and
	 * {@link PRIORITY}
	 */
	protected abstract void init();

	/**
	 * Call when plugin is closed.
	 */
	protected abstract void close();

	/**
	 * Register all event handlers by calling {@link addPluginsEventHandler}.
	 */
	protected abstract void registerEventHandlers();

	protected abstract void registerCommandHandler(String cmd);

	@Override
	public void onEnable() {
		try {
			beforeInit();
			init();
			afterInit();
		} catch (Exception e) {
			error(e);
		}
	}

	private void beforeInit() throws Exception {
		logger = new Logger(getLogger());
		info("Starting a ChibiPlugin");

		saveDefaultConfig();
		reloadConfig();

		server = getServer();
	}

	private void afterInit() throws Exception {
		ROOT = "plugins/" + NAME;
		saveConfig();

		registerEventHandlers(); // Register all the event handlers set by
									// the plugin.
		registerEvents(); // Start listening all the events.

		registerCommandHandlers();
	}

	public final String getPluginName() {
		return NAME;
	}

	/**
	 * Register the {@link EventListener} to all events.
	 */
	public void registerEvents() {
		getServer().getPluginManager().registerEvents(
				new EventListener(this, eventHandlers), this);
	}

	/**
	 * Register your {@link EventHandler} to listen to event.
	 * 
	 * @param eventHandler
	 */
	protected void addPluginsEventHandler(EventHandler eventHandler) {
		eventHandler.setManagedEvent();
		Map<Class<? extends Event>, Integer> map = eventHandler
				.getManagedEvent();
		for (Class<? extends Event> event : map.keySet()) {
			addEventHandler(event, eventHandler, map.get(event));
		}
	}

	/**
	 * This method add an {@link EventHandler}, linked with an {@link Event}, to
	 * the event handlers list. It can have a priority set. Lower int means
	 * highest priority.
	 * 
	 * @param handler
	 * @param priority
	 * @param event
	 */
	private void addEventHandler(Class<? extends Event> event,
			IEventHandler handler, int priority) {
		TreeMap<Integer, IEventHandler> map = new TreeMap<Integer, IEventHandler>();
		map.put(priority, handler);
		TreeMap<Integer, IEventHandler> tmp = eventHandlers.get(event);
		if (tmp == null) {
			tmp = new TreeMap<Integer, IEventHandler>();
		}
		tmp.putAll(map);
		eventHandlers.put(event, tmp);
	}

	private void registerCommandHandlers() {
		for (String cmd : getDescription().getCommands().keySet()) {
			registerCommandHandler(cmd);
		}
	}

	protected void setCommandExecutor(String cmd, CommandHandler handler) {
		getCommand(cmd).setExecutor(handler);
	}

	@Override
	public void onDisable() {
		try {
			// super.onDisable();
			close();
			// saveConfig();
			// info("Closing " + NAME + " plugin");
		} catch (Exception e) {
			error(e);
		}
	}

	public void info(String msg) {
		logger.info(msg);
	}

	public void error(Exception e) {
		logger.error(e);
	}

	public Player getPlayer(String name) {
		return getServer().getPlayerExact(name);
	}
}
