package org.us._42.laphicet.gomoku.minecraft;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.BiConsumer;

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
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
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
	private PlayerStats[] stats = { new PlayerStats(), new PlayerStats() };
	
//	private static ItemStack[] damageTowers = { new ItemStack(), new ItemStack(), new ItemStack(), new ItemStack() };
	private static Map<String,BiConsumer<ArenaInstance,Tower>> towerUpgrade = new HashMap<String,BiConsumer<ArenaInstance,Tower>>();
	private static ItemStack[] damageTowerItems = new ItemStack[4];
	private static ItemStack[] speedTowerItems = new ItemStack[4];
	private static ItemStack[] healthTowerItems = new ItemStack[4];
	private static ItemStack[] luckTowerItems = new ItemStack[4];
	
	private static ItemStack createIcon(Material icon, BiConsumer<ArenaInstance,Tower> action, String title, String... desc) {
		ItemStack item = new ItemStack(icon);
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(title);
		meta.setLore(Arrays.asList(desc));
		meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
		item.setItemMeta(meta);
		towerUpgrade.put(title, action);
		return (item);
	}
	
	private void placeEntity(Tower tower) {
		int posx = origin.getBlockX() - (this.arenaSize / 2);
		int posy = this.origin.getBlockY() + 1;
		int posz = origin.getBlockZ() - (this.map.size() / 2);
		
//		this.origin.getWorld().getBlockAt(posx + tower.posx, posy, posz + tower.posz).setType(material);
	}
	
	private static void upgradeTower(ArenaInstance game, Tower tower) {
		if (game.stats[tower.token - 1].balance < Tower.UPGRADE_PRICE[tower.tier]) {
			game.players[tower.token - 1].sendMessage("Insufficient points");
		}
		else {
			game.stats[tower.token - 1].balance -= Tower.UPGRADE_PRICE[tower.tier];
			if (tower.type == Tower.Type.DAMAGE) {
				game.stats[tower.token - 1].attackBonus += Tower.UPGRADE_BONUS[tower.tier];
				game.placeEntity(tower);
			}
			else if (tower.type == Tower.Type.SPEED) {
				game.stats[tower.token - 1].speedBonus += Tower.UPGRADE_BONUS[tower.tier];
				game.placeEntity(tower);
			}
			else if (tower.type == Tower.Type.HEALTH) {
				game.stats[tower.token - 1].healthBonus += Tower.UPGRADE_BONUS[tower.tier];
				game.placeEntity(tower);
			}
			else if (tower.type == Tower.Type.LUCK) {
				game.stats[tower.token - 1].eliteBonus += Tower.UPGRADE_BONUS[tower.tier];
				game.stats[tower.token - 1].powerfulBonus += 1;
				game.placeEntity(tower);
			}

			game.stats[tower.token - 1].score -= Tower.TOWER_SCORE[tower.tier];
			tower.tier += 1;
			game.stats[tower.token - 1].score += Tower.TOWER_SCORE[tower.tier];
			
			game.players[tower.token - 1].sendMessage("Your " + tower.type + " tower has been upgraded to tier " + tower.tier + ".");
			game.populateTowerMenu(tower);
			game.updateSidebar();
		}
	}
	
	static {
		damageTowerItems[0] = createIcon(Material.WOODEN_SWORD, (game, tower) -> {
				tower.type = Tower.Type.DAMAGE;
				upgradeTower(game, tower);
			},
			"Convert To Damage Tower - 100 points",
			"",
			ChatColor.BLUE + "Bonus income from tower: 10 points",
			"",
			"Use this to convert your tower to a damage tower.",
			"Any monster beacons you own will gain bonus damage.",
			"",
			ChatColor.YELLOW + "Tier 1: 1% damage bonus, upgrade cost: 100 points",
			ChatColor.YELLOW + "Tier 2: 3% damage bonus, upgrade cost: 300 points",
			ChatColor.YELLOW + "Tier 3: 5% damage bonus, upgrade cost: 500 points"
		);
		
		damageTowerItems[1] = createIcon(Material.STONE_SWORD, ArenaInstance::upgradeTower, "Upgrade To Tier 2 Damage Tower - 300 points",
			"",
			ChatColor.BLUE + "Bonus income from tower: 30 points",
			"",
			"This is currently a tier 1 damage tower.",
			"Current tower rewards 1% damage bonus (stacks with other tower)",
			"",
			ChatColor.YELLOW + "Tier 2: 3% damage bonus, upgrade cost: 300 points",
			ChatColor.YELLOW + "Tier 3: 5% damage bonus, upgrade cost: 500 points"
		);
		
		damageTowerItems[2] = createIcon(Material.IRON_SWORD, ArenaInstance::upgradeTower, "Upgrade To Tier 3 Damage Tower - 500 points",
			"",
			ChatColor.BLUE + "Bonus income from tower: 50 points",
			"",
			"This is currently a tier 2 damage tower.",
			"Current tower rewards 3% damage bonus (stacks with other tower)",
			"",
			ChatColor.YELLOW + "Tier 3: 5% damage bonus, upgrade cost: 500 points"
		);
	
		damageTowerItems[3] = createIcon(Material.DIAMOND_SWORD, (game, tower) -> {
				game.players[tower.token - 1].sendMessage("Tower cannot be further upgraded.");
			},
			"Tier 3 Damage Tower",
			"",
			ChatColor.BLUE + "Bonus income from tower: 100 points",
			"",
			"This is currently a tier 3 damage tower.",
			"Current tower rewards 5% damage bonus (stacks with other tower)"
		);
		
		speedTowerItems[0] = createIcon(Material.LEATHER_BOOTS, (game, tower) -> {
				tower.type = Tower.Type.SPEED;
				upgradeTower(game, tower);
			},
			"Convert To Speed Tower - 100 points",
			"",
			ChatColor.BLUE + "Bonus income from tower: 10 points",
			"",
			"Use this to convert your tower to a speed tower.",
			"Any monster beacons you own will gain bonus speed.",
			"",
			ChatColor.YELLOW + "Tier 1: 1% speed bonus, upgrade cost: 100 points",
			ChatColor.YELLOW + "Tier 2: 3% speed bonus, upgrade cost: 300 points",
			ChatColor.YELLOW + "Tier 3: 5% speed bonus, upgrade cost: 500 points"
		);
			
		speedTowerItems[1] = createIcon(Material.CHAINMAIL_BOOTS, ArenaInstance::upgradeTower, "Upgrade To Tier 2 Speed Tower - 300 points",
			"",
			ChatColor.BLUE + "Bonus income from tower: 30 points",
			"",
			"This is currently a tier 1 speed tower.",
			"Current tower rewards 1% speed bonus (stacks with other tower)",
			"",
			ChatColor.YELLOW + "Tier 2: 3% speed bonus, upgrade cost: 300 points",
			ChatColor.YELLOW + "Tier 3: 5% speed bonus, upgrade cost: 500 points"
		);
			
		speedTowerItems[2] = createIcon(Material.IRON_BOOTS, ArenaInstance::upgradeTower, "Upgrade To Tier 3 Speed Tower - 500 points",
			"",
			ChatColor.BLUE + "Bonus income from tower: 50 points",
			"",
			"This is currently a tier 2 speed tower.",
			"Current tower rewards 3% speed bonus (stacks with other tower)",
			"",
			ChatColor.YELLOW + "Tier 3: 5% speed bonus, upgrade cost: 500 points"
		);
			
		speedTowerItems[3] = createIcon(Material.DIAMOND_BOOTS, (game, tower) -> {
				game.players[tower.token - 1].sendMessage("Tower cannot be further upgraded.");
			}, 
			"Tier 3 Speed Tower",
			"",
			ChatColor.BLUE + "Bonus income from tower: 100 points",
			"",
			"This is currently a tier 3 speed tower.",
			"Current tower rewards 5% speed bonus (stacks with other tower)"
		);
		
		healthTowerItems[0] = createIcon(Material.LEATHER_CHESTPLATE, (game, tower) -> {
				tower.type = Tower.Type.HEALTH;
				upgradeTower(game, tower);
			},
			"Convert To Health Tower - 100 points",
			"",
			ChatColor.BLUE + "Bonus income from tower: 10 points",
			"",
			"Use this to convert your tower to a health tower.",
			"Any monster beacons you own will gain bonus health.",
			"",
			ChatColor.YELLOW + "Tier 1: 1% health bonus, upgrade cost: 100 points",
			ChatColor.YELLOW + "Tier 2: 3% health bonus, upgrade cost: 300 points",
			ChatColor.YELLOW + "Tier 3: 5% health bonus, upgrade cost: 500 points"
		);
			
		healthTowerItems[1] = createIcon(Material.CHAINMAIL_CHESTPLATE, ArenaInstance::upgradeTower, "Upgrade To Tier 2 Health Tower - 300 points",
			"",
			ChatColor.BLUE + "Bonus income from tower: 30 points",
			"",
			"This is currently a tier 1 health tower.",
			"Current tower rewards 1% health bonus (stacks with other tower)",
			"",
			ChatColor.YELLOW + "Tier 2: 3% health bonus, upgrade cost: 300 points",
			ChatColor.YELLOW + "Tier 3: 5% health bonus, upgrade cost: 500 points"
		);
			
		healthTowerItems[2] = createIcon(Material.IRON_CHESTPLATE, ArenaInstance::upgradeTower, "Upgrade To Tier 3 Health Tower - 500 points",
			"",
			ChatColor.BLUE + "Bonus income from tower: 50 points",
			"",
			"This is currently a tier 2 health tower.",
			"Current tower rewards 3% health bonus (stacks with other tower)",
			"",
			ChatColor.YELLOW + "Tier 3: 5% health bonus, upgrade cost: 500 points"
		);
			
		healthTowerItems[3] = createIcon(Material.DIAMOND_CHESTPLATE, (game, tower) -> {
				game.players[tower.token - 1].sendMessage("Tower cannot be further upgraded.");
			},
			"Tier 3 Health Tower",
			"",
			ChatColor.BLUE + "Bonus income from tower: 100 points",
			"",
			"This is currently a tier 3 health tower.",
			"Current tower rewards 5% health bonus (stacks with other tower)"
		);
		
		luckTowerItems[0] = createIcon(Material.COAL, (game, tower) -> {
				tower.type = Tower.Type.LUCK;
				upgradeTower(game, tower);
			},
			"Convert To Luck Tower - 100 points",
			"",
			ChatColor.BLUE + "Bonus income from tower: 10 points",
			"",
			"Use this to convert your tower to a luck tower.",
			"Any monster beacons you own will increase chance to spawn stronger enemies.",
			"",
			ChatColor.YELLOW + "Tier 1: Increase 1% chance to spawn elite",
			ChatColor.YELLOW + "Tier 1: Upgrade cost 100 points",
			ChatColor.YELLOW + "Tier 2: Increase 3% chance to spawn elite, 1% to spawn powerful monster",
			ChatColor.YELLOW + "Tier 2: Upgrade cost 300 points",
			ChatColor.YELLOW + "Tier 3: Increase 5% chance to spawn elite, 2% to spawn powerful monster",
			ChatColor.YELLOW + "Tier 3: Upgrade cost: 500 points"
		);
			
		luckTowerItems[1] = createIcon(Material.IRON_INGOT, ArenaInstance::upgradeTower, "Upgrade To Tier 2 Luck Tower - 300 points",
			"",
			ChatColor.BLUE + "Bonus income from tower: 30 points",
			"",
			"This is currently a tier 1 luck tower.",
			"Current tower rewards increase 1% chance to spawn elite (stacks with other tower)",
			"",
			ChatColor.YELLOW + "Tier 2: Increase 3% chance to spawn elite, 1% to spawn powerful monster",
			ChatColor.YELLOW + "Tier 2: Upgrade cost 300 points",
			ChatColor.YELLOW + "Tier 3: Increase 5% chance to spawn elite, 2% to spawn powerful monster",
			ChatColor.YELLOW + "Tier 3: Upgrade cost: 500 points"
		);
			
		luckTowerItems[2] = createIcon(Material.GOLD_INGOT, ArenaInstance::upgradeTower, "Upgrade To Tier 3 Luck Tower - 500 points",
			"",
			ChatColor.BLUE + "Bonus income from tower: 50 points",
			"",
			"This is currently a tier 2 luck tower.",
			"Current tower rewards increase 3% chance to spawn elite",
			"1% to spawn powerful monster (stacks with other tower)",
			"",
			ChatColor.YELLOW + "Tier 3: Increase 5% chance to spawn elite, 2% to spawn powerful monster",
			ChatColor.YELLOW + "Tier 3: Upgrade cost: 500 points"
		);
			
		luckTowerItems[3] = createIcon(Material.DIAMOND_ORE, (game, tower) -> {
				game.players[tower.token - 1].sendMessage("Tower cannot be further upgraded.");
			},
			"Tier 3 Luck Tower",
			"",
			ChatColor.BLUE + "Bonus income from tower: 100 points",
			"",
			"This is currently a tier 3 luck tower.",
			"Current tower rewards increase 5% chance to spawn elite",
			"2% to spawn powerful monster (stacks with other tower)"
		);
		
	}
	
	private static class PlayerStats {
		int balance = 100;
		int score = 0;
		int income = 50;
		int eliteBonus = 0;
		int powerfulBonus = 0;
		int attackBonus = 0;
		int speedBonus = 0;
		int healthBonus = 0;
		
		Inventory towerMenu = Bukkit.createInventory(null, 9, "Tower Menu");
		Tower selectedTower = null;
	}
	
	private static class Tower {
		static enum Type {
			DEFAULT,
			SPEED,
			DAMAGE,
			HEALTH,
			LUCK;
		}
		static final int[] UPGRADE_PRICE = {100, 300, 500};
		static final int[] TOWER_INCOME = {10, 30, 50, 100};
		static final int[] TOWER_SCORE = {1, 3, 5, 10};
		static final int[] UPGRADE_BONUS = {1, 2, 2};
		
		int tier;
		int health = 10;
		Type type = Type.DEFAULT;
		int token = 0;

		List<Tower> base;
		int posx;
		int posz;
		int x;
		int y;
		
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
		
		this.setLine(14, ChatColor.AQUA + ChatColor.BOLD.toString() + ChatColor.stripColor(this.players[0].getDisplayName()) + ChatColor.WHITE +  ":");
		this.setLine(13, "Captures: 0 | Towers: 0");
		this.setLine(12, "Spawners: 0 | Score: 0");
		this.setLine(11, "Balance: " + this.stats[0].balance);
		this.setLine(10, "Income: " + this.stats[0].income);
		this.setLine(9, "Bonus: ATK 0% - SPD 0% - HP 0%");
		this.setLine(8, "Spawn: Elite 0% - Powerful 0%");
		this.setLine(7, " ");
		
		this.setLine(6, ChatColor.RED + ChatColor.BOLD.toString() + ChatColor.stripColor(this.players[1].getDisplayName()) + ChatColor.WHITE +  ":");
		this.setLine(5, "Captures: 0 | Towers: 0");
		this.setLine(4, "Spawners: 0 | Score: 0");
		this.setLine(3, "Balance: " + this.stats[1].balance);
		this.setLine(2, "Income: " + this.stats[1].income);
		this.setLine(1, "Bonus: ATK 0% - SPD 0% - HP 0%");
		this.setLine(0, "Spawn: Elite 0% - Powerful 0%");
	}
	
	@Override
	protected void updateSidebar() {
		this.setLine(13, "Captures: " + this.game.getCaptureCount(1) + " | Towers: " + this.game.getTokensPlaced(1));
		this.setLine(12, "Spawners: 0 | Score: " + this.stats[0].score);
		this.setLine(11, "Balance: " + this.stats[0].balance);
		this.setLine(10, "Income: " + this.stats[0].income);
		this.setLine(9, "Bonus: ATK " + this.stats[0].attackBonus + "% - SPD " + this.stats[0].speedBonus + "% - HP " + this.stats[0].healthBonus + "%");
		this.setLine(8, "Spawn: Elite " + this.stats[0].eliteBonus + "% - Powerful " + this.stats[0].powerfulBonus + "%");
		
		this.setLine(5, "Captures: " + this.game.getCaptureCount(2) + " | Towers: " + this.game.getTokensPlaced(2));
		this.setLine(4, "Spawners: 0 | Score: " + this.stats[1].score);
		this.setLine(3, "Balance: " + this.stats[1].balance);
		this.setLine(2, "Income: " + this.stats[1].income);
		this.setLine(1, "Bonus: ATK " + this.stats[1].attackBonus + "% - SPD " + this.stats[1].speedBonus + "% - HP " + this.stats[1].healthBonus + "%");
		this.setLine(0, "Spawn: Elite " + this.stats[1].eliteBonus + "% - Powerful " + this.stats[1].powerfulBonus + "%");
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
	
	public void setStartingBalance(int balance) {
		stats[0].balance = balance;
		stats[1].balance = balance;
	}
	
	public void setStartingIncome(int income){
		stats[0].income = income;
		stats[1].income = income;
	}
	
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
	
	@Override
	public void logTurn(Gomoku game, Collection<String> logs) {
		if (game.getWinner() == 0) {
			this.stats[game.getTurn() % 2].balance += this.stats[game.getTurn() % 2].income;
		}
		super.logTurn(game, logs);
	}
	
	public void buildTower(int x, int y, int token) {
		Tower core = this.towerLookup[y][x];
		this.stats[token - 1].income += Tower.TOWER_INCOME[0];
		this.stats[token - 1].score += Tower.TOWER_SCORE[0];
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
		this.stats[core.token - 1].income -= Tower.TOWER_INCOME[core.tier];
		this.stats[core.token - 1].score -= Tower.TOWER_SCORE[core.tier];
		
		if (core.type == Tower.Type.DAMAGE) {
			this.stats[core.token - 1].attackBonus -= ((core.tier - 1) * 2 + 1);
		}
		else if (core.type == Tower.Type.SPEED) {
			this.stats[core.token - 1].speedBonus -= ((core.tier - 1) * 2 + 1);
		}
		else if (core.type == Tower.Type.HEALTH) {
			this.stats[core.token - 1].healthBonus -= ((core.tier - 1) * 2 + 1);
		}
		else if (core.type == Tower.Type.LUCK) {
			this.stats[core.token - 1].eliteBonus -= ((core.tier - 1) * 2 + 1);
			this.stats[core.token - 1].powerfulBonus -= core.tier;
		}
		core.token = 0;
		core.health = 0;
		core.type = Tower.Type.DEFAULT;
		core.tier = 0;
		
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
	
	public void doTowerUpgrade(Tower tower, ItemStack item) {
		towerUpgrade.get(item.getItemMeta().getDisplayName()).accept(this, tower);
	}
	
	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		int token;
		if (((Player)event.getWhoClicked()).equals(this.players[0])) {
			token  = 1;
		}
		else if (((Player)event.getWhoClicked()).equals(this.players[1])) {
			token = 2;
		}
		else {
			return;
		}
		
		if (event.getInventory().equals(this.stats[token - 1].towerMenu)) {
			event.setCancelled(true);
			this.doTowerUpgrade(this.stats[token - 1].selectedTower, event.getCurrentItem());
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
	
	private void populateTowerMenu(Tower tower) {
		int token = tower.token;
		this.stats[token - 1].selectedTower = tower;
		this.stats[token - 1].towerMenu.clear();
		if (tower.type == Tower.Type.DEFAULT) {
			this.stats[token - 1].towerMenu.setItem(1, damageTowerItems[0]);
			this.stats[token - 1].towerMenu.setItem(3, speedTowerItems[0]);
			this.stats[token - 1].towerMenu.setItem(5, healthTowerItems[0]);
			this.stats[token - 1].towerMenu.setItem(7, luckTowerItems[0]);
		}
		else if (tower.type == Tower.Type.DAMAGE) {
			this.stats[token - 1].towerMenu.setItem(4, damageTowerItems[tower.tier]);
		}
		else if (tower.type == Tower.Type.SPEED) {
			this.stats[token - 1].towerMenu.setItem(4, speedTowerItems[tower.tier]);
		}
		else if (tower.type == Tower.Type.HEALTH) {
			this.stats[token - 1].towerMenu.setItem(4, healthTowerItems[tower.tier]);
		}
		else if (tower.type == Tower.Type.LUCK) {
			this.stats[token - 1].towerMenu.setItem(4, luckTowerItems[tower.tier]);
		}
	}
	
	private void onTowerInteract(PlayerInteractEvent event, Tower tower) {
		int token = tower.token;
		if ((token != 0) && event.getPlayer().equals(this.players[token - 1])) {
			//TODO update menu before opening it
			this.populateTowerMenu(tower);
			event.getPlayer().openInventory(this.stats[token - 1].towerMenu);
		}
	}
	
	private void onConstructTower(PlayerInteractEvent event, Tower tower) {
		int player = this.game.getTurn() % 2;
		if (!(this.players[player].equals(event.getPlayer()))) {
			if (this.players[(player + 1) % 2].equals(event.getPlayer())) {
				event.getPlayer().sendMessage(ChatColor.RED + "It's not your turn.");
			}
			return;
		}
		
		MCPlayer controller = (MCPlayer)this.game.getPlayerController(player + 1);
		controller.x = tower.x;
		controller.y = tower.y;
		this.game.next();
	}
	
	@Override
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		if ((event.getHand() != EquipmentSlot.HAND) || (event.getAction() == Action.PHYSICAL) || (event.getAction() == Action.RIGHT_CLICK_AIR)) {
			return;
		}
		
		Block block;
		if (event.getAction() == Action.LEFT_CLICK_AIR) {
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
			
			tower = tower.base.get(0);
			
			if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
				this.onTowerInteract(event, tower);
			}
			else {
				this.onConstructTower(event, tower);
			}
			event.setCancelled(true);
		}
	}
}
