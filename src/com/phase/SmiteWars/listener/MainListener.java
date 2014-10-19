package com.phase.SmiteWars.listener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;

import com.phase.SmiteWars.arena.Arena;
import com.phase.SmiteWars.arena.ArenaState;
import com.phase.SmiteWars.lib.MessageManager;
import com.phase.SmiteWars.main.SmiteWars;

public class MainListener implements Listener {
	
	ArrayList<UUID> lightningDelay = new ArrayList<UUID>();
	HashMap<UUID, Location> lastLightning = new HashMap<UUID, Location>();
	HashMap<UUID, UUID> lastDamage = new HashMap<UUID, UUID>();
	
	@EventHandler
	public void breakBlock(BlockBreakEvent e){
		if(Arena.isInGame(e.getPlayer()))
			e.setCancelled(true);
	}
	
	@SuppressWarnings("deprecation")
	@EventHandler
	public void interact(PlayerInteractEvent e){
		final Player p = e.getPlayer();
		if(!Arena.isInGame(p)) return;
		if(!Arena.getArena(p).getState().equals(ArenaState.IN_GAME)) return;
		if(p.getItemInHand().getType().equals(Material.IRON_AXE) && e.getAction() == Action.RIGHT_CLICK_AIR){
			Location l = p.getTargetBlock(null, 50).getLocation();
			if(lightningDelay.contains(p.getUniqueId())) return;
			lightningDelay.add(p.getUniqueId());
			l.getWorld().strikeLightning(l);
			lastLightning.put(p.getUniqueId(), l);
			Bukkit.getScheduler().scheduleSyncDelayedTask(SmiteWars.getInstance(), new Runnable() {
				public void run() {
					lastLightning.remove(p.getUniqueId());
					lightningDelay.remove(p.getUniqueId());
					p.getWorld().playSound(p.getLocation(), Sound.LEVEL_UP, 1, 2);
				}
			}, 20*2);
		}
	}

	@EventHandler
	public void damageEn(EntityDamageByEntityEvent e) {
		if (e.getEntity() instanceof Player && e.getDamager() instanceof Player) {
			Player p = (Player) e.getEntity();
			Player d = (Player) e.getDamager();
			if (!Arena.isInGame(p) || !Arena.isInGame(d))
				return;
			if (Arena.getArena(p).getState() == ArenaState.OUT_OF_GAME) {
				e.setCancelled(true);
				return;
			}
			for(UUID u : lastDamage.keySet()){
				if(lastDamage.get(u) == p.getUniqueId()){
					lastDamage.remove(u);
				}
			}
			lastDamage.put(d.getUniqueId(), p.getUniqueId());
		}
	}
	
	@EventHandler
	public void death(PlayerDeathEvent e){
		Player p = e.getEntity();
		if(Arena.isInGame(p)){
			e.getDrops().clear();
			if(Arena.getArena(p).getState() == ArenaState.OUT_OF_GAME){
				return;
			}
			try{
			e.setDeathMessage(MessageManager.prefix+" §a► §7"+p.getKiller().getName() + " killed " + p.getName() + "!");
			Arena.getArena(p).kill(p, p.getKiller(), 1);
			}catch(NullPointerException e1){
				for(UUID u1 : lastDamage.keySet()){
					if(lastDamage.get(u1) == p.getUniqueId()){
						for(Player n : Bukkit.getOnlinePlayers()){
							if(n.getUniqueId() == u1){
								e.setDeathMessage(MessageManager.prefix+" §a► §7"+n.getName() + " scorched " + p.getName() + "!");
								Arena.getArena(p).kill(p, n, 1);
							}
						}
					}
				}
			}
		}
	}
	
	@EventHandler
	public void respawn(PlayerRespawnEvent e){
		final Player p = e.getPlayer(); if(!Arena.isInGame(p)) return;
		Bukkit.getScheduler().scheduleSyncDelayedTask(SmiteWars.getInstance(), new Runnable() {
			public void run() {
				if(Arena.getArena(p).getState() == ArenaState.OUT_OF_GAME){
					Arena.getArena(p).lobby(p);
					return;
				}
				Arena.getArena(p).respawn(p);
				Arena.getArena(p).giveKit(p);
			}
		}, 1);
	}
	
	@EventHandler
	public void damage(EntityDamageEvent e){
		if(!(e.getEntity() instanceof Player)) return;
		if(!Arena.isInGame(((Player)e.getEntity())))	return;
		Player p = (Player)e.getEntity();
		final Location l = p.getLocation();
		if(e.getCause() == DamageCause.LIGHTNING){
			for(UUID u : lastLightning.keySet()){
				for (Player n : Bukkit.getOnlinePlayers()) {
					if (n.getUniqueId() == u) {
						if (Arena.isInGame(n)) {
							if (lastLightning.get(u).distance(l) < 3) {
								lastDamage.put(n.getUniqueId(), p.getUniqueId());
							}
						}
					}
				}
			}
		}
		if(e.getCause() == DamageCause.VOID){
			if(Arena.getArena(p).getState() == ArenaState.OUT_OF_GAME){
				p.teleport(Arena.getArena(p).getCenter());
				return;
			}
			e.setCancelled(true);
			for(UUID u : lastDamage.keySet())
				if(lastDamage.get(u) == p.getUniqueId())
					try {
						Arena.getArena(p).kill(p, Arena.getArena(p).getPlayer(u), 1);
						return;} 
                 catch (Exception e1) {Arena.getArena(p).respawn(p);return;}
			Arena.getArena(p).respawn(p);
		}
	}

	
	public static boolean isASign(Block b){
		return b.getType() == Material.SIGN || b.getType() == Material.WALL_SIGN || b.getType() == Material.SIGN_POST;
	}
	public static Sign getSign(Block b){
		return (Sign) b.getState();
	}
	
	@EventHandler
	public void SignClick(PlayerInteractEvent e){
		Player p = e.getPlayer();
		if(e.getClickedBlock() != null){
			if(isASign(e.getClickedBlock())){
				if(e.getAction() == Action.RIGHT_CLICK_BLOCK)
				if(getSign(e.getClickedBlock()).getLine(0).equalsIgnoreCase(MessageManager.prefix)){
					Arena a = Arena.getArena(ChatColor.stripColor(getSign(e.getClickedBlock()).getLine(1)));
					if(Arena.isInGame(p)){
						MessageManager.sendMessage(p, "You are already in a game!");
						return;
					}
					a.addPlayer(p);
				}
			}
		}
	}
	
	@EventHandler
	public void SignChange(SignChangeEvent e){
		if(!isASign(e.getBlock())) return;
		Sign sign = (Sign) e.getBlock().getState();
		Player p = e.getPlayer();
		if(!p.isOp()) return;
		if(e.getLine(0).equalsIgnoreCase("smite")){
				MessageManager.sendMessage(p, e.getLine(1));
				if(Arena.getArena(e.getLine(1)) != null){
					e.setLine(0, MessageManager.prefix);
					e.setLine(1, "§a"+e.getLine(1));
					e.setLine(2, "§0§l§oClick to");
					e.setLine(3, "§0§l§ojoin!");
					sign.update();
				}else{
					MessageManager.sendMessage(p, "That is not an arena!");
				}
		}
		
	}
	
	@EventHandler
	public void leave(PlayerQuitEvent e){
		Player p = e.getPlayer();
		if(Arena.isInGame(p))
			Arena.getArena(p).removePlayer(p);
	}
	
	@EventHandler
	public void move(PlayerMoveEvent e){
		Player p = e.getPlayer();
		if(Arena.isInGame(p)){
			if (p.getGameMode() != GameMode.CREATIVE
					&& p.getLocation().clone().subtract(0, 1, 0).getBlock()
							.getType() != Material.AIR && !p.isFlying()) {
				p.setAllowFlight(true);
			}
		}
	}
	
	@EventHandler
	public void flight(PlayerToggleFlightEvent e){
		Player p = e.getPlayer();
		if(Arena.isInGame(p)){
			if(p.getGameMode()!=GameMode.CREATIVE){
				e.setCancelled(true);
				p.setAllowFlight(false);
				p.setFlying(false);
				p.setVelocity(p.getLocation().getDirection().multiply(1.25).setY(1));
				p.getWorld().playSound(p.getLocation(), Sound.ENDERDRAGON_WINGS, 1, 1);
			}
		}
	}
	
	
	@EventHandler
	public void drop(PlayerDropItemEvent e){
		if(Arena.isInGame(e.getPlayer()))
			e.setCancelled(true);
	}
	
	@EventHandler
	public void pickup(PlayerPickupItemEvent e){
		if(Arena.isInGame(e.getPlayer()))
			e.setCancelled(true);
	}
	
}
