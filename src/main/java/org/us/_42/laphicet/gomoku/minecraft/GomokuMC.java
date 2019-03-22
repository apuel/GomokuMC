package org.us._42.laphicet.gomoku.minecraft;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.us._42.laphicet.gomoku.Gomoku;

import net.md_5.bungee.api.ChatColor;

public class GomokuMC extends JavaPlugin {
	public Map<Player, GomokuInstance> games = new HashMap<Player, GomokuInstance>();
	
	public static final Material BOARD_BORDER = Material.BLACK_CONCRETE;
	public static final Material BOARD_MATERIAL = Material.DRIED_KELP_BLOCK;
	
	public static final Material TOKEN_A = Material.BIRCH_PRESSURE_PLATE;
	public static final Material TOKEN_B = Material.DARK_OAK_PRESSURE_PLATE;
	
	public static final int BOARD_OFFSET = Gomoku.BOARD_LENGTH / 2;
	public static final int BOARD_SIZE = Gomoku.BOARD_LENGTH + 2;
	
	@Override
	public void onEnable() {
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
					sender.sendMessage(ChatColor.YELLOW + "USAGE: /startgame <mode> <player1> <player2>");
					return (true);
				}
				
				int i = (new Random()).nextInt(2);
				Player player1 = Bukkit.getPlayerExact(args[i]);
				Player player2 = Bukkit.getPlayerExact(args[(i + 1) % 2]);
				
				if (player1 == null || player2 == null) {
					sender.sendMessage(ChatColor.RED + "Invalid player name.");
					return (true);
				}
				
				if (player1.equals(player2)) {
					sender.sendMessage(ChatColor.RED + "Not enough players.");
					sender.sendMessage(ChatColor.YELLOW + "USAGE: /startgame <mode> <player1> <player2>");
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
						game = new ArenaInstance(this, origin, player1, player2, "map");
					} catch (IOException e) {
						e.printStackTrace();
						sender.sendMessage(ChatColor.RED + "Your mum gey.");
						return true;
					}
				}
				else {
					sender.sendMessage(ChatColor.RED + "Unknown game mode.");
					return (true);
				}
				game.generate();
				
				this.games.put(player1, game);
				this.games.put(player2, game);
				
				Bukkit.getPluginManager().registerEvents(game, this);
				player1.sendMessage(ChatColor.YELLOW + "It is " + player1.getDisplayName() + ChatColor.RESET + ChatColor.YELLOW + "'s turn.");
				player2.sendMessage(ChatColor.YELLOW + "It is " + player1.getDisplayName() + ChatColor.RESET + ChatColor.YELLOW + "'s turn.");
			}
			return (true);
		}
		if (command.getName().contentEquals("forfeit") || command.getName().contentEquals("ff")) {
			if (sender instanceof Player) {
				GomokuInstance game = this.games.get(sender);
				if (game != null) {
					PlayerQuitEvent event = new PlayerQuitEvent((Player)sender, "Forfeited Gomoku.");
					game.onPlayerQuit(event);
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
