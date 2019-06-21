package org.us._42.laphicet.gomoku.minecraft;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.io.Files;

import net.md_5.bungee.api.ChatColor;

public class GomokuMC extends JavaPlugin {
	public Map<Player, GomokuInstance> games = new HashMap<Player, GomokuInstance>();
	
	@Override
	public void onEnable() {
		File dataFolder = this.getDataFolder();
		if (dataFolder.exists() && !(dataFolder.isDirectory())) {
			dataFolder.delete();
		}
		if (!(dataFolder.exists())) {
			dataFolder.mkdir();
			try {
				Files.copy(new File(this.getClassLoader().getResource("map").getFile()), new File(dataFolder, "default"));
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public void onDisable() {
		HandlerList.unregisterAll(this);
		this.games.clear();
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (command.getName().contentEquals("startgame")) {
			if (sender instanceof Player) {
				if (args.length < 3) {
					sender.sendMessage(ChatColor.RED + "Not enough players.");
					sender.sendMessage(ChatColor.YELLOW + "USAGE: /startgame <mode> <player1> <player2> [map] [flags]");
					sender.sendMessage(ChatColor.GRAY + "Modes: Classic, Arena");
					sender.sendMessage(ChatColor.GRAY + "Arena Mode Flags: i### (set income), b### (set balance) where ### is amount.");
					return (true);
				}
				
				int i = (new Random()).nextInt(2);
				Player player1 = Bukkit.getPlayerExact(args[1 + i]);
				Player player2 = Bukkit.getPlayerExact(args[1 + ((i + 1) % 2)]);
				
				if (player1 == null || player2 == null) {
					sender.sendMessage(ChatColor.RED + "Invalid player name.");
					return (true);
				}
				
				if (player1.equals(player2)) {
					sender.sendMessage(ChatColor.RED + "Not enough players.");
					sender.sendMessage(ChatColor.YELLOW + "USAGE: /startgame <mode> <player1> <player2> [map]");
					return (true);
				}
				
				if (this.games.containsKey(player1) || this.games.containsKey(player2)) {
					sender.sendMessage(ChatColor.RED + "One or more players already in game.");
					return (true);
				}
				
				Location origin = ((Player)sender).getLocation().add(0, -1, 0).getBlock().getLocation();
				GomokuInstance game;
				if (args[0].equalsIgnoreCase("classic")) {
					game = new GomokuInstance(this, origin, player1, player2);
				}
				else if (args[0].equalsIgnoreCase("arena")) {
					try {
						if (args.length < 4) {
							game = new ArenaInstance(this, origin, player1, player2, "default");
						}
						else {
							game = new ArenaInstance(this, origin, player1, player2, args[3]);
						}
						if (args.length > 4) {
							for (int m = 4; m < args.length; m++) {
								try {
									if (args[m].charAt(0) == 'b') {
										((ArenaInstance)game).setStartingBalance(Integer.parseInt(args[m].substring(1)));
									}
									else if (args[m].charAt(0) == 'i') {
										((ArenaInstance)game).setStartingIncome(Integer.parseInt(args[m].substring(1)));
									}
									else {
										sender.sendMessage(ChatColor.RED + "Invalid Flag.");
										return (true);
									}
								}
								catch (NumberFormatException e) {
									sender.sendMessage(ChatColor.RED + "Invalid balance or income value.");
									return (true);
								}
							}
						}
					}
					catch (IOException e) {
						e.printStackTrace();
						sender.sendMessage(ChatColor.RED + "Unexpected error loading map.");
						return (true);
					}
				}
				else {
					sender.sendMessage(ChatColor.RED + "Unknown game mode.");
					return (true);
				}
				
				game.generate();
				game.begin();
			}
			return (true);
		}
		if (command.getName().contentEquals("forfeit") || command.getName().contentEquals("ff")) {
			if (sender instanceof Player) {
				GomokuInstance game = this.games.get(sender);
				if (game != null) {
					game.onPlayerQuit(new PlayerQuitEvent((Player)sender, "Forfeited Gomoku."));
					sender.sendMessage(((Player)sender).getDisplayName() + ChatColor.RESET + " has forfeited.");
				}
				else {
					sender.sendMessage(ChatColor.RED + "You are not in a game!");
				}
			}
			return (true);
		}
		return (false);
	}
}
