package org.chibitomo.slender;

import org.bukkit.World;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitTask;
import org.chibitomo.plugin.Plugin;

public class Slender extends Plugin {

	public static final String WORLD_NAME_PATH = "world";
	public static final String PAGE_QUANTITY_PATH = "page_qty";
	public static final String PAGE_MESSAGES_PATH = "page_messages";
	public static final String VIEW_DIST_PATH = "view_dist";
	public static final String VIEW_ANGLE_PATH = "view_angle";
	public static final String MIN_DAMAGE_DIST = "min_damage_dist";
	public static final String MAX_HEATH = "max_health";
	public static final String MAX_DAMAGE_PERCENT = "max_damage_percent";
	public static final String PAGE_LOCATION_PATH = "page_locations";

	private boolean isClosing;
	private boolean gameIsStarted;

	private Gameplay gameplay;
	private PageManager pageManager;
	private FrameManager frameManager;
	private BukkitTask eternalNightTask = null;

	@Override
	protected void init() {
		debugOn = false;

		NAME = "Slender Multi";
		EVENTPRIORITY = 1;
		isClosing = false;
		gameIsStarted = false;

		gameplay = new Gameplay(this);
		pageManager = new PageManager(this);
		frameManager = new FrameManager(this);
	}

	@Override
	protected void close() {
		isClosing = true;
		if (gameIsStarted) {
			gameStop();
		}

		frameManager.removeFrames();
	}

	@Override
	protected void registerEventHandlers() {
		addPluginsEventHandler(new GameplayEventHandler(this, EVENTPRIORITY));
		addPluginsEventHandler(new CommandEventHandler(this, EVENTPRIORITY + 1));
		addPluginsEventHandler(new SlenderEventHandler(this, EVENTPRIORITY + 2));
	}

	@Override
	protected void registerCommandHandler(String cmd) {
		setCommandExecutor(cmd, new SlenderCommandHandler(this));
	}

	private void eternal_night_on() {
		if (eternalNightTask != null) {
			return;
		}
		eternalNightTask = Utils.time(this, this, "setNight", 0, 100);
	}

	public void setNight() {
		gameplay.getWorld().setTime(15000);
	}

	private void eternal_night_off() {
		if (eternalNightTask == null) {
			return;
		}
		eternalNightTask.cancel();
	}

	public void setPlayerVisibility(Player player, Boolean isVisible) {
		World world = player.getWorld();

		for (Player p : world.getPlayers()) {
			if (isVisible) {
				p.showPlayer(player);
			} else {
				p.hidePlayer(player);
			}
		}
	}

	public boolean gameisStarted() {
		return gameIsStarted;
	}

	public boolean removePage(ItemFrame page) {
		return pageManager.remove(page);
	}

	public Slenderman getSlenderman() {
		return gameplay.getSlenderman();
	}

	public void gameStart() {
		frameManager.start();
		Utils.delay(this, this, "starting");
	}

	public void starting() {
		pageManager.start();
		Utils.delay(this, this, "startingGameplay");
	}

	public void startingGameplay() {
		gameplay.start();

		eternal_night_on();
		gameIsStarted = true;
	}

	public void gameStop() {
		frameManager.stop();
		pageManager.stop();
		gameplay.stop();

		eternal_night_off();
		gameIsStarted = false;

		server.broadcastMessage("Stop current game.");
	}

	public String addMessage(String msg) {
		return pageManager.addMessage(msg);
	}

	public void addPage(PlayerInteractEvent event) {
		gameplay.addPage(event);
	}

	public void takePage(Player player, ItemFrame entity) {
		gameplay.takePage(player, entity);
	}

	public void playerAddPage(Player player) {
		gameplay.playerAddPage(player);
	}

	public FrameManager getFrameManager() {
		return frameManager;
	}

	public PageManager getPageManager() {
		return pageManager;
	}

	public Gameplay getGameplay() {
		return gameplay;
	}

	public boolean isClosing() {
		return isClosing;
	}
}
