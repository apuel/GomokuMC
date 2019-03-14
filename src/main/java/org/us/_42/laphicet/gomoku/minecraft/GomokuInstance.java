package org.us._42.laphicet.gomoku.minecraft;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public class GomokuInstance {
	
	private Location origin;
	private Player[] players = new Player[2];
	
	public GomokuInstance(Location origin, Player one, Player two) {
		this.origin = origin;
		this.players[0] = one;
		this.players[1] = two;
	}
	
	
	
}
