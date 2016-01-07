package com.untamedears.PrisonPearl.managers;

import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import com.untamedears.PrisonPearl.PrisonPearlPlugin;

/**
 * Extracting common functions as abstract class to allow switching out ban managers
 * 
 * @author ProgrammerDan <programmerdan@gmail.com>
 *
 */
public abstract class BanManager implements Listener {

	public abstract boolean initialize();
	public abstract void setBanMessage(String msg);
	public abstract String getBanMessage();
	public abstract boolean isBanned(UUID uuidName);
	public abstract Set<UUID> listBannedUUIDS();
	public abstract void ban(UUID UUIDName);
	public abstract void pardon(UUID UUIDName);
	public abstract void setBanState(UUID UUIDName, boolean isBanned);
	public abstract boolean hasBannedPlayers();
	
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerLogin(AsyncPlayerPreLoginEvent event) {
		StringBuilder sb = new StringBuilder();
		final UUID uuidName = event.getUniqueId();
		sb.append("Alt-Ban-Info: ");
		sb.append("UUID: " + uuidName.toString());
		sb.append(" EventLoginResult: " + event.getLoginResult().toString());
		if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
			return;
		}
		if (!isBanned(uuidName)) {
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
		event.disallow(
			AsyncPlayerPreLoginEvent.Result.KICK_BANNED, getBanMessage());
	}
	
}
