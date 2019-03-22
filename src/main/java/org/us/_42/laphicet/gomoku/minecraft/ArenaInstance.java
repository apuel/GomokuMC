package org.us._42.laphicet.gomoku.minecraft;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ArenaInstance extends GomokuInstance {
	private List<String> content;
	
	private Material path;
	private Material space;
	private Material border;
	private Material playerOne;
	private Material playerTwo;
	private List<Material> tower; 
	private byte minMobs;
	private byte maxMobs;
	private byte spawnDelay;
	
	
	public ArenaInstance(GomokuMC plugin, Location origin, Player one, Player two, String mapfile) throws IOException {
		super(plugin, origin, one, two);
		File file = new File(plugin.getDataFolder(), mapfile);
		this.content = Files.readAllLines(file.toPath());
	}
	
	private void parseContent() {
		String[] input;
		for (String info : content) {
			switch(info.charAt(0))
			{
			case 'P':
				input = info.split("=")[1].split(" ");
				path = Material.valueOf(input[0]);
			case 'T':
			case 'B':
				input = info.split("=");
				input = input[1].split(" ");
				border = Material.valueOf(input[0]);
			case '1':
			case '2':
			case 'M':
			case ' ':
			}
		}
	}
	
	@Override
	public void generate() {
		this
		.
		parseContent
		(
				)
		;
		for (int x = 0; x < GomokuMC.BOARD_SIZE; x++) {
			for (int z = 0; z < GomokuMC.BOARD_SIZE; z++) {
				if (x == 0 || z == 0 || x == 20 || z == 20) {
					this.origin.getWorld().getBlockAt(this.origin.getBlockX() + x - GomokuMC.BOARD_OFFSET - 1, this.origin.getBlockY() + 1, this.origin.getBlockZ() + z - GomokuMC.BOARD_OFFSET - 1).setType(Material.AIR);
					this.origin.getWorld().getBlockAt(this.origin.getBlockX() + x - GomokuMC.BOARD_OFFSET - 1, this.origin.getBlockY(), this.origin.getBlockZ() + z - GomokuMC.BOARD_OFFSET - 1).setType(GomokuMC.BOARD_BORDER);
				}
				else {
					this.origin.getWorld().getBlockAt(this.origin.getBlockX() + x - GomokuMC.BOARD_OFFSET - 1, this.origin.getBlockY() + 1, this.origin.getBlockZ() + z - GomokuMC.BOARD_OFFSET - 1).setType(Material.AIR);
					this.origin.getWorld().getBlockAt(this.origin.getBlockX() + x - GomokuMC.BOARD_OFFSET - 1, this.origin.getBlockY(), this.origin.getBlockZ() + z - GomokuMC.BOARD_OFFSET - 1).setType(GomokuMC.BOARD_MATERIAL);
				}
			}
		}
	}
}
