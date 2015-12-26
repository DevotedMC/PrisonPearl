package com.untamedears.PrisonPearl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.minelink.ctplus.compat.api.NpcIdentity;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.java.JavaPlugin;

import com.untamedears.PrisonPearl.database.PrisonPearlMysqlStorage;
import com.untamedears.PrisonPearl.events.PrisonPearlEvent;
import com.untamedears.PrisonPearl.events.SummonEvent;
import com.untamedears.PrisonPearl.managers.BanManager;
import com.untamedears.PrisonPearl.managers.BroadcastManager;
import com.untamedears.PrisonPearl.managers.CombatTagManager;
import com.untamedears.PrisonPearl.managers.DamageLogManager;
import com.untamedears.PrisonPearl.managers.PrisonPearlManager;
import com.untamedears.PrisonPearl.managers.PrisonPortaledPlayerManager;
import com.untamedears.PrisonPearl.managers.SummonManager;
import com.untamedears.PrisonPearl.managers.WorldBorderManager;

import vg.civcraft.mc.bettershards.BetterShardsAPI;
import vg.civcraft.mc.bettershards.events.PlayerChangeServerReason;
import vg.civcraft.mc.mercury.MercuryAPI;
import vg.civcraft.mc.namelayer.NameAPI;

public class PrisonPearlPlugin extends JavaPlugin implements Listener {
	private static PrisonPearlPlugin globalInstance = null;
	
	private PPConfig ppconfig;

	private PrisonPearlStorage pearls;
	private EnderExpansion ee;
	private DamageLogManager damageman;
	public static PrisonPearlManager pearlman;
	private SummonManager summonman;
	private PrisonPortaledPlayerManager portalman;
	private BroadcastManager broadcastman;
	private AltsList altsList;
	private BanManager banManager_;
	private WorldBorderManager wbManager;
	private static File data;
	private static Logger log;
	private static final Integer maxImprisonedAlts = 2;
	//private static long loginDelay = 10*60*1000;
	private static final String kickMessage = "You have too many imprisoned alts! If you think this is an error, please message the mods on /r/civcraft";
	//private static String delayMessage = "You cannot switch alt accounts that quickly, please wait ";
	private HashMap<UUID, Long> lastLoggout;
	
	private CombatTagManager combatTagManager;
	
	private boolean isNameLayer;
	private boolean isMercury;
	private boolean isWorldBorder;
	private boolean isBetterShards;
	private PrisonPearlMysqlStorage mysqlStorage = null;
	
	private Map<String, PermissionAttachment> attachments;
	
	private final boolean startupFeed = true; //ADDED SO ONE CAN DISABLE STARTUP FEED
	
	public void onEnable() {
		isNameLayer = Bukkit.getPluginManager().isPluginEnabled("NameLayer");
		isMercury = Bukkit.getPluginManager().isPluginEnabled("Mercury");
		isWorldBorder = Bukkit.getPluginManager().isPluginEnabled("WorldBorder");
		isBetterShards = Bukkit.getPluginManager().isPluginEnabled("BetterShards");
		globalInstance = this;
		File dat = getDataFolder();
		data=dat;
		getConfig().options().copyDefaults(true);
		saveConfig();
		
		ppconfig = new PPConfig(getConfig());
		
		log = this.getLogger();
		
		banManager_ = new BanManager(this);
		banManager_.setBanMessage(kickMessage);
		banManager_.initialize();
		
		//lastLoggout = new HashMap<String, Long>();
		//wasKicked = new HashMap<String, Boolean>();
		
		pearls = new PrisonPearlStorage(this);
		 // updates files to uuid
		try {
			if (getMysqlStorage() == null) {
				updateToUUID();
				updateToRemoveDur();
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		pearlman = new PrisonPearlManager(this, pearls, ee);
		load(pearls, getPrisonPearlsFile());
		damageman = new DamageLogManager(this);
		ee= new EnderExpansion(pearls);
		summonman = new SummonManager(this, pearls);
		load(summonman, getSummonFile());
		portalman = new PrisonPortaledPlayerManager(this, pearls);
		load(portalman, getPortaledPlayersFile());
		broadcastman = new BroadcastManager();
		combatTagManager = new CombatTagManager(this.getServer(), log);
		wbManager = new WorldBorderManager(this);
		load(wbManager, getWhiteListedLocationsFile());
		loadAlts();
		// Isnt needed since loadAlts() does this already
		// checkBanAllAlts();
	
		
//		if (Bukkit.getPluginManager().isPluginEnabled("PhysicalShop"))
//			new PhysicalShopListener(this, pearls);
		if (Bukkit.getPluginManager().isPluginEnabled("CombatTag"))
			new CombatTagListener(this, pearlman);
		
		Bukkit.getPluginManager().registerEvents(this, this);
		Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
			public void run() {
				saveAll(false);
			}
		}, 0, getConfig().getLong("save_ticks"));
		
		PrisonPearlCommands commands = new PrisonPearlCommands(this, damageman, pearls, pearlman, summonman, broadcastman, wbManager);
		
		for (String command : getDescription().getCommands().keySet()) {
			if (command.equals("ppkill") && !getConfig().getBoolean("ppkill_enabled"))
				continue;
			
			getCommand(command).setExecutor(commands);
		}
		
		attachments = new HashMap<String, PermissionAttachment>();
		for (Player player : Bukkit.getOnlinePlayers())
			updateAttachment(player);
		
		if (startupFeed){
			//try{
				//Thread.sleep(1000 * 60);
				log.log(Level.INFO, pearls.feedPearls(pearlman));;
			//}
			//catch(Exception e){
				//System.out.println("A straight foolish error has occurred while loading PrisonPearl.");
			//}
		}
		
		if (isMercury){
			getServer().getPluginManager().registerEvents(new MercuryListener(this, pearls), this);
			MercuryAPI.instance.registerPluginMessageChannel(MercuryListener.channels);
			Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable(){

				@Override
				public void run() {
					updateAllPearlLocations();
				}
				
			}, 300, 150);
		}
		if (isBetterShards) {
			getServer().getPluginManager().registerEvents(new BetterShardsListener(), this);
		}
	}

	public void onDisable() {
		saveAll(true);
		
		for (PermissionAttachment attachment : attachments.values())
			attachment.remove();
		globalInstance = null;
	}
	
	public void updateToUUID() throws IOException{
		File file = new File(getDataFolder(), "alts.txt");
		File newFile = null;
		
		if (file.exists()){ // update alts.txt to new file format
			log.log(Level.INFO, "Updating to new uuid format for alts.txt");
			newFile = new File(getDataFolder(), "altsUUID.txt");
			FileInputStream fis;
			fis = new FileInputStream(file);
			BufferedReader br = new BufferedReader(new InputStreamReader(fis));
			String line;
			Map<UUID, List<UUID>> uuids = new HashMap<UUID, List<UUID>>();
			while ((line = br.readLine()) != null) {
				if (line.length() > 1) {
					List<String> parts = Arrays.asList(line.split(" "));
					List<UUID> accounts = new ArrayList<UUID>();
					for (String part : parts) {
						UUID uuid = null;
			            if (isNameLayer)
			            	uuid = NameAPI.getUUID(part);
			            else
			            	uuid = Bukkit.getOfflinePlayer(part).getUniqueId();
						if (uuid == null){
							uuid = Bukkit.getOfflinePlayer(part).getUniqueId();
						}
						accounts.add(uuid);
					}
					for (UUID uuid: accounts)
						uuids.put(uuid, accounts);
				}
			}
			br.close();
			
			FileOutputStream fos = new FileOutputStream(newFile);
			BufferedWriter brr = new BufferedWriter(new OutputStreamWriter(fos));
			for (UUID uuid: uuids.keySet()){
				for (UUID alt: uuids.get(uuid))
					brr.append(alt.toString() + " ");
				brr.append("\n");
			}
			brr.flush();
			brr.close();
			file.delete();
		}
		
		file = new File(getDataFolder(), "portaledplayers.txt");
		if (file.exists()){
			log.log(Level.INFO, "Updating to new uuid format for portaledplayers.txt");
			newFile = new File(getDataFolder(), "portaledplayersUUID.txt");
			FileInputStream fis;
			fis = new FileInputStream(file);
			BufferedReader br = new BufferedReader(new InputStreamReader(fis));
			
			FileOutputStream fos = new FileOutputStream(newFile);
			BufferedWriter brr = new BufferedWriter(new OutputStreamWriter(fos));
			
			String line;
			while ((line = br.readLine()) != null){
				UUID uuid = null;
	            if (isNameLayer)
	            	uuid = NameAPI.getUUID(line);
	            else
	            	uuid = Bukkit.getOfflinePlayer(line).getUniqueId();
				if (uuid == null)
					uuid = Bukkit.getOfflinePlayer(line).getUniqueId();
				brr.append(uuid.toString() + "\n");
			}
	
			brr.flush();
			brr.close();
			br.close();
			file.delete();
		}
		
		file = new File(getDataFolder(), "prisonpearls.txt");
		if (file.exists()){
			log.log(Level.INFO, "Updating to new uuid format for prisonpearls.txt");
			newFile = new File(getDataFolder(), "prisonpearlsUUID.txt");
			
			FileInputStream fis;
			fis = new FileInputStream(file);
			BufferedReader br = new BufferedReader(new InputStreamReader(fis));
			
			FileOutputStream fos = new FileOutputStream(newFile);
			BufferedWriter brr = new BufferedWriter(new OutputStreamWriter(fos));
			
			String line = br.readLine();
			brr.append(line + "\n");
			while ((line = br.readLine()) != null){
				String[] parts = line.split(" ");
				String name = parts[1];
				UUID uuid = null;
	            if (isNameLayer)
	            	uuid = NameAPI.getUUID(name);
	            else
	            	uuid = Bukkit.getOfflinePlayer(name).getUniqueId();
				if (uuid == null)
					uuid = Bukkit.getOfflinePlayer(name).getUniqueId();
				brr.append(parts[0] + " " + uuid.toString() + " ");
				for (int x = 2; x < parts.length; x++)
					brr.append(parts[x] + " ");
				brr.append("\n");
			}
			brr.flush();
			brr.close();
			br.close();
			file.delete();
		}
		
		file = new File(getDataFolder(), "summons.txt");
		if (file.exists()){
			log.log(Level.INFO, "Updating to new uuid format for summons.txt");
			newFile = new File(getDataFolder(), "summonsUUID.txt");
			
			FileInputStream fis;
			fis = new FileInputStream(file);
			BufferedReader br = new BufferedReader(new InputStreamReader(fis));
			
			FileOutputStream fos = new FileOutputStream(newFile);
			BufferedWriter brr = new BufferedWriter(new OutputStreamWriter(fos));
			
			String line;
			while ((line = br.readLine()) != null){
				String[] parts = line.split(" ");
				String name = parts[0];
				UUID uuid = NameAPI.getUUID(name);
				if (uuid == null)
					uuid = Bukkit.getOfflinePlayer(name).getUniqueId();
				brr.append(uuid.toString() + " ");
				for (int x = 1; x < parts.length; x++)
					brr.append(parts[x] + " ");
				brr.append("\n");
			}
			brr.flush();
			brr.close();
			br.close();
			file.delete();
		}
		
		file = new File(getDataFolder(), "ban_journal.dat");
		if (file.exists()){
			log.log(Level.INFO, "Updating to new uuid format for ban_journal.dat");
			newFile = new File(getDataFolder(), "ban_journalUUID.dat");
			
			FileInputStream fis;
			fis = new FileInputStream(file);
			BufferedReader br = new BufferedReader(new InputStreamReader(fis));
			
			FileOutputStream fos = new FileOutputStream(newFile);
			BufferedWriter brr = new BufferedWriter(new OutputStreamWriter(fos));
			
			String line;
			while ((line = br.readLine()) != null){
				String[] parts = line.split("|");
				String name = parts[0];
				UUID uuid = null;
	            if (isNameLayer)
	            	uuid = NameAPI.getUUID(name);
	            else
	            	uuid = Bukkit.getOfflinePlayer(name).getUniqueId();
				if (uuid == null)
					uuid = Bukkit.getOfflinePlayer(name).getUniqueId();
				brr.append(uuid+"|"+parts[1]+"\n");
			}
			brr.flush();
			brr.close();
			br.close();
			file.delete();
		}
	}
	
	private void updateToRemoveDur() {
		File file = getPrisonPearlsFile();
		FileInputStream fis;
		try {
			fis = new FileInputStream(file);
			BufferedReader br = new BufferedReader(new InputStreamReader(fis));
			try {
				Integer.parseInt(br.readLine());
			} catch (NumberFormatException e) {
				br.close();
				return;
			}
			
			File tempFile = new File(data, "temp.txt");
			tempFile.createNewFile();
			FileOutputStream fos = new FileOutputStream(tempFile);
			PrintWriter brr = new PrintWriter(new OutputStreamWriter(fos), false);
			
			String line = "";
			Random rand = new Random();
			while ((line = br.readLine()) != null) {
				if(line.matches("lastFeed:([0-9]+)"))
					continue;
				StringBuilder newLine = new StringBuilder();
				String[] parts = line.split(" ");
				for (int x = 1; x < parts.length; x++) {
					newLine.append(parts[x] + " ");
					if (x == 5)
						newLine.append(rand.nextInt(1000) + " ");
				}
				brr.append(newLine.toString() +  "\n");
			}
			brr.flush();
			brr.close();
			br.close();
			file.delete();
			tempFile.renameTo(file);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void saveAll(boolean force) {
		if (force || pearls.isDirty())
			save(pearls, getPrisonPearlsFile());
		if (force || summonman.isDirty())
			save(summonman, getSummonFile());
		if (force || portalman.isDirty())
			save(portalman, getPortaledPlayersFile());
		if (force || wbManager.isDirty())
			save(wbManager, getWhiteListedLocationsFile());
	}
	
	private static void load(SaveLoad obj, File file) {
		try {
			obj.load(file);
		} catch (FileNotFoundException e) {
			System.out.println(file.getName() + " not exist, creating.");

			try {
				obj.save(file);
			} catch (IOException e2) {
                throw new RuntimeException("Failed to create " + file.getAbsolutePath(), e2);
			}
		} catch (IOException e) {
			throw new RuntimeException("Failed to load prison pearls from " + file.getAbsolutePath(), e);
		}
	}
	
	private static void save(SaveLoad obj, File file) {
		try {
			if (globalInstance.getMysqlStorage() != null) {
				return;
			}
			File newfile = new File(file.getAbsolutePath() + ".new");
			File bakfile = new File(file.getAbsolutePath() + ".bak");
			
			obj.save(newfile);

            if (bakfile.exists()) {
                //noinspection ResultOfMethodCallIgnored
                bakfile.delete();
            }

			if (file.exists() && !file.renameTo(bakfile))
				throw new IOException("Failed to rename " + file.getAbsolutePath() + " to " + bakfile.getAbsolutePath());
			if (!newfile.renameTo(file))
				throw new IOException("Failed to rename " + newfile.getAbsolutePath() + " to " + file.getAbsolutePath());
		} catch (IOException e) {
			throw new RuntimeException("Failed to save prison pearls to " + file.getAbsolutePath(), e);
		}
	}

	private static File getPrisonPearlsFile() {
		return new File(data, "prisonpearlsUUID.txt");
	}
	
	private File getSummonFile() {
		return new File(getDataFolder(), "summonsUUID.txt");
	}
	
	private File getPortaledPlayersFile() {
		return new File(getDataFolder(), "portaledplayersUUID.txt");
	}
	
	private File getWhiteListedLocationsFile() {
		return new File(getDataFolder(), "whitelistedlocations.txt");
	}

	private File getAltsListFile() {
		return getFile("altsUUID.txt");
	}
	private File getUUIDListFile() {
		return getFile("uuids.txt");
	}
	private File getFile(String name){
		File file = new File(getDataFolder(), name);
		if (!file.exists()) {
			try {
				FileOutputStream fos = new FileOutputStream(file);
				BufferedWriter br = new BufferedWriter(new OutputStreamWriter(fos));
				br.write("\n");
				br.flush();
				fos.close();
			} catch(IOException ex) {
			}
		}
		return file;
		
	}
	
	
	// Free player if he was free'd while offline
	// otherwise, correct his spawn location if necessary
	@EventHandler(priority=EventPriority.HIGHEST)
	public void onPlayerJoin(PlayerJoinEvent event) {
		final Player player = event.getPlayer();
		updateAttachment(player);
		checkBan(player.getUniqueId());
		
		if (player.isDead())
			return;
		
		Location loc = player.getLocation();
		Location newloc = getRespawnLocation(player, loc);
		if (newloc != null) {
			if (loc.getWorld() == getPrisonWorld() && (newloc.getWorld() != loc.getWorld() || newloc == RESPAWN_PLAYER)) {
				player.sendMessage("While away, you were freed!"); // he was freed offline
			}
			delayedTp(player, newloc, ppconfig.getPpsummonClearInventory());
		} else {
			prisonMotd(player); 
		}
	}
	
	// don't let people escape through the end portal
	@EventHandler(priority=EventPriority.HIGHEST)
	public void onPlayerPortalEvent(PlayerPortalEvent event) {
		Player player = event.getPlayer();
		
		if (pearls.isImprisoned(player) && !summonman.isSummoned(player)) { // if in prison but not imprisoned
            Location toLoc = event.getTo();
			if (toLoc != null && toLoc.getWorld() != getPrisonWorld()) {
				prisonMotd(player);
				delayedTp(player, getPrisonSpawnLocation(), false);
			}
		}
	}
	
	// remove permission attachments and record the time players log out
	@EventHandler(priority=EventPriority.MONITOR)
	public void onPlayerQuit(PlayerQuitEvent event) {
		PermissionAttachment attachment = attachments.remove(event.getPlayer().getName());
		if (attachment != null)
			attachment.remove();
	}

	// adjust spawnpoint if necessary
	@EventHandler(priority=EventPriority.HIGHEST)
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		prisonMotd(event.getPlayer());
		Location newloc = getRespawnLocation(event.getPlayer(), event.getRespawnLocation());
		if (newloc != null && newloc != RESPAWN_PLAYER)
			event.setRespawnLocation(newloc);
	}

	// called when a player joins or spawns
	private void prisonMotd(Player player) {
		if (pearls.isImprisoned(player) && !summonman.isSummoned(player)) { // if player is imprisoned
			for (String line : getConfig().getStringList("prison_motd")) // give him prison_motd
				player.sendMessage(line);
            player.sendMessage(pearls.getByImprisoned(player).getMotd());
		}
	}	
	
	private static final Location RESPAWN_PLAYER = new Location(null, 0, 0, 0);
	
	// gets where the player should be respawned at
	// returns null if the curloc is an acceptable respawn location
	private Location getRespawnLocation(Player player, Location curloc) {
		if (isMercury && isBetterShards) {
			if (pearls.isImprisoned(player)) {
				String server = MercuryAPI.serverName();
				String toServer = getConfig().getString("prison_server");
				if (!server.equals(toServer)) {
					BetterShardsAPI.connectPlayer(player, toServer, PlayerChangeServerReason.PLUGIN);
					return null;
				}
			}
			
		} else if (pearls.isImprisoned(player)) { // if player is imprisoned
			if (summonman.isSummoned(player)) { // if summoned
				return null; // don't modify location
            } else if (curloc.getWorld() != getPrisonWorld()) { // but not in prison world
				return getPrisonSpawnLocation(); // should bre respawned in prison
            }
		} else if (curloc.getWorld() == getPrisonWorld() && !portalman.isPlayerPortaledToPrison(player)) { // not imprisoned, but spawning in prison?
			// This indicates that the player was freed while logged out.
			if (player.getBedSpawnLocation() != null) { // if he's got a bed
				return player.getBedSpawnLocation(); // spawn him there
            } else if (getConfig().getBoolean("free_respawn")) { // if we should respawn instead of tp to spawn
				return RESPAWN_PLAYER; // kill the player
            } else {
				return getFreeWorld().getSpawnLocation(); // otherwise, respawn him at the spawn of the free world
            }
		}
		
		return null; // don't modify respawn location
	}
	
	// Imprison people upon death
	@EventHandler(priority=EventPriority.HIGHEST)
	public void onEntityDeath(EntityDeathEvent event) {
		if (!(event.getEntity() instanceof Player))
			return;
		
		Player player = (Player)event.getEntity();
		UUID uuid = player.getUniqueId();
		String playerName = player.getName();
		
		if (combatTagManager.isCombatTagNPC(event.getEntity()))  {
			playerName = player.getName();
			// UUID being passed isn't the right one.
			uuid = NameAPI.getUUID(playerName);
			//String realName = combatTagManager.getNPCPlayerName(player);
			log.info("NPC Player: "+playerName+", ID: "+ uuid);
//			if (!realName.equals("")) {
//				playerName = realName;
//			}
		}
		else if (combatTagManager.isCombatTagPlusNPC(player)){
			NpcIdentity iden = combatTagManager.getCombatTagPlusNPCIdentity(player);
			uuid = iden.getId();
			playerName = iden.getName();
			log.info("NPC Player: " + playerName + ", ID: " + uuid);
		} else if (combatTagManager.isEnabled() && !combatTagManager.isCombatTagged(player)) {
			log.info("Player: " + playerName + " is out of combatTag, immune from pearling.");
			return;
		}
		
		PrisonPearl pp = pearls.getByImprisoned(uuid); // find out if the player is imprisoned
		if (pp != null) { // if imprisoned
			if (!getConfig().getBoolean("prison_stealing") || player.getLocation().getWorld() == getPrisonWorld()) {// bail if prisoner stealing isn't allowed, or if the player is in prison (can't steal prisoners from prison ever)
				// reveal location of pearl to damaging players if pearl stealing is disabled
				for (Player damager : damageman.getDamagers(player)) {
					damager.sendMessage(ChatColor.GREEN+"[PrisonPearl] "+playerName+" cannot be pearled here because they are already "+pp.describeLocation());
				}
				return;
			}
		}
		
		for (Player damager : damageman.getDamagers(player)) { // check to see if anyone can imprison him
			if (pp != null && pp.getHolderPlayer() == damager) // if this damager has already imprisoned this person
				break; // don't be confusing and re-imprison him, just let him die
			
			int firstpearl = Integer.MAX_VALUE; // find the first regular enderpearl in their inventory
			for (Entry<Integer, ? extends ItemStack> entry : damager.getInventory().all(Material.ENDER_PEARL).entrySet()) {
				ItemStack stack = entry.getValue();
				if (!stack.hasItemMeta())
					firstpearl = Math.min(entry.getKey(), firstpearl);
			}
			
			if (firstpearl == Integer.MAX_VALUE) // no pearl
				continue; // no imprisonment
			
			if (getConfig().getBoolean("prison_musthotbar") && firstpearl > 9) // bail if it must be in the hotbar
				continue; 
				
			if (pearlman.imprisonPlayer(uuid, damager)) // otherwise, try to imprison
				break;
		}
	}

	// Announce prison pearl events
	// Teleport players when freed
	@EventHandler(priority=EventPriority.MONITOR)
	public void onPrisonPearlEvent(PrisonPearlEvent event) {
		if (event.isCancelled())
			return;
		
		PrisonPearl pp = event.getPrisonPearl();
		UUID playerId = pp.getImprisonedId();
		String playerName = Bukkit.getOfflinePlayer(playerId).getName();
		Player player = Bukkit.getPlayer(playerId);
		if (event.getType() == PrisonPearlEvent.Type.NEW) {
			OfflinePlayer p = Bukkit.getOfflinePlayer(playerId);
			if (p.isOnline())
				updateAttachment(p.getPlayer());
			
			// Log the capturing PrisonPearl event.
			Player imprisoner = event.getImprisoner();
			String imprisonerLoc = serializeLocation(imprisoner.getLocation());
			String playerLoc = null;
			if (p.isOnline())
				serializeLocation(p.getPlayer().getLocation());
			String message = String.format("%s [%s] has bound %s [%s] to a PrisonPearl", 
											imprisoner.getDisplayName(), imprisonerLoc,
											playerName, playerLoc);
			log.info(message);
			
			imprisoner.sendMessage(ChatColor.GREEN+"You've bound " + playerName + ChatColor.GREEN+" to a prison pearl!");
			if (p.isOnline()) {
				p.getPlayer().sendMessage(ChatColor.RED+"You've been bound to a prison pearl owned by " + imprisoner.getDisplayName());
			}

			UUID[] alts = altsList.getAltsArray(playerId);
			checkBans(alts);
			
		} else if (event.getType() == PrisonPearlEvent.Type.DROPPED || event.getType() == PrisonPearlEvent.Type.HELD) {
			if (player != null) {
				String loc = pp.describeLocation();
				player.sendMessage(ChatColor.GREEN + "Your prison pearl is " + loc);
				broadcastman.broadcast(player, ChatColor.GREEN + playerName + ": " + loc);
			}
		} else if (event.getType() == PrisonPearlEvent.Type.FREED) {
			updateAttachment(player);
			if (player != null) {
				Location currentLoc = player.getLocation();
				if (!player.isDead() && currentLoc.getWorld() == getPrisonWorld()) {
					// if the player isn't dead and is in prison world
					Location loc = null;
					if (getConfig().getBoolean("free_tppearl")) // if we tp to pearl on players being freed
						loc = fuzzLocation(pp.getLocation()); // get the location of the pearl
					if (loc == null) // if we don't have a location yet
						loc = getRespawnLocation(player, currentLoc); // get the respawn location for the player

					if (loc == RESPAWN_PLAYER) { // if we're supposed to respawn the player
						player.setHealth(0.0); // kill him
					} else {
						player.teleport(loc); // otherwise teleport
					}
				}
				if (ppconfig.getPpsummonClearInventory()) {
					dropInventory(player, currentLoc, ppconfig.getPpsummonLeavePearls());
				}
			}
			UUID[] alts = altsList.getAltsArray(playerId);
			checkBans(alts);
			
			// Log the free'ing PrisonPearl event with coordinates.
			Player imprisoner = event.getPrisonPearl().getHolderPlayer();
			String playerLoc = player != null ? serializeLocation(player.getLocation()) : "[???]";
			String message = String.format("%s [%s] was freed", playerName, playerLoc);
			
			if (imprisoner != null) {
				String imprisonerLoc = serializeLocation(imprisoner.getLocation());
				message = String.format("%s [%s] was freed by %s [%s]", 
												playerName, playerLoc,
												imprisoner.getDisplayName(), imprisonerLoc);
			} 
			
			log.info(message);
			
			if (player != null) {
				player.sendMessage("You've been freed!");
				broadcastman.broadcast(player, playerName + " was freed!");
			}
		}
	}

    public void dropInventory(Player player, Location loc, boolean leavePearls) {
		if (loc == null) {
			loc = player.getLocation();
		}
		World world = loc.getWorld();
		Inventory inv = player.getInventory();
		int end = inv.getSize();
		for (int i = 0; i < end; ++i) {
			ItemStack item = inv.getItem(i);
			if (item == null) {
				continue;
			}
			if (leavePearls && item.getType().equals(Material.ENDER_PEARL)
					&& item.getDurability() == 0) {
				continue;
			}
			inv.clear(i);
			world.dropItemNaturally(loc, item);
		}
	}

	// Announce summon events
	// Teleport player when summoned or returned
	@SuppressWarnings("fallthrough")
	@EventHandler(priority=EventPriority.MONITOR)
	public void onSummonEvent(SummonEvent event) {
		if (event.isCancelled())
			return;
		
		PrisonPearl pp = event.getPrisonPearl();
		Player player = pp.getImprisonedPlayer();
		if (player == null)
			return;

		switch (event.getType()) {
		case SUMMONED:
			player.sendMessage(ChatColor.RED+"You've been summoned to your prison pearl!");
			if (ppconfig.getPpsummonClearInventory()) {
				Location oldLoc = player.getLocation();
				player.teleport(fuzzLocation(event.getLocation()));
				dropInventory(player, oldLoc, ppconfig.getPpsummonLeavePearls());
			} else {
				player.teleport(fuzzLocation(event.getLocation()));
			}
			break;

		case RETURNED:
			if (ppconfig.getPpreturnKills()) {
				player.setHealth(0.0);
				// Fall through to case KILLED
			} else {
				player.sendMessage(ChatColor.RED+"You've been returned to your prison");
				if (player.isInsideVehicle())
					player.eject();
				player.teleport(event.getLocation());
				break;
			}

		case KILLED:
			player.sendMessage(ChatColor.RED+"You've been struck down by your pearl!");
			break;
		}
	}
	
	private void updateAttachment(Player player) {
		if (player == null) {
			return;
		}
		PermissionAttachment attachment = attachments.get(player.getName());
		if (attachment == null) {
			attachment = player.addAttachment(this);
			attachments.put(player.getName(), attachment);
		}
		
		if (pearls.isImprisoned(player)) {
			for (String grant : getConfig().getStringList("prison_grant_perms"))
				attachment.setPermission(grant, true);
			for (String deny : getConfig().getStringList("prison_deny_perms"))
				attachment.setPermission(deny, false);			
		} else {
			for (String grant : getConfig().getStringList("prison_grant_perms"))
				attachment.unsetPermission(grant);
			for (String deny : getConfig().getStringList("prison_deny_perms"))
				attachment.unsetPermission(deny);		
		}
		
		player.recalculatePermissions();
	}
	
	
	public World getFreeWorld() {
		return Bukkit.getWorld(getConfig().getString("free_world"));
	}
	
	private World getPrisonWorld() {
		return Bukkit.getWorld(getConfig().getString("prison_world"));
	}
	
	// hill climbing algorithm which attempts to randomly spawn prisoners while actively avoiding pits
	// the obsidian pillars, or lava.
	private Location getPrisonSpawnLocation() {
		Random rand = new Random();
		Location loc = getPrisonWorld().getSpawnLocation(); // start at spawn
		for (int i=0; i<30; i++) { // for up to 30 iterations
			if (loc.getY() > 40 && loc.getY() < 70 && i > 5 && !isObstructed(loc)) // if the current candidate looks reasonable and we've iterated at least 5 times
				return loc; // we're done
			
			Location newloc = loc.clone().add(rand.nextGaussian()*(2*i), 0, rand.nextGaussian()*(2*i)); // pick a new location near the current one
			newloc = moveToGround(newloc);
			if (newloc == null)
				continue;
			
			if (newloc.getY() > loc.getY()+(int)(rand.nextGaussian()*3) || loc.getY() > 70) // if its better in a fuzzy sense, or if the current location is too high
				loc = newloc; // it becomes the new current location
		}

		return loc;
	}
	
	private Location moveToGround(Location loc) {
		Location ground = new Location(loc.getWorld(), loc.getX(), 100, loc.getZ());
		while (ground.getBlockY() >= 1) {
			if (!ground.getBlock().isEmpty())
				return ground;
			ground.add(0, -1, 0);
		}
		return null;
	}
	
    private boolean isObstructed(Location loc) {
		Location ground = new Location(loc.getWorld(), loc.getX(), 100, loc.getZ());
		while (ground.getBlockY() >= 1) {
			if (!ground.getBlock().isEmpty())
				break;
				
			ground.add(0, -1, 0);
		}
		
		for (int x=-2; x<=2; x++) {
			for (int y=-2; y<=2; y++) {
				for (int z=-2; z<=2; z++) {
					Location l = ground.clone().add(x, y, z);
					Material type = l.getBlock().getType();
					if (type == Material.LAVA || type == Material.STATIONARY_LAVA || type == Material.ENDER_PORTAL || type == Material.BEDROCK)
						return true;
				}
			}
		}
		
		return false;
	}
	
	private Location fuzzLocation(Location loc) {
		if (loc == null)
			return null;

		double rad = Math.random()*Math.PI*2;
		Location newloc = loc.clone();
		newloc.add(1.2*Math.cos(rad), 1.2*Math.sin(rad), 0);
		return newloc;
	}
	
	private void delayedTp(final Player player, final Location loc, final boolean dropInventory) {
		if (dropInventory) {
		}
		final boolean respawn = loc == RESPAWN_PLAYER;
		final Location oldLoc = player.getLocation();
		if (respawn) {
			player.setHealth(0.0);
		}
		Bukkit.getScheduler().callSyncMethod(this, new Callable<Void>() {
			public Void call() {
				if (!respawn) {
					player.teleport(loc);
				}
				if (dropInventory) {
					dropInventory(player, oldLoc, ppconfig.getPpsummonLeavePearls());
				}
				return null;
			}
		});
	}

    @EventHandler(priority=EventPriority.NORMAL)
    private boolean onPlayerChatEvent(AsyncPlayerChatEvent event) {
        if (summonman.isSummoned(event.getPlayer()) && !summonman.getSummon(event.getPlayer()).isCanSpeak()) {
           event.setCancelled(true);
        }

        return true;
    }

    @EventHandler(priority=EventPriority.NORMAL)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {

        if (!(event.getDamager() instanceof Player)) {
            return;
        }

        Player player = (Player)event.getDamager();

        if(summonman.isSummoned(player) && !summonman.getSummon(player).isCanDealDamage()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority=EventPriority.NORMAL)
    public void onBlockBreakEvent(BlockBreakEvent event) {

        Player player = event.getPlayer();

        if(summonman.isSummoned(player) && !summonman.getSummon(player).isCanBreakBlocks()) {
            event.setCancelled(true);
        }
    }
    
    public boolean isCombatTagged(String playerName) {
    	if (combatTagManager != null) {
    		return combatTagManager.isCombatTagged(playerName);
    	}
    	return false;
    }

    public CombatTagManager getCombatTagManager() {
        return combatTagManager;
    }

	public void loadAlts() {
		if (altsList == null) {
			altsList = new AltsList(this);
			Bukkit.getPluginManager().registerEvents(altsList, this);
		}
		if (!banManager_.hasBannedPlayers()) // shouldn't be too much of a bother
			altsList.queryForUpdatedAltLists(pearls.getAllUUIDSforPearls());
	}
	
	public void checkBanAllAlts() {
		if (altsList != null) {
			Integer bannedCount = 0, unbannedCount = 0, total = 0, result;
            for (UUID name : altsList.getAllNames()) {
                //log.info("checking "+name);
                result = checkBan(name);
                total++;
                if (result == 2) {
                    bannedCount++;
                } else if (result == 1) {
                    unbannedCount++;
                }
            }
			log.info("checked "+total+" accounts, banned "+bannedCount+" accounts, unbanned "+unbannedCount+" accounts");
		}
	}
	
	
	
	//gets the most recent time an alt account has logged out (returns 0 if there are none recorded)
	private Long getMostRecentAltLogout(String[] alts) {
		Long time = (long) 0;
		Long temp;
        for (String alt : alts) {
            if (lastLoggout.containsKey(alt)) {
                temp = lastLoggout.get(alt);
                if (temp > time) {
                    time = temp;
                }
            }
        }
		return time;
	}
	
	public void checkBanForAlts(UUID id){
		for (UUID alt: altsList.getAltsArray(id))
			checkBan(alt);
	}
	
	public int checkBan(UUID id) {
		//log.info("checking "+name);
		UUID[] alts = altsList.getAltsArray(id);
		Integer pearledCount = pearls.getImprisonedCount(alts);
		UUID[] imprisonedNames = pearls.getImprisonedIds(alts);
		String names = "";
		String name = "";
		if (isNameLayer)
			name = NameAPI.getCurrentName(id);
		else 
			name = Bukkit.getOfflinePlayer(id).getName();
		for (int i = 0; i < imprisonedNames.length; i++) {
			names = names + imprisonedNames[i];
			if (i < imprisonedNames.length-1) {
				names = names + ", ";
			}
		}
		if (pearledCount >= maxImprisonedAlts) {
			if (!pearls.isImprisoned(id)) {
				banAndKick(id, pearledCount, names);
				return 2;
			}
			int count = 0;
			for (UUID imprisonedName : imprisonedNames) {
				if (imprisonedName.compareTo(id) < 0) {
					count++;
				}
				if (count >= maxImprisonedAlts) {
					banAndKick(id, pearledCount, names);
					return 2;
				}
			}
		} else if (banManager_.isBanned(id)) {
			if (pearledCount <= 0) {
				log.info("pardoning "+name+" for having no imprisoned alts");
			} else {
				log.info("pardoning "+name+" who only has "+pearledCount+" imprisoned alts");
			}
			banManager_.pardon(id);
			return 1;
		}
		return 0;
	}
	
	private void banAndKick(UUID id, int pearledCount, String names) {
		Player p = this.getServer().getPlayer(id);
		if (p != null) {
			p.kickPlayer(kickMessage);
		}
		String name = NameAPI.getCurrentName(id);
		if (banManager_.isBanned(id)) {
			log.info(name+" still banned for having "+pearledCount+" imprisoned alts: "+names);
			return;
		}
		banManager_.ban(id);
		log.info("banning "+name+" for having "+pearledCount+" imprisoned alts: "+names);
	}
	
	private void checkBans(UUID[] ids) {
		Integer pearledCount;
		UUID[] imprisonedNames;
		UUID[] alts;
        for (UUID id : ids) {
        	String name = NameAPI.getCurrentName(id);
            log.info("checking " + name);
            alts = altsList.getAltsArray(id);
            imprisonedNames = pearls.getImprisonedIds(alts);
            String iNames = "";
            for (int j = 0; j < imprisonedNames.length; j++) {
            	String n = "";
            	if (isNameLayer)
            		n = NameAPI.getCurrentName(imprisonedNames[j]);
            	else
            		n = Bukkit.getOfflinePlayer(imprisonedNames[j]).getName();
                iNames = iNames + n;
                if (j < imprisonedNames.length - 1) {
                    iNames = iNames + ", ";
                }
            }
            pearledCount = pearls.getImprisonedCount(alts);
            if (pearledCount >= maxImprisonedAlts) {
                Player p = this.getServer().getPlayer(id);
                if (p != null) {
                    p.kickPlayer(kickMessage);
                }
                banManager_.ban(id);
                log.info("banning " + name + ", for having " + pearledCount + " imprisoned alts: " + iNames);
            } else if (banManager_.isBanned(id)) {
                banManager_.pardon(id);
                log.info("unbanning " + name + ", no longer has too many imprisoned alts.");
            }
        }
	}
	
	public boolean isTempBanned(UUID id) {
		return banManager_.isBanned(id);
	}
	
	public int getImprisonedCount(UUID id) {
		return pearls.getImprisonedCount(altsList.getAltsArray(id));
	}
	
	public String getImprisonedAltsString(UUID id) {
		String result = "";
		UUID[] alts = pearls.getImprisonedIds(altsList.getAltsArray(id));
		for (int i = 0; i < alts.length; i++) {
			result = result + "alts[i]";
			if (i < alts.length - 1) {
				result = result + ", ";
			}
		}
		return result;
	}
	
	public PPConfig getPPConfig() {
		return ppconfig;
	}

    public static void info(String msg) {
        log.info(msg);
    }
    
    public AltsList getAltsList() {
    	return altsList;
    }
    
    public void setAlts(UUID id, UUID[] confirmedAlts) throws IOException
    {
    	UUID[] alts = altsList.getAltsArray(id);
    	
    	if (alts.length == 0)
    	{
    		return;
    	}
    	File saveFile = new File(this.getDataFolder().getParentFile().getParentFile(), "text" + File.separator + "excluded_alts.txt");
		FileOutputStream fileOutputStream = new FileOutputStream(saveFile,true);
		BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(fileOutputStream));
    	for (UUID alt : alts)
    	{
    		if (!(arrayContains(alt, confirmedAlts)))
    		{
    			bufferedWriter.append(id + " " + alt);
    			bufferedWriter.append("\n");
    		}
    	}
		bufferedWriter.flush();
		fileOutputStream.close();
    }
    
    private boolean arrayContains(UUID checkValue, UUID[] array)
    {
    	for (UUID element : array)
    	{
    		if (checkValue.equals(element))
    		{
    			return true;
    		}
    	}
    	return false;
    }
    public static PrisonPearlManager getPrisonPearlManager(){
    	
    	return pearlman;
    }
    
    public BanManager getBanManager() {
        return banManager_;
    }
    
    public WorldBorderManager getWBManager() {
        return wbManager;
    }

    /**
     * Log a message to plugin {@link Logger}.
     * @param message - the message to log.
     */
    public static void log(String message) {
    	if (log != null) {
    		log.log(Level.INFO, message);
    	}
    }
    
    /**
     * @param location - the location to serialize into user-friendly text.
     * @return the serialized user-friendly string representing location.
     */
    private static String serializeLocation(Location location) {
    	return String.format("%s, (%d, %d, %d)", location.getWorld().getName(),
    							location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }
    
    public boolean isWorldBorder(){
    	return isWorldBorder;
    }
    
    public boolean isMercuryLoaded(){
    	return isMercury;
    }
    
    public boolean isNameLayerLoaded(){
    	return isNameLayer;
    }
    
    public boolean isBetterShardsEnabled(){
    	return isBetterShards;
    }
    
    public PrisonPearlMysqlStorage getMysqlStorage(){
    	if (mysqlStorage == null && ppconfig.getMysqlEnabled())
    		mysqlStorage = new PrisonPearlMysqlStorage(this);
    	return mysqlStorage;
    }
    
    private void updateAllPearlLocations(){
    	List<UUID> uuids = pearls.getAllUUIDSforPearls();
    	for (UUID uuid: uuids){
    		PrisonPearl pp = pearls.getByImprisoned(uuid);
    		Location loc = pp.getLocation();
    		String message = uuid.toString() + " " + loc.getWorld().getName() + " " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ() + " " +
    				pp.getUniqueIdentifier() + " " + pp.getMotd();
    		MercuryAPI.instance.sendMessage(message, "PrisonPearlMove");
    	}
    }
}
