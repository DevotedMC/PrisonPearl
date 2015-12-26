package com.untamedears.PrisonPearl.managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.MinecraftServer;
import net.minecraft.server.v1_8_R3.PlayerInteractManager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.BrewingStand;
import org.bukkit.block.Chest;
import org.bukkit.block.Dispenser;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.Furnace;
import org.bukkit.configuration.Configuration;
import org.bukkit.craftbukkit.v1_8_R3.CraftServer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import vg.civcraft.mc.bettershards.events.PlayerChangeServerEvent;
import vg.civcraft.mc.mercury.MercuryAPI;
import vg.civcraft.mc.namelayer.NameAPI;

import com.mojang.authlib.GameProfile;
import com.untamedears.PrisonPearl.EnderExpansion;
import com.untamedears.PrisonPearl.PrisonPearl;
import com.untamedears.PrisonPearl.PrisonPearlPlugin;
import com.untamedears.PrisonPearl.PrisonPearlStorage;
import com.untamedears.PrisonPearl.events.PrisonCommandEvent;
import com.untamedears.PrisonPearl.events.PrisonPearlEvent;

public class PrisonPearlManager implements Listener {
	private final PrisonPearlPlugin plugin;
	private final PrisonPearlStorage pearls;
	private EnderExpansion ee;
	private boolean isNameLayer;
	private boolean isMercury;

	public PrisonPearlManager(PrisonPearlPlugin plugin, PrisonPearlStorage pearls, EnderExpansion ee) {
		this.plugin = plugin;
		this.pearls = pearls;
		this.ee = ee;
		isNameLayer = Bukkit.getPluginManager().isPluginEnabled("NameLayer");
		isMercury = plugin.isMercuryLoaded();
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	public boolean imprisonPlayer(Player imprisoned, Player imprisoner) {
		return imprisonPlayer(imprisoned.getUniqueId(), imprisoner);
	}
	
	/**
	 * @param imprisonedname
	 * @param imprisoner
	 * @return
	 */
	public boolean imprisonPlayer(UUID imprisonedId, Player imprisoner) {
		World respawnworld = Bukkit.getWorld(getConfig().getString("free_world"));
		
		plugin.getAltsList().cacheAltListFor(imprisonedId);
		
		// set up the imprisoner's inventory
		Inventory inv = imprisoner.getInventory();
		ItemStack stack = null;
		int stacknum = -1;

		// scan for the smallest stack of normal ender pearls
		for (Entry<Integer, ? extends ItemStack> entry :
				inv.all(Material.ENDER_PEARL).entrySet()) {
			ItemStack newstack = entry.getValue();
			int newstacknum = entry.getKey();
			if (!newstack.hasItemMeta()) {
				if (stack != null) {
					// don't keep a stack bigger than the previous one
					if (newstack.getAmount() > stack.getAmount()) {
						continue;
					}
					// don't keep an identical sized stack in a higher slot
					if (newstack.getAmount() == stack.getAmount() &&
							newstacknum > stacknum) {
						continue;
					}
				}

				stack = newstack;
				stacknum = entry.getKey();
			}
		}

		int pearlnum;
		ItemStack dropStack = null;
		if (stacknum == -1) { // no pearl (admin command)
			// give him a new one at the first empty slot
			pearlnum = inv.firstEmpty();
		} else if (stack.getAmount() == 1) { // if he's just got one pearl
			pearlnum = stacknum; // put the prison pearl there
		} else {
			// otherwise, put the prison pearl in the first empty slot
			pearlnum = inv.firstEmpty();
			if (pearlnum > 0) {
				// and reduce his stack of pearls by one
				stack.setAmount(stack.getAmount() - 1);
				inv.setItem(stacknum, stack);
			} else { // no empty slot?
				inv.clear(stacknum); // clear before drop
				dropStack = new ItemStack(Material.ENDER_PEARL, stack.getAmount() - 1);
				pearlnum = stacknum; // then overwrite his stack of pearls
			}
		}

		// drop pearls that otherwise would be deleted
		if (dropStack != null) {
			imprisoner.getWorld().dropItem(imprisoner.getLocation(), dropStack);
			Bukkit.getLogger().info(
				imprisoner.getLocation() + ", " + dropStack.getAmount());
		}

		if (!imprisoner.hasPermission("prisonpearl.normal.pearlplayer")) {
			return false;
		}

		// create the prison pearl
		String name = "";
		if (isNameLayer){
			name = NameAPI.getCurrentName(imprisonedId);
		} else {
	        name = Bukkit.getOfflinePlayer(imprisonedId).getName();
		}
		
		PrisonPearl pp = pearls.newPearl(name, imprisonedId, imprisoner);
		// set off an event
		if (!prisonPearlEvent(pp, PrisonPearlEvent.Type.NEW, imprisoner)) {
			pearls.deletePearl(pp, "Something canceled the creation of the pearl for player " + pp.getImprisonedName() +
					" uuid: " + pp.getImprisonedId());
			return false;
		}
		updateLocationToMercury(pp, PrisonPearlEvent.Type.NEW);
		pp.markMove();

		// Create the inventory item
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
		// Give it to the imprisoner
		inv.setItem(pearlnum, is);
		// Reason for edit: Gives pearl enchantment effect (distinguishable, unstackable) Gives name of prisoner in inventory.


        //check if imprisoned was imprisoned by themselves
		if(imprisoner.getUniqueId() == pp.getImprisonedId()){
			//ok player imprisoned themselves throw the pearl on the floor
			imprisoner.getWorld().dropItem(imprisoner.getLocation(), is);
			Bukkit.getLogger().info("Player attempted to imprison themself. Pearl was dropped");
		}
		
		if (getConfig().getBoolean("prison_resetbed")) {
			Player imprisoned = Bukkit.getPlayer(imprisonedId);
			// clear out the players bed
			if (imprisoned != null) {
				imprisoned.setBedSpawnLocation(respawnworld.getSpawnLocation());
			}
		}
		
		return true;
	}

	public boolean freePlayer(Player player, String reason) {
		PrisonPearl pp = pearls.getByImprisoned(player);
		return pp != null && freePearl(pp, reason);
	}

	public boolean freePearl(PrisonPearl pp, String reason) {
		// set off an event
		if (!prisonPearlEvent(pp, PrisonPearlEvent.Type.FREED)) {
			return false;
		}
		pearls.deletePearl(pp, reason); // delete the pearl first
		// unban alts and players if they are allowed to be
		plugin.checkBan(pp.getImprisonedId());
		plugin.checkBanForAlts(pp.getImprisonedId());
		updateLocationToMercury(pp, PrisonPearlEvent.Type.FREED);
		return true;
	}
	
	public boolean freePearlFromMercury(PrisonPearl pp, String reason) {
		pearls.deletePearlMercuryCase(pp);
		// set off an event
		prisonPearlEvent(pp, PrisonPearlEvent.Type.FREED);
		// unban alts and players if they are allowed to be
		plugin.checkBan(pp.getImprisonedId());
		plugin.checkBanForAlts(pp.getImprisonedId());
		return true;
	}

	// Announce the person in a pearl when a player holds it
	@EventHandler(priority = EventPriority.MONITOR)
	public void onItemHeldChange(PlayerItemHeldEvent event) {

		Inventory inv = event.getPlayer().getInventory();
		ItemStack item = inv.getItem(event.getNewSlot());
		ItemStack newitem = announcePearl(event.getPlayer(), item);
		if (newitem != null)
			inv.setItem(event.getNewSlot(), newitem);
	}

	private ItemStack announcePearl(Player player, ItemStack item) {
		if (item == null)
			return null;

		if (item.getType() == Material.ENDER_PEARL && item.getDurability() != 0) {
			PrisonPearl pp = pearls.getPearlbyItemStack(item);

			if (pp == null) {
				return new ItemStack(Material.ENDER_PEARL, 1);
			}
			pp.markMove();
		}

		return null;
	}

	// Free pearls when right clicked
	@EventHandler(priority = EventPriority.LOW)
	public void onPlayerInteract(PlayerInteractEvent event) {

		PrisonPearl pp = pearls.getByItemStack(event.getItem());
		if (pp == null)
			return;
		pp.markMove();

		if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			Material m = event.getClickedBlock().getType();
			if (m == Material.CHEST || m == Material.WORKBENCH
					|| m == Material.FURNACE || m == Material.DISPENSER
					|| m == Material.BREWING_STAND)
				return;
		} else if (event.getAction() != Action.RIGHT_CLICK_AIR) {
			return;
		}

		Player player = event.getPlayer();
		player.getInventory().setItemInHand(null);
		event.setCancelled(true);

		freePearl(pp, pp.getImprisonedName() + "("+pp.getImprisonedId() + ") is being freed. Reason: " 
		+ player.getDisplayName() + " threw the pearl.");
		player.sendMessage("You've freed " + pp.getImprisonedName());
	}

	// Called from CombatTagListener.onNpcDespawn
	public void handleNpcDespawn(UUID plruuid, Location loc) {
		World world = loc.getWorld();
		Player player = plugin.getServer().getPlayer(plruuid);
		if (player == null) { // If player is offline
			MinecraftServer server = ((CraftServer)plugin.getServer()).getServer();
			GameProfile prof = new GameProfile(plruuid, null);
			EntityPlayer entity = new EntityPlayer(
				server, server.getWorldServer(0), prof,
				new PlayerInteractManager(server.getWorldServer(0)));
			player = (entity == null) ? null : (Player) entity.getBukkitEntity();
			if (player == null) {
				return;
			}
			player.loadData();
		}

		Inventory inv = player.getInventory();
		int end = inv.getSize();
		for (int slot = 0; slot < end; ++slot) {
			ItemStack item = inv.getItem(slot);
			if (item == null) {
				continue;
			}
			if (!item.getType().equals(Material.ENDER_PEARL)) {
			   continue;
			}
			PrisonPearl pp = pearls.getByItemStack(item);
			if (pp==null){
				continue;
			}
			inv.clear(slot);
			world.dropItemNaturally(loc, item);  // drops pearl to ground.
		}
		player.saveData();
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void playerChangeServerEvent(PlayerChangeServerEvent event) {
		Inventory inv = Bukkit.getPlayer(event.getPlayerUUID()).getInventory();
		for (Entry<Integer, ? extends ItemStack> entry :
				inv.all(Material.ENDER_PEARL).entrySet()) {
			ItemStack item = entry.getValue();
			PrisonPearl pp = pearls.getByItemStack(item);
			if (pp == null) {
				continue;
			}
			updateTransferToMercury(event.getPlayerUUID(), pp.getImprisonedId());
		}
		PrisonPearlStorage.playerIsTransfering(event.getPlayerUUID());
	}

	// Drops a pearl when a player leaves.
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerQuit(PlayerQuitEvent event) {
		Player imprisoner = event.getPlayer();
		CombatTagManager ctm = plugin.getCombatTagManager();
		if (ctm.isCombatTagged(imprisoner) || 
				PrisonPearlStorage.transferedPlayers.contains(imprisoner.getUniqueId())) { 
			// if player is tagged or if player is transfered to another server.
			return;
		}
		Location loc = imprisoner.getLocation();
		World world = imprisoner.getWorld();
		Inventory inv = imprisoner.getInventory();
		for (Entry<Integer, ? extends ItemStack> entry :
				inv.all(Material.ENDER_PEARL).entrySet()) {
			ItemStack item = entry.getValue();
			PrisonPearl pp = pearls.getByItemStack(item);
			if (pp == null) {
				continue;
			}
			pp.markMove();
			int slot = entry.getKey();
			inv.clear(slot);
			world.dropItemNaturally(loc, item);
		}
		imprisoner.saveData();
	}

	// Free the pearl if it despawns
	@EventHandler(priority = EventPriority.MONITOR)
	public void onItemDespawn(ItemDespawnEvent event) {
		PrisonPearl pp = pearls
				.getByItemStack(event.getEntity().getItemStack());
		if (pp == null)
			return;

		Player player = Bukkit.getPlayer(pp.getImprisonedId());
		freePearl(pp, pp.getImprisonedName() + "("+pp.getImprisonedId() + ") is being freed. Reason: PrisonPearl item despawned.");
	}

	private Map<UUID, BukkitTask> unloadedPearls = new HashMap<UUID, BukkitTask>();
	// Free the pearl if its on a chunk that unloads
	@EventHandler(priority = EventPriority.MONITOR)
	public void onChunkUnload(ChunkUnloadEvent event) {		
		for (Entity e : event.getChunk().getEntities()) {
			if (!(e instanceof Item))
				continue;

			final PrisonPearl pp = pearls.getByItemStack(((Item) e).getItemStack());
			
			if (pp == null)
				continue;
			
			final Player player = Bukkit.getPlayer(pp.getImprisonedId());
			final Entity entity = e;
			final World world = entity.getWorld();
			final int chunkX = event.getChunk().getX();
			final int chunkZ = event.getChunk().getZ();
			// doing this in onChunkUnload causes weird things to happen

			event.setCancelled(true);
			final UUID uuid = pp.getImprisonedId();
			if (unloadedPearls.containsKey(uuid))
				return;
			BukkitTask count = Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
				public void run(){
					// Check that chunk is still unloaded before freeing
					if (!world.isChunkLoaded(chunkX, chunkZ)
						&& freePearl(pp, pp.getImprisonedName() + "("+
								pp.getImprisonedId() + ") is being freed. Reason: Chunk with PrisonPearl unloaded."))
					{
						entity.remove();
					}

					unloadedPearls.remove(uuid);
				}
			}, plugin.getPPConfig().getChunkUnloadDelay());
			unloadedPearls.put(uuid, count);
		}
	}
	
	// Prevent dropped Prison Pearl's from being despawned.
	// TODO: PrisonPearl items specifically aren't being picked up from chunk.getEntities()
	/*@EventHandler(priority = EventPriority.MONITOR)
	public void onChunkLoad(ChunkLoadEvent event) {
		//	Don't need to check for unloaded pearls if there are none.
		if (unloadedPearls.isEmpty() || event.isNewChunk()) return;
		
		// Search for currently unloaded Prison Pearls.
		for (Entity entity : event.getChunk().getEntities()) {
			if (entity instanceof Item) {
				Item item = (Item) entity;
				PrisonPearl prisonPearl = pearls.getByItemStack(item.getItemStack());
				
				if (prisonPearl != null) {
					UUID imprisonedUuid = prisonPearl.getImprisonedId();
					
					if (unloadedPearls.containsKey(imprisonedUuid)) {
						unloadedPearls.get(imprisonedUuid).cancel();
						unloadedPearls.remove(imprisonedUuid);
					}
				}
			}
		}
	}*/
	
	// Free the pearl if it combusts in lava/fire
	@EventHandler(priority = EventPriority.MONITOR)
	public void onEntityCombustEvent(EntityCombustEvent event) {
		if (!(event.getEntity() instanceof Item))
			return;

		PrisonPearl pp = pearls.getByItemStack(
			((Item) event.getEntity()).getItemStack());
		if (pp == null)
			return;

		Player player = Bukkit.getPlayer(pp.getImprisonedId());
		String reason = pp.getImprisonedName() + "("+pp.getImprisonedId() + ") is being freed. Reason: PrisonPearl combusted(lava/fire).";
		freePearl(pp, reason);
	}
	
	// Handle inventory dragging properly.
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onInventoryDrag(InventoryDragEvent event) {
		if(event.isCancelled())
			return;
		
		Map<Integer, ItemStack> items = event.getNewItems();

		for(Integer slot : items.keySet()) {
			ItemStack item = items.get(slot);
			
			PrisonPearl pearl = pearls.getByItemStack(item);
			if(pearl != null) {
				boolean clickedTop = event.getView().convertSlot(slot) == slot;
				
				InventoryHolder holder = clickedTop ? event.getView().getTopInventory().getHolder() : event.getView().getBottomInventory().getHolder();
				
				pearl.markMove();
				updatePearlHolder(pearl, holder, event);
				
				if(event.isCancelled()) {
					return;
				}
			}
		}
	}

	
	// Prevent imprisoned players from placing PrisonPearls in their inventory.
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
	public void onPrisonPearlClick(InventoryClickEvent event) {
		Player clicker = (Player) event.getWhoClicked();
		
		if (pearls.isPrisonPearl(event.getCurrentItem()) 
			&& pearls.isImprisoned(clicker)) {
			clicker.sendMessage(ChatColor.RED + "Imprisoned players cannot pick up prison pearls!");
			event.setCancelled(true);	// Prevent imprisoned player from grabbing PrisonPearls.
		}
	}
	
	// Track the location of a pearl
	// Forbid pearls from being put in storage minecarts
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onInventoryClick(InventoryClickEvent event) {
		if(event.isCancelled())
			return;
		
		// announce an prisonpearl if it is clicked
		ItemStack newitem = announcePearl(
			(Player) event.getWhoClicked(), event.getCurrentItem());
		if (newitem != null)
			event.setCurrentItem(newitem);
		
		if(event.getAction() == InventoryAction.COLLECT_TO_CURSOR
				|| event.getAction() == InventoryAction.PICKUP_ALL
				|| event.getAction() == InventoryAction.PICKUP_HALF
				|| event.getAction() == InventoryAction.PICKUP_ONE) {
			PrisonPearl pearl = pearls.getByItemStack(event.getCurrentItem());
			
			if(pearl != null) {
				pearl.markMove();
				updatePearl(pearl, (Player) event.getWhoClicked(), true);
			}
		}
		else if(event.getAction() == InventoryAction.PLACE_ALL
				|| event.getAction() == InventoryAction.PLACE_SOME
				|| event.getAction() == InventoryAction.PLACE_ONE) {	
			PrisonPearl pearl = pearls.getByItemStack(event.getCursor());
			
			if(pearl != null) {
				boolean clickedTop = event.getView().convertSlot(event.getRawSlot()) == event.getRawSlot();
				
				InventoryHolder holder = clickedTop ? event.getView().getTopInventory().getHolder() : event.getView().getBottomInventory().getHolder();
				if (holder==null){
					Player player=pearl.getHolderPlayer();
					pearl.markMove();
					// Ender Expansion isn't currently being used anyways.  Not sure if this method will work.
					ee.updateEnderStoragePrison(pearl, event, player.getTargetBlock(new HashSet<Material>(), 5).getLocation());
				}
				else{
				pearl.markMove();
				updatePearlHolder(pearl, holder, event);
				}
				if (!(holder instanceof Player)&&plugin.getWBManager().isMaxFeed(pearl.getLocation())){
					HumanEntity human = event.getWhoClicked();
					if (human instanceof Player){
						Player p = (Player) human;
						p.sendMessage(ChatColor.GREEN + "The Pearl of " + pearl.getImprisonedName() + " was placed outside of the feed "
								+ "range, if you leave him here the pearl will be freed restart.");
					}
				}
			}
		}
		else if(event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {			
			PrisonPearl pearl = pearls.getByItemStack(event.getCurrentItem());
			
			if(pearl != null) {
				boolean clickedTop = event.getView().convertSlot(event.getRawSlot()) == event.getRawSlot();
				
				InventoryHolder holder = !clickedTop ? event.getView().getTopInventory().getHolder() : event.getView().getBottomInventory().getHolder();
				if (holder==null){
					Player player=pearl.getHolderPlayer();
					pearl.markMove();
					ee.updateEnderStoragePrison(pearl, event, player.getTargetBlock(new HashSet<Material>(), 5).getLocation());
				}
				else if(holder.getInventory().firstEmpty() >= 0) {
					pearl.markMove();
					updatePearlHolder(pearl, holder, event);
				}
				if (!(holder instanceof Player)&&plugin.getWBManager().isMaxFeed(pearl.getLocation())){
					HumanEntity human = event.getWhoClicked();
					if (human instanceof Player){
						Player p = (Player) human;
						p.sendMessage(ChatColor.GREEN + "The Pearl of " + pearl.getImprisonedName() + " was placed outside of the feed "
								+ "range, if you leave him here the pearl will be freed restart.");
					}
				}
			}
		}
		else if(event.getAction() == InventoryAction.HOTBAR_SWAP) {
			PlayerInventory playerInventory = event.getWhoClicked().getInventory();
			PrisonPearl pearl = pearls.getByItemStack(playerInventory.getItem(event.getHotbarButton()));
			
			if(pearl != null) {
				boolean clickedTop = event.getView().convertSlot(event.getRawSlot()) == event.getRawSlot();
				
				InventoryHolder holder = clickedTop ? event.getView().getTopInventory().getHolder() : event.getView().getBottomInventory().getHolder();
				
				pearl.markMove();
				updatePearlHolder(pearl, holder, event);
			}
			
			if(event.isCancelled())
				return;
			
			pearl = pearls.getByItemStack(event.getCurrentItem());
			
			if(pearl != null) {
				pearl.markMove();
				updatePearl(pearl, (Player) event.getWhoClicked());
			}
		}
		else if (event.getAction() == InventoryAction.SWAP_WITH_CURSOR) {
			PrisonPearl pearl = pearls.getByItemStack(event.getCursor());
			
			if(pearl != null) {
				boolean clickedTop = event.getView().convertSlot(event.getRawSlot()) == event.getRawSlot();
				
				InventoryHolder holder = clickedTop ? event.getView().getTopInventory().getHolder() : event.getView().getBottomInventory().getHolder();
				
				pearl.markMove();
				updatePearlHolder(pearl, holder, event);
			}
			
			if(event.isCancelled())
				return;
			
			pearl = pearls.getByItemStack(event.getCurrentItem());
			
			if(pearl != null) {
				pearl.markMove();
				updatePearl(pearl, (Player) event.getWhoClicked(), true);
			}
		}
		else if(event.getAction() == InventoryAction.DROP_ALL_CURSOR
				|| event.getAction() == InventoryAction.DROP_ALL_SLOT
				|| event.getAction() == InventoryAction.DROP_ONE_CURSOR
				|| event.getAction() == InventoryAction.DROP_ONE_SLOT) {
			// Handled by onItemSpawn
		}
		else {
			if(pearls.getByItemStack(event.getCurrentItem()) != null || pearls.getByItemStack(event.getCursor()) != null) {
				((Player) event.getWhoClicked()).sendMessage(ChatColor.RED + "Error: PrisonPearl doesn't support this inventory functionality quite yet!");
				
				event.setCancelled(true);
			}
		}
	}
	
	private void updatePearlHolder(PrisonPearl pearl, InventoryHolder holder, Cancellable event) {
		
		if (holder instanceof Chest) {
			updatePearl(pearl, (Chest) holder);
		} else if (holder instanceof DoubleChest) {
			updatePearl(pearl, (Chest) ((DoubleChest) holder).getLeftSide());
		} else if (holder instanceof Furnace) {
			updatePearl(pearl, (Furnace) holder);
		} else if (holder instanceof Dispenser) {
			updatePearl(pearl, (Dispenser) holder);
		} else if (holder instanceof BrewingStand) {
			updatePearl(pearl, (BrewingStand) holder);
		} else if (holder instanceof Player) {
			updatePearl(pearl, (Player) holder);
		}else {
			event.setCancelled(true);
		}
	}
	
	// Track the location of a pearl if it spawns as an item for any reason
	@EventHandler(priority = EventPriority.MONITOR)
	public void onItemSpawn(ItemSpawnEvent event) {
		Item item = event.getEntity();
		PrisonPearl pp = pearls.getByItemStack(item.getItemStack());
		if (pp == null)
			return;
		pp.markMove();
		updatePearl(pp, item);
	}
	

	// Track the location of a pearl if a player picks it up
	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onPlayerPickupItem(PlayerPickupItemEvent event) {
		PrisonPearl pp = pearls.getByItemStack(event.getItem().getItemStack());
		if (pp == null)
			return;
		
		pp.markMove();
		updatePearl(pp, event.getPlayer());
		
		// If pearl was in unloaded chunk
		if (unloadedPearls.isEmpty())
			return;
		UUID want = pp.getImprisonedId();
		for (UUID uuid: unloadedPearls.keySet()){
			if (want.equals(uuid)){
				unloadedPearls.get(uuid).cancel();
				unloadedPearls.remove(uuid);
			}
		}
	}
	
	
	// Prevent imprisoned players from picking up PrisonPearls.
	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerPickupPearl(PlayerPickupItemEvent event) {
		if (pearls.isPrisonPearl(event.getItem().getItemStack()) 
			&& pearls.isImprisoned(event.getPlayer())) {
			event.setCancelled(true);
		}
	}
	
	
	// Deny pearls traveling to other worlds.
	@EventHandler(priority = EventPriority.HIGHEST)
	public void worldChangeEvent(PlayerTeleportEvent event){
		TeleportCause cause = event.getCause();
		if (!cause.equals(TeleportCause.PLUGIN))
			return;
		List<String> denyWorlds = getConfig().getStringList("deny_pearls_worlds");
		Location previous = event.getFrom();
		Location to = event.getTo();
		World newWorld = to.getWorld();
		if (!denyWorlds.contains(newWorld.getName()))
			return;
      
                World oldWorld = previous.getWorld();
                //not changing worlds, no reason to free pearls
                if(newWorld.getName().equals(oldWorld.getName()))
                        return;
		
		Inventory inv = event.getPlayer().getInventory();
		boolean message = false;
		for (Entry<Integer, ? extends ItemStack> entry :
			inv.all(Material.ENDER_PEARL).entrySet()) {
			ItemStack item = entry.getValue();
			PrisonPearl pp = pearls.getByItemStack(item);
			if (pp == null) {
				continue;
			}
			pp.markMove();
			int slot = entry.getKey();
			inv.clear(slot);
			previous.getWorld().dropItemNaturally(previous, item);
			message = true;
		}
		if (message)
			event.getPlayer().sendMessage(ChatColor.RED + "This world is not allowed " +
				"to have Prison Pearls in it. Your prisoner was dropped where " +
				"you were in the previous world.");
	}

	private void updatePearl(PrisonPearl pp, Item item) {
		pp.setHolder(item);
		pearls.markDirty();
		Bukkit.getPluginManager().callEvent(
				new PrisonPearlEvent(pp, PrisonPearlEvent.Type.DROPPED));
		updateLocationToMercury(pp, PrisonPearlEvent.Type.DROPPED);
	}

	private void updatePearl(PrisonPearl pp, Player player) {
	    updatePearl(pp, player, false);
	}

	private void updatePearl(PrisonPearl pp, Player player, boolean isOnCursor) {
		if (isOnCursor) {
			pp.setCursorHolder(player);
		} else {
			pp.setHolder(player);
		}
		pearls.markDirty();
		Bukkit.getPluginManager().callEvent(
				new PrisonPearlEvent(pp, PrisonPearlEvent.Type.HELD));
		updateLocationToMercury(pp, PrisonPearlEvent.Type.HELD);
	}

	private <ItemBlock extends InventoryHolder & BlockState> void updatePearl(
			PrisonPearl pp, ItemBlock block) {
		pp.setHolder(block);
		pearls.markDirty();
		Bukkit.getPluginManager().callEvent(
				new PrisonPearlEvent(pp, PrisonPearlEvent.Type.HELD));
		updateLocationToMercury(pp, PrisonPearlEvent.Type.HELD);
	}

	private boolean prisonPearlEvent(PrisonPearl pp, PrisonPearlEvent.Type type) {
		return prisonPearlEvent(pp, type, null);
	}

	private boolean prisonPearlEvent(PrisonPearl pp,
			PrisonPearlEvent.Type type, Player imprisoner) {
		PrisonPearlEvent event = new PrisonPearlEvent(pp, type, imprisoner);
		Bukkit.getPluginManager().callEvent(event);
		return !event.isCancelled();
	}
	public boolean prisonCommandEvent(String command){
		PrisonCommandEvent event= new PrisonCommandEvent(command);
		Bukkit.getPluginManager().callEvent(event);
		return !event.isCancelled();
	}
	private Configuration getConfig() {
		return plugin.getConfig();
	}
	
	private void updateLocationToMercury(PrisonPearl pp, PrisonPearlEvent.Type type){
		if (isMercury){
			String message = "";
			Location loc = pp.getLocation();
			message = type.name() + " " + pp.getImprisonedId().toString() + " " + pp.getImprisonedName() + " " +
			loc.getWorld().getName() + " " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ();
			message += " " + pp.getUniqueIdentifier();
			
			Player p;
			if ((p = pp.getHolderPlayer()) != null){
				message += " " + p.getName();
			}
			MercuryAPI.sendGlobalMessage(message, "PrisonPearlUpdate");
		}
	}

	public boolean isItemStackPrisonPearl(PrisonPearl pp, ItemStack stack) {
		if (!stack.hasItemMeta())
			return false;
		if (!stack.getItemMeta().hasLore())
			return false;
		List<String> lore = stack.getItemMeta().getLore();
		if (lore.size() != 3)
			return false;
		String uuid = lore.get(1).split(" ")[1];
		int unique = Integer.parseInt(lore.get(2).split(" ")[1]);
		return uuid.equals(pp.getImprisonedId().toString()) && unique == pp.getUniqueIdentifier();
	}
	
	private void updateTransferToMercury(UUID imprisoner, UUID pearled) {
		String message = "";
		message += imprisoner.toString() + " " + pearled.toString();
		MercuryAPI.sendGlobalMessage(message, "PrisonPearlTransfer");
	}
}
