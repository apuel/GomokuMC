package org.us._42.laphicet.gomoku.minecraft;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Team;
import org.bukkit.scoreboard.Team.Option;
import org.bukkit.scoreboard.Team.OptionStatus;
import org.bukkit.util.RayTraceResult;
import org.us._42.laphicet.gomoku.Gomoku;

import net.md_5.bungee.api.ChatColor;

public class ArenaInstance extends GomokuInstance {
	private List<String> content;
	
	private List<Material> tower = new ArrayList<Material>(); 
	private int minMobs;
	private int maxMobs;
	private int spawnDelay;
	private List<char[]> map = new ArrayList<char[]>();
	private int arenaSize;
	private Map<Character,Material> mapInfo = new HashMap<Character,Material>();
	
	private static class Tower {
		static enum Type {
			DEFAULT,
			SPEED,
			DAMAGE,
			HEALTH,
			LUCK;
		}
		
		List<Tower> base;
		int posx;
		int posz;
		int x;
		int y;
		
		int health = 10;
		Type type = Type.DEFAULT;
		int token = 0;
		
		Tower(int posx, int posz, int x, int y, List<Tower> base) {
			this.posx = posx;
			this.posz = posz;
			this.x = x;
			this.y = y;
			this.base = base;
			this.base.add(this);
		}
		
		Tower(int posx, int posz, int x, int y) {
			this(posx, posz, x, y, new ArrayList<Tower>());
		}
		
		Tower extend(int posx, int posz) {
			return (new Tower(posx, posz, this.x, this.y, this.base));
		}
	}
	private List<Tower[]> towerMap = new ArrayList<Tower[]>();
	private Tower[][] towerLookup = new Tower[Gomoku.BOARD_LENGTH][Gomoku.BOARD_LENGTH];
	
	public ArenaInstance(GomokuMC plugin, Location origin, Player one, Player two, String mapfile) throws IOException {
		super(plugin, origin, one, two);
		File file = new File(plugin.getDataFolder(), mapfile);
		this.content = Files.readAllLines(file.toPath());
	}
	
	private String[] parseMaterialInfo(String info, String delimeter) {
		String[] ret;
		ret = info.split("=");
		ret = ret[1].split(delimeter);
		return (ret);
	}
	
	private void initMapInfo() {
		this.mapInfo.put('M', Material.BEACON);
		this.mapInfo.put('P', Material.WHITE_CONCRETE);
		this.mapInfo.put('t', Material.END_STONE_BRICKS);
		this.mapInfo.put('T', Material.END_STONE_BRICKS);
		this.mapInfo.put('B', Material.BARRIER);
		this.mapInfo.put('1', Material.LIGHT_BLUE_TERRACOTTA);
		this.mapInfo.put('2', Material.RED_TERRACOTTA);
		this.mapInfo.put(' ', Material.BLACK_CONCRETE);
	}
	
	private void mapMaterial(String info) {
		String[] input;
		switch(info.charAt(0)) {
			case 'P':
				this.mapInfo.put('P', Material.valueOf(parseMaterialInfo(info, " ")[0]));
				break;
			case 'T':
				input = parseMaterialInfo(info, " ");
				input = input[0].split(",");
				for (String block : input) {
					if (block.equals("X")) {
						this.tower.add(null);
					}
					else {
						this.tower.add(Material.valueOf(block));
					}
				}
				break;
			case 't':
				Material tBlock = Material.valueOf(parseMaterialInfo(info, " ")[0]);
				this.mapInfo.put('t', tBlock);
				this.mapInfo.put('T', tBlock);
				break;
			case 'B':
				this.mapInfo.put('B', Material.valueOf(parseMaterialInfo(info, " ")[0]));
				break;
			case '1':
				this.mapInfo.put('1', Material.valueOf(parseMaterialInfo(info, " ")[0]));
				break;
			case '2':
				this.mapInfo.put('2', Material.valueOf(parseMaterialInfo(info, " ")[0]));
				break;
			case 'M':
				input = parseMaterialInfo(info, ",");
				this.minMobs = Integer.parseInt(input[0]);
				this.maxMobs = Integer.parseInt(input[1]);
				this.spawnDelay = Integer.parseInt(input[2].split(" ")[0]);
				break;
			case ' ':
				this.mapInfo.put(' ', Material.valueOf(parseMaterialInfo(info, " ")[0]));
				break;
		}
	}
	
	private void mapTower(Tower tower, int x, int z) {
		char[] zmap = this.map.get(z);
		Tower[] ztowers = this.towerMap.get(z);
		if ((zmap.length > (x + 1)) && (zmap[x + 1] == 'T')) {
			if (ztowers[x + 1] == null) {
				ztowers[x + 1] = tower.extend(x + 1, z);
				this.mapTower(tower, x + 1, z);
			}
		}
		if ((x > 0) && (zmap[x - 1] == 'T')) {
			if (ztowers[x - 1] == null) {
				ztowers[x - 1] = tower.extend(x - 1, z);
				this.mapTower(tower, x - 1, z);
			}
		}
		if (z > 0) {
			zmap = this.map.get(z - 1);
			ztowers = this.towerMap.get(z - 1);
			if ((zmap.length > x) && (zmap[x] == 'T')) {
				if (ztowers[x] == null) {
					ztowers[x] = tower.extend(x, z - 1);
					this.mapTower(tower, x, z - 1);
				}
			}
		}
		if ((z + 1) < this.map.size()) {
			zmap = this.map.get(z + 1);
			ztowers = this.towerMap.get(z + 1);
			if ((zmap.length > x) && (zmap[x] == 'T')) {
				if (ztowers[x] == null) {
					ztowers[x] = tower.extend(x, z + 1);
					this.mapTower(tower, x, z + 1);
				}
			}
		}
	}
	
	private void mapTowers() {
		for (Tower[] towers : this.towerLookup) {
			for (Tower tower : towers) {
				this.mapTower(tower, tower.posx, tower.posz);
			}
		}
	}
	
	private void parseContent() {
		int mapPos = 0;
		int x = 0;
		int y = 0;
		
		this.initMapInfo();
		for (int i = 0; i < this.content.size(); i++) {
			String info = this.content.get(i);
			
			if (mapPos > 0) {
				if (this.arenaSize < info.length()) {
					this.arenaSize = info.length();
				}
				
				int posz = this.map.size();
				Tower[] towers = new Tower[info.length()];
				char[] mapInput = new char[info.length()];
				
				for (int posx = 0; posx < info.length(); posx++) {
					mapInput[posx] = info.charAt(posx);
					if (mapInput[posx] == 't') {
						if (y == Gomoku.BOARD_LENGTH) {
							//bad number of towers on the map >:0
							continue;
						}
						towers[posx] = new Tower(posx, posz, x, y);
						this.towerLookup[y][x] = towers[posx];
						if (++x == Gomoku.BOARD_LENGTH) {
							y++;
							x = 0;
						}
					}
				}
				
				this.towerMap.add(towers);
				this.map.add(mapInput);
				continue;
			}
			if (info.isEmpty()) {
				mapPos = 1;
				continue;
			}
			this.mapMaterial(info);
		}
		
		if (y != Gomoku.BOARD_LENGTH || x != 0) {
			//bad number of towers on the map >:0
		}
		this.mapTowers();
	}
	
	@Override
	public void generate() {
		this.parseContent();
		Bukkit.broadcastMessage(ChatColor.GREEN + "Generating Gomoku arena, this may take a while...");
		
		for (int x = 0; x < this.map.size(); x++) {
			char[] mapBlocks = this.map.get(x);
			
			boolean blank = true;
			for (int z = 0; z < mapBlocks.length; z++) {
				if (blank && (mapBlocks[z] == ' ')) {
					continue;
				}
				if (mapBlocks[z] == 'B') {
					blank = false;
				}
				
				Block block = this.origin.getWorld().getBlockAt(this.origin.getBlockX() + x - (this.arenaSize / 2), this.origin.getBlockY(), this.origin.getBlockZ() + z - (this.map.size() / 2));
				
				if (mapBlocks[z] == 'B') {
					block.setType(mapInfo.get(' '));
					
					Block bBlock = block;
					for (int y = 1; y < (this.tower.size() + 15); y++) {
						bBlock = bBlock.getLocation().add(0, 1, 0).getBlock();
						bBlock.setType(mapInfo.get('B'));
					}
				}
				else {
					block.setType(mapInfo.get(mapBlocks[z]));
				}
				
				if (mapBlocks[z] == 'M') {
					block.getLocation().subtract(0, 1, 0).getBlock().setType(Material.IRON_BLOCK);
				}
				if (!blank) {
					block.getLocation().add(0, this.tower.size() + 15, 0).getBlock().setType(mapInfo.get('B'));
				}
			}
		}
		
		Bukkit.broadcastMessage(ChatColor.GREEN + "Arena generated!");
	}
	
	private Map<Integer,String> lineMap = new HashMap<Integer,String>();
	private Set<String> lines = new HashSet<String>();
	
	private void setLine(int index, String line) {
		String oldLine = lineMap.get(index);
		if (oldLine != null) {
			this.scoreboard.resetScores(oldLine);
			lines.remove(oldLine);
		}
		while (lines.contains(line)) {
			line += ChatColor.RESET;
		}
		this.sidebar.getScore(line).setScore(index);
		lineMap.put(index, line);
		lines.add(line);
	}
	
	@Override
	protected void populateSidebar() {
		this.sidebar.setDisplayName(ChatColor.BOLD + "Gomoku: Arena Mode");
		
		this.setLine(12, ChatColor.AQUA + ChatColor.BOLD.toString() + ChatColor.stripColor(this.players[0].getDisplayName()) + ChatColor.WHITE +  ":");
		this.setLine(11, "Captures: 0");
		this.setLine(10, "Towers: 0");
		this.setLine(9, "Balance: 0");
		this.setLine(8, "Spawners: 0");
		this.setLine(7, "Score: 0");
		this.setLine(6, " ");
		
		this.setLine(5, ChatColor.RED + ChatColor.BOLD.toString() + ChatColor.stripColor(this.players[1].getDisplayName()) + ChatColor.WHITE +  ":");
		this.setLine(4, "Captures: 0");
		this.setLine(3, "Towers: 0");
		this.setLine(2, "Balance: 0");
		this.setLine(1, "Spawners: 0");
		this.setLine(0, "Score: 0");
	}
	
	@Override
	protected void updateSidebar() {
		this.setLine(11, "Captures: " + this.game.getCaptureCount(1));
		this.setLine(10, "Towers: " + this.game.getTokensPlaced(1));
		
		this.setLine(4, "Captures: " + this.game.getCaptureCount(2));
		this.setLine(3, "Towers: " + this.game.getTokensPlaced(2));
	}
	
	private BukkitRunnable task = new BukkitRunnable() {
		@Override
		public void run() {
			for (Player player : ArenaInstance.this.players) {
				player.removePotionEffect(PotionEffectType.JUMP);
				player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 2, 10, false, false, true));
				player.setFoodLevel(20);
			}
		}
	};
	
	@Override
	public void begin() {
		Team team = this.scoreboard.registerNewTeam("Gomoku");
		team.setOption(Option.COLLISION_RULE, OptionStatus.NEVER);
		
		Location spawnpoint = this.origin.clone().add(0, 1, 0);
		for (Player player : this.players) {
			player.teleport(spawnpoint);
			player.getInventory().clear();
			player.setGameMode(GameMode.SURVIVAL);
			player.setHealth(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
			team.addEntry(player.getName());
			
			ItemStack elytra = new ItemStack(Material.ELYTRA);
			ItemMeta meta = elytra.getItemMeta();
			meta.setUnbreakable(true);
			meta.addEnchant(Enchantment.BINDING_CURSE, 1, true);
			elytra.setItemMeta(meta);
			player.getInventory().setChestplate(elytra);
		}
		this.task.runTaskTimer(this.plugin, 0, 1);
		super.begin();
	}
	
	@Override
	public void end() {
		super.end();
		for (Player player : this.players) {
			player.getInventory().setChestplate(new ItemStack(Material.ELYTRA));
		}
		this.task.cancel();
	}
	
	@Override
	protected void victorySequence(int winner) {
		for (Player player : this.players) {
			player.sendTitle(this.game.getPlayerController(winner).name(this.game, winner) + " has won!", "", 10, 70, 20);
			player.teleport(this.origin.clone().add(0, this.tower.size() + 16, 0));
		}
		
		new BukkitRunnable() {
			Random rng = new Random();
			int i;
			
			@Override
			public void run() {
				Location location = ArenaInstance.this.origin.getBlock().getLocation().add(
					(ArenaInstance.this.arenaSize * rng.nextDouble()) - (ArenaInstance.this.arenaSize / 2),
					ArenaInstance.this.origin.getBlockY() + ArenaInstance.this.tower.size() + 16,
					(ArenaInstance.this.map.size() * rng.nextDouble()) - (ArenaInstance.this.map.size() / 2)
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
				
				if (++i == 40) {
					this.cancel();
				}
			}
		}.runTaskTimer(this.plugin, 0, 5);
	}
	
	public void buildTower(int x, int y, int token) {
		Tower core = this.towerLookup[y][x];
		core.token = token;
		
		int posx = origin.getBlockX() - (this.arenaSize / 2);
		int posy = this.origin.getBlockY() + 1;
		int posz = origin.getBlockZ() - (this.map.size() / 2);
		
		for (Material material : this.tower) {
			if (material == null) {
				material = this.mapInfo.get((char)('0' + token));
			}
			for (Tower tower : core.base) {
				this.origin.getWorld().getBlockAt(posx + tower.posx, posy, posz + tower.posz).setType(material);
			}
			posy++;
		}
	}
	
	public void destroyTower(int x, int y) {
		Tower core = this.towerLookup[y][x];
		core.token = 0;
		
		int posx = origin.getBlockX() - (this.arenaSize / 2);
		int posy = this.origin.getBlockY() + 1;
		int posz = origin.getBlockZ() - (this.map.size() / 2);
		
		for (int i = 0; i < core.base.size(); i++) {
			for (Tower tower : core.base) {
				this.origin.getWorld().getBlockAt(posx + tower.posx, posy, posz + tower.posz).setType(Material.AIR);
			}
			posy++;
		}
	}
	
	@Override
	public void reportChange(Gomoku game, int x, int y, int value) {
		if (value == 0) {
			this.destroyTower(x, y);
		}
		else {
			this.buildTower(x, y, value);
		}
	}
	
	@EventHandler
	public void onPlayerDropItem(PlayerDropItemEvent event) {
		if (event.getPlayer().equals(this.players[0]) || event.getPlayer().equals(this.players[1])) {
			event.setCancelled(true);
		}
	}
	
	@EventHandler
	public void onPlayerGameModeChange(PlayerGameModeChangeEvent event) {
		if (event.getPlayer().equals(this.players[0]) || event.getPlayer().equals(this.players[1])) {
			event.setCancelled(true);
		}
	}
	
	@Override
	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		if (event.getBlock().getX() <= (origin.getBlockX() + (this.arenaSize / 2) + 1) &&
			event.getBlock().getX() >= (origin.getBlockX() - (this.arenaSize / 2) - 1) &&
			event.getBlock().getZ() <= (origin.getBlockZ() + (this.map.size() / 2) + 1) &&
			event.getBlock().getZ() >= (origin.getBlockZ() - (this.map.size() / 2) - 1)) {
			event.setCancelled(true);
		}
	}
	
	@Override
	@EventHandler
	public void onBlockPlace(BlockPlaceEvent event) {
		if (event.getBlock().getX() <= (origin.getBlockX() + (this.arenaSize / 2) + 1) &&
			event.getBlock().getX() >= (origin.getBlockX() - (this.arenaSize / 2) - 1) &&
			event.getBlock().getZ() <= (origin.getBlockZ() + (this.map.size() / 2) + 1) &&
			event.getBlock().getZ() >= (origin.getBlockZ() - (this.map.size() / 2) - 1)) {
			event.setCancelled(true);
		}
	}
	
	@Override
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		if ((event.getHand() != EquipmentSlot.HAND) || (event.getAction() == Action.PHYSICAL)) {
			return;
		}
		
		Block block;
		if ((event.getAction() == Action.RIGHT_CLICK_AIR) || (event.getAction() == Action.LEFT_CLICK_AIR)) {
			Location position = event.getPlayer().getEyeLocation();
			RayTraceResult result = position.getWorld().rayTraceBlocks(position, position.getDirection(), 20);
			block = result.getHitBlock();
		}
		else {
			block = event.getClickedBlock();
		}
		if (block == null) {
			return;
		}
		
		if (block.getX() <= (origin.getBlockX() + (this.arenaSize / 2) + 1) &&
			block.getX() >= (origin.getBlockX() - (this.arenaSize / 2) - 1) &&
			block.getZ() <= (origin.getBlockZ() + (this.map.size() / 2) + 1) &&
			block.getZ() >= (origin.getBlockZ() - (this.map.size() / 2) - 1)) {
			int x = block.getX() - (origin.getBlockX() - (this.arenaSize / 2));
			int z = block.getZ() - (origin.getBlockZ() - (this.map.size() / 2));
			
			Tower[] towers = this.towerMap.get(z);
			if (towers.length <= x) {
				event.setCancelled(true);
				return;
			}
			
			Tower tower = towers[x];
			if (tower == null) {
				event.setCancelled(true);
				return; //There is no tower here
			}
			
			int player = this.game.getTurn() % 2;
			if (!(this.players[player].equals(event.getPlayer()))) {
				if (this.players[(player + 1) % 2].equals(event.getPlayer())) {
					event.getPlayer().sendMessage(ChatColor.RED + "It's not your turn.");
				}
				event.setCancelled(true);
				return;
			}
			
			tower = tower.base.get(0);
			if (tower.token != 0) {
				event.setCancelled(true);
				return; //Tower is already owned
			}
			
			MCPlayer controller = (MCPlayer)this.game.getPlayerController(player + 1);
			controller.x = tower.x;
			controller.y = tower.y;
			this.game.next();
			event.setCancelled(true);
		}
	}
}
