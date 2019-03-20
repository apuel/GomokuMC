package org.us._42.laphicet.gomoku.minecraft;

import java.util.Collection;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.RenderType;
import org.bukkit.scoreboard.Scoreboard;
import org.us._42.laphicet.gomoku.GameStateReporter;
import org.us._42.laphicet.gomoku.Gomoku;
import org.us._42.laphicet.gomoku.PlayerController;

import net.md_5.bungee.api.ChatColor;

public class GomokuInstance implements GameStateReporter, Listener {
	private GomokuMC plugin;
	private Gomoku game;
	private Location origin;
	private Player[] players = new Player[2];
	
	private Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
	private Objective captures = this.scoreboard.registerNewObjective("Captures", "dummy", "Captures", RenderType.INTEGER);
	
	private static class MCPlayer implements PlayerController {
		Player player;

		int x = -1;
		int y = -1;
		
		MCPlayer(Player player) {
			this.player = player;
		}
		
		@Override
		public String name(Gomoku game, int value) {
			return (player.getDisplayName() + ChatColor.RESET);
		}
		
		@Override
		public void report(Gomoku game, String message) {
			player.sendMessage(ChatColor.RED + message);
		}
		
		@Override
		public void informChange(Gomoku game, int x, int y, int value) { }
		
		@Override
		public void informWinner(Gomoku game, int value) { }
		
		@Override
		public boolean getMove(Gomoku game, int value, long key) {
			if ((this.x == -1) || (this.y == -1)) {
				return (false);
			}
			game.submitMove(this.x, this.y, key);
			this.x = -1;
			this.y = -1;
			return (true);
		}
		
		@Override
		public void gameStart(Gomoku game, int value) { }
		
		@Override
		public void gameEnd(Gomoku game) { }
	}
	
	public GomokuInstance(GomokuMC plugin, Location origin, Player one, Player two) {
		this.plugin = plugin;
		this.game = new Gomoku(this, new MCPlayer(one), new MCPlayer(two));
		this.origin = origin;
		this.players[0] = one;
		this.players[1] = two;
		
		this.captures.setDisplaySlot(DisplaySlot.SIDEBAR);
		this.captures.getScore(one.getName()).setScore(0);
		this.captures.getScore(two.getName()).setScore(0);
		one.setScoreboard(this.scoreboard);
		two.setScoreboard(this.scoreboard);
	}
	
	private void victorySequence(int winner) {
		HandlerList.unregisterAll(this);
		for (Player player : this.players) {
			this.plugin.games.remove(player);
			player.sendTitle(this.game.getPlayerController(winner).name(this.game, winner) + " has won!", "", 10, 70, 20);
			player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
		}
		
		new BukkitRunnable() {
			Random rng = new Random();
			int i;
			
			@Override
			public void run() {
				Location location = GomokuInstance.this.origin.getBlock().getLocation().add(
					(Gomoku.BOARD_LENGTH * rng.nextDouble()) - (Gomoku.BOARD_LENGTH / 2),
					1,
					(Gomoku.BOARD_LENGTH * rng.nextDouble()) - (Gomoku.BOARD_LENGTH / 2)
				);
				Firework firework = location.getWorld().spawn(location, Firework.class);
				FireworkMeta meta = firework.getFireworkMeta();
				meta.addEffect(FireworkEffect.builder()
					.withColor(Color.fromRGB(rng.nextInt(0x1000000)))
					.trail(true)
					.withFade(Color.fromRGB(rng.nextInt(0x1000000)))
				.build());
				meta.setPower(1);
				firework.setFireworkMeta(meta);
				
				if (++i == 10) {
					this.cancel();
				}
			}
		}.runTaskTimer(this.plugin, 0, 10);
	}
	
	@Override
	public void logTurn(Gomoku game, Collection<String> logs) {
		logs.forEach(this.players[0]::sendMessage);
		logs.forEach(this.players[1]::sendMessage);
		this.captures.getScore(this.players[0].getName()).setScore(game.getCaptureCount(1));
		this.captures.getScore(this.players[1].getName()).setScore(game.getCaptureCount(2));
		
		int winner = game.getWinner();
		if (winner > 0) {
			this.victorySequence(winner);
		}
	}
	
	@Override
	public void reportChange(Gomoku game, int x, int y, int value) {
		Location board = this.origin.clone().subtract(GomokuMC.BOARD_OFFSET, 0, GomokuMC.BOARD_OFFSET);
		if (value == 0) {
			board.add(x, 1, y).getBlock().setType(Material.AIR);
		}
		else if (value == 1) {
			board.add(x, 1, y).getBlock().setType(GomokuMC.TOKEN_A);
		}
		else {
			board.add(x, 1, y).getBlock().setType(GomokuMC.TOKEN_B);
		}
	}
	
	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		if (event.getBlock().getX() <= (origin.getBlockX() + GomokuMC.BOARD_OFFSET + 1) &&
			event.getBlock().getX() >= (origin.getBlockX() - GomokuMC.BOARD_OFFSET - 1) &&
			event.getBlock().getZ() <= (origin.getBlockZ() + GomokuMC.BOARD_OFFSET + 1) &&
			event.getBlock().getZ() >= (origin.getBlockZ() - GomokuMC.BOARD_OFFSET - 1) &&
			((event.getBlock().getY() == origin.getBlockY()) || (event.getBlock().getY() == origin.getBlockY() + 1))) {
			event.setCancelled(true);
		}
	}
	
	@EventHandler
	public void onBlockPlace(BlockPlaceEvent event) {
		if (event.getBlock().getX() <= (origin.getBlockX() + GomokuMC.BOARD_OFFSET + 1) &&
			event.getBlock().getX() >= (origin.getBlockX() - GomokuMC.BOARD_OFFSET - 1) &&
			event.getBlock().getZ() <= (origin.getBlockZ() + GomokuMC.BOARD_OFFSET + 1) &&
			event.getBlock().getZ() >= (origin.getBlockZ() - GomokuMC.BOARD_OFFSET - 1) &&
			((event.getBlock().getY() == origin.getBlockY()) || (event.getBlock().getY() == origin.getBlockY() + 1))) {
			event.setCancelled(true);
		}
	}
	
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (event.getHand() != EquipmentSlot.HAND || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
			return;
		}
		
		Block block = event.getClickedBlock();
		if (block.getX() <= (origin.getBlockX() + GomokuMC.BOARD_OFFSET) &&
			block.getX() >= (origin.getBlockX() - GomokuMC.BOARD_OFFSET) &&
			block.getZ() <= (origin.getBlockZ() + GomokuMC.BOARD_OFFSET) &&
			block.getZ() >= (origin.getBlockZ() - GomokuMC.BOARD_OFFSET) &&
			((block.getY() == origin.getBlockY()) || (block.getY() == origin.getBlockY() + 1))) {
			int player = this.game.getTurn() % 2;
			if (!(this.players[player].equals(event.getPlayer()))) {
				if (this.players[(player + 1) % 2].equals(event.getPlayer())) {
					event.getPlayer().sendMessage(ChatColor.RED + "It's not your turn.");
				}
				return;
			}
			
			MCPlayer controller = (MCPlayer)this.game.getPlayerController(player + 1);
			controller.x = block.getX() - (origin.getBlockX() - GomokuMC.BOARD_OFFSET);
			controller.y = block.getZ() - (origin.getBlockZ() - GomokuMC.BOARD_OFFSET);
			this.game.next();
			event.setCancelled(true);
		}
	}
	
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		if (event.getPlayer().equals(this.players[0])) {
			this.players[1].sendMessage(this.players[0].getDisplayName() + ChatColor.RESET + "has forfeited.");
			this.players[1].sendMessage(this.players[1].getDisplayName() + ChatColor.RESET + "has won.");
			this.victorySequence(2);
		}
		else if (event.getPlayer().equals(this.players[1])) {
			this.players[1].sendMessage(this.players[1].getDisplayName() + ChatColor.RESET + "has forfeited.");
			this.players[1].sendMessage(this.players[0].getDisplayName() + ChatColor.RESET + "has won.");
			this.victorySequence(1);
		}
		else {
			return;
		}
	}
}
