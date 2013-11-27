package org.chibitomo.slender_old;

import org.apache.commons.lang.StringUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.chibitomo.plugin.CommandHandler;

public class SlenderCommandHandler extends CommandHandler {
	public SlenderCommandHandler(OldSlender plugin) {
		super(plugin);
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

		if (((OldSlender) plugin).gameisStarted()) {
			sendError(sender, "A game is already running.");
			return true;
		}

		Player player = (Player) sender;
		((OldSlender) plugin).gameStart(player);
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

		((OldSlender) plugin).addPage = true;
		return true;
	}

	public boolean add_message(CommandSender sender, String[] args) {
		if (args.length < 1) {
			sendError(sender, "To few arguments.");
			return false;
		}

		((OldSlender) plugin).addMessage(StringUtils.join(args, " "));

		sender.sendMessage("New message added: " + args[0]);
		return true;
	}

	public boolean stop_slenderman(CommandSender sender, String[] args) {
		if (args.length > 0) {
			sendError(sender, "To many arguments.");
			return false;
		}

		if (!((OldSlender) plugin).gameisStarted()) {
			sendError(sender, "No game is running.");
			return true;
		}

		((OldSlender) plugin).gameStop();
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
		((OldSlender) plugin).getSlenderman().setSlenderman(player);
		return true;
	}
}