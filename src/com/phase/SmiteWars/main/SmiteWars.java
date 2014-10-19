package com.phase.SmiteWars.main;

import java.io.File;
import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.phase.SmiteWars.arena.Arena;
import com.phase.SmiteWars.arena.ArenaState;
import com.phase.SmiteWars.lib.MessageManager;
import com.phase.SmiteWars.listener.MainListener;

public class SmiteWars extends JavaPlugin {
	
	private static Plugin instance;
	
	File f;
	FileConfiguration config;
	
	Location MainLobby = null;
	Location Leave = null;
	
	public void onEnable(){
		instance = this;
		f = new File(getDataFolder() +"/arenas.yml");
		if(!f.exists())
			f.mkdir();
		config = YamlConfiguration.loadConfiguration(f);
		Bukkit.getPluginManager().registerEvents(new MainListener(), this);
		if(config.contains("MainLobby.lobby")){
			MainLobby = deserializeLocation(config.getString("MainLobby.lobby"));
		}
		if(config.contains("MainLobby.leave")){
			Leave = deserializeLocation(config.getString("MainLobby.leave"));
		}
		if(config.contains("Arenas")){
			for(String a : config.getConfigurationSection("Arenas").getKeys(false)){
				int maxScore = 30;
				int maxPlayers = 16;
				int minPlayers = 8;
				try{
					maxScore = config.getInt("Arenas."+a+".maxScore");
					maxPlayers = config.getInt("Arenas."+a+".maxPlayers");
					minPlayers = config.getInt("Arenas."+a+".minPlayers");
				}catch(Exception e){}
				new Arena(a, MainLobby, deserializeLocation(config.getString("Arenas."+a+".spawn")), MainLobby, maxScore, maxPlayers, minPlayers );
			}
		}
	}
	
	public void onDisable(){
		if(MainLobby != null){
			config.set("MainLobby.lobby", serializeLocation(MainLobby));
		}
		if(Leave != null){
			config.set("MainLobby.leave", serializeLocation(Leave));
		}
		for(Arena a : Arena.arenaObjects){
			config.set("Arenas."+a.getName()+".spawn", serializeLocation(a.getCenter()));
			config.set("Arenas."+a.getName()+".maxScore", a.getMaxScore());
			config.set("Arenas."+a.getName()+".maxPlayers", a.getMaxPlayers());
			config.set("Arenas."+a.getName()+".minPlayers", a.getMinPlayers());
			a.stop(false);
		}
		try {
			config.save(f);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static Plugin getInstance() {
		return instance;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String c, String[] args) {
		if(!(sender instanceof Player)){
			sender.sendMessage("You have to be a player to do this!");
			return false;
		}
		Player p = (Player)sender;
		
		if(c.equalsIgnoreCase("sm")){
			if(!p.isOp()){
				p.sendMessage("§c§lYou do not have permission to view the commands.");
				return false;
			}
			if(args.length == 0){
				sm(p, "/sm <create|setlobby|forcestart>");
				return false;
			}
			if(args[0].equalsIgnoreCase("create")){
				if(!p.isOp()){
					p.sendMessage("§c§lYou do not have permission to create a game.");
					return false;
				}
				if(MainLobby == null){
					sm(p, "The lobby is null! Do '/sm setlobby' to set it!");
					return false;
				}
				if(args.length != 2){
					sm(p, "/sm create <name> | This will create an arena where you are standing.");
					return false;
				}
				if(Arena.getArena(args[1]) == null){
					sm(p, "Arena §a" + args[1] + " §7has been created!");
					new Arena(args[1], MainLobby, p.getLocation(), MainLobby, 30, 16, 8);
				}else{
					sm(p, "Arena §a" + args[1] + " §7already exist!");
				}
			}
			if(args[0].equalsIgnoreCase("setlobby")){
				if(!p.isOp()){
					p.sendMessage("§c§lYou do not have permission to set the lobby.");
					return false;
				}
				sm(p, "Set lobby to your location!");
				MainLobby = p.getLocation();
			}
			if(args[0].equalsIgnoreCase("forcestart") || args[0].equalsIgnoreCase("fs")){
				if(!p.isOp()){
					p.sendMessage("§c§lYou do not have permission to start the game.");
					return false;
				}
				if(Arena.isInGame(p)){
					if(Arena.getArena(p).getState() == ArenaState.OUT_OF_GAME)
					Arena.getArena(p).start();
					else sm(p, "That game has already started!");
				}else{
					if(args.length != 2){
						sm(p, "/sm forcestart <arena>");
						return false;
					}
					if(Arena.getArena(args[1]).getState() == ArenaState.OUT_OF_GAME)
					Arena.getArena(args[1]).start();
					else sm(p, "That game has already started!");
				}
			}
			if(args[0].equalsIgnoreCase("setleave")){
				Leave = p.getLocation();
				sm(p, "Leave location set to your position!");
			}
		}
		else if(c.equalsIgnoreCase("leave")){
			if(Arena.isInGame(p)){
				Arena.getArena(p).removePlayer(p);
				if(Leave != null){
					p.teleport(Leave);
				}
			}else{
				sm(p, "You are not in a game!");
			}
		}
		return false;
	}
	
	private void sm(Player p, String s) {
		MessageManager.sendMessage(p, s);
	}
	
	public static String serializeLocation(Location l) {
		return l.getWorld().getName() + "," + l.getX() + "," + l.getY() + ","
				+ l.getZ();
	}

	public static Location deserializeLocation(String s) {
		return new Location(Bukkit.getWorld(s.split(",")[0]),
				Double.parseDouble(s.split(",")[1]), Double.parseDouble(s
						.split(",")[2]), Double.parseDouble(s.split(",")[3]));
	}
}
