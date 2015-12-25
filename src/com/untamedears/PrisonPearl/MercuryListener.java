package com.untamedears.PrisonPearl;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import com.untamedears.PrisonPearl.events.PrisonPearlEvent;

import vg.civcraft.mc.mercury.events.AsyncPluginBroadcastMessageEvent;

public class MercuryListener implements Listener{
	
	public static String[] channels = {
		"PrisonPearlUpdate"
	};
	
	private final PrisonPearlPlugin plugin;
	private final PrisonPearlStorage pearls;
	
	public MercuryListener(PrisonPearlPlugin plugin, PrisonPearlStorage storage){
		this.plugin = plugin;
		pearls = storage;
	}

	@EventHandler()
	public void merucyrListener(AsyncPluginBroadcastMessageEvent event){
		String channel = event.getChannel();
		String message = event.getMessage();
		
		if(channel.equals(channels[0]))
			pearlUpdate(message);
	}
	
	private void pearlUpdate(String message){
		String[] parts = message.split(" ");
		PrisonPearlEvent.Type type = PrisonPearlEvent.Type.valueOf(parts[0]);
		UUID id = UUID.fromString(parts[1]);
		String name = parts[2];
		FakeLocation loc = new FakeLocation(parts[3], Double.parseDouble(parts[4]), Double.parseDouble(parts[5]),
				Double.parseDouble(parts[6]));
		String player = null;
		int unique = Integer.parseInt(parts[7]);
		if (parts.length == 9)
			player = parts[8];
		
		if (type.equals(PrisonPearlEvent.Type.NEW)){
			
			PrisonPearl pp = new PrisonPearl(name, id, loc, unique);
			pearls.addPearl(pp);
			return;
		}
		else if (type.equals(PrisonPearlEvent.Type.DROPPED) ||
				type.equals(PrisonPearlEvent.Type.HELD)){
			PrisonPearl pp = pearls.getByImprisoned(id);
			pp.setHolder(loc);
		}
		else if (type.equals(PrisonPearlEvent.Type.FREED)){
			PrisonPearl pp = pearls.getByImprisoned(id);
			pearls.deletePearl(pp, "This pearl was freed on another server. Removing instance.");
		}
	}
}
