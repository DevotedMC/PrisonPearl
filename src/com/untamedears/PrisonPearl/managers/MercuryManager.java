package com.untamedears.PrisonPearl.managers;

import java.util.List;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import vg.civcraft.mc.mercury.MercuryAPI;

import com.untamedears.PrisonPearl.FakeLocation;
import com.untamedears.PrisonPearl.PrisonPearl;
import com.untamedears.PrisonPearl.PrisonPearlPlugin;
import com.untamedears.PrisonPearl.events.PrisonPearlEvent;

public class MercuryManager {

	private static PrisonPearlPlugin plugin;
	
	public MercuryManager(PrisonPearlPlugin p) {
		plugin = p;
	}
	
	public static void updateAllPearlLocations(){
    	List<UUID> uuids = plugin.getPrisonPearlStorage().getAllUUIDSforPearls();
    	for (UUID uuid: uuids){
    		PrisonPearl pp = plugin.getPrisonPearlStorage().getByImprisoned(uuid);
    		Location loc = pp.getLocation();
    		
    		if (loc instanceof FakeLocation)
    			continue; // If it isn't your pearl don't worry about it.  The server that has it will send the messages.
    		if (pp.getHolderPlayer() == null) // If they aren't being held by a player then no need to update the location.
    			continue;
    		String message = uuid.toString() + "|" + loc.getWorld().getName() + "|" + loc.getBlockX() + "|" + loc.getBlockY() + "|" + loc.getBlockZ() + "|" +
    				pp.getUniqueIdentifier() + "|" + pp.getMotd();
    		MercuryAPI.sendGlobalMessage(message, "PrisonPearlMove");
    	}
    }
	
	public static void updateTransferToMercury(UUID imprisoner, UUID pearled) {
		String message = "";
		message += imprisoner.toString() + "|" + pearled.toString();
		MercuryAPI.sendGlobalMessage(message, "PrisonPearlTransfer");
	}
	
	public static void updateLocationToMercury(PrisonPearl pp, PrisonPearlEvent.Type type){
		if (plugin.isMercuryLoaded()){
			String message = "";
			Location loc = pp.getLocation();
			message = type.name() + "|" + pp.getImprisonedId().toString() + "|" + pp.getImprisonedName() + "|" +
			loc.getWorld().getName() + "|" + loc.getBlockX() + "|" + loc.getBlockY() + "|" + loc.getBlockZ();
			message += "|" + pp.getUniqueIdentifier();
			
			if (type.equals(PrisonPearlEvent.Type.FREED) && plugin.getConfig().getBoolean("free_tppearl"))
				message += "|" + MercuryAPI.serverName();
			MercuryAPI.sendGlobalMessage(message, "PrisonPearlUpdate");
		}
	}
	
	public static void requestPPSummon(UUID uuid) {
		String message = "request|" + uuid.toString() + "|" + MercuryAPI.serverName();
		MercuryAPI.sendGlobalMessage(message, "PrisonPearlSummon");
	}
	
	public static void denyPPSummon(UUID uuid, String reason) {
		String message = "deny|" + uuid.toString() + "|" + reason;
		MercuryAPI.sendGlobalMessage(message, "PrisonPearlSummon");
	}
	
	public static void returnPPSummon(UUID uuid) {
		String message = "return|" + uuid.toString();
		MercuryAPI.sendGlobalMessage(message, "PrisonPearlSummon");
	}
}
