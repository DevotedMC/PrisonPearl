package com.untamedears.PrisonPearl.managers;

import java.util.Map.Entry;

import vg.civcraft.mc.bettershards.BetterShardsAPI;
import vg.civcraft.mc.bettershards.events.PlayerChangeServerEvent;
import vg.civcraft.mc.bettershards.events.PlayerChangeServerReason;
import vg.civcraft.mc.bettershards.misc.PlayerStillDeadException;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.untamedears.PrisonPearl.EnderExpansion;
import com.untamedears.PrisonPearl.PrisonPearl;
import com.untamedears.PrisonPearl.PrisonPearlPlugin;
import com.untamedears.PrisonPearl.PrisonPearlStorage;

public class BetterShardsPrisonPearlManager implements Listener{
	private final PrisonPearlPlugin plugin;
	private final PrisonPearlStorage pearls;
	private EnderExpansion ee;
	
	public BetterShardsPrisonPearlManager(PrisonPearlPlugin plugin, PrisonPearlStorage pearls, EnderExpansion ee) {
		this.plugin = plugin;
		this.pearls = pearls;
		this.ee = ee;
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
			MercuryManager.updateTransferToMercury(event.getPlayerUUID(), pp.getImprisonedId());
		}
		PrisonPearlStorage.playerIsTransfering(event.getPlayerUUID());
	}
	
}
