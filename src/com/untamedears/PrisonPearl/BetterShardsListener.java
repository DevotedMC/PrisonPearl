package com.untamedears.PrisonPearl;

import java.util.UUID;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import vg.civcraft.mc.bettershards.events.PlayerChangeServerEvent;

public class BetterShardsListener implements Listener{

	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled = true)
	public void portalTransferEvent(PlayerChangeServerEvent event) {
		UUID uuid = event.getPlayerUUID();
		PrisonPearlStorage.playerIsTransfering(uuid);
	}
}
