package org.us._42.laphicet.gomoku.minecraft;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public class ArenaInstance extends GomokuInstance {
	public ArenaInstance(GomokuMC plugin, Location origin, Player one, Player two) {
		super(plugin, origin, one, two);
	}
}
