package vg.civcraft.mc.prisonpearl.listener;

import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import vg.civcraft.mc.prisonpearl.PrisonPearlConfig;
import vg.civcraft.mc.prisonpearl.PrisonPearlPlugin;
import vg.civcraft.mc.prisonpearl.events.AltsListEvent;
import vg.civcraft.mc.prisonpearl.managers.AltsListManager;
import vg.civcraft.mc.prisonpearl.managers.BanManager;

public class AltsListListener implements Listener {

	private BanManager ban;
	private AltsListManager altsManager;

	public AltsListListener() {
		// Don't bother registering, if not enabled.
		if (!PrisonPearlConfig.getShouldEnableAltsManager()) {
			Bukkit.getPluginManager().registerEvents(this, PrisonPearlPlugin.getInstance());
		}
		ban = PrisonPearlPlugin.getBanManager();
		altsManager = PrisonPearlPlugin.getAltsListManager();
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onAltsListUpdate(final AltsListEvent event) {
		if (!PrisonPearlConfig.getShouldEnableAltsManager())
			return;
		Bukkit.getScheduler().runTaskAsynchronously(PrisonPearlPlugin.getInstance(), new Runnable() {

			@Override
			public void run() {
				runAltlistEvent(event);
			}
			
		});
			
	}

	private void runAltlistEvent(AltsListEvent event) {
		PrisonPearlPlugin.log("Grabbing alts for players");
		final List<UUID> altsList = event.getAltsList();
		// Just add the altslist to the altsManager
		for (UUID playerUUID : altsList) {
			altsManager.addAltsHash(playerUUID, altsList);
		}
		
		// Run through all the accounts and make check if they should be banned or unbanned.
		int bannedCount = 0, unbannedCount = 0, total = 0, result;
		for (UUID altUUID : altsList) {
			result = ban.checkBan(altUUID);
			if (result == 2)
				bannedCount++;
			else if (result == 1)
				unbannedCount++;
			total++;
		}
		PrisonPearlPlugin.log(bannedCount + " players were banned, " + unbannedCount + " were unbanned out of " + total
				+ " accounts.");
	}
}