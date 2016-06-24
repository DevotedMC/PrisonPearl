package vg.civcraft.mc.prisonpearl.managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import vg.civcraft.mc.prisonpearl.PrisonPearl;
import vg.civcraft.mc.prisonpearl.PrisonPearlPlugin;

public class BroadcastManager {
	private final Map<UUID, List<UUID>> broadcasts; // pearled, List of receivers.
	private final Map<UUID, UUID> request; // receiver, pearled.
	
	public BroadcastManager() {
		broadcasts = new HashMap<UUID, List<UUID>>();
		request = new HashMap<UUID, UUID>();
	}
	
	/** For this method make sure you later call Mercury to alert other servers about the broadcast.
	 * This method also removes the receiever from the request list.
	 */
	public boolean addBroadcast(UUID player, UUID receiver) {
		if (!broadcasts.containsKey(player))
			broadcasts.put(player, new ArrayList<UUID>());
		request.remove(receiver);
		return broadcasts.get(player).add(receiver);
	}
	
	/**
	 * This method sends a message to the receiver asking them if they want to receiver pplocates.  You do not need to alert mercury 
	 * as this method does it for you.
	 * @param pearled
	 * @param receiver
	 * @return
	 */
	public boolean requestBroadcast(UUID pearled, UUID receiver) {
		Player p = Bukkit.getPlayer(receiver);
		if (PrisonPearlPlugin.isMercuryEnabled()) {
			// Sharding code. Lel flying monkeys everywhere.
			if (!MercuryManager.isPlayerOnline(receiver))
				return false;
			if (p == null) {
				MercuryManager.requestBroadcast(pearled, receiver);
			}
			else 
				p.sendMessage(ChatColor.GREEN + String.format("You have been sent a request to allow listens to pplocate by %1$s,"
						+ " to accept type /ppconfirm %1$s", NameLayerManager.getName(pearled)));
			request.put(receiver, pearled);
			return true;
		}
		if (p == null)
			return false;
		p.sendMessage(ChatColor.GREEN + String.format("You have been sent a request to allow listens to pplocate by %1$s,"
				+ " to accept type /ppconfirm %1$s", NameLayerManager.getName(pearled)));
		request.put(receiver, pearled);
		return true;
	}
	
	/**
	 * This method is used by a mercury message to alert a player that they are being sent a boradcast request.
	 * @param pearled
	 * @param receiever
	 */
	public void requestBroadcastFromMercury(UUID pearled, UUID receiver) {
		Player p = Bukkit.getPlayer(receiver);
		if (p == null)
			return;
		p.sendMessage(ChatColor.GREEN + String.format("You have been sent a request to allow listens to pplocate by %1$s,"
				+ " to accept type /ppconfirm %1$s", NameLayerManager.getName(pearled)));
		request.put(receiver, pearled);
	}
	
	/**
	 * Removes all players who are receiving a broadcast.
	 * @param player
	 * @return
	 */
	public boolean removeBroadcasts(UUID pearled, UUID receiver) {
		return broadcasts.get(pearled).remove(receiver);
	}
	
	public boolean removeAllBroadcasts(UUID pearled) {
		return broadcasts.remove(pearled) != null;
	}
	
	/**
	 * Will send a broadcast to everyone on the server and if mercury is on the server
	 * it will send to other servers as well.
	 * @param pearled
	 */
	public void broadcast(UUID pearled) {
		List<UUID> uuids = broadcasts.get(pearled);
		if (uuids == null)
			return;
		MercuryManager.sendBroadcast(pearled, uuids);
		PrisonPearl pp = PrisonPearlPlugin.getPrisonPearlManager().getByImprisoned(pearled);
		for (UUID uuid: uuids) {
			Player p;
			if ((p = Bukkit.getPlayer(uuid)) != null)
				broadcastMessage(p, pp);
		}
	}
	
	/**
	 * This method will send a broadcast message only to the Player specified.
	 * @param receiver
	 * @param pp
	 */
	public void broadcastMessage(Player receiver, PrisonPearl pp) {
		receiver.sendMessage(ChatColor.GREEN + pp.getImprisonedName() + " is " + pp.describeLocation() + ".");
	}
	
	public boolean isRequestedPlayer(UUID receiver, UUID pearled) {
		if (request.get(receiver) == null)
			return false;
		return request.get(receiver).equals(pearled);
	}
	
	public UUID getRequestedPlayer(UUID receiver) {
		return request.get(receiver);
	}
	
	public List<UUID> getAllBroadcasters(UUID pearled) {
		return broadcasts.get(pearled);
	}
}
