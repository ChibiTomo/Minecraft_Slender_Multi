package org.chibitomo.slender.plugin;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.chibitomo.misc.Utils;
import org.chibitomo.plugin.Plugin;
import org.chibitomo.slender.event.CommandEventHandler;
import org.chibitomo.slender.event.GameplayEventHandler;
import org.chibitomo.slender.event.SlenderEventHandler;
import org.chibitomo.slender.gameplay.Gameplay;
import org.chibitomo.slender.gameplay.Slenderman;
import org.chibitomo.slender.page.FrameManager;
import org.chibitomo.slender.page.PageManager;

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
	public static final String SLENDERMEN_HAVE_COMPASS = "slendermen_have_compass";
	public static final String CAN_REGAIN_HEALTH = "can_regain_health";
	public static final String MAX_REGAIN_HEALTH = "max_regain_health";

	private boolean isClosing;
	private boolean gameIsStarted;

	private Gameplay gameplay;
	private PageManager pageManager;
	private FrameManager frameManager;
	private Misc misc;

	private BukkitTask eternalNightTask = null;

	@Override
	protected void init() {
		debugOn = true;

		NAME = "Slender Multi";
		EVENTPRIORITY = 1;
		isClosing = false;
		gameIsStarted = false;

		gameplay = new Gameplay(this);
		pageManager = new PageManager(this);
		frameManager = new FrameManager(this);
		misc = new Misc(this);
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
		debug("Start FrameManager");
		frameManager.start();
		Utils.delay(this, this, "starting", 10);
	}

	public void starting() {
		debug("Start PageManager");
		pageManager.start();
		Utils.delay(this, this, "startingGameplay", 10);
	}

	public void startingGameplay() {
		debug("Start Gameplay");
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

	public void addPage(Player player, Block block, BlockFace face) {
		misc.addPage(player, block, face);
	}

	public void takePage(Player player, ItemFrame entity) {
		gameplay.takePage(player, entity);
	}

	public void listenAddPage(Player player) {
		misc.listenAddPage(player);
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

	public boolean canRegainHealth() {
		return gameplay.canRegainHealth();
	}

	public double getMaxRegainHealth() {
		return gameplay.getMaxRegainHealth();
	}
}
