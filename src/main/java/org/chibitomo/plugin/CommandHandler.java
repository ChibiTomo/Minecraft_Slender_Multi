package org.chibitomo.plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.chibitomo.interfaces.ICommandHandler;

public class CommandHandler implements ICommandHandler {

	protected Plugin plugin;

	public CommandHandler(Plugin plugin) {
		this.plugin = plugin;
	}

	public final boolean onCommand(CommandSender sender, Command cmd,
			String alias, String[] args) {
		try {
			Method method = this.getClass().getDeclaredMethod(cmd.getName(),
					CommandSender.class, String[].class);
			return (Boolean) method.invoke(this, sender, args);
		} catch (InvocationTargetException e) {
			plugin.error((Exception) e.getCause());
		} catch (Exception e) {
			plugin.error(e);
		}
		return false;
	}

	protected void sendError(CommandSender sender, String msg) {
		sender.sendMessage(ChatColor.DARK_RED + msg);
	}

	protected void sendError(CommandSender sender) {
		sender.sendMessage(ChatColor.DARK_RED
				+ "You don't have permission to do this.");
	}
}