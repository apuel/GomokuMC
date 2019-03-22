package org.us._42.laphicet.gomoku.minecraft;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ArenaInstance extends GomokuInstance {
	private List<String> content;
	
	private List<Material> tower; 
	private int minMobs;
	private int maxMobs;
	private int spawnDelay;
	private List<char[]> map;
	private int arenaSize;
	private HashMap<Character,Material> mapInfo;
	
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
	
	private void parseContent() {
		int mapPos = 0;
		this.initMapInfo();
		for (String info : this.content) {
			if (mapPos > -1) {
				if (this.arenaSize < info.length()) {
					this.arenaSize = info.length();
				}
				char[]  mapInput = new char[info.length()];
				for (int i = 0; i < info.length(); i++) {
					mapInput[i] = info.charAt(i);
				}
				map.add(mapInput);
				continue;
			}
			if (info.isEmpty()) {
				mapPos = 1;
				continue;
			}
			String[] input;
			switch(info.charAt(0))
			{
				case 'P':
					this.mapInfo.put('P', Material.valueOf(parseMaterialInfo(info, " ")[0]));
					break;
				case 'T':
					input = parseMaterialInfo(info, " ");
					input = input[0].split(",");
					for (String block : input) {
						this.tower.add(Material.valueOf(block));
					}
					break;
				case 't':
					this.mapInfo.put('t', Material.valueOf(parseMaterialInfo(info, " ")[0]));
					this.mapInfo.put('T', Material.valueOf(parseMaterialInfo(info, " ")[0]));
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
	}
	
	@Override
	public void generate() {
		this.parseContent();
		for (int x = 0; x < this.map.size(); x++) {
			char[] mapBlocks = this.map.get(x);
			for (int z = 0; z < mapBlocks.length; z++) {
				this.origin.getWorld().getBlockAt(this.origin.getBlockX() + x - (this.arenaSize / 2) - 1, this.origin.getBlockY() + 1, this.origin.getBlockZ() + z - (this.arenaSize / 2) - 1).setType(mapInfo.get(mapBlocks[z]));
			}
		}
	}
}
