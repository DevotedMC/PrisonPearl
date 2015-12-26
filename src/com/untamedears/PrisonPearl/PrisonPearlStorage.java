package com.untamedears.PrisonPearl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.BrewingStand;
import org.bukkit.block.Chest;
import org.bukkit.block.Dispenser;
import org.bukkit.block.Furnace;
import org.bukkit.configuration.Configuration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import vg.civcraft.mc.namelayer.NameAPI;

import com.untamedears.PrisonPearl.database.PrisonPearlMysqlStorage;
import com.untamedears.PrisonPearl.managers.PrisonPearlManager;

import vg.civcraft.mc.namelayer.NameAPI;

//import com.untamedears.EnderExpansion.Enderplugin;

public class PrisonPearlStorage implements SaveLoad {
	private static PrisonPearlPlugin plugin;
	private final Map<UUID, PrisonPearl> pearls_byimprisoned;
	
	public static List<UUID> transferedPlayers;
	
	private long lastFeed = 0;
	
	private boolean isNameLayer;
	private boolean isMysql;
	private boolean dirty;
	private PrisonPearlMysqlStorage PrisonPearlMysqlStorage;
	
	public PrisonPearlStorage(PrisonPearlPlugin plugin) {
		isNameLayer = Bukkit.getPluginManager().isPluginEnabled("NameLayer");
		isMysql = plugin.getPPConfig().getMysqlEnabled();
		PrisonPearlMysqlStorage = plugin.getMysqlStorage();
		transferedPlayers = new ArrayList<UUID>();
		this.plugin = plugin;
		pearls_byimprisoned = new HashMap<UUID, PrisonPearl>();
	}
	
	public boolean isDirty() {
		return dirty;
	}
	
	public void markDirty() {
		dirty = true;
	}

//    public String normalizeName(String name) {
//        return name.toLowerCase();
//    }
	
	public List<UUID> getAllUUIDSforPearls(){
		List<UUID> uuids = new ArrayList<UUID>();
		for (UUID uuid: pearls_byimprisoned.keySet()){
			uuids.add(uuid);
		}
		return uuids;
	}

	public void load(File file) throws IOException {
		if (isMysql){
			loadMysql();
			return;
		}
		
		FileInputStream fis = new FileInputStream(file);
		BufferedReader br = new BufferedReader(new InputStreamReader(fis));
		
		String line;
		while ((line = br.readLine()) != null) {
			if(line.matches("lastFeed:([0-9]+)")) {
				lastFeed = Long.parseLong(line.split(":")[1]);
				continue;
			}
			String parts[] = line.split(" ");
			if (parts.length <= 1)
				continue;
			UUID imprisoned = UUID.fromString(parts[0]);
			Location loc = new Location(Bukkit.getWorld(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]), Integer.parseInt(parts[4]));
			String name = "";
			if (isNameLayer)
				name = NameAPI.getCurrentName(imprisoned);
			else
				name = Bukkit.getOfflinePlayer(imprisoned).getName();
			int unique = Integer.parseInt(parts[5]);
			PrisonPearl pp = PrisonPearl.makeFromLocation(name, imprisoned, loc, unique);
			if (parts.length > 6) {
				String motd = "";
				for (int i = 6; i < parts.length; i++) {
					motd = motd.concat(parts[i] + " ");
				}
				if (pp != null)
					pp.setMotd(motd);
			}
			if (pp == null) {
				System.err.println("PrisonPearl for " + imprisoned + " didn't validate, so is now set free. Chunks and/or prisonpearls.txt are corrupt");
				continue;
			}
			
			addPearl(pp);
		}
		
		fis.close();
		
		dirty = false;
	}
	
	public void save(File file) throws IOException {
		if (isMysql){
			saveMysql();
			return;
		}
		
		FileOutputStream fos = new FileOutputStream(file);
		BufferedWriter br = new BufferedWriter(new OutputStreamWriter(fos));
		
		for (PrisonPearl pp : pearls_byimprisoned.values()) {
			if (pp.getHolderBlockState() == null)
				continue;
			
			Location loc = pp.getLocation();
			br.append(pp.getImprisonedId().toString());
			br.append(" ");
			br.append(loc.getWorld().getName());
			br.append(" ");
			br.append(String.valueOf(loc.getBlockX()));
			br.append(" ");
			br.append(String.valueOf(loc.getBlockY()));
			br.append(" ");
			br.append(String.valueOf(loc.getBlockZ()));
			br.append(" ");
			br.append(String.valueOf(pp.getUniqueIdentifier()));
			br.append(" ");
			br.append(pp.getMotd());
			br.append("\n");
		}
		
		br.write("lastFeed:" + lastFeed);
		br.flush();
		fos.close();
		
		dirty = false;
	}
	
	public PrisonPearl newPearl(OfflinePlayer imprisoned, Player imprisoner) {
		return newPearl(imprisoned.getName(), imprisoned.getUniqueId(), imprisoner);
	}
	
	public PrisonPearl newPearl(String imprisonedName, UUID imprisonedId, Player imprisoner) {
		Random rand = new Random();
		PrisonPearl pp = new PrisonPearl(imprisonedName, imprisonedId, imprisoner, rand.nextInt(1000));
		addPearl(pp);
		return pp;
	}

	private final int HolderStateToInventory_SUCCESS = 0;
	private final int HolderStateToInventory_BADPARAM = 1;
	private final int HolderStateToInventory_NULLSTATE = 2;
	private final int HolderStateToInventory_BADCONTAINER = 3;
	private final int HolderStateToInventory_NULLINV = 4;

	private int HolderStateToInventory(PrisonPearl pp, Inventory inv[]) {
		if (pp == null || inv == null) {
			return HolderStateToInventory_BADPARAM;
		}
		BlockState inherentViolence = pp.getHolderBlockState();
//		if (Bukkit.getPluginManager().isPluginEnabled("EnderExpansion")){
//			if (pp.getLocation().getBlock().getType() == Material.ENDER_CHEST){
//				inv[0] = Enderplugin.getchestInventory(pp.getLocation());
//				return HolderStateToInventory_SUCCESS;
//			}
//		}
		if (inherentViolence == null) {
			return HolderStateToInventory_NULLSTATE;
		}
		Material mat = inherentViolence.getType();
		
		switch(mat) {
		case FURNACE:
			inv[0] = ((Furnace)inherentViolence).getInventory();
			break;
		case DISPENSER:
			inv[0] = ((Dispenser)inherentViolence).getInventory();
			break;
		case BREWING_STAND:
			inv[0] = ((BrewingStand)inherentViolence).getInventory();
			break;
		case CHEST:
		case TRAPPED_CHEST:
			Chest c = ((Chest)inherentViolence);
			DoubleChestInventory dblInv = null;
			try {
				dblInv = (DoubleChestInventory)c.getInventory();
				inv[0] = dblInv.getLeftSide();
				inv[1] = dblInv.getRightSide();
			} catch(Exception e){
				inv[0] = c.getInventory();
			}
			break;
		default:
			return HolderStateToInventory_BADCONTAINER;
		}
		if (inv[0] == null && inv[1] == null) {
			return HolderStateToInventory_NULLINV;
		}
		return HolderStateToInventory_SUCCESS;
	}

	public void removePearlFromContainer(PrisonPearl pp) {
		Inventory inv[] = new Inventory[2];
		if (HolderStateToInventory(pp, inv) != HolderStateToInventory_SUCCESS) {
			return;
		}
		Inventory real_inv = null;
		int pearlslot = -1;
		for (int inv_idx = 0; inv_idx <= 1 && pearlslot == -1; ++inv_idx) {
			 if (inv[inv_idx] == null) {
				 continue;
			 }
			 HashMap<Integer, ? extends ItemStack> inv_contents = inv[inv_idx].all(Material.ENDER_PEARL);
			 for (int inv_slot : inv_contents.keySet()) {
				 ItemStack slot_item = inv_contents.get(inv_slot);
				 if (PrisonPearlPlugin.getPrisonPearlManager().isItemStackPrisonPearl(pp, slot_item)) {
					 real_inv = inv[inv_idx];
					 pearlslot = inv_slot;
					 break;
				 }
			 }
		}
		if (real_inv == null || pearlslot == -1) {
			return;
		}
		real_inv.setItem(pearlslot, new ItemStack(Material.ENDER_PEARL));
	}
	
	// We need to remove the pearl from list without doing anything else.
	public void deletePearlMercuryCase(PrisonPearl pp) {
		pearls_byimprisoned.remove(pp.getImprisonedId());
	}
	
	public void deletePearl(PrisonPearl pp, String reason) {
		removePearlFromContainer(pp);
		pearls_byimprisoned.remove(pp.getImprisonedId());
		dirty = true;
		plugin.getLogger().info(reason);
		if (isMysql)
			PrisonPearlMysqlStorage.deletePearl(pp);
	}
	
	public void addPearl(PrisonPearl pp) {
		
		pearls_byimprisoned.put(pp.getImprisonedId(), pp);
		if (isMysql){
			if (PrisonPearlMysqlStorage.getPearl(pp.getImprisonedId()) != null)
				return;
			PrisonPearlMysqlStorage.addPearl(pp);
		}
		dirty = true;
	}
	
	public PrisonPearl getByItemStack(ItemStack item) {
		if (item == null || item.getType() != Material.ENDER_PEARL || item.getDurability() != 0)
			return null;
		else
			return getPearlbyItemStack(item);
	}
	
	public PrisonPearl getByImprisoned(UUID id) {
		return pearls_byimprisoned.get(id);
	}
	
	public PrisonPearl getByImprisoned(Player player) {
		return pearls_byimprisoned.get(player.getUniqueId());
	}
	
	/**
	 * @param itemStack - the item stack to check for being a PrisonPearl.
	 * @return true, if itemStack is a PrisonPearl, false otherwise.
	 */
	public boolean isPrisonPearl(ItemStack itemStack) {
		return getByItemStack(itemStack) != null;
	}
	
	public Integer getPearlCount(){
		return pearls_byimprisoned.size();
	}
	
	public boolean isImprisoned(UUID id) {
		return pearls_byimprisoned.containsKey(id);
	}
	
	public boolean isImprisoned(Player player) {
		return pearls_byimprisoned.containsKey(player.getUniqueId());
	}
	
	public Integer getImprisonedCount(UUID[] ids) {
		Integer count = 0;
		for (UUID id : ids) {
			if (pearls_byimprisoned.containsKey(id)) {
				count++;
			}
		}
		return count;
	}
	
	public UUID[] getImprisonedIds(UUID[] ids) {
		List<UUID> imdIds = new ArrayList<UUID>();
		for (UUID id : ids) {
			if (pearls_byimprisoned.containsKey(id)) {
				imdIds.add(id);
			}
		}
		int count = imdIds.size();
		UUID[] results = new UUID[count];
		for (int i = 0; i < count; i++) {
			results[i] = imdIds.get(i);
		}
		return results;
	}

	public boolean upgradePearl(Inventory inv, PrisonPearl pp) {
		final UUID prisonerId = pp.getImprisonedId();
		final String prisoner;
		if (plugin.isNameLayerLoaded())
			prisoner = NameAPI.getCurrentName(pp.getImprisonedId());
		else
			prisoner = Bukkit.getOfflinePlayer(prisonerId).getName();
		ItemStack is = new ItemStack(Material.ENDER_PEARL, 1);
		if (inv == null) {
			return false;
		}
		for (ItemStack existing_is: inv.getContents()) {
			if (existing_is == null || existing_is.getType() != Material.ENDER_PEARL)
				continue;
			int pearlslot = inv.first(existing_is);
			if (existing_is != null) {
				existing_is.setDurability((short) 0);
				ItemMeta existing_meta = existing_is.getItemMeta();
				if (existing_meta != null) {
					String existing_name = existing_meta.getDisplayName();
					List<String> lore = existing_meta.getLore();
					if (existing_name != null && prisoner != null &&
							existing_name.compareTo(prisoner) == 0 && lore != null && lore.size() == 3) {
						// This check says all existing stuff is there so return true.
						return true;
					}
					else if (existing_name != null && 
							prisoner != null && existing_name.compareTo(prisoner) != 0) 
						// If we don't have the right pearl keep looking.
						continue;
					else if (existing_name == null)
						// This pearl can't even be right so just return.
						return true;
				}
			}
			ItemMeta im = is.getItemMeta(); 
			// Rename pearl to that of imprisoned player 
			im.setDisplayName(prisoner);
			List<String> lore = new ArrayList<String>(); 
			lore.add(prisoner + " is held within this pearl");
			lore.add("UUID: " + pp.getImprisonedId().toString());
			lore.add("Unique: " + pp.getUniqueIdentifier());
			// Given enchantment effect
			// Durability used because it doesn't affect pearl behaviour
			im.addEnchant(Enchantment.DURABILITY, 1, true);
			im.setLore(lore);
			is.setItemMeta(im);
			inv.clear(pearlslot);
			inv.setItem(pearlslot, is);
			return true;
		}
		return false;
	}

	public String feedPearls(PrisonPearlManager pearlman){
		String message = "";
		String log = "";
		ConcurrentHashMap<UUID,PrisonPearl> map = new ConcurrentHashMap<UUID,PrisonPearl>(pearls_byimprisoned);

		long inactive_seconds = this.getConfig().getLong("ignore_feed.seconds", 0);
		long inactive_hours = this.getConfig().getLong("ignore_feed.hours", 0);
		long inactive_days = this.getConfig().getLong("ignore_feed.days", 0);

		long feedDelay = getConfig().getLong("feed_delay", 72000000);	//if pearls have been fed in the last x millis it wont feed, defaults to 20 hours
		if(lastFeed >= System.currentTimeMillis() - feedDelay) {
			return "Pearls have already been fed, not gonna do it again just yet";
		} else {
			log+="\nSetting last feed time";
			lastFeed = System.currentTimeMillis();
		}
		
		int pearlsfed = 0;
		int coalfed = 0;
		int freedpearls = 0;
		for (PrisonPearl pp : map.values()) {
			final UUID prisonerId = pp.getImprisonedId();
			fixAllPearlMissing(prisonerId);
			//final String prisoner = Bukkit.getPlayer(prisonerId).getName();
			Inventory inv[] = new Inventory[2];
			int retval = HolderStateToInventory(pp, inv);
			Location loc = pp.getLocation();
			if (loc instanceof FakeLocation) { // Not on server
				continue; // Other server will handle feeding
			}
			if (!upgradePearl(inv[0], pp) && inv[1] != null) {
				upgradePearl(inv[1], pp);
			}
			if (retval == HolderStateToInventory_BADCONTAINER) {
				String reason = prisonerId + " is being freed. Reason: Freed during coal feed, container was corrupt.";
				pearlman.freePearl(pp, reason);
				log+="\n freed:"+prisonerId+",reason:"+"badcontainer";
				freedpearls++;
				continue;
			} else if (retval != HolderStateToInventory_SUCCESS) {
				continue;
			}
			else if (plugin.getWBManager().isMaxFeed(loc)){
				String reason = prisonerId + " is being freed. Reason: Freed during coal feed, was outside max distance.";
				pearlman.freePearl(pp, reason);
				log+="\n freed:"+prisonerId+",reason:"+"maxDistance";
				freedpearls++;
				continue;
			}
			if (inactive_seconds != 0 || inactive_hours != 0 || inactive_days != 0) {
				long inactive_time = pp.getImprisonedOfflinePlayer().getLastPlayed();
				long inactive_millis = inactive_seconds * 1000 + inactive_hours * 3600000 + inactive_days * 86400000;
				inactive_time += inactive_millis;
				if (inactive_time <= System.currentTimeMillis()) {
					// if player has not logged on in the set amount of time than ignore feeding
					log += "\nnot fed inactive: " + prisonerId;
					continue;
				}
			}
			message = message + "Pearl Id: " + prisonerId + " in a " + pp.getHolderBlockState().getType();
			ItemStack requirement = plugin.getPPConfig().getUpkeepResource();
			int requirementSize = requirement.getAmount();

			if(inv[0].containsAtLeast(requirement,requirementSize)) {
				int pearlnum;
				pearlnum = inv.length;
				message = message + "\n Chest contains enough purestrain coal.";
				inv[0].removeItem(requirement);
				pearlsfed++;
				coalfed += requirementSize;
				log+="\n fed:" + prisonerId + ",location:"+ pp.describeLocation();
			} else if(inv[1] != null && inv[1].containsAtLeast(requirement,requirementSize)){
				message = message + "\n Chest contains enough purestrain coal.";
				inv[1].removeItem(requirement);
				pearlsfed++;
				coalfed += requirementSize;
				log+="\n fed:" + prisonerId + ",location:"+ pp.describeLocation();
			} else {
				message = message + "\n Chest does not contain enough purestrain coal.";
				String reason = prisonerId + " is being freed. Reason: Freed during coal feed, container did not have enough coal.";
				pearlman.freePearl(pp, reason);
				log+="\n freed:"+prisonerId+",reason:"+"nocoal"+",location:"+pp.describeLocation();
				freedpearls++;
			}
		}
		message = message + "\n Feeding Complete. " + pearlsfed + " were fed " + coalfed + " coal. " + freedpearls + " players were freed.";
		return message;
	}
	
	public String restorePearls(PrisonPearlManager pearlman, String config){
		//Read pearl config
		
		//For each entry
		
		//Create pearl for player
		
		//Place in chest
		
		//Check imprisonment status
		
		//Report restoration
		return "";
	}
	private Configuration getConfig() {
		return plugin.getConfig();
	}
	
	public void loadMysql(){
    	List<PrisonPearl> pearls = PrisonPearlMysqlStorage.getAllPearls();
    	for (PrisonPearl pearl: pearls){
    		pearls_byimprisoned.put(pearl.getImprisonedId(), pearl);
    	}
    	lastFeed = PrisonPearlMysqlStorage.getLastRestart();
    }
    
    public void saveMysql(){
    	for (PrisonPearl pp: pearls_byimprisoned.values()){
    		if (pp.getLocation() instanceof FakeLocation)
    			continue;
    		PrisonPearlMysqlStorage.updatePearl(pp);
    	}
    	PrisonPearlMysqlStorage.updateLastRestart(lastFeed);
    }
    
    public static void playerIsTransfering(final UUID uuid){
    	transferedPlayers.add(uuid);
    	Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable(){

			@Override
			public void run() {
				// After a certain amount of time remove the player because they have left.
				transferedPlayers.remove(uuid);
			}
    		
    	}, 20); // wait 20 ticks
    }
    
    public PrisonPearl getPearlbyItemStack(ItemStack stack) {
    	if (stack == null || !stack.hasItemMeta() || stack.getType() != Material.ENDER_PEARL)
    		return null;
    	if (!stack.getItemMeta().hasLore())
    		return null;
    	List<String> lore = stack.getItemMeta().getLore();
    	if (lore.size() != 3)
    		return null;
    	UUID uuid = UUID.fromString(lore.get(1).split(" ")[1]);
    	PrisonPearl pearl = pearls_byimprisoned.get(uuid);
    	if (pearl == null)
    		return null;
    	int id = Integer.parseInt(lore.get(2).split(" ")[1]);
    	if (pearl.getUniqueIdentifier() != id)
    		return null;
    	return pearl;
    }
    
    private void fixAllPearlMissing(UUID uuid) {
    	if (!plugin.getPPConfig().getShouldFixMissingPearls())
    		return;
    	PrisonPearl pp = pearls_byimprisoned.get(uuid);
    	Location loc = pp.getLocation();
    	Block b = pp.getLocation().getBlock();
    	Inventory[] inv = new Inventory[2];
    	BlockState inherentViolence = pp.getHolderBlockState();
    	// Grabs the inventories.
    	switch(b.getType()) {
		case FURNACE:
			inv[0] = ((Furnace)inherentViolence).getInventory();
			break;
		case DISPENSER:
			inv[0] = ((Dispenser)inherentViolence).getInventory();
			break;
		case BREWING_STAND:
			inv[0] = ((BrewingStand)inherentViolence).getInventory();
			break;
		case CHEST:
		case TRAPPED_CHEST:
			Chest c = ((Chest)inherentViolence);
			DoubleChestInventory dblInv = null;
			try {
				dblInv = (DoubleChestInventory)c.getInventory();
				inv[0] = dblInv.getLeftSide();
				inv[1] = dblInv.getRightSide();
			} catch(Exception e){
				inv[0] = c.getInventory();
			}
			break;
		default:
			inv[0] = null;
			inv[1] = null;
		}
		ItemStack stack = null;
		// Scans the inventories looking for the prisonpearl.
    	for (Inventory i: inv) {
    		if (i == null)
    			continue;
    		for (int x = 0; x < i.getSize(); x++) {
    			ItemStack s = i.getItem(x);
    			if (s == null || s.getType() != Material.ENDER_PEARL)
    				continue;
    			PrisonPearl tmp = getPearlbyItemStack(s);
    			if (tmp == null)
    				continue;
    			if (tmp.getImprisonedId().equals(uuid)) {
    				stack = s;
    				break;
    			}
    		}
    		if (stack != null)
    			break;
    	}
    	if (stack == null)
    		for (Inventory i: inv) {
            	if (stack != null)
        			break;
        		for (int x = 0; x < i.getSize(); x++) {
        			ItemStack current = i.getItem(x);
        			if (getPearlbyItemStack(current) == null) {
        				deletePearl(pp, "Regenerating pearl cause it was lost. UUID is: " + pp.getImprisonedId().toString());
        				String name = "";
        				if (isNameLayer){
        					name = NameAPI.getCurrentName(uuid);
        				} else {
        			        name = Bukkit.getOfflinePlayer(uuid).getName();
        				}
        				pp = new PrisonPearl(name, uuid, loc, new Random().nextInt(1000));
        				addPearl(pp);
        				ItemStack is = new ItemStack(Material.ENDER_PEARL, 1);
        				ItemMeta im = is.getItemMeta();
        				// Rename pearl to that of imprisoned player
        				im.setDisplayName(name);
        				List<String> lore = new ArrayList<String>();
        				// Gives pearl lore that says more info when hovered over
        				lore.add(name + " is held within this pearl");
        				lore.add("UUID: "+pp.getImprisonedId());
        				lore.add("Unique: " + pp.getUniqueIdentifier());
        				// Given enchantment effect (durability used because it doesn't affect pearl behaviour)
        				im.addEnchant(Enchantment.DURABILITY, 1, true);
        				im.setLore(lore);
        				is.setItemMeta(im);
        				i.clear(x);
        				i.setItem(x, is);
        				stack = is;
        				break;
        			}
        		}
    		}
    }
}