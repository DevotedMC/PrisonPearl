package com.untamedears.PrisonPearl;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import com.untamedears.PrisonPearl.events.PrisonPearlEvent;
import com.untamedears.PrisonPearl.managers.PrisonPearlManager;

import vg.civcraft.mc.mercury.MercuryAPI;
import vg.civcraft.mc.mercury.events.AsyncPluginBroadcastMessageEvent;

public class MercuryListener implements Listener{
	
	public static String[] channels = {
		"PrisonPearlUpdate",
		"PrisonPearlTransfer",
		"PrisonPearlMove"
	};
	
	private final PrisonPearlPlugin plugin;
	private final PrisonPearlStorage pearls;
	private final PrisonPearlManager manager;
	
	public MercuryListener(PrisonPearlPlugin plugin, PrisonPearlStorage storage){
		this.plugin = plugin;
		manager = PrisonPearlPlugin.getPrisonPearlManager();
		pearls = storage;
	}

	@EventHandler()
	public void mercuryListener(AsyncPluginBroadcastMessageEvent event){
		String channel = event.getChannel();
		String message = event.getMessage();
		if (event.getOriginServer().equals(MercuryAPI.serverName()))
			return;
		
		if(channel.equalsIgnoreCase(channels[0]))
			pearlUpdate(message);
		else if(channel.equalsIgnoreCase(channels[1]))
			pearlTransfer(message);
		else if(channel.equalsIgnoreCase(channels[2]))
			PrisonPearlMove(message);
	}
	
	private void pearlTransfer(String message){
		String[] parts = message.split(" ");
		UUID holder = UUID.fromString(parts[0]);
		UUID pearl = UUID.fromString(parts[1]);
		PrisonPearl pp = pearls.getByImprisoned(pearl);
		Player p = Bukkit.getPlayer(holder);
		if (p == null) // Player is not on the server.
			return; // Some other server will pick it up.
		pp.setHolder(p);
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
			pp.setHolder(loc);
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
			manager.freePearlFromMercury(pp, "This pearl was freed on another server. Removing instance.");
		}
	}
	
	private void PrisonPearlMove(String message){
		String[] parts = message.split(" ");
		UUID uuid = UUID.fromString(parts[0]);
		String world = parts[1];
		FakeLocation loc = new FakeLocation(world, Double.parseDouble(parts[2]), Double.parseDouble(parts[3]),
				Double.parseDouble(parts[4]));
		PrisonPearl pp = pearls.getByImprisoned(uuid);
		pp.setHolder(loc);
	}
}
