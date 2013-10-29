package org.chibitomo.interfaces;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public interface ICommandHandler extends CommandExecutor {
	public boolean onCommand(CommandSender sender, Command cmd, String alias,
			String[] args);
}
