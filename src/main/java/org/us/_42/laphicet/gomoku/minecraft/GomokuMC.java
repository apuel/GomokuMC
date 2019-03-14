package org.us._42.laphicet.gomoku.minecraft;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import net.md_5.bungee.api.ChatColor;

public class GomokuMC extends JavaPlugin implements Listener {
	
	private Map<Player, GomokuInstance> games = new HashMap<Player, GomokuInstance>();
	
	private static final Material BOARD_BORDER = Material.BLACK_CONCRETE;
	private static final Material BOARD_MATERIAL = Material.DRIED_KELP_BLOCK;
	private static final Material TOKEN_A = Material.BIRCH_PRESSURE_PLATE;
	private static final Material TOKEN_B = Material.DARK_OAK_PRESSURE_PLATE;
	
	private static final int BOARD_OFFSET = 9;
	private static final int BOARD_SIZE = 21;
	
	@Override
	public void onEnable() {
		Bukkit.getPluginManager().registerEvents(this, this);
	}
	
	@Override
	public void onDisable() {
		HandlerList.unregisterAll((Plugin)this);
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (command.getName().equals("test")) {
			if (sender instanceof Player) {
				Location origin = ((Player)sender).getLocation();
				sender.sendMessage("X: " + origin.getBlockX() + " Y: " + origin.getBlockY() + " Z: " + origin.getBlockZ());
			}
			return (true);
		}
		if (command.getName().contentEquals("startgame")) {
			if (sender instanceof Player) {
				if (args.length < 2) {
					sender.sendMessage(ChatColor.RED + "Not enough players.");
					sender.sendMessage(ChatColor.YELLOW + "USAGE: /startgame <player1> <player2>");
					return (true);
				}
				
				Player player1 = Bukkit.getPlayerExact(args[0]);
				Player player2 = Bukkit.getPlayerExact(args[1]);
				if (player1 == null || player2 == null) {
					sender.sendMessage(ChatColor.RED + "Invalid player name.");
					return (true);
				}
				
				if (games.containsKey(player1) || games.containsKey(player2)) {
					sender.sendMessage(ChatColor.RED + "One or more players already in game.");
					return (true);
				}
				
				Location origin = ((Player)sender).getLocation();
				origin.add(0, -1, 0);
				GomokuInstance newGame = new GomokuInstance(origin, player1, player2);
				games.put(player1, newGame);
				games.put(player2, newGame);
				
				// Generate board
				for (int x = 0; x < BOARD_SIZE; x++) {
					for (int z = 0; z < BOARD_SIZE; z++) {
						if (x == 0 || z == 0 || x == 20 || z == 20) {
							((Player)sender).getWorld().getBlockAt(origin.getBlockX() + x - BOARD_OFFSET, origin.getBlockY() + 1, origin.getBlockZ() + z - BOARD_OFFSET).setType(Material.AIR);
							((Player)sender).getWorld().getBlockAt(origin.getBlockX() + x - BOARD_OFFSET, origin.getBlockY(), origin.getBlockZ() + z - BOARD_OFFSET).setType(BOARD_BORDER);
						}
						else {
							((Player)sender).getWorld().getBlockAt(origin.getBlockX() + x - BOARD_OFFSET, origin.getBlockY() + 1, origin.getBlockZ() + z - BOARD_OFFSET).setType(Material.AIR);
							((Player)sender).getWorld().getBlockAt(origin.getBlockX() + x - BOARD_OFFSET, origin.getBlockY(), origin.getBlockZ() + z - BOARD_OFFSET).setType(BOARD_MATERIAL);
						}
					}
				}
//				sender.sendMessage(((Player)sender).getUniqueId().toString());
			}
		}
		return (false);
	}
	
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
			event.getPlayer().sendMessage("Michael is the best");
		}
	}
}
