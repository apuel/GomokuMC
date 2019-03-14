package org.us._42.laphicet.gomoku.minecraft;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class GomokuMC extends JavaPlugin implements Listener {
	@Override
	public void onEnable() {
	}
	
	@Override
	public void onDisable() {
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (command.getName().equals("test")) {
			if (sender instanceof Player) {
				Location origin = ((Player)sender).getLocation();
				sender.sendMessage("test");
			}
			return (true);
		}
		return (false);
	}
	
	@EventHandler
	public void onPlayerInteractEvent(PlayerInteractEvent event) {
		if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
		}
	}
}
