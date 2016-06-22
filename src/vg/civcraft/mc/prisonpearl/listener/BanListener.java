package vg.civcraft.mc.prisonpearl.listener;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import vg.civcraft.mc.prisonpearl.PrisonPearlPlugin;
import vg.civcraft.mc.prisonpearl.managers.AltsListManager;
import vg.civcraft.mc.prisonpearl.managers.BanManager;

public class BanListener implements Listener{
	
	private BanManager ban;
	private AltsListManager alts;
	
	public BanListener() {
		Bukkit.getPluginManager().registerEvents(this, PrisonPearlPlugin.getInstance());
		ban = PrisonPearlPlugin.getBanManager();
		alts = PrisonPearlPlugin.getAltsListManager();
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerLogin(AsyncPlayerPreLoginEvent event) {
		if (!PrisonPearlConfig.getShouldEnableAltsManager())
			return;
		StringBuilder sb = new StringBuilder();
		final UUID uuidName = event.getUniqueId();
		sb.append("Alt-Ban-Info: ");
		sb.append("UUID: " + uuidName.toString());
		sb.append(" EventLoginResult: " + event.getLoginResult().toString());
		if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
			return;
		}
		
		if (!ban.isBanned(uuidName)) {
			sb.append(" Banned UUID: " + false);
			PrisonPearlPlugin.log(sb.toString());
			return;
		}
		final OfflinePlayer offline = Bukkit.getOfflinePlayer(uuidName);
		if (offline != null) {
			if (offline.isBanned() || offline.isOp()) {
				sb.append(" Offline Banned: " + offline.isBanned());
				PrisonPearlPlugin.log(sb.toString());
				return;
			}
		}
		sb.append(" UUID KICK_BANNED");
		PrisonPearlPlugin.log(sb.toString());
		if (!PrisonPearlPlugin.isCBanManagementEnabled())
			event.disallow(
					AsyncPlayerPreLoginEvent.Result.KICK_BANNED, ban.getBanMessage());
	}
}
