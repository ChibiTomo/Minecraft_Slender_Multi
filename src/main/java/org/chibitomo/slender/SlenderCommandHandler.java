package org.chibitomo.slender;

import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.chibitomo.plugin.CommandHandler;

public class SlenderCommandHandler extends CommandHandler {
	private Slender slender;

	public SlenderCommandHandler(Slender plugin) {
		super(plugin);
		slender = plugin;
	}

	public boolean start_slenderman(CommandSender sender, String[] args) {
		if (!(sender instanceof Player)) {
			sendError(sender, "Only players can use this command.");
			return true;
		}
		if (args.length > 0) {
			sendError(sender, "To many arguments.");
			return false;
		}

		if (slender.gameisStarted()) {
			sendError(sender, "A game is already running.");
			return true;
		}

		slender.gameStart();
		return true;
	}

	public boolean stop_slenderman(CommandSender sender, String[] args) {
		if (args.length > 0) {
			sendError(sender, "To many arguments.");
			return false;
		}

		if (!slender.gameisStarted()) {
			sendError(sender, "No game is running.");
			return true;
		}

		slender.gameStop();
		return true;
	}

	public boolean add_page(CommandSender sender, String[] args) {
		if (!(sender instanceof Player)) {
			sendError(sender, "Only players can use this command.");
			return true;
		}
		if (args.length > 0) {
			sendError(sender, "To many arguments.");
			return false;
		}

		Player player = (Player) sender;
		player.sendMessage(ChatColor.YELLOW
				+ "Click where you want to place a page.");
		slender.playerAddPage(player);
		return true;
	}

	public boolean add_message(CommandSender sender, String[] args) {
		if (args.length < 1) {
			sendError(sender, "To few arguments.");
			return false;
		}

		String msg = StringUtils.join(args, " ");
		String result = slender.addMessage(msg);
		String message = ChatColor.GREEN + "New message added: ";
		if (result == null) {
			message = ChatColor.RED + "Message not added, already exists: ";
		}

		sender.sendMessage(message + ChatColor.RESET + msg);
		return true;
	}

	public boolean set_slenderman(CommandSender sender, String[] args) {
		if (args.length == 0) {
			sendError(sender, "To few arguments.");
			return false;
		}
		if (args.length > 1) {
			sendError(sender, "To many arguments.");
			return false;
		}

		Player player = plugin.getPlayer(args[0]);
		if (player.equals(null)) {
			sendError(sender, "Cannot find player " + args[0]);
			return false;
		}
		slender.getSlenderman().setSlenderman(player);
		return true;
	}

	public boolean test(CommandSender sender, String[] args) {
		if (!plugin.debigIsOn()) {
			return true;
		}
		Player p1 = (Player) sender;
		Player p2 = plugin.getServer().getPlayer("toto");
		sender.sendMessage("Can see toto? " + Utils.canSee(p1, p2, 15, 100));
		return true;
	}
}
