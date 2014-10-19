package com.phase.SmiteWars.arena;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import com.phase.SmiteWars.lib.MessageManager;
import com.phase.SmiteWars.main.SmiteWars;

public class Arena {

	public static ArrayList<Arena> arenaObjects = new ArrayList<Arena>();

	public ArrayList<UUID> Players = new ArrayList<UUID>();

	public HashMap<UUID, Integer> TotalKills = new HashMap<UUID, Integer>();
	public HashMap<UUID, Integer> KillStreak = new HashMap<UUID, Integer>();
	
	public HashMap<UUID, ItemStack[]> items = new HashMap<UUID, ItemStack[]>();
	public HashMap<UUID, ItemStack[]> armor = new HashMap<UUID, ItemStack[]>();
	
	String name;
	Location lobby;
	Location centerSpawn;
	Location leave;
	ArenaState state;
	Scoreboard scoreboard;
	
	int Score_to_win;
	int maxPlayers;
	int minPlayers;
	
	public Arena(String name, Location lobby, Location spawn, Location leave, int maxScore, int maxPlayers, int minPlayers) {
		this.name = name;
		this.centerSpawn = spawn;
		this.lobby = lobby;
		this.state = ArenaState.OUT_OF_GAME;
		this.leave = leave;
		this.Score_to_win = maxScore;
		this.maxPlayers = maxPlayers;
		this.minPlayers = minPlayers;
		scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
		Objective o = scoreboard.registerNewObjective("scores", "dummy");
		o.setDisplayName("§a< "+MessageManager.prefix+"§r §a>");
		o.setDisplaySlot(DisplaySlot.SIDEBAR);
		Objective health = scoreboard.registerNewObjective("health", "health");
		health.setDisplaySlot(DisplaySlot.BELOW_NAME);
		health.setDisplayName("Health");
		arenaObjects.add(this);
	}

	public String getName() {
		return name;
	}
	
	public Location getCenter() {
		return centerSpawn;
	}
	
	public void setCenter(Location l){
		this.centerSpawn = l;
	}
	
	public Location getLeave() {
		return centerSpawn;
	}
	
	public void setLeave(Location l){
		this.leave = l;
	}
	
	public ArrayList<UUID> getPlayers(){
		return Players;
	}
	
	public static Arena getArena(Player p){
		for(Arena a : arenaObjects)
			if(a.getPlayers().contains(p.getUniqueId()))
				return a;
		return null;
	}
	
	public void addPlayer(Player p) {
		if(state.canJoin()){
			if (!(getPlayers().size() >= maxPlayers) || p.hasPermission("smitewars.joinfull")) {
				saveItems(p);
				p.teleport(lobby);
				p.setScoreboard(scoreboard);
				Players.add(p.getUniqueId());
				MessageManager.sendMessage(p, "You have joined the game!");
				checkStart();
				giveKit(p);
				p.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 99999, 99999));
			} else {
				MessageManager.sendMessage(p, "That game is currently full!");
			}
		}else{
			try {
				MessageManager.sendMessage(p, "That game is currently in progress! Highest Score: " + getPlayer(getWinner()).getName() + " - " + TotalKills.get(getWinner()));
			} catch (Exception e) {
				MessageManager.sendMessage(p, "That game is currently in progress!");
			}
		}
	}

	@SuppressWarnings("deprecation")
	public void giveKit(Player p) {
		ItemStack axe = new ItemStack(Material.IRON_AXE);
		axe.addUnsafeEnchantment(Enchantment.KNOCKBACK, 3);
		ItemMeta im = axe.getItemMeta();
		List<String> lore = new ArrayList<String>();
		lore.add("");
		lore.add("§c§lLeft-Click §eto §6§lPVP§e!");
		lore.add("");
		lore.add("§3§lRight-Click §eto §6§lShoot Lightning§e!");
		lore.add("");
		im.setLore(lore);
		im.setDisplayName("§6§lThor's Hammer");
		axe.setItemMeta(im);
		p.getInventory().addItem(axe);
		Arena.giveBook(p);
		ItemStack hat = new ItemStack(Material.CHAINMAIL_HELMET);
		ItemStack chest = new ItemStack(Material.CHAINMAIL_CHESTPLATE);
		ItemStack legs = new ItemStack(Material.CHAINMAIL_LEGGINGS);
		ItemStack boots = new ItemStack(Material.CHAINMAIL_BOOTS);
		ItemMeta mhat = hat.getItemMeta();
		ItemMeta mchest = chest.getItemMeta();
		ItemMeta mlegs = legs.getItemMeta();
		ItemMeta mboots = boots.getItemMeta();
		mhat.setDisplayName("§6§lThor's Helmet");
		mchest.setDisplayName("§6§lThor's Chestplate");
		mlegs.setDisplayName("§6§lThor's Pants");
		mboots.setDisplayName("§6§lThor's Shoes");
		hat.setItemMeta(mhat);
		chest.setItemMeta(mchest);
		legs.setItemMeta(mlegs);
		boots.setItemMeta(mboots);
		p.getInventory().setHelmet(hat);
		p.getInventory().setChestplate(chest);
		p.getInventory().setLeggings(legs);
		p.getInventory().setBoots(boots);
		p.updateInventory();
	}

	public void removePlayer(Player p) {
		reloadItems(p);
		p.teleport(leave);
		Players.remove(p.getUniqueId());
		p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
		MessageManager.sendMessage(p, "You have left the game!");
		p.removePotionEffect(PotionEffectType.SATURATION);
		p.removePotionEffect(PotionEffectType.INCREASE_DAMAGE);
		if(Players.size() < 1){
			stop(false);
		}
	}

	public void respawn(Player p) {
		Random r = new Random();
		p.teleport(centerSpawn.clone().add(r.nextInt(10) - 5, 0,
				r.nextInt(10) - 5));
		KillStreak.put(p.getUniqueId(), 0);
		p.setHealth(20d);
		p.setFoodLevel(20);
		p.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 99999, 999999, false));
	}
	
	public void lobby(Player p){
		p.setHealth(20d);
		p.setFoodLevel(20);
		p.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 99999, 999999, false));
		p.teleport(lobby);
	}

	@SuppressWarnings("deprecation")
	public void start() {
		state = ArenaState.IN_GAME;
		for (UUID u : Players) {
			try {
				respawn(getPlayer(u));
				getPlayer(u).getWorld().playSound(getPlayer(u).getLocation(), Sound.FIREWORK_LARGE_BLAST, 1, 2);
				getPlayer(u).getInventory().clear();
				getPlayer(u).getInventory().setArmorContents(null);
				giveKit(getPlayer(u));
			}catch(Exception e){}
		}
		if(scoreboard.getObjective("scores")==null)
		scoreboard.registerNewObjective("scores", "dummy");
		scoreboard.getObjective("scores").setDisplaySlot(DisplaySlot.SIDEBAR);
		for(UUID u : Players){
			TotalKills.put(u, 0);
			KillStreak.put(u, 0);
			try {
				scoreboard.getObjective("scores").getScore(getPlayer(u)).setScore(0);
			} catch (Exception e) {}
		}
		sendMessage("§a§lThe game has started!");
	}

	@SuppressWarnings("deprecation")
	public void stop(boolean continue1) {
		state = ArenaState.OUT_OF_GAME;
		for(UUID u : Players){
			for (Player p : Bukkit.getOnlinePlayers()) {
				if (p.getUniqueId() == u) {
					p.teleport(lobby);
					p.setHealth(20d);
					p.setFoodLevel(20);
					p.getWorld().playSound(p.getLocation(),
							Sound.FIREWORK_LARGE_BLAST, 1, 2);
					scoreboard.getObjective(DisplaySlot.SIDEBAR).getScore(p)
							.setScore(0);
					p.getInventory().clear();
					p.getInventory().setArmorContents(null);
					Arena.giveBook(p);
				}
			}
			TotalKills.put(u, 0);
			KillStreak.put(u, 0);
		}
		scoreboard.clearSlot(DisplaySlot.SIDEBAR);
		try {
			sendMessage("The Game has ended! " + getPlayer(getWinner()).getName() + " has won the game!");
		} catch (Exception e) {
			sendMessage("The Game has ended!");
		}
		if(!continue1){
			if(Players.size() > 1)
			for(UUID u : Players){
				for(Player p : Bukkit.getOnlinePlayers()){
					if(p.getUniqueId() == u){
						removePlayer(p);
					}
				}
			}
			return;
		}
		sendMessage("Next game starting in 10 seconds!");
		Bukkit.getScheduler().scheduleSyncDelayedTask(SmiteWars.getInstance(), new Runnable() {
			public void run() {
				checkStart();
			}
		}, 10*20);
	}

	private static void giveBook(Player p) {
		ItemStack i = new ItemStack(Material.WRITTEN_BOOK);
		BookMeta bm = (BookMeta) i.getItemMeta();
		bm.setTitle("§d§l§oInstructions Manual");
		bm.setAuthor("§7§lWither Games Network");
		bm.addPage("§0§lContents\n" +
				"\n" +
				"§5§lPage 2: Overview\n" +
				"§5§lPage 3: Points\n" +
				"§5§lPage 4: Killstreaks\n",
				"§0§lOverview:\n" +
				"\n§0The overview of the game is kill everyone in the game using either a Lightning Bolt, or PVP. The score to win is 30.",
				"§0§lPoints:\n§0\n+3 Lightning Bolt kill.\n+1 PVP kill.",
				"§0§lKillstreaks:\n\n§0To get a  killstreak, you must have a certain amount of kills in a row without dying. Each killstreak you get adds to your strength. A killstreak of 3 gives you strength 3 and etc.");
		List<String> lore2 = new ArrayList<String>();
		bm.setLore(lore2);
		i.setItemMeta(bm);
		p.getInventory().addItem(i);
	}

	public void checkStart() {
		if(state == ArenaState.IN_GAME) return;
		if(Players.size() > minPlayers)
			start();
		else{
			Bukkit.getScheduler().scheduleSyncDelayedTask(SmiteWars.getInstance(), new Runnable() {
				public void run() {
					if(state == ArenaState.IN_GAME) return;
					sendMessage("Not enough players, checking again in 10 seconds...");
					checkStart();
				}
			}, 10*20);
		}
		
	}

	public void sendMessage(String s) {
		for (UUID u : Players)
			for (Player p : Bukkit.getOnlinePlayers())
				if (p.getUniqueId() == u)
					MessageManager.sendMessage(p, s);
	}

	public Player getPlayer(UUID u) throws Exception {
		for (Player p : Bukkit.getOnlinePlayers())
			if (p.getUniqueId() == u)
				return p;
		return null;
	}
	
	public UUID getWinner(){
		UUID winner = null;
		for(UUID u : Players)
			if(TotalKills.containsKey(u))
				if(winner == null || TotalKills.get(winner) < TotalKills.get(u))
					winner = u;
		return winner;
	}
	
	public void killStreak(Player p){
		int score = KillStreak.get(p.getUniqueId());
		if(score == 0 || score == 1) return;
		sendMessage(p.getName()+" got a " + getKillStreak(score) + " Kill!");
		p.getWorld().playSound(p.getLocation(), Sound.ENDERMAN_SCREAM, score, score);
	}

	private String getKillStreak(int score) {
		switch(score){
		case 2: return "§a§lDouble";
		case 3: return "§b§lTriple";
		case 4: return "§c§lQuadruple";
		case 5: return "§d§lPenta";
		case 6: return "§e§lHexa";
		case 7: return "§f§lHepta";
		case 8: return "§9§lOcto";
		case 9: return "§1§lEnnea";
		case 10: return "§2§lDeca";
		case 11: return "§3§lHendeca";
		case 12: return "§4§lDodeca";
		case 13: return "§5§lTridecakill";
		case 14: return "§6§lTetradeca";
		case 15: return "§7§lPendedeca";
		case 16: return "§8§lHexdeca";
		case 17: return "§a§lHeptdeca";
		case 18: return "§0§o§lOctdeca";
		case 19: return "§b§o§lEnneadeca";
		case 20: return "§c§o§lIcosa";
		default: return "§4§o§l" + score + "";
		}
	}
	
	@SuppressWarnings("deprecation")
	public void saveItems(Player p){
		items.put(p.getUniqueId(), p.getInventory().getContents());
		armor.put(p.getUniqueId(), p.getInventory().getArmorContents());
		p.getInventory().clear();
		p.getInventory().setArmorContents(null);
		p.updateInventory();
	}
	
	@SuppressWarnings("deprecation")
	public void reloadItems(Player p){
		p.getInventory().clear();
		p.getInventory().setArmorContents(null);
		p.getInventory().setContents(items.get(p.getUniqueId()));
		p.getInventory().setArmorContents(armor.get(p.getUniqueId()));
		p.updateInventory();
	}

	public static boolean isInGame(Player player) {
		return getArena(player) != null;
	}

	public ArenaState getState() {
		return state;
	}

	@SuppressWarnings("deprecation")
	public void kill(Player p, Player killer, int score) {
		KillStreak.put(p.getUniqueId(), 0);
		if (KillStreak.containsKey(killer.getUniqueId())) {
			KillStreak.put(killer.getUniqueId(),
					KillStreak.get(killer.getUniqueId()) + 1);
		} else {
			KillStreak.put(killer.getUniqueId(), 1);
		}
		killer.removePotionEffect(PotionEffectType.INCREASE_DAMAGE);
		killer.addPotionEffect(new PotionEffect(
				PotionEffectType.INCREASE_DAMAGE, 15*20, KillStreak.get(killer
						.getUniqueId()) - 1));
		killStreak(killer);
		if (TotalKills.containsKey(killer.getUniqueId())) {
			TotalKills.put(killer.getUniqueId(),
					TotalKills.get(killer.getUniqueId()) + score);
		} else {
			TotalKills.put(killer.getUniqueId(), score);
		}
		scoreboard.getObjective("scores").getScore(killer)
				.setScore(TotalKills.get(killer.getUniqueId()));
		respawn(p);
		checkWin();
	}

	public void checkWin() {
		for(UUID u : Players){
			if(TotalKills.containsKey(u))
			if(TotalKills.get(u) >= Score_to_win){
				stop(true);
			}
		}
	}

	public static Arena getArena(String line) {
		for(Arena a : arenaObjects){
			if(a.getName().equalsIgnoreCase(line))
				return a;
		}
		return null;
	}

	public int getMaxScore() {
		return Score_to_win;
	}
	
	public int getMaxPlayers(){
		return maxPlayers;
	}
	
	public int getMinPlayers(){
		return minPlayers;
	}
}
