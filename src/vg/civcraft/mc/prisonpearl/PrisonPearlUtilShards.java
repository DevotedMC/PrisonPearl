package vg.civcraft.mc.prisonpearl;

import java.util.Random;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import vg.civcraft.mc.bettershards.BetterShardsAPI;
import vg.civcraft.mc.bettershards.events.PlayerChangeServerReason;
import vg.civcraft.mc.bettershards.misc.BedLocation;
import vg.civcraft.mc.bettershards.misc.PlayerStillDeadException;
import vg.civcraft.mc.bettershards.misc.TeleportInfo;
import vg.civcraft.mc.mercury.MercuryAPI;
import vg.civcraft.mc.prisonpearl.managers.PrisonPearlManager;
import vg.civcraft.mc.prisonpearl.managers.SummonManager;
import vg.civcraft.mc.prisonpearl.misc.FakeLocation;
import static vg.civcraft.mc.prisonpearl.PrisonPearlUtil.*;

public class PrisonPearlUtilShards {

	private static PrisonPearlManager manager;
	private static SummonManager summon;
	
	public PrisonPearlUtilShards() {
		manager = PrisonPearlPlugin.getPrisonPearlManager();
		summon = PrisonPearlPlugin.getSummonManager();
	}
	
	public static boolean respawnPlayerCorrectlyShards(Player p, PrisonPearl passPearl) { 
		// We want this method to deal with all cases: Respawn on death, Respawn on summoning, returning,
		// different shards transport, everything. 
		
		UUID uuid = p.getUniqueId();
		boolean freeToPearl = PrisonPearlConfig.shouldTpPearlOnFree();
		PrisonPearl pp = manager.getByImprisoned(uuid);

		if (manager.isImprisoned(uuid)) {
			String server = MercuryAPI.serverName();
			String toServer = manager.getImprisonServer();
			// This check is incase the player is being summoned;
			if (summon.isSummoned(p) && (
					(summon.getSummon(p).isToBeReturned() && (summon.getSummon(p).getReturnLocation() instanceof FakeLocation)) 
					|| summon.getSummon(p).isJustCreated())
					&& toServer.equals(server)) {
				if (pp.getLocation() instanceof FakeLocation)
					toServer = ((FakeLocation) pp.getLocation()).getServerName();
			}
			if (!server.equals(toServer)) {
				try {
					FakeLocation loc = null;
					if (summon.isSummoned(p)) {
						Summon s = summon.getSummon(p);
						if (s.isToBeReturned()) {
							if (PrisonPearlConfig.getShouldPPReturnKill())
								p.setHealth(0);
							loc = (FakeLocation) s.getReturnLocation();
							TeleportInfo info = new TeleportInfo(loc.getWorldName(), loc.getServerName(), loc.getBlockX(), loc.getBlockY() + 1,
									loc.getBlockZ());
							BetterShardsAPI.teleportPlayer(info.getServer(), p.getUniqueId(), info);
							return BetterShardsAPI.connectPlayer(p, toServer, PlayerChangeServerReason.PLUGIN);
						}
						else if (s.isJustCreated()) {
							if (PrisonPearlConfig.shouldPpsummonClearInventory()) {
								dropInventory(p, p.getLocation(), PrisonPearlConfig.shouldPpsummonLeavePearls());
							}
							loc = (FakeLocation) s.getPearlLocation();
							TeleportInfo info = new TeleportInfo(loc.getWorldName(), loc.getServerName(), loc.getBlockX(), loc.getBlockY() + 1,
									loc.getBlockZ());
							BetterShardsAPI.teleportPlayer(info.getServer(), p.getUniqueId(), info);
							return BetterShardsAPI.connectPlayer(p, toServer, PlayerChangeServerReason.PLUGIN);
						}
					}
					
					BetterShardsAPI.randomSpawnPlayer(toServer, p.getUniqueId());
					return BetterShardsAPI.connectPlayer(p, toServer, PlayerChangeServerReason.PLUGIN);
				} catch (PlayerStillDeadException e) {
					e.printStackTrace();
				}
			}
			else { // For if pearl is on the same server.
				if (summon.isSummoned(p)) {
					Summon s = summon.getSummon(p);
					if (s.isToBeReturned()) {
						if (PrisonPearlConfig.getShouldPPReturnKill())
							p.setHealth(0);
						Location newLoc = s.getReturnLocation();
						newLoc.setY(newLoc.getY() + 1);
						p.teleport(newLoc);
					}
					else if (s.isJustCreated()) {
						if (PrisonPearlConfig.shouldPpsummonClearInventory()) {
							dropInventory(p, p.getLocation(), PrisonPearlConfig.shouldPpsummonLeavePearls());
						}
						p.teleport(s.getPearlLocation());
					}
				}
				else if (!p.getWorld().equals(manager.getPrisonSpawnLocation().getWorld()))
					p.teleport(manager.getPrisonSpawnLocation());
				return true;
			}
		}
		try {
			if (freeToPearl && passPearl != null && passPearl.getLocation() instanceof FakeLocation) { // Player was just freed and should be returned to his holder.
				FakeLocation loc = (FakeLocation) passPearl.getLocation();
				TeleportInfo info = new TeleportInfo(loc.getWorldName(), loc.getServerName(), loc.getBlockX(), loc.getBlockY(), 
						loc.getBlockZ());
				BetterShardsAPI.teleportPlayer(loc.getServerName(), uuid, info);
				return BetterShardsAPI.connectPlayer(p, loc.getServerName(), PlayerChangeServerReason.PLUGIN);
			}
			else if (passPearl != null){ // Some other cases.
				if (BetterShardsAPI.hasBed(uuid)) { // Like if a player has a bed ;)
					BedLocation bed = BetterShardsAPI.getBedLocation(uuid);
					BetterShardsAPI.teleportPlayer(bed.getServer(), uuid, bed.getTeleportInfo());
					return BetterShardsAPI.connectPlayer(p, bed.getServer(), PlayerChangeServerReason.PLUGIN);
				}
				// Randomly respawn the player on the server where the pearl was located.
				Random r = new Random();
				int num = r.nextInt(Math.max(1, MercuryAPI.getAllConnectedServers().size()));
				BetterShardsAPI.randomSpawnPlayer((String) MercuryAPI.getAllConnectedServers().toArray()[num], uuid);
				return true;
			}
		} catch (PlayerStillDeadException e) {
			e.printStackTrace();
		}
		return false;
	}	
}
