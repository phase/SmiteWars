package com.phase.SmiteWars.lib;

import org.bukkit.entity.Player;

public class MessageManager {

	public static final String prefix = "§b§lSmiteWars";
	
	public static void sendMessage(Player p, String s) {
		p.sendMessage(prefix+" §a► §7"+s);
	}

}
